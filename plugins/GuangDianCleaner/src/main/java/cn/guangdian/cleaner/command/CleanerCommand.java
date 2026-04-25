package cn.guangdian.cleaner.command;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.manager.CleanManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 扫地娘命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "gdclean", description = "扫地娘清理系统", permission = "gdclean.use")
public class CleanerCommand extends BaseCommand {

    private final GuangDianCleaner plugin;
    private final ConfigManager configManager;
    private final CleanManager cleanManager;
    private final MiniMessageService miniMessage;
    private final MessageService messageService;

    public CleanerCommand(GuangDianCleaner plugin, ConfigManager configManager, CleanManager cleanManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cleanManager = cleanManager;
        this.miniMessage = MiniMessageService.getInstance();
        RPGCore rpgCore = RPGCore.getInstance();
        this.messageService = rpgCore != null ? rpgCore.getMessageService() : null;
    }

    /**
     * 发送消息给发送者
     */
    private void send(CommandSender sender, String message) {
        if (messageService != null) {
            messageService.send(sender, message);
        } else {
            Component component = miniMessage.parse(message);
            sender.sendMessage(component);
        }
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelpDefault(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    /**
     * 手动执行清理
     */
    @SubCommand(name = "clean", permission = "gdclean.clean")
    @Description("手动执行清理")
    public void clean(CommandContext ctx) {
        if (cleanManager.isCleaning()) {
            ctx.sendError("正在清理中，请稍候...");
            return;
        }

        // Paper 1.21.4 要求实体操作必须在主线程执行
        plugin.runSync(() -> {
            cleanManager.performClean(true);
        });

        ctx.sendSuccess("正在执行清理...");
    }

    /**
     * 开关自动清理
     */
    @SubCommand(name = "toggle", permission = "gdclean.admin")
    @Description("开关自动清理")
    public void toggle(CommandContext ctx) {
        boolean currentState = configManager.isAutoCleanEnabled();

        if (currentState) {
            cleanManager.stopAutoCleanTask();
            ctx.sendError("自动清理已关闭!");
        } else {
            cleanManager.startAutoCleanTask();
            ctx.sendSuccess("自动清理已开启!");
        }
    }

    /**
     * 查看当前状态
     */
    @SubCommand(name = "status")
    @Description("查看当前状态")
    public void status(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        send(sender, "<gold>========== 当前状态 ==========");
        send(sender, "<yellow>自动清理: " + (configManager.isAutoCleanEnabled() ? "<green>开启" : "<red>关闭"));
        send(sender, "<yellow>清理间隔: <green>" + configManager.getAutoCleanInterval() + "秒");
        send(sender, "<yellow>预警时间: <green>" + configManager.getWarningTime() + "秒");
        send(sender, "<yellow>过滤模式: <green>" + configManager.getFilterMode().name());
        send(sender, "<yellow>世界模式: <green>" + configManager.getWorldMode().name());
        send(sender, "<yellow>保护命名物品: " + (configManager.isProtectNamedItems() ? "<green>是" : "<red>否"));
        send(sender, "<yellow>保护玩家掉落: " + (configManager.isProtectPlayerDrops() ? "<green>是" : "<red>否"));
        send(sender, "<gold>================================");
    }

    /**
     * 查看清理统计
     */
    @SubCommand(name = "stats")
    @Description("查看清理统计")
    public void stats(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        send(sender, "<gold>========== 清理统计 ==========");
        send(sender, "<yellow>累计清理物品: <green>" + configManager.getTotalCleanedItems() + "个");
        send(sender, "<yellow>累计清理实体: <green>" + configManager.getTotalCleanedEntities() + "个");

        if (ctx.hasPermission("gdclean.admin")) {
            send(sender, "<gray>使用 <yellow>/gdclean resetstats <gray>重置统计");
        }
        send(sender, "<gold>================================");
    }

    /**
     * 重新加载配置
     */
    @SubCommand(name = "reload", permission = "gdclean.admin")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        configManager.loadConfig();
        cleanManager.restartAutoCleanTask();
        ctx.sendSuccess("配置已重新加载!");
    }

    /**
     * 显示帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public void showHelp(CommandSender sender) {
        send(sender, "<gold><bold>========== 扫地娘命令帮助 ==========");
        send(sender, "<yellow>/gdclean clean <gray>- 手动执行清理");
        send(sender, "<yellow>/gdclean status <gray>- 查看当前状态");
        send(sender, "<yellow>/gdclean stats <gray>- 查看清理统计");

        if (sender.hasPermission("gdclean.admin")) {
            send(sender, "<yellow>/gdclean toggle <gray>- 开关自动清理");
            send(sender, "<yellow>/gdclean reload <gray>- 重载配置文件");
            send(sender, "<yellow>/gdclean resetstats <gray>- 重置统计数据");
        }

        send(sender, "<yellow>/gdclean help <gray>- 显示此帮助");
        send(sender, "<gold><bold>=====================================");
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 0) {
            completions.addAll(List.of("clean", "status", "stats", "help"));
            if (context.hasPermission("gdclean.admin")) {
                completions.addAll(List.of("toggle", "reload"));
            }
        }

        String lastArg = context.getStringArgOrDefault(context.getArgCount() - 1, "").toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
