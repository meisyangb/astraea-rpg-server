package cn.guangdian.holo.command;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

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
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize("<red>用法: /gholo create <名称>"));
            return true;
        }

        String name = args[1];
        Location location = player.getLocation();

        if (plugin.getHologramManager().getHologram(name) != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-already-exists", "name", name)));
            return true;
        }

        Hologram holo = plugin.getHologramManager().createHologram(name, location);
        if (holo != null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-created", "name", name)));
        } else {
            player.sendMessage(miniMessage.deserialize("<red>创建失败!"));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<red>用法: /gholo delete <名称>"));
            return true;
        }

        String name = args[1];
        if (plugin.getHologramManager().deleteHologram(name)) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-deleted", "name", name)));
        } else {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-list", "count", String.valueOf(plugin.getHologramManager().getHologramCount()))));
        for (Hologram holo : plugin.getHologramManager().getAllHolograms()) {
            String worldName = holo.getWorldName();
            Location loc = holo.getLocation();
            String locationStr = String.format("%s: %.1f, %.1f, %.1f",
                worldName, loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage(miniMessage.deserialize("<white>  - <white>" + holo.getName() + " <gray>(" + locationStr + ") <green>" + holo.getLineCount() + "行"));
        }
        return true;
    }

    private boolean handleAddLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize("<red>用法: /gholo addline <名称> <文本>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        holo.addLine(text);
        plugin.getHologramManager().respawnHologram(holo);
        sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("line-added", "line", String.valueOf(holo.getLineCount()))));
        return true;
    }

    private boolean handleRemoveLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize("<red>用法: /gholo removeline <名称> <行号>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        try {
            int lineIndex = Integer.parseInt(args[2]) - 1;
            holo.removeLine(lineIndex);
            plugin.getHologramManager().respawnHologram(holo);
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("line-removed", "line", args[2])));
        } catch (NumberFormatException e) {
            sender.sendMessage(miniMessage.deserialize("<red>行号必须是数字!"));
        }
        return true;
    }

    private boolean handleSetLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(miniMessage.deserialize("<red>用法: /gholo setline <名称> <行号> <文本>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        try {
            int lineIndex = Integer.parseInt(args[2]) - 1;
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            holo.setLine(lineIndex, text);
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("line-set", "line", args[2])));
        } catch (NumberFormatException e) {
            sender.sendMessage(miniMessage.deserialize("<red>行号必须是数字!"));
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize("<red>用法: /gholo move <名称>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        holo.setLocation(player.getLocation());
        plugin.getHologramManager().respawnHologram(holo);
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-moved", "name", name)));
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize("<red>用法: /gholo tp <名称>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        Location loc = holo.getLocation();
        if (loc != null) {
            player.teleport(loc);
            player.sendMessage(miniMessage.deserialize("<green>已传送到全息显示: " + name));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<red>用法: /gholo info <名称>"));
            return true;
        }

        String name = args[1];
        Hologram holo = plugin.getHologramManager().getHologram(name);
        if (holo == null) {
            sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("hologram-not-found", "name", name)));
            return true;
        }

        sender.sendMessage(miniMessage.deserialize("<gold>========== 全息显示信息 =========="));
        sender.sendMessage(miniMessage.deserialize("<white>名称: <white>" + holo.getName()));
        sender.sendMessage(miniMessage.deserialize("<white>世界: <white>" + holo.getWorldName()));
        Location loc = holo.getLocation();
        if (loc != null) {
            sender.sendMessage(miniMessage.deserialize(String.format("<white>位置: <white>%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())));
        }
        sender.sendMessage(miniMessage.deserialize("<white>行数: <white>" + holo.getLineCount()));
        sender.sendMessage(miniMessage.deserialize("<white>视距: <white>" + holo.getViewDistance()));
        sender.sendMessage(miniMessage.deserialize("<white>行高: <white>" + holo.getLineHeight()));
        sender.sendMessage(miniMessage.deserialize("<white>持久: <white>" + (holo.isPersistent() ? "是" : "否")));
        sender.sendMessage(miniMessage.deserialize("<white>内容:"));
        for (int i = 0; i < holo.getLines().size(); i++) {
            sender.sendMessage(miniMessage.deserialize("  " + (i + 1) + ". <white>" + holo.getLines().get(i)));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getMessage("reloaded")));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gold>========== GuangDianHolo 帮助 =========="));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo create <名称> <gray>- 创建全息显示"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo delete <名称> <gray>- 删除全息显示"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo list <gray>- 列出所有全息显示"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo addline <名称> <文本> <gray>- 添加一行"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo removeline <名称> <行号> <gray>- 移除一行"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo setline <名称> <行号> <文本> <gray>- 设置行内容"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo move <名称> <gray>- 移动到当前位置"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo tp <名称> <gray>- 传送到全息显示"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo info <名称> <gray>- 查看详细信息"));
        sender.sendMessage(miniMessage.deserialize("<white>/gholo reload <gray>- 重载配置"));
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
