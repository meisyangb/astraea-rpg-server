package cn.guangdian.tab;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 降级命令执行器 - 当 CommandFramework 不可用时使用
 */
public class FallbackCommandExecutor implements CommandExecutor {

    private final GuangDianTab plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public FallbackCommandExecutor(GuangDianTab plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tablist")) {
            sendInfo(sender);
            return true;
        }

        if (!command.getName().equalsIgnoreCase("gdtab")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("guangdian.tab.reload")) {
                sender.sendMessage(translate("<red>没有权限."));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadFormats();
            plugin.restartTasks();
            plugin.refreshAll();
            plugin.updateAllHeadersAndFooters();
            sender.sendMessage(translate("<green>GuangDianTab 已重载."));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("cache")) {
            sender.sendMessage(translate("<yellow>缓存名称数: <white>" + plugin.getCachedNamesCount()));
            sender.sendMessage(translate("<yellow>缓存页眉数: <white>" + plugin.getCachedHeadersCount()));
            sender.sendMessage(translate("<yellow>缓存页脚数: <white>" + plugin.getCachedFootersCount()));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translate("<gold>/gdtab reload <gray>- 重载配置"));
        sender.sendMessage(translate("<gold>/gdtab info <gray>- 插件信息"));
        sender.sendMessage(translate("<gold>/gdtab cache <gray>- 缓存信息"));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(translate("<gold>GuangDianTab <gray>v" + plugin.getDescription().getVersion()));
        sender.sendMessage(translate("<yellow>刷新间隔: <white>" + (plugin.getRefreshTicks() * 50L) + "ms"));
        sender.sendMessage(translate("<yellow>页眉/页脚刷新: <white>" + (plugin.getHeaderFooterTicks() * 50L) + "ms"));
        if (plugin.getExternalServices() != null) {
            sender.sendMessage(translate("<yellow>外部服务: <white>" + plugin.getExternalServices().getExternalServiceStatus()));
        } else {
            sender.sendMessage(translate("<yellow>外部服务: <red>未连接"));
        }
    }

    private net.kyori.adventure.text.Component translate(String input) {
        return miniMessage.deserialize(input);
    }
}
