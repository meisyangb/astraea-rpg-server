package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.PlayerForgeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * /forgeadmin 命令 - 管理员命令
 */
public class ForgeAdminCommand implements CommandExecutor {
    private final GuangDianForge plugin;

    public ForgeAdminCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.forge.admin")) {
            sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(Component.text("=== 锻造管理 ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/forgeadmin reload - 重载配置", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/forgeadmin setlevel <玩家> <等级> - 设置锻造等级", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/forgeadmin setexp <玩家> <经验> - 设置锻造经验", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/forgeadmin unlock <玩家> <图纸ID> - 解锁图纸", NamedTextColor.YELLOW));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getRecipeManager().loadRecipes();
                sender.sendMessage(Component.text("配置已重载!", NamedTextColor.GREEN));
            }
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /forgeadmin setlevel <玩家> <等级>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("等级必须是数字!", NamedTextColor.RED));
                    return true;
                }
                PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                data.setForgeLevel(level);
                plugin.getPlayerDataManager().save(data);
                sender.sendMessage(Component.text("已设置 " + target.getName() + " 的锻造等级为 " + level, NamedTextColor.GREEN));
            }
            case "setexp" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /forgeadmin setexp <玩家> <经验>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                long exp;
                try {
                    exp = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("经验必须是数字!", NamedTextColor.RED));
                    return true;
                }
                PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                data.setForgeExp(exp);
                plugin.getPlayerDataManager().save(data);
                sender.sendMessage(Component.text("已设置 " + target.getName() + " 的锻造经验为 " + exp, NamedTextColor.GREEN));
            }
            case "unlock" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /forgeadmin unlock <玩家> <图纸ID>", NamedTextColor.RED));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String recipeId = args[2].toLowerCase();
                PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                data.learnRecipe(recipeId);
                plugin.getPlayerDataManager().save(data);
                sender.sendMessage(Component.text("已为 " + target.getName() + " 解锁图纸: " + recipeId, NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("未知命令!", NamedTextColor.RED));
        }
        return true;
    }
}