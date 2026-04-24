package cn.guangdian.decompose.command;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分解管理员命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "decomposeadmin", description = "分解管理", permission = "guangdian.decompose.admin")
public class DecomposeAdminCommand extends BaseCommand {
    private final GuangDianDecompose plugin;

    public DecomposeAdminCommand(GuangDianDecompose plugin) {
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
    @SubCommand(name = "reload")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        ctx.sendSuccess("配置已重新加载！");
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
        sender.sendMessage(msg.colorize("<gold><bold>===== 分解管理 ====="));
        sender.sendMessage(msg.colorize("<yellow>/decomposeadmin reload <gray>- 重新加载配置"));
        sender.sendMessage(msg.colorize("<yellow>/decomposeadmin help <gray>- 显示帮助信息"));
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 0) {
            completions.addAll(List.of("reload", "help"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(context.getStringArgOrDefault(0, "").toLowerCase()))
            .collect(Collectors.toList());
    }
}
