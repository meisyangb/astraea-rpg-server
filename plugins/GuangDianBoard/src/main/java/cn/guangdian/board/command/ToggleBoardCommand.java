package cn.guangdian.board.command;

import cn.guangdian.board.GuangDianBoard;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 快速切换侧边栏命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "toggleboard", description = "快速切换侧边栏", permission = "guangdian.board.toggle", playerOnly = true)
public class ToggleBoardCommand extends BaseCommand {
    private final GuangDianBoard plugin;

    public ToggleBoardCommand(GuangDianBoard plugin) {
        this.plugin = plugin;
    }

    /**
     * 切换侧边栏显示
     */
    @SubCommand(name = "")
    @Description("切换侧边栏显示")
    public void toggle(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.toggleBoard(player);
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        return List.of();
    }
}
