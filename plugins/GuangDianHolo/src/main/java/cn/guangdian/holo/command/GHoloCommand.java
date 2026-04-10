package cn.guangdian.holo.command;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GHoloCommand implements CommandExecutor, TabCompleter {

    private final GuangDianHolo plugin;

    public GHoloCommand(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "addline" -> handleAddLine(sender, args);
            case "removeline" -> handleRemoveLine(sender, args);
            case "setline" -> handleSetLine(sender, args);
            case "move" -> handleMove(sender, args);
            case "tp" -> handleTp(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /gholo create <名称>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Location location = player.getLocation();

        if (plugin.getHologramManager().getHologram(name) != null) {
            player.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-already-exists", "name", name)));
            return true;
        }

        Hologram holo = plugin.getHologramManager().createHologram(name, location);
        if (holo != null) {
            player.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-created", "name", name)));
        } else {
            player.sendMessage(Component.text("创建失败!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gholo delete <名称>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        if (plugin.getHologramManager().deleteHologram(name)) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-deleted", "name", name)));
        } else {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-list", "count", String.valueOf(plugin.getHologramManager().getHologramCount()))));
        for (Hologram holo : plugin.getHologramManager().getAllHolograms()) {
            String worldName = holo.getWorldName();
            Location loc = holo.getLocation();
            String locationStr = String.format("%s: %.1f, %.1f, %.1f", 
                worldName, loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage(Component.text("  §f- " + holo.getName() + " §7(" + locationStr + ") §a" + holo.getLineCount() + "行"));
        }
        return true;
    }

    private boolean handleAddLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /gholo addline <名称> <文本>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        holo.addLine(text);
        plugin.getHologramManager().respawnHologram(holo);
        sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("line-added", "line", String.valueOf(holo.getLineCount()))));
        return true;
    }

    private boolean handleRemoveLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /gholo removeline <名称> <行号>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        try {
            int lineIndex = Integer.parseInt(args[2]) - 1;
            holo.removeLine(lineIndex);
            plugin.getHologramManager().respawnHologram(holo);
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("line-removed", "line", args[2])));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("行号必须是数字!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleSetLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("用法: /gholo setline <名称> <行号> <文本>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        try {
            int lineIndex = Integer.parseInt(args[2]) - 1;
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            holo.setLine(lineIndex, text);
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("line-set", "line", args[2])));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("行号必须是数字!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /gholo move <名称>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            player.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        holo.setLocation(player.getLocation());
        plugin.getHologramManager().respawnHologram(holo);
        player.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-moved", "name", name)));
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /gholo tp <名称>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            player.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        Location loc = holo.getLocation();
        if (loc != null) {
            player.teleport(loc);
            player.sendMessage(Component.text("已传送到全息显示: " + name, NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gholo info <名称>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        sender.sendMessage(Component.text("========== 全息显示信息 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("名称: §f" + holo.getName()));
        sender.sendMessage(Component.text("世界: §f" + holo.getWorldName()));
        Location loc = holo.getLocation();
        if (loc != null) {
            sender.sendMessage(Component.text(String.format("位置: §f%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
        }
        sender.sendMessage(Component.text("行数: §f" + holo.getLineCount()));
        sender.sendMessage(Component.text("视距: §f" + holo.getViewDistance()));
        sender.sendMessage(Component.text("行高: §f" + holo.getLineHeight()));
        sender.sendMessage(Component.text("持久: §f" + (holo.isPersistent() ? "是" : "否")));
        sender.sendMessage(Component.text("内容:"));
        for (int i = 0; i < holo.getLines().size(); i++) {
            sender.sendMessage(Component.text("  " + (i + 1) + ". §f" + holo.getLines().get(i)));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("reloaded")));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== GuangDianHolo 帮助 ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gholo create <名称> §7- 创建全息显示"));
        sender.sendMessage(Component.text("/gholo delete <名称> §7- 删除全息显示"));
        sender.sendMessage(Component.text("/gholo list §7- 列出所有全息显示"));
        sender.sendMessage(Component.text("/gholo addline <名称> <文本> §7- 添加一行"));
        sender.sendMessage(Component.text("/gholo removeline <名称> <行号> §7- 移除一行"));
        sender.sendMessage(Component.text("/gholo setline <名称> <行号> <文本> §7- 设置行内容"));
        sender.sendMessage(Component.text("/gholo move <名称> §7- 移动到当前位置"));
        sender.sendMessage(Component.text("/gholo tp <名称> §7- 传送到全息显示"));
        sender.sendMessage(Component.text("/gholo info <名称> §7- 查看详细信息"));
        sender.sendMessage(Component.text("/gholo reload §7- 重载配置"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list", "addline", "removeline", 
                "setline", "move", "tp", "info", "reload"));
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (!subCmd.equals("create") && !subCmd.equals("list") && !subCmd.equals("reload")) {
                completions.addAll(plugin.getHologramManager().getHologramNames());
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        return completions;
    }
}
