package cn.guangdian.battlepass.gui;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.BattlePassLevel;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.battlepass.model.Season;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BattlePassGUI implements Listener {
    
    private final GuangDianBattlePass plugin;
    
    public BattlePassGUI(GuangDianBattlePass plugin) {
        this.plugin = plugin;
    }
    
    public void openBattlePass(Player player) {
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) {
            player.sendMessage(Component.text("当前没有进行中的赛季！").color(NamedTextColor.RED));
            return;
        }
        
        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
        if (bp == null) {
            player.sendMessage(Component.text("无法加载战令数据！").color(NamedTextColor.RED));
            return;
        }
        
        int totalPages = (int) Math.ceil(season.getMaxLevel() / 7.0);
        openPage(player, 1, season, bp, totalPages);
    }
    
    private void openPage(Player player, int page, Season season, PlayerBattlePass bp, int totalPages) {
        int slots = 54;
        String title = "战令 - " + season.getSeasonName() + " (第" + page + "/" + totalPages + "页)";
        Inventory inventory = Bukkit.createInventory(new BattlePassHolder(page), slots, title);
        
        int startLevel = (page - 1) * 7 + 1;
        int endLevel = Math.min(startLevel + 6, season.getMaxLevel());
        
        for (int i = startLevel; i <= endLevel; i++) {
            int slot = (i - startLevel) * 7 + 1;
            addLevelItem(inventory, slot, i, season, bp);
        }
        
        ItemStack infoItem = createInfoItem(bp, season);
        inventory.setItem(4, infoItem);
        
        if (page > 1) {
            inventory.setItem(45, createNavigationItem(Material.ARROW, "上一页", page - 1));
        }
        
        if (page < totalPages) {
            inventory.setItem(53, createNavigationItem(Material.ARROW, "下一页", page + 1));
        }
        
        if (!bp.isPremium()) {
            inventory.setItem(49, createPurchaseItem());
        }
        
        player.openInventory(inventory);
    }
    
    private void addLevelItem(Inventory inventory, int slot, int level, Season season, PlayerBattlePass bp) {
        BattlePassLevel bpLevel = season.getLevel(level);
        if (bpLevel == null) return;
        
        boolean isCurrentLevel = bp.getLevel() == level;
        boolean isUnlocked = bp.getLevel() >= level;
        boolean freeClaimed = bp.hasClaimedFreeReward(level);
        boolean premiumClaimed = bp.hasClaimedPremiumReward(level);
        
        Material material;
        NamedTextColor color;
        
        if (isCurrentLevel) {
            material = Material.GOLD_BLOCK;
            color = NamedTextColor.GOLD;
        } else if (isUnlocked) {
            material = Material.EMERALD_BLOCK;
            color = NamedTextColor.GREEN;
        } else {
            material = Material.REDSTONE_BLOCK;
            color = NamedTextColor.RED;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("等级 " + level).color(color).decoration(TextDecoration.BOLD, true));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("所需经验: " + bpLevel.getRequiredExp()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        
        if (bpLevel.getFreeReward() != null) {
            String status = freeClaimed ? "§c[已领取]" : (isUnlocked ? "§a[可领取]" : "§7[未解锁]");
            lore.add(Component.text("免费奖励: " + status).color(NamedTextColor.YELLOW));
        }
        
        if (bpLevel.getPremiumReward() != null) {
            String status = premiumClaimed ? "§c[已领取]" : (isUnlocked && bp.isPremium() ? "§a[可领取]" : "§7[未解锁]");
            lore.add(Component.text("高级奖励: " + status).color(NamedTextColor.LIGHT_PURPLE));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        inventory.setItem(slot, item);
        
        if (bpLevel.getFreeReward() != null) {
            ItemStack freeItem = createRewardItem(bpLevel.getFreeReward(), "免费", isUnlocked && !freeClaimed);
            inventory.setItem(slot + 1, freeItem);
        }
        
        if (bpLevel.getPremiumReward() != null) {
            ItemStack premiumItem = createRewardItem(bpLevel.getPremiumReward(), "高级", isUnlocked && bp.isPremium() && !premiumClaimed);
            inventory.setItem(slot + 2, premiumItem);
        }
    }
    
    private ItemStack createRewardItem(cn.guangdian.battlepass.model.BattlePassReward reward, String type, boolean canClaim) {
        Material material = canClaim ? Material.CHEST : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(type + "奖励").color(NamedTextColor.AQUA));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(reward.getDisplayName()).color(NamedTextColor.WHITE));
        if (canClaim) {
            lore.add(Component.text("点击领取").color(NamedTextColor.GREEN));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createInfoItem(PlayerBattlePass bp, Season season) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("战令信息").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("赛季: " + season.getSeasonName()).color(NamedTextColor.YELLOW));
        lore.add(Component.text("等级: " + bp.getLevel() + "/" + season.getMaxLevel()).color(NamedTextColor.GREEN));
        lore.add(Component.text("经验: " + bp.getCurrentExp()).color(NamedTextColor.AQUA));
        lore.add(Component.text("状态: " + (bp.isPremium() ? "§d高级战令" : "§e免费战令")).color(NamedTextColor.WHITE));
        lore.add(Component.text("剩余时间: " + season.getRemainingDays() + "天").color(NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createNavigationItem(Material material, String name, int page) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPurchaseItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("购买高级战令").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("点击购买高级战令").color(NamedTextColor.GREEN));
        lore.add(Component.text("解锁所有高级奖励").color(NamedTextColor.YELLOW));
        
        int price = plugin.getConfig().getInt("premium-price", 1000);
        lore.add(Component.text("价格: " + price + " 点券").color(NamedTextColor.GOLD));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BattlePassHolder)) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        int slot = event.getSlot();
        BattlePassHolder holder = (BattlePassHolder) event.getInventory().getHolder();
        int currentPage = holder.getPage();
        
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) return;
        
        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
        if (bp == null) return;
        
        if (slot == 45 && currentPage > 1) {
            int totalPages = (int) Math.ceil(season.getMaxLevel() / 7.0);
            openPage(player, currentPage - 1, season, bp, totalPages);
            return;
        }
        
        if (slot == 53) {
            int totalPages = (int) Math.ceil(season.getMaxLevel() / 7.0);
            if (currentPage < totalPages) {
                openPage(player, currentPage + 1, season, bp, totalPages);
            }
            return;
        }
        
        if (slot == 49 && !bp.isPremium()) {
            int price = plugin.getConfig().getInt("premium-price", 1000);
            if (plugin.takePoints(player, price)) {
                bp.setPremium(true);
                player.sendMessage(Component.text("成功购买高级战令！").color(NamedTextColor.GREEN));
                openBattlePass(player);
            } else {
                player.sendMessage(Component.text("点券不足！需要 " + price + " 点券").color(NamedTextColor.RED));
            }
            return;
        }
        
        int startLevel = (currentPage - 1) * 7 + 1;
        int levelSlot = (slot - 1) / 7;
        int level = startLevel + levelSlot;
        
        if (level >= startLevel && level <= Math.min(startLevel + 6, season.getMaxLevel())) {
            int offset = (slot - 1) % 7;
            
            if (offset == 1) {
                if (bp.canClaimFreeReward(level)) {
                    if (plugin.getBattlePassManager().claimFreeReward(player, level)) {
                        player.sendMessage(Component.text("成功领取等级 " + level + " 的免费奖励！").color(NamedTextColor.GREEN));
                        openBattlePass(player);
                    }
                }
            } else if (offset == 2) {
                if (bp.canClaimPremiumReward(level)) {
                    if (plugin.getBattlePassManager().claimPremiumReward(player, level)) {
                        player.sendMessage(Component.text("成功领取等级 " + level + " 的高级奖励！").color(NamedTextColor.GREEN));
                        openBattlePass(player);
                    }
                }
            }
        }
    }
    
    private static class BattlePassHolder implements InventoryHolder {
        private final int page;
        
        public BattlePassHolder(int page) {
            this.page = page;
        }
        
        public int getPage() {
            return page;
        }
        
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
