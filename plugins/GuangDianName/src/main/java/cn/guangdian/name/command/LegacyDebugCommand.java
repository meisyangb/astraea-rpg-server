package cn.guangdian.name.command;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试命令处理器 - 传统 Bukkit 实现 (降级方案)
 * 当 RPGCore CommandFramework 不可用时使用
 */
public class LegacyDebugCommand implements CommandExecutor, TabCompleter {

    private final GuangDianName plugin;
    private final MiniMessageService miniMessage;

    public LegacyDebugCommand(GuangDianName plugin) {
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
            case "health":
                sendHealthDebug(sender);
                break;
            case "refresh":
                refreshAll(sender);
                break;
            case "monitor":
                toggleMonitor(sender);
                break;
            case "debug":
                toggleDebug(sender);
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
        sendMessage(sender, "<gold>=== GuangDianName 命令 ===");
        sendMessage(sender, "<green>/gdname reload <gray>- 重载配置文件");
        sendMessage(sender, "<green>/gdname status <gray>- 显示插件状态");
        sendMessage(sender, "<green>/gdname health <gray>- 显示所有玩家血量");
        sendMessage(sender, "<green>/gdname refresh <gray>- 刷新所有玩家显示");
        sendMessage(sender, "<green>/gdname monitor <gray>- 开关实时血量监控日志");
        sendMessage(sender, "<green>/gdname debug <gray>- 开关详细调试日志");
    }

    private void reloadConfig(CommandSender sender) {
        sendMessage(sender, "<yellow>正在重载配置文件...");

        plugin.reloadConfig();
        plugin.getNameDisplayManager().loadSettings();

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getNameDisplayManager().removeAllDisplays(player);
            plugin.getNameDisplayManager().initPlayer(player);
        }

        sendMessage(sender, "<green>配置文件已重载！");
    }

    private void sendStatus(CommandSender sender) {
        sendMessage(sender, "<gold>=== GuangDianName 状态 ===");
        sendMessage(sender, "<green>在线玩家: <white>" + Bukkit.getOnlinePlayers().size());
        String rpgcoreStatus = Bukkit.getPluginManager().isPluginEnabled("RPGCore") ? "<green>已启用" : "<red>未启用";
        sendMessage(sender, "<green>RPGCore: <white>" + rpgcoreStatus);
        sendMessage(sender, "<green>显示模式: <white>全TextDisplay");
    }

    private void sendHealthDebug(CommandSender sender) {
        sendMessage(sender, "<gold>=== 玩家血量信息 ===");

        for (Player player : Bukkit.getOnlinePlayers()) {
            double health = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            int displayHealth = (int) Math.ceil(health);

            sendMessage(sender, String.format("<green>%s: <white>%.1f/%.1f (显示: %d)",
                player.getName(), health, maxHealth, displayHealth));
        }
    }

    private void refreshAll(CommandSender sender) {
        sendMessage(sender, "<green>正在刷新所有玩家显示...");

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getNameDisplayManager().removeAllDisplays(player);
            plugin.getNameDisplayManager().initPlayer(player);
        }

        sendMessage(sender, "<green>刷新完成！");
    }

    private void toggleMonitor(CommandSender sender) {
        boolean currentState = plugin.getHealthMonitor().isEnabled();
        plugin.getHealthMonitor().setEnabled(!currentState);

        String status = !currentState ? "<green>启用" : "<red>禁用";
        sendMessage(sender, "<green>血量监控已" + status);
        sendMessage(sender, "<gray>监控日志将输出到服务器日志文件");
    }

    private void toggleDebug(CommandSender sender) {
        boolean currentState = plugin.getNameDisplayManager().isDebug();
        plugin.getNameDisplayManager().setDebug(!currentState);

        String status = !currentState ? "<green>启用" : "<red>禁用";
        sendMessage(sender, "<green>详细调试日志已" + status);
        sendMessage(sender, "<gray>调试日志将输出详细信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            String[] commands = {"reload", "status", "health", "refresh", "monitor", "debug"};
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }
}
