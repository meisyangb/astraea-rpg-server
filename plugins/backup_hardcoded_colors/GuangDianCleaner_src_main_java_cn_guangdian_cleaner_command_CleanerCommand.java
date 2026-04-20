package cn.guangdian.cleaner.command;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.manager.CleanManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

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

    public CleanerCommand(GuangDianCleaner plugin, ConfigManager configManager, CleanManager cleanManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cleanManager = cleanManager;
    }

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * 使用 MiniMessage 解析颜色代码
     */
    private Component color(String message) {
        if (message == null) return Component.empty();
        // 将 & 颜色代码转换为 MiniMessage 格式
        String miniMessageText = message
            .replace("&0", "<black>").replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>")
            .replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&k", "<obfuscated>").replace("&l", "<bold>")
            .replace("&m", "<strikethrough>").replace("&n", "<underlined>")
            .replace("&o", "<italic>").replace("&r", "<reset>");
        return miniMessage.deserialize(miniMessageText);
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
                sender.sendMessage(configManager.getMessagePrefix() + color("&c未知的子命令!"));
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
            sender.sendMessage(configManager.getMessagePrefix() + color("&c正在清理中，请稍候..."));
            return;
        }

        // Paper 1.21.4 要求实体操作必须在主线程执行
        // 使用runSync将清理操作调度到主线程
        plugin.runSync(() -> {
            cleanManager.performClean(true);
        });

        sender.sendMessage(configManager.getMessagePrefix() + color("&a正在执行清理..."));
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
            sender.sendMessage(configManager.getMessagePrefix() + color("&c自动清理已关闭!"));
        } else {
            cleanManager.startAutoCleanTask();
            sender.sendMessage(configManager.getMessagePrefix() + color("&a自动清理已开启!"));
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

        Component status = configManager.isAutoCleanEnabled() ? color("&a开启") : color("&c关闭");
        sender.sendMessage(color(configManager.getMessagePrefix() + "当前状态:"));
        sender.sendMessage(color("  &e自动清理: ").append(status));
        sender.sendMessage(color("  &e清理间隔: &a" + configManager.getAutoCleanInterval() + "秒"));
        sender.sendMessage(color("  &e预警时间: &a" + configManager.getWarningTime() + "秒"));
        sender.sendMessage(color("  &e过滤模式: &a" + configManager.getFilterMode().name()));
        sender.sendMessage(color("  &e世界模式: &a" + configManager.getWorldMode().name()));
        sender.sendMessage(color("  &e保护命名物品: " + (configManager.isProtectNamedItems() ? "&a是" : "&c否")));
        sender.sendMessage(color("  &e保护玩家掉落: " + (configManager.isProtectPlayerDrops() ? "&a是" : "&c否")));
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
        sender.sendMessage(color("  &e累计清理物品: &a" + configManager.getTotalCleanedItems() + "个"));
        sender.sendMessage(color("  &e累计清理实体: &a" + configManager.getTotalCleanedEntities() + "个"));

        // 提供重置选项
        if (sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(color("  &7使用 &e/gdclean resetstats &7重置统计"));
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
        sender.sendMessage(color("  &e/gdclean clean &7- 手动执行清理"));
        sender.sendMessage(color("  &e/gdclean status &7- 查看当前状态"));
        sender.sendMessage(color("  &e/gdclean stats &7- 查看清理统计"));

        if (sender.hasPermission("gdclean.admin")) {
            sender.sendMessage(color("  &e/gdclean toggle &7- 开关自动清理"));
            sender.sendMessage(color("  &e/gdclean reload &7- 重载配置文件"));
            sender.sendMessage(color("  &e/gdclean resetstats &7- 重置统计数据"));
        }

        sender.sendMessage(color("  &e/gdclean help &7- 显示此帮助"));
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