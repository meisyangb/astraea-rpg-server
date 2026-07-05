package cn.guangdian.fakeonline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 伪装在线数命令处理器
 */
public class FakeOnlineCommand implements CommandExecutor, TabCompleter {

    private final GuangDianFakeOnline plugin;

    public FakeOnlineCommand(GuangDianFakeOnline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "status":
                handleStatus(sender);
                break;

            case "set":
                handleSet(sender, args);
                break;

            case "mode":
                handleMode(sender, args);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("gdfakeonline.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "配置已重新加载");
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("gdfakeonline.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }

        int realOnline = Bukkit.getOnlinePlayers().size();
        FakeOnlineConfig config = plugin.getFakeOnlineConfig();
        int fakeOnline = config.getFakeOnlineCount(realOnline);

        sender.sendMessage(ChatColor.AQUA + "=== GuangDianFakeOnline 状态 ===");
        sender.sendMessage(ChatColor.YELLOW + "模式: " + config.getMode());
        sender.sendMessage(ChatColor.YELLOW + "真实在线: " + realOnline);
        sender.sendMessage(ChatColor.YELLOW + "伪装在线: " + fakeOnline);
        sender.sendMessage(ChatColor.YELLOW + "伪装最大: " + config.getFakeMaxPlayers());
        sender.sendMessage(ChatColor.YELLOW + "自定义MOTD: " + (config.hasCustomMotd() ? "启用" : "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "自定义玩家列表: " + (config.hasCustomSamplePlayers() ? "启用" : "禁用"));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gdfakeonline.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /gdfakeonline set <参数> <值>");
            sender.sendMessage(ChatColor.YELLOW + "可用参数:");
            sender.sendMessage(ChatColor.YELLOW + "  fixed-online, min-online, max-online");
            sender.sendMessage(ChatColor.YELLOW + "  multiplier, fake-max-players");
            return;
        }

        String param = args[1].toLowerCase();
        String value = args[2];

        try {
            switch (param) {
                case "fixed-online":
                    plugin.getConfig().set("fixed-online", Integer.parseInt(value));
                    sender.sendMessage(ChatColor.GREEN + "已设置固定在线数为: " + value);
                    break;

                case "min-online":
                    plugin.getConfig().set("min-online", Integer.parseInt(value));
                    sender.sendMessage(ChatColor.GREEN + "已设置最小在线数为: " + value);
                    break;

                case "max-online":
                    plugin.getConfig().set("max-online", Integer.parseInt(value));
                    sender.sendMessage(ChatColor.GREEN + "已设置最大在线数为: " + value);
                    break;

                case "multiplier":
                    plugin.getConfig().set("multiplier", Double.parseDouble(value));
                    sender.sendMessage(ChatColor.GREEN + "已设置倍数为: " + value);
                    break;

                case "fake-max-players":
                    plugin.getConfig().set("fake-max-players", Integer.parseInt(value));
                    sender.sendMessage(ChatColor.GREEN + "已设置伪装最大玩家数为: " + value);
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "未知参数: " + param);
                    break;
            }

            plugin.saveConfig();
            plugin.reloadConfig();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的数值: " + value);
        }
    }

    private void handleMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gdfakeonline.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gdfakeonline mode <模式>");
            sender.sendMessage(ChatColor.YELLOW + "可用模式: FIXED, RANGE, MULTIPLIER, ADD");
            return;
        }

        String mode = args[1].toUpperCase();
        if (!Arrays.asList("FIXED", "RANGE", "MULTIPLIER", "ADD").contains(mode)) {
            sender.sendMessage(ChatColor.RED + "无效的模式: " + mode);
            sender.sendMessage(ChatColor.YELLOW + "可用模式: FIXED, RANGE, MULTIPLIER, ADD");
            return;
        }

        plugin.getConfig().set("mode", mode);
        plugin.saveConfig();
        plugin.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "已切换到模式: " + mode);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== GuangDianFakeOnline 帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/gdfakeonline reload - 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/gdfakeonline status - 查看当前状态");
        sender.sendMessage(ChatColor.YELLOW + "/gdfakeonline set <参数> <值> - 设置参数");
        sender.sendMessage(ChatColor.YELLOW + "/gdfakeonline mode <模式> - 切换模式");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "status", "set", "mode"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(Arrays.asList(
                    "fixed-online", "min-online", "max-online",
                    "multiplier", "fake-max-players"
                ));
            } else if (args[0].equalsIgnoreCase("mode")) {
                completions.addAll(Arrays.asList("FIXED", "RANGE", "MULTIPLIER", "ADD"));
            }
        }

        return completions;
    }
}