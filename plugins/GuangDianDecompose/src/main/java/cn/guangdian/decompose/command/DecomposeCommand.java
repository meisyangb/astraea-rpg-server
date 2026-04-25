package cn.guangdian.decompose.command;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分解命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "decompose", description = "物品分解", permission = "guangdian.decompose.use")
public class DecomposeCommand extends BaseCommand {
    private final GuangDianDecompose plugin;

    public DecomposeCommand(GuangDianDecompose plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开分解界面
     */
    @SubCommand(name = "", playerOnly = true)
    @Description("打开分解界面")
    public void openDefault(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.getDecomposeGUI().open(player);
    }

    /**
     * 打开分解界面
     */
    @SubCommand(name = "open", playerOnly = true)
    @Description("打开分解界面")
    public void open(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.getDecomposeGUI().open(player);
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        return new ArrayList<>();
    }
}
