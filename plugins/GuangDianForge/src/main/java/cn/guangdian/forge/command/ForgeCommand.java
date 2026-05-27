package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.gui.RecipeSelectGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /forge 命令
 */
public class ForgeCommand implements CommandExecutor {
    private final GuangDianForge plugin;

    public ForgeCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行!");
            return true;
        }
        
        if (!player.hasPermission("guangdian.forge.use")) {
            player.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "info" -> {
                    var data = plugin.getPlayerDataManager().get(player.getUniqueId());
                    player.sendMessage(Component.text("=== 锻造信息 ===", NamedTextColor.GOLD));
                    player.sendMessage(Component.text("锻造等级: " + data.getForgeLevel(), NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("锻造经验: " + data.getForgeExp(), NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("总锻造次数: " + data.getTotalForges(), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("成功次数: " + data.getSuccessForges(), NamedTextColor.GREEN));
                    return true;
                }
                case "recipes" -> {
                    player.sendMessage(Component.text("已学图纸:", NamedTextColor.GOLD));
                    var data = plugin.getPlayerDataManager().get(player.getUniqueId());
                    for (var recipe : plugin.getRecipeManager().getAllRecipes()) {
                        if (data.hasLearned(recipe.getId())) {
                            player.sendMessage(Component.text(" - " + recipe.getDisplayName(), NamedTextColor.GREEN));
                        }
                    }
                    return true;
                }
            }
        }
        
        // 打开图纸选择界面
        RecipeSelectGUI gui = new RecipeSelectGUI(plugin, player);
        gui.open();
        return true;
    }
}