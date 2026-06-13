package cn.guangdian.world.command;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GWorldCommand implements CommandExecutor, TabCompleter {

    private final GuangDianWorld plugin;

    public GWorldCommand(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // list 和 info 不需要管理员权限，其他命令需要
        boolean needsAdmin = !subCommand.equals("list") && !subCommand.equals("info");
        if (needsAdmin && !sender.hasPermission("guangdian.world.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令!", NamedTextColor.RED));
            return true;
        }

        return switch (subCommand) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "load" -> handleLoad(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "tp" -> handleTp(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 世界列表 ==========", NamedTextColor.GOLD));
        for (GDWorld world : plugin.getWorldManager().getAllWorlds()) {
            Component status = world.isLoaded()
                ? Component.text("[已加载]", NamedTextColor.GREEN)
                : Component.text("[未加载]", NamedTextColor.RED);

            Component alias = (world.getAlias() != null && !world.getAlias().isEmpty())
                ? Component.text(" (" + world.getAlias() + ")", NamedTextColor.GRAY)
                : Component.empty();

            Component line = Component.text("  - ", NamedTextColor.WHITE)
                .append(Component.text(world.getName(), NamedTextColor.WHITE))
                .append(alias)
                .append(Component.text(" "))
                .append(status);

            sender.sendMessage(line);
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld info <世界名>", NamedTextColor.RED));
            return true;
        }

        GDWorld world = plugin.getWorldManager().getWorld(args[1]);
        if (world == null) {
            sender.sendMessage(Component.text("世界不存在: " + args[1], NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("========== 世界信息 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("名称: ").append(Component.text(world.getName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("别名: ").append(Component.text(
            world.getAlias() != null ? world.getAlias() : "无", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("环境: ").append(Component.text(world.getEnvironment().name(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("难度: ").append(Component.text(world.getDifficulty(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("游戏模式: ").append(Component.text(world.getGamemode(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("PVP: ").append(Component.text(world.isPvp() ? "开启" : "关闭", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("飞行: ").append(Component.text(world.isAllowFlight() ? "允许" : "禁止", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("生物生成: ").append(Component.text(world.isDoMobSpawning() ? "开启" : "关闭", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("死亡不掉落: ").append(Component.text(world.isKeepInventory() ? "开启" : "关闭", NamedTextColor.WHITE)));

        Component status = world.isLoaded()
            ? Component.text("已加载", NamedTextColor.GREEN)
            : Component.text("未加载", NamedTextColor.RED);
        sender.sendMessage(Component.text("状态: ").append(status));

        if (world.getSpawnLocation() != null) {
            var loc = world.getSpawnLocation();
            sender.sendMessage(Component.text(String.format("出生点: %.1f, %.1f, %.1f",
                loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.WHITE));
        }
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld create <世界名> [normal|nether|end]", NamedTextColor.RED));
            return true;
        }

        String worldName = args[1];
        World.Environment env = World.Environment.NORMAL;

        if (args.length >= 3) {
            env = switch (args[2].toLowerCase()) {
                case "nether" -> World.Environment.NETHER;
                case "end" -> World.Environment.THE_END;
                default -> World.Environment.NORMAL;
            };
        }

        if (plugin.getWorldManager().getWorld(worldName) != null) {
            sender.sendMessage(Component.text("世界已存在: " + worldName, NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("正在创建世界: " + worldName + "...", NamedTextColor.YELLOW));

        GDWorld world = plugin.getWorldManager().createWorld(worldName, env);
        if (world != null) {
            sender.sendMessage(Component.text("世界创建成功: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("世界创建失败!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld delete <世界名>", NamedTextColor.RED));
            return true;
        }

        String worldName = args[1];
        if (plugin.getWorldManager().deleteWorld(worldName)) {
            sender.sendMessage(Component.text("世界已删除: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("删除失败，世界不存在或正在使用中", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld load <世界名>", NamedTextColor.RED));
            return true;
        }

        if (plugin.getWorldManager().loadWorld(args[1])) {
            sender.sendMessage(Component.text("世界已加载: " + args[1], NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("加载失败，世界不存在或无法加载", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld unload <世界名>", NamedTextColor.RED));
            return true;
        }

        if (plugin.getWorldManager().unloadWorld(args[1])) {
            sender.sendMessage(Component.text("世界已卸载: " + args[1], NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("卸载失败，世界不存在或未加载", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gworld tp <世界名>", NamedTextColor.RED));
            return true;
        }

        if (plugin.getWorldManager().teleportToWorld(player, args[1])) {
            sender.sendMessage(Component.text("已传送到世界: " + args[1], NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("传送失败，世界不存在或未加载", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(Component.text("配置已重新加载!", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== GuangDianWorld 帮助 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gworld list ").append(Component.text("- 列出所有世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld info <世界> ").append(Component.text("- 查看世界信息", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld create <世界> [类型] ").append(Component.text("- 创建世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld delete <世界> ").append(Component.text("- 删除世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld load <世界> ").append(Component.text("- 加载世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld unload <世界> ").append(Component.text("- 卸载世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld tp <世界> ").append(Component.text("- 传送到世界", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/gworld reload ").append(Component.text("- 重载配置", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "create", "delete", "load", "unload", "tp", "reload"));
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("info") || subCmd.equals("delete") || subCmd.equals("load")
                || subCmd.equals("unload") || subCmd.equals("tp")) {
                completions.addAll(plugin.getWorldManager().getWorldNames());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("normal", "nether", "end"));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        return completions;
    }
}
