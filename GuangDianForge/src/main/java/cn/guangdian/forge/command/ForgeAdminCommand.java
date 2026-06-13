package cn.guangdian.forge.command;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /forgeadmin 命令 - 锻造系统管理
 */
public class ForgeAdminCommand implements CommandExecutor {
    private final GuangDianForge plugin;

    public ForgeAdminCommand(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.forge.admin")) {
            sender.sendMessage(Component.text("没有管理员权限!", NamedTextColor.RED));
            return true;
        }
        
        // 无参数时显示帮助
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        
        // 处理子命令
        switch (args[0].toLowerCase()) {
            case "help", "?" -> {
                showAdminHelp(sender);
                return true;
            }
            case "give" -> {
                handleGive(sender, args);
                return true;
            }
            case "setlevel", "level" -> {
                handleSetLevel(sender, args);
                return true;
            }
            case "addexp", "exp" -> {
                handleAddExp(sender, args);
                return true;
            }
            case "reset" -> {
                handleReset(sender, args);
                return true;
            }
            case "reload" -> {
                handleReload(sender);
                return true;
            }
            case "stats" -> {
                handleStats(sender);
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("未知命令: " + args[0], NamedTextColor.RED));
                sender.sendMessage(Component.text("使用 /forgeadmin help 查看帮助", NamedTextColor.GRAY));
                return true;
            }
        }
    }
    
    /**
     * 显示管理帮助
     */
    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("═══ 锻造管理命令 ═══", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /forgeadmin give <玩家> <图纸> [数量]  - 给予图纸", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /forgeadmin setlevel <玩家> <等级>     - 设置等级", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /forgeadmin addexp <玩家> <经验>       - 添加经验", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /forgeadmin reset <玩家>               - 重置数据", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /forgeadmin reload                     - 重载配置", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /forgeadmin stats                      - 查看统计", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("═════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
    }
    
    /**
     * 给予图纸
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /forgeadmin give <玩家> <图纸ID> [数量]", NamedTextColor.RED));
            return;
        }
        
        // 先尝试在线玩家（解决新玩家首次加入时 hasPlayedBefore 返回 false 的问题）
        Player onlinePlayer = Bukkit.getPlayer(args[1]);
        OfflinePlayer target;
        if (onlinePlayer != null) {
            target = onlinePlayer;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage(Component.text("玩家不存在: " + args[1], NamedTextColor.RED));
                return;
            }
        }
        
        String recipeId = args[2];
        ForgeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            sender.sendMessage(Component.text("未知的图纸ID: " + recipeId, NamedTextColor.RED));
            return;
        }
        
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage(Component.text("数量必须在 1-64 之间", NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("无效的数量: " + args[3], NamedTextColor.RED));
                return;
            }
        }
        
        // 创建图纸物品
        ItemStack blueprintItem = LearnRecipeListener.createRecipeBook(recipe, plugin);
        blueprintItem.setAmount(amount);
        
        // 获取 MiniMessage 解析器
        MiniMessage miniMessage = plugin.getMiniMessageParser();
        Component displayComponent = miniMessage.deserialize(recipe.getDisplayName());
        
        // 给予玩家（onlinePlayer 已在上面获取，直接复用）
        if (onlinePlayer != null) {
            // 玩家在线，直接给予物品
            var leftover = onlinePlayer.getInventory().addItem(blueprintItem);
            if (!leftover.isEmpty()) {
                // 背包满了，掉落在地上
                for (ItemStack remaining : leftover.values()) {
                    onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), remaining);
                }
                sender.sendMessage(Component.text("背包已满，部分图纸已掉落在地上", NamedTextColor.YELLOW));
            }
            sender.sendMessage(Component.text("已给予 " + onlinePlayer.getName() + " " + amount + " 张图纸: ", NamedTextColor.GREEN).append(displayComponent));
            onlinePlayer.sendMessage(Component.text("管理员给予了您 " + amount + " 张图纸: ", NamedTextColor.AQUA).append(displayComponent));
        } else {
            // 玩家离线，无法给予物品
            sender.sendMessage(Component.text("玩家 " + target.getName() + " 不在线，无法给予物品", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("提示: 图纸只能给予在线玩家", NamedTextColor.GRAY));
        }
    }
    
    /**
     * 设置锻造等级
     */
    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /forgeadmin setlevel <玩家> <等级>", NamedTextColor.RED));
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("玩家不存在: " + args[1], NamedTextColor.RED));
            return;
        }
        
        String levelStr = args[2];
        int level;
        
        if (levelStr.equalsIgnoreCase("max")) {
            // 设置为最高等级
            var thresholds = getLevelThresholds();
            level = thresholds.keySet().stream().mapToInt(Integer::intValue).max().orElse(100);
        } else {
            try {
                level = Integer.parseInt(levelStr);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("无效的等级: " + levelStr, NamedTextColor.RED));
                return;
            }
        }
        
        PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        data.setForgeLevel(level);
        plugin.getPlayerDataManager().save(data);
        
        sender.sendMessage(Component.text("已设置 " + target.getName() + " 的锻造等级为 " + level, NamedTextColor.GREEN));
        
        // 通知在线玩家
        Player onlinePlayer = target.getPlayer();
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(Component.text("管理员已将你的锻造等级设置为 " + level, NamedTextColor.AQUA));
        }
    }
    
    /**
     * 添加锻造经验
     */
    private void handleAddExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法: /forgeadmin addexp <玩家> <经验值>", NamedTextColor.RED));
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("玩家不存在: " + args[1], NamedTextColor.RED));
            return;
        }
        
        long exp;
        try {
            exp = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("无效的经验值: " + args[2], NamedTextColor.RED));
            return;
        }
        
        PlayerForgeData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        plugin.getPlayerDataManager().addExp(data, exp);
        plugin.getPlayerDataManager().save(data);
        
        sender.sendMessage(Component.text("已给 " + target.getName() + " 添加 " + exp + " 锻造经验", NamedTextColor.GREEN));
        
        // 通知在线玩家
        Player onlinePlayer = target.getPlayer();
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(Component.text("管理员已给你添加 " + exp + " 锻造经验", NamedTextColor.AQUA));
        }
    }
    
    /**
     * 重置玩家数据
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /forgeadmin reset <玩家>", NamedTextColor.RED));
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("玩家不存在: " + args[1], NamedTextColor.RED));
            return;
        }
        
        // 重置数据
        PlayerForgeData newData = new PlayerForgeData(target.getUniqueId());
        plugin.getPlayerDataManager().save(newData);
        
        sender.sendMessage(Component.text("已重置 " + target.getName() + " 的锻造数据", NamedTextColor.GREEN));
        
        // 通知在线玩家
        Player onlinePlayer = target.getPlayer();
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(Component.text("管理员已重置你的锻造数据", NamedTextColor.AQUA));
        }
    }
    
    /**
     * 重载配置
     */
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getRecipeManager().loadRecipes();
        
        sender.sendMessage(Component.text("配置和图纸已重载!", NamedTextColor.GREEN));
    }
    
    /**
     * 查看统计信息
     */
    private void handleStats(CommandSender sender) {
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("═══ 锻造系统统计 ═══", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        
        var recipes = plugin.getRecipeManager().getAllRecipes();
        sender.sendMessage(Component.text("  总图纸数: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(recipes.size()), NamedTextColor.AQUA)));
        
        // TODO: 添加更多统计信息
        
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("═══════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("", NamedTextColor.WHITE));
    }
    
    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, Long> getLevelThresholds() {
        java.util.Map<Integer, Long> thresholds = new java.util.TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("level-thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    long exp = section.getLong(key);
                    thresholds.put(level, exp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return thresholds;
    }
}
