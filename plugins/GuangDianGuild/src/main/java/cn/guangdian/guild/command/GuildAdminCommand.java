package cn.guangdian.guild.command;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会管理命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "guildadmin", description = "公会管理", permission = "guangdian.guild.admin")
public class GuildAdminCommand extends BaseCommand {

    private final GuangDianGuild plugin;

    public GuildAdminCommand(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 重载配置
     */
    @SubCommand(name = "reload")
    @Description("重载插件配置")
    public void reload(CommandContext ctx) {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        ctx.sendSuccess("配置已重新加载!");
    }

    /**
     * 删除公会
     */
    @SubCommand(name = "delete", minArgs = 1)
    @Description("删除指定公会")
    public void delete(CommandContext ctx) {
        String name = ctx.getJoinedArgs();

        if (plugin.disbandGuild(name)) {
            ctx.sendSuccess("公会 " + name + " 已删除!");
        } else {
            ctx.sendError("找不到公会: " + name);
        }
    }

    /**
     * 帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equalsIgnoreCase("delete")) {
            if (context.getArgCount() == 0 || context.getArgCount() == 1) {
                String partial = context.getArgCount() == 0 ? "" : context.getStringArg(0).toLowerCase();
                for (GuangDianGuild.Guild g : plugin.getAllGuilds()) {
                    if (g.name.toLowerCase().startsWith(partial)) {
                        completions.add(g.name);
                    }
                }
            }
        }

        return completions;
    }
}
