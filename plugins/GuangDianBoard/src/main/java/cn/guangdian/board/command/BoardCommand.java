package cn.guangdian.board.command;

import cn.guangdian.board.GuangDianBoard;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 侧边栏命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "gdboard", description = "侧边栏管理", permission = "guangdian.board.use")
public class BoardCommand extends BaseCommand {
    private final GuangDianBoard plugin;

    public BoardCommand(GuangDianBoard plugin) {
        this.plugin = plugin;
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
     * 重新加载配置
     */
    @SubCommand(name = "reload", permission = "guangdian.board.reload")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        // 重新加载配置
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.shouldShowBoardPublic(player)) {
                plugin.createBoard(player);
            }
        }

        ctx.sendSuccess(plugin.getConfig().getString("messages.config-reloaded", "<green>侧边栏配置已重新加载!"));
    }

    /**
     * 切换侧边栏显示
     */
    @SubCommand(name = "toggle", playerOnly = true, permission = "guangdian.board.toggle")
    @Description("切换侧边栏显示")
    public void toggle(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.toggleBoard(player);
    }

    /**
     * 显示插件信息
     */
    @SubCommand(name = "info")
    @Description("显示插件信息")
    public void info(CommandContext ctx) {
        ctx.sendMessage("<gold><bold>===== 光点侧边栏插件信息 =====");
        ctx.sendMessage("<yellow>版本: <white>" + plugin.getDescription().getVersion());
        ctx.sendMessage("<yellow>作者: <white>Gumin");
        ctx.sendMessage("<yellow>QQ: <white>2271257344");
        ctx.sendMessage("<yellow>状态: <green>已启用");
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
    public void showHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold><bold>===== 光点侧边栏插件 ====="));
        sender.sendMessage(msg.colorize("<yellow>/gdboard reload <gray>- 重新加载配置"));
        sender.sendMessage(msg.colorize("<yellow>/gdboard toggle <gray>- 切换侧边栏显示"));
        sender.sendMessage(msg.colorize("<yellow>/gdboard info <gray>- 显示插件信息"));
        sender.sendMessage(msg.colorize("<yellow>/gdboard help <gray>- 显示帮助信息"));
        sender.sendMessage(msg.colorize("<yellow>/toggleboard <gray>- 快速切换侧边栏"));
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 0) {
            completions.addAll(List.of("reload", "toggle", "info", "help"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(context.getStringArgOrDefault(0, "").toLowerCase()))
            .collect(Collectors.toList());
    }
}
