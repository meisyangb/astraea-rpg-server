package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 锻造管理员命令 - 使用 RPGCore CommandFramework
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "forgeadmin", description = "锻造管理", permission = "guangdian.forge.admin")
public class ForgeAdminCommand extends BaseCommand {
    private final GuangDianForge plugin;

    public ForgeAdminCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    /**
     * 显示帮助
     */
    @SubCommand(name = "")
    @Description("显示管理命令帮助")
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
        plugin.getRecipeManager().loadRecipes();
        ctx.sendSuccess("配置已重载!");
    }

    /**
     * 列出所有图纸
     */
    @SubCommand(name = "list")
    @Description("列出所有图纸")
    public void list(CommandContext ctx) {
        var recipes = plugin.getRecipeManager().getAllRecipes();
        ctx.sendMessage("<gold>=== 所有锻造图纸 (共 " + recipes.size() + " 个) ===");
        for (var recipe : recipes) {
            ctx.sendMessage("<yellow>ID: <white>" + recipe.getId() +
                " <gray>| 名称: <aqua>" + recipe.getDisplayName() +
                " <gray>| 等级要求: <green>" + recipe.getRequiredForgeLevel() + "级");
        }
    }

    /**
     * 设置锻造等级
     */
    @SubCommand(name = "setlevel", minArgs = 2)
    @Description("设置玩家锻造等级")
    public void setLevel(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        int level;
        try {
            level = Integer.parseInt(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("等级必须是数字!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        data.setForgeLevel(level);
        plugin.getPlayerDataManager().save(data);
        ctx.sendSuccess("已设置 " + target.getName() + " 的锻造等级为 " + level);
    }

    /**
     * 设置锻造经验
     */
    @SubCommand(name = "setexp", minArgs = 2)
    @Description("设置玩家锻造经验")
    public void setExp(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        long exp;
        try {
            exp = Long.parseLong(ctx.getStringArg(1));
        } catch (NumberFormatException e) {
            ctx.sendError("经验必须是数字!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        data.setForgeExp(exp);
        plugin.getPlayerDataManager().save(data);
        ctx.sendSuccess("已设置 " + target.getName() + " 的锻造经验为 " + exp);
    }

    /**
     * 解锁图纸
     */
    @SubCommand(name = "unlock", minArgs = 2)
    @Description("为玩家解锁图纸")
    public void unlock(CommandContext ctx) {
        String targetName = ctx.getStringArg(0);
        String recipeId = ctx.getStringArg(1).toLowerCase();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        data.learnRecipe(recipeId);
        plugin.getPlayerDataManager().save(data);
        ctx.sendSuccess("已为 " + target.getName() + " 解锁图纸: " + recipeId);
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (subCommandName.equals("setlevel") || subCommandName.equals("setexp") || subCommandName.equals("unlock")) {
            if (context.getArgCount() == 1) {
                // 玩家名称补全
                String partial = context.getStringArgOrDefault(0, "").toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            } else if (subCommandName.equals("unlock") && context.getArgCount() == 2) {
                // 图纸ID补全
                String partial = context.getStringArgOrDefault(1, "").toLowerCase();
                for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
                    if (recipe.getId().toLowerCase().startsWith(partial)) {
                        completions.add(recipe.getId());
                    }
                }
            }
        }

        return completions;
    }
}
