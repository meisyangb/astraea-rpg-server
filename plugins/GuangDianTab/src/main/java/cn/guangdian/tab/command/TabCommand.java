package cn.guangdian.tab.command;

import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.tab.GuangDianTab;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab列表命令 - 使用 RPGCore CommandFramework
 */
@CommandInfo(
    name = "gdtab",
    description = "Tab列表管理命令",
    permission = "guangdian.tab.use"
)
public class TabCommand extends BaseCommand {

    private final GuangDianTab plugin;

    public TabCommand(GuangDianTab plugin) {
        this.plugin = plugin;
    }

    @SubCommand(name = "reload", permission = "guangdian.tab.reload")
    @Description("重载配置文件")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        plugin.loadFormats();
        plugin.restartTasks();
        plugin.refreshAll();
        plugin.updateAllHeadersAndFooters();
        ctx.sendSuccess(plugin.getConfig().getString("messages.config-reloaded", "GuangDianTab 已重载."));
    }

    @SubCommand(name = "info")
    @Description("查看插件信息")
    public void info(CommandContext ctx) {
        ctx.sendMessage("<gold>GuangDianTab <gray>v" + plugin.getDescription().getVersion());
        ctx.sendMessage("<yellow>刷新间隔: <white>" + (plugin.getRefreshTicks() * 50L) + "ms");
        ctx.sendMessage("<yellow>页眉/页脚刷新: <white>" + (plugin.getHeaderFooterTicks() * 50L) + "ms");

        if (plugin.getExternalServices() != null) {
            ctx.sendMessage("<yellow>外部服务: <white>" + plugin.getExternalServices().getExternalServiceStatus());
        } else {
            ctx.sendMessage("<yellow>外部服务: <red>未连接");
        }
    }

    @SubCommand(name = "cache")
    @Description("查看缓存信息")
    public void cache(CommandContext ctx) {
        ctx.sendMessage("<yellow>缓存名称数: <white>" + plugin.getCachedNamesCount());
        ctx.sendMessage("<yellow>缓存页眉数: <white>" + plugin.getCachedHeadersCount());
        ctx.sendMessage("<yellow>缓存页脚数: <white>" + plugin.getCachedFootersCount());
    }

    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    /**
     * Tab补全 - 第一级参数
     */
    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        return new ArrayList<>();
    }
}
