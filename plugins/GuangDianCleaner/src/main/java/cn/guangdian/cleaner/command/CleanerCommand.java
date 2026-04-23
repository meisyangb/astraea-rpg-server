package cn.guangdian.cleaner.command;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.manager.CleanManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 扫地娘命令处理器
 */
public class CleanerCommand implements CommandExecutor, TabExecutor {

    private final GuangDianCleaner plugin;
    private final ConfigManager configManager;
    private final CleanManager cleanManager;
    private final MiniMessageService miniMessage;

    public CleanerCommand(GuangDianCleaner plugin, ConfigManager configManager, CleanManager cleanManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cleanManager = cleanManager;
        this.miniMessage = MiniMessageService.getInstance();
    }

    private Component color(String message) {
        if (message == null) return Component.empty();
        return miniMessage.parse(message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "clean":
                handleClean(sender);
                break;

            case "toggle":
                handleToggle(sender);
                break;

            case "status":
                handleStatus(sender);
                break;

            case "stats":
                handleStats(sender);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(configManager.getMessagePrefix() + color("<red>未知的子命令!"));
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * 处理清理命令
     */
    private void handleClean(CommandSender sender) {
        if (!sender.hasPermission("gdclean.clean")) {
            sender.sendMessage(configManager.getMessageNoPermission());
            return;
        }

        if (cleanManager.isCleaning()) {
            sender.sendMessage(configManager.getMessagePrefix() + color("<red>正在清理中，请稍候..."));
            return;
        }

        // Paper 1.21.4 要求实体操作必须在主线程执行
        // 使用runSync将清理操作调度到主线程
        plugin.runSync(() -> {
            cleanManager.performClean(true);
        });

        sender.sendMessage(configManager.getMessagePrefix() + color("<green>正在执行清理..."));
    }

    /**
     * 处理开关命令
     */
    private void handleToggle(CommandSender sender) {
        if (!sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(configManager.getMessageNoPermission());
            return;
        }

        boolean currentState = configManager.isAutoCleanEnabled();

        // 这里需要通过修改配置来实现持久化开关
        // 简单起见，我们直接重启任务
        if (currentState) {
            cleanManager.stopAutoCleanTask();
            sender.sendMessage(configManager.getMessagePrefix() + color("<red>自动清理已关闭!"));
        } else {
            cleanManager.startAutoCleanTask();
            sender.sendMessage(configManager.getMessagePrefix() + color("<green>自动清理已开启!"));
        }
    }

    /**
     * 处理状态命令
     */
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("gdclean.status")) {
            sender.sendMessage(configManager.getMessageNoPermission());
            return;
        }

        Component status = configManager.isAutoCleanEnabled() ? color("<green>开启") : color("<red>关闭");
        sender.sendMessage(color(configManager.getMessagePrefix() + "当前状态:"));
        sender.sendMessage(color("  <yellow>自动清理: ").append(status));
        sender.sendMessage(color("  <yellow>清理间隔: <green>" + configManager.getAutoCleanInterval() + "秒"));
        sender.sendMessage(color("  <yellow>预警时间: <green>" + configManager.getWarningTime() + "秒"));
        sender.sendMessage(color("  <yellow>过滤模式: <green>" + configManager.getFilterMode().name()));
        sender.sendMessage(color("  <yellow>世界模式: <green>" + configManager.getWorldMode().name()));
        sender.sendMessage(color("  <yellow>保护命名物品: " + (configManager.isProtectNamedItems() ? "<green>是" : "<red>否")));
        sender.sendMessage(color("  <yellow>保护玩家掉落: " + (configManager.isProtectPlayerDrops() ? "<green>是" : "<red>否")));
    }

    /**
     * 处理统计命令
     */
    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("gdclean.stats")) {
            sender.sendMessage(configManager.getMessageNoPermission());
            return;
        }

        sender.sendMessage(configManager.getMessagePrefix() + color("清理统计:"));
        sender.sendMessage(color("  <yellow>累计清理物品: <green>" + configManager.getTotalCleanedItems() + "个"));
        sender.sendMessage(color("  <yellow>累计清理实体: <green>" + configManager.getTotalCleanedEntities() + "个"));

        // 提供重置选项
        if (sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(color("  <gray>使用 <yellow>/gdclean resetstats <gray>重置统计"));
        }
    }

    /**
     * 处理重载命令
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(configManager.getMessageNoPermission());
            return;
        }

        configManager.loadConfig();
        cleanManager.restartAutoCleanTask();

        sender.sendMessage(configManager.getMessagePrefix() + configManager.getMessageReload());
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(configManager.getMessagePrefix() + color("命令帮助:"));
        sender.sendMessage(color("  <yellow>/gdclean clean <gray>- 手动执行清理"));
        sender.sendMessage(color("  <yellow>/gdclean status <gray>- 查看当前状态"));
        sender.sendMessage(color("  <yellow>/gdclean stats <gray>- 查看清理统计"));

        if (sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(color("  <yellow>/gdclean toggle <gray>- 开关自动清理"));
            sender.sendMessage(color("  <yellow>/gdclean reload <gray>- 重载配置文件"));
            sender.sendMessage(color("  <yellow>/gdclean resetstats <gray>- 重置统计数据"));
        }

        sender.sendMessage(color("  <yellow>/gdclean help <gray>- 显示此帮助"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("clean", "status", "stats", "help");

            if (sender.hasPermission("gdclean.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("toggle");
                subCommands.add("reload");
                subCommands.add("resetstats");
            }

            for (String subCmd : subCommands) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        }

        return completions;
    }
}