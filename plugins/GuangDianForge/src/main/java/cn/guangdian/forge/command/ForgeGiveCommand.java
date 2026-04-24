package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 锻造图纸给予命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "forgegive", description = "给予锻造图纸", permission = "guangdian.forge.admin")
public class ForgeGiveCommand extends BaseCommand {
    private final GuangDianForge plugin;

    public ForgeGiveCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    /**
     * 给予图纸书
     */
    @SubCommand(name = "", minArgs = 2)
    @Description("给予玩家图纸书")
    public void giveDefault(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        String recipeId = ctx.getStringArg(1).toLowerCase();

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ctx.sendError("玩家不在线!");
            return;
        }

        ForgeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            ctx.sendError("图纸不存在: " + recipeId);
            return;
        }

        ItemStack book = LearnRecipeListener.createRecipeBook(recipe, plugin);
        target.getInventory().addItem(book);
        target.sendMessage(msg.colorize("<green>你获得了一张图纸: " + recipe.getDisplayName()));
        ctx.sendSuccess("已给予 " + target.getName() + " 图纸: " + recipe.getDisplayName());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 1) {
            // 玩家名称补全
            String partial = context.getStringArgOrDefault(0, "").toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (context.getArgCount() == 2) {
            // 图纸ID补全
            String partial = context.getStringArgOrDefault(1, "").toLowerCase();
            for (ForgeRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
                if (recipe.getId().toLowerCase().startsWith(partial)) {
                    completions.add(recipe.getId());
                }
            }
        }

        return completions;
    }
}
