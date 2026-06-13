package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.model.ForgeRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /forgegive 命令 - 给予图纸书
 */
public class ForgeGiveCommand implements CommandExecutor {
    private final GuangDianForge plugin;

    public ForgeGiveCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.forge.admin")) {
            sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /forgegive <玩家> <图纸ID>", NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线!", NamedTextColor.RED));
            return true;
        }
        
        String recipeId = args[1].toLowerCase();
        ForgeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            sender.sendMessage(Component.text("图纸不存在: " + recipeId, NamedTextColor.RED));
            return true;
        }
        
        // 获取 MiniMessage 解析器
        MiniMessage miniMessage = plugin.getMiniMessageParser();
        Component displayComponent = miniMessage.deserialize(recipe.getDisplayName());
        
        ItemStack book = LearnRecipeListener.createRecipeBook(recipe, plugin);
        target.getInventory().addItem(book);
        target.sendMessage(Component.text("你获得了一张图纸: ", NamedTextColor.GREEN).append(displayComponent));
        sender.sendMessage(Component.text("已给予 " + target.getName() + " 图纸: ", NamedTextColor.GREEN).append(displayComponent));
        return true;
    }
}