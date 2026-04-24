package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.gui.RecipeSelectGUI;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 锻造命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "forge", description = "锻造系统", permission = "guangdian.forge.use")
public class ForgeCommand extends BaseCommand {
    private final GuangDianForge plugin;

    public ForgeCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开锻造界面
     */
    @SubCommand(name = "", playerOnly = true)
    @Description("打开锻造界面")
    public void openDefault(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        RecipeSelectGUI gui = new RecipeSelectGUI(plugin, player);
        gui.open();
    }

    /**
     * 查看锻造信息
     */
    @SubCommand(name = "info", playerOnly = true)
    @Description("查看锻造信息")
    public void info(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
        ctx.sendMessage("<gold>=== 锻造信息 ===");
        ctx.sendMessage("<yellow>锻造等级: <white>" + data.getForgeLevel());
        ctx.sendMessage("<yellow>锻造经验: <white>" + data.getForgeExp());
        ctx.sendMessage("<yellow>总锻造次数: <gray>" + data.getTotalForges());
        ctx.sendMessage("<green>成功次数: <white>" + data.getSuccessForges());
    }

    /**
     * 查看已学图纸
     */
    @SubCommand(name = "recipes", playerOnly = true)
    @Description("查看已学图纸")
    public void recipes(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        var data = plugin.getPlayerDataManager().get(player.getUniqueId());
        ctx.sendMessage("<gold>已学图纸:");
        for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
            if (data.hasLearned(recipe.getId())) {
                ctx.sendMessage("<green> - " + recipe.getDisplayName());
            }
        }
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        return new ArrayList<>();
    }
}
