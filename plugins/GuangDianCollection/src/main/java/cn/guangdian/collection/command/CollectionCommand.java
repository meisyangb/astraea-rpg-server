package cn.guangdian.collection.command;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    
    public CollectionCommand(GuangDianCollection plugin, CollectionService collectionService) {
        this.plugin = plugin;
        this.collectionService = collectionService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                return true;
            }
            openMainGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "rewards":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                    return true;
                }
                openRewardsGUI(player);
                return true;
                
            case "claim":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§c用法: /collection claim <奖励ID>"));
                    return true;
                }
                claimReward(player, args[1]);
                return true;
                
            case "reload":
                if (!sender.hasPermission("collection.admin")) {
                    sender.sendMessage(Component.text(plugin.getConfigManager().getMessage("no-permission")).color(NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().reload();
                collectionService.reloadData();
                sender.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§a配置已重新加载"));
                return true;
                
            case "stats":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行").color(NamedTextColor.RED));
                    return true;
                }
                showStats(player);
                return true;
                
            default:
                sender.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§c未知命令"));
                return true;
        }
    }
    
    private void openMainGUI(Player player) {
        String title = plugin.getConfig().getString("gui.title", "图鉴收集");
        Inventory gui = Bukkit.createInventory(new CollectionGUIHolder(), 54, title);
        
        for (CollectionCategory category : collectionService.getCategories().values()) {
            ItemStack icon = createCategoryIcon(player, category);
            gui.setItem(category.getSlot(), icon);
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createCategoryIcon(Player player, CollectionCategory category) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        int progress = collectionService.getCategoryProgress(player, category.getId());
        int total = category.getTotalEntries();
        boolean complete = progress >= total;
        
        meta.displayName(Component.text(category.getName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(category.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("进度: " + progress + "/" + total)
            .color(complete ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        
        if (complete) {
            lore.add(Component.text("已完成!").color(NamedTextColor.GOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void openRewardsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(new RewardsGUIHolder(), 27, "可领取奖励");
        
        int slot = 0;
        for (CollectionReward reward : collectionService.getAvailableRewards(player)) {
            if (slot >= 27) break;
            
            ItemStack item = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = item.getItemMeta();
            
            meta.displayName(Component.text(reward.getName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(reward.getDescription()).color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            
            if (reward.getMoney() > 0) {
                lore.add(Component.text("金币: " + reward.getMoney()).color(NamedTextColor.GOLD));
            }
            if (reward.getPoints() > 0) {
                lore.add(Component.text("点券: " + reward.getPoints()).color(NamedTextColor.AQUA));
            }
            
            lore.add(Component.empty());
            lore.add(Component.text("点击领取").color(NamedTextColor.GREEN));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            gui.setItem(slot++, item);
        }
        
        if (slot == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("暂无可领取奖励").color(NamedTextColor.RED));
            empty.setItemMeta(meta);
            gui.setItem(13, empty);
        }
        
        player.openInventory(gui);
    }
    
    private void claimReward(Player player, String rewardId) {
        boolean success = collectionService.claimReward(player, rewardId);
        
        if (!success) {
            player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§c无法领取该奖励"));
        }
    }
    
    private void showStats(Player player) {
        int totalItems = collectionService.getTotalItemsCollected(player.getUniqueId());
        int totalKills = collectionService.getTotalKills(player.getUniqueId());
        
        player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + "§6===== 图鉴统计 ====="));
        player.sendMessage(Component.text("§e收集物品总数: §f" + totalItems));
        player.sendMessage(Component.text("§e击杀怪物总数: §f" + totalKills));
        
        for (CollectionCategory category : collectionService.getCategories().values()) {
            int progress = collectionService.getCategoryProgress(player, category.getId());
            int total = category.getTotalEntries();
            String status = progress >= total ? "§a已完成" : "§e" + progress + "/" + total;
            player.sendMessage(Component.text("§7- " + category.getName() + ": " + status));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("rewards", "claim", "reload", "stats");
            return filterStartsWith(subCommands, args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            if (sender instanceof Player player) {
                List<String> rewardIds = new ArrayList<>();
                for (CollectionReward reward : collectionService.getAvailableRewards(player)) {
                    rewardIds.add(reward.getId());
                }
                return filterStartsWith(rewardIds, args[1]);
            }
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
    
    public static class CollectionGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
    
    public static class RewardsGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}
