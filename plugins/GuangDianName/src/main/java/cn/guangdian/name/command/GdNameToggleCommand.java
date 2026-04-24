package cn.guangdian.name.command;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.name.NameDisplayManager;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.SubCommand;
import cn.guangdian.rpgcore.command.Description;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * gdnametoggle 命令 - 使用 RPGCore CommandFramework
 * 玩家切换显示命令: title, guild, marriage, health, status
 */
@CommandInfo(name = "gdnametoggle", description = "头顶显示切换命令", permission = "guangdian.name.use", playerOnly = true)
public class GdNameToggleCommand extends BaseCommand {

    private final GuangDianName plugin;
    private final NameDisplayManager displayManager;

    public GdNameToggleCommand(GuangDianName plugin, NameDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "")
    @Description("显示帮助信息")
    public void showHelp(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        ctx.sendMessage("<gold>===== 头顶显示帮助 =====");
        ctx.sendMessage("<yellow>/gdnametoggle health <gray>- 切换血量显示");
        ctx.sendMessage("<yellow>/gdnametoggle title <gray>- 切换称号显示");
        ctx.sendMessage("<yellow>/gdnametoggle guild <gray>- 切换工会显示");
        ctx.sendMessage("<yellow>/gdnametoggle marriage <gray>- 切换婚姻显示");
        ctx.sendMessage("<yellow>/gdnametoggle status <gray>- 查看当前状态");
    }

    /**
     * 切换称号显示
     */
    @SubCommand(name = "title")
    @Description("切换称号显示")
    public void toggleTitle(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        boolean newState = displayManager.toggleTitle(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        ctx.sendMessage("<yellow>[头顶显示] <gray>称号显示已" + status);
    }

    /**
     * 切换工会显示
     */
    @SubCommand(name = "guild")
    @Description("切换工会显示")
    public void toggleGuild(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        boolean newState = displayManager.toggleGuild(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        ctx.sendMessage("<yellow>[头顶显示] <gray>工会显示已" + status);
    }

    /**
     * 切换婚姻显示
     */
    @SubCommand(name = "marriage")
    @Description("切换婚姻显示")
    public void toggleMarriage(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        boolean newState = displayManager.toggleMarriage(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        ctx.sendMessage("<yellow>[头顶显示] <gray>婚姻显示已" + status);
    }

    /**
     * 切换血量显示
     */
    @SubCommand(name = "health")
    @Description("切换血量显示")
    public void toggleHealth(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        boolean newState = displayManager.toggleHealth(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        ctx.sendMessage("<yellow>[头顶显示] <gray>血量显示已" + status);
    }

    /**
     * 查看当前状态
     */
    @SubCommand(name = "status")
    @Description("查看当前状态")
    public void showStatus(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String titleStatus = displayManager.getShowTitleStatus(player);
        String guildStatus = displayManager.getShowGuildStatus(player);
        String marriageStatus = displayManager.getShowMarriageStatus(player);
        String healthStatus = displayManager.getShowHealthStatus(player);

        ctx.sendMessage("<gold>===== 头顶显示状态 =====");
        ctx.sendMessage("<yellow>血量显示: " + healthStatus);
        ctx.sendMessage("<yellow>称号显示: " + titleStatus);
        ctx.sendMessage("<yellow>工会显示: " + guildStatus);
        ctx.sendMessage("<yellow>婚姻显示: " + marriageStatus);
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        String[] args = context.getArgs();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.isEmpty() && args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            String[] commands = {"title", "guild", "marriage", "health", "status"};
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
            return completions;
        }

        return new ArrayList<>();
    }
}
