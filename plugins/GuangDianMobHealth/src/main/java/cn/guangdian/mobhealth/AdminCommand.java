package cn.guangdian.mobhealth;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final GuangDianMobHealth plugin;

    public AdminCommand(GuangDianMobHealth plugin) {
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
                reloadConfig(sender);
                break;
            case "status":
                sendStatus(sender);
                break;
            case "debug":
                toggleDebug(sender);
                break;
            case "clear":
                clearDisplays(sender);
                break;
            default:
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GuangDianMobHealth 命令 ===");
        sender.sendMessage(ChatColor.GREEN + "/gdmobhealth reload " + ChatColor.GRAY + "- 重载配置文件");
        sender.sendMessage(ChatColor.GREEN + "/gdmobhealth status " + ChatColor.GRAY + "- 显示插件状态");
        sender.sendMessage(ChatColor.GREEN + "/gdmobhealth debug " + ChatColor.GRAY + "- 开关调试模式");
        sender.sendMessage(ChatColor.GREEN + "/gdmobhealth clear " + ChatColor.GRAY + "- 清除所有显示");
    }

    private void reloadConfig(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "正在重载配置文件...");
        plugin.reloadConfiguration();
        sender.sendMessage(ChatColor.GREEN + "配置文件已重载！");
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GuangDianMobHealth 状态 ===");
        sender.sendMessage(ChatColor.GREEN + "启用状态: " + ChatColor.WHITE + (plugin.isPluginEnabled() ? "启用" : "禁用"));
        sender.sendMessage(ChatColor.GREEN + "调试模式: " + ChatColor.WHITE + (plugin.isDebug() ? "开启" : "关闭"));
        sender.sendMessage(ChatColor.GREEN + "当前显示数量: " + ChatColor.WHITE + plugin.getDisplayManager().getDisplayCount());
        sender.sendMessage(ChatColor.GREEN + "MythicMobs: " + ChatColor.WHITE + 
            (plugin.getMythicMobsHook().isMythicMobsEnabled() ? ChatColor.GREEN + "已挂钩" : ChatColor.RED + "未挂钩"));
    }

    private void toggleDebug(CommandSender sender) {
        boolean newState = !plugin.isDebug();
        plugin.setDebug(newState);
        sender.sendMessage(ChatColor.GREEN + "调试模式已" + (newState ? "开启" : "关闭"));
    }

    private void clearDisplays(CommandSender sender) {
        int count = plugin.getDisplayManager().getDisplayCount();
        plugin.getDisplayManager().clear();
        sender.sendMessage(ChatColor.GREEN + "已清除 " + count + " 个显示");
    }
}
