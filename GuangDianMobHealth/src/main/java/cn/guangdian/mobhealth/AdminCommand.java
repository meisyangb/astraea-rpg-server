package cn.guangdian.mobhealth;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final GuangDianMobHealth plugin;
    private final MiniMessageService miniMessage;

    public AdminCommand(GuangDianMobHealth plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessageService();
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

    private void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(miniMessage.colorize(text));
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gold>=== GuangDianMobHealth 命令 ===");
        sendMessage(sender, "<green>/gdmobhealth reload <gray>- 重载配置文件");
        sendMessage(sender, "<green>/gdmobhealth status <gray>- 显示插件状态");
        sendMessage(sender, "<green>/gdmobhealth debug <gray>- 开关调试模式");
        sendMessage(sender, "<green>/gdmobhealth clear <gray>- 清除所有显示");
    }

    private void reloadConfig(CommandSender sender) {
        sendMessage(sender, "<yellow>正在重载配置文件...");
        plugin.reloadConfiguration();
        sendMessage(sender, "<green>配置文件已重载！");
    }

    private void sendStatus(CommandSender sender) {
        sendMessage(sender, "<gold>=== GuangDianMobHealth 状态 ===");
        sendMessage(sender, "<green>启用状态: " + (plugin.isPluginEnabled() ? "<green>启用" : "<red>禁用"));
        sendMessage(sender, "<green>调试模式: " + (plugin.isDebug() ? "<green>开启" : "<gray>关闭"));
        sendMessage(sender, "<green>当前显示数量: <yellow>" + plugin.getDisplayManager().getDisplayCount());
        String mythicStatus = plugin.getMythicMobsHook().isMythicMobsEnabled() ? "<green>已挂钩" : "<red>未挂钩";
        sendMessage(sender, "<green>MythicMobs: " + mythicStatus);
    }

    private void toggleDebug(CommandSender sender) {
        boolean newState = !plugin.isDebug();
        plugin.setDebug(newState);
        sendMessage(sender, "<green>调试模式已" + (newState ? "<green>开启" : "<gray>关闭"));
    }

    private void clearDisplays(CommandSender sender) {
        int count = plugin.getDisplayManager().getDisplayCount();
        plugin.getDisplayManager().clear();
        sendMessage(sender, "<green>已清除 <yellow>" + count + " <green>个显示");
    }
}
