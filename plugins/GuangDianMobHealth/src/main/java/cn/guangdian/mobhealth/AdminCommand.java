package cn.guangdian.mobhealth;


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
        sender.sendMessage(net.kyori.adventure.text.Component.text("=== GuangDianMobHealth 命令 ===", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/gdmobhealth reload - 重载配置文件", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/gdmobhealth status - 显示插件状态", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/gdmobhealth debug - 开关调试模式", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/gdmobhealth clear - 清除所有显示", net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    private void reloadConfig(CommandSender sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("正在重载配置文件...", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        plugin.reloadConfiguration();
        sender.sendMessage(net.kyori.adventure.text.Component.text("配置文件已重载！", net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("=== GuangDianMobHealth 状态 ===", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        sender.sendMessage(net.kyori.adventure.text.Component.text("启用状态: " + (plugin.isPluginEnabled() ? "启用" : "禁用"), net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("调试模式: " + (plugin.isDebug() ? "开启" : "关闭"), net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("当前显示数量: " + plugin.getDisplayManager().getDisplayCount(), net.kyori.adventure.text.format.NamedTextColor.GREEN));
        sender.sendMessage(net.kyori.adventure.text.Component.text("MythicMobs: " + 
            (plugin.getMythicMobsHook().isMythicMobsEnabled() ? "已挂钩" : "未挂钩"), 
            plugin.getMythicMobsHook().isMythicMobsEnabled() ? net.kyori.adventure.text.format.NamedTextColor.GREEN : net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void toggleDebug(CommandSender sender) {
        boolean newState = !plugin.isDebug();
        plugin.setDebug(newState);
        sender.sendMessage(net.kyori.adventure.text.Component.text("调试模式已" + (newState ? "开启" : "关闭"), net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    private void clearDisplays(CommandSender sender) {
        int count = plugin.getDisplayManager().getDisplayCount();
        plugin.getDisplayManager().clear();
        sender.sendMessage(net.kyori.adventure.text.Component.text("已清除 " + count + " 个显示", net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }
}
