package cn.guangdian.collection.gui;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import cn.guangdian.collection.model.CollectionSet;
import cn.guangdian.collection.model.PlayerCollectionData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.PointsService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CollectionGUIListener implements Listener {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    private final Map<UUID, String> playerSubmitEntry = new HashMap<>();
    
    private static final int SUBMIT_SLOT = 13;
    private static final int CONFIRM_SLOT = 15;
    private static final int BACK_SLOT = 17;
    private static final int[] GLASS_SLOTS = {3, 4, 5, 12, 14, 21, 22, 23};
    
    public CollectionGUIListener(GuangDianCollection plugin, CollectionService collectionService) {
        this.plugin = plugin;
        this.collectionService = collectionService;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;
        
        if (holder instanceof MainGUIHolder) {
            event.setCancelled(true);
            handleMainGUIClick(player, event.getSlot());
        } else if (holder instanceof SetGUIHolder setHolder) {
            event.setCancelled(true);
            handleSetGUIClick(player, setHolder.getSetId(), event.getSlot());
        } else if (holder instanceof CategoryGUIHolder categoryHolder) {
            event.setCancelled(true);
            handleCategoryGUIClick(player, categoryHolder.getCategoryId(), event.getSlot());
        } else if (holder instanceof SubmitGUIHolder submitHolder) {
            handleSubmitGUIClick(player, submitHolder.getEntryId(), event);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SubmitGUIHolder) {
            returnItemsToPlayer(player, event.getInventory());
            playerSubmitEntry.remove(player.getUniqueId());
        }
    }
    
    private void handleMainGUIClick(Player player, int slot) {
        for (CollectionSet set : collectionService.getSets().values()) {
            if (set.getSlot() == slot) {
                openSetGUI(player, set.getId());
                return;
            }
        }
    }
    
    private void handleSetGUIClick(Player player, String setId, int slot) {
        if (slot == 26) {
            openMainGUI(player);
            return;
        }
        
        Optional<CollectionSet> setOpt = collectionService.getSet(setId);
        if (setOpt.isEmpty()) return;
        
        CollectionSet set = setOpt.get();
        for (String categoryId : set.getCategoryIds()) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
            if (catOpt.isPresent() && catOpt.get().getSlot() == slot) {
                openCategoryGUI(player, categoryId);
                return;
            }
        }
    }
    
    private void handleCategoryGUIClick(Player player, String categoryId, int slot) {
        if (slot == 26) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
            if (catOpt.isPresent()) {
                openSetGUI(player, catOpt.get().getSetId());
            } else {
                openMainGUI(player);
            }
            return;
        }
        
        Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
        if (catOpt.isEmpty()) return;
        
        CollectionCategory category = catOpt.get();
        for (CollectionEntry entry : category.getEntries().values()) {
            if (entry.getSlot() == slot) {
                PlayerCollectionData data = collectionService.getPlayerData(player);
                if (!data.hasCollected(entry.getId())) {
                    openSubmitGUI(player, entry);
                }
                return;
            }
        }
    }
    
    private void handleSubmitGUIClick(Player player, String entryId, InventoryClickEvent event) {
        int slot = event.getSlot();
        
        if (slot == SUBMIT_SLOT) {
            event.setCancelled(false);
            return;
        }
        
        if (event.getClickedInventory() == player.getInventory()) {
            event.setCancelled(false);
            return;
        }
        
        if (slot == CONFIRM_SLOT) {
            event.setCancelled(true);
            handleConfirmSubmit(player, entryId, event.getInventory());
            return;
        }
        
        if (slot == BACK_SLOT) {
            event.setCancelled(true);
            returnItemsToPlayer(player, event.getInventory());
            player.closeInventory();
            String categoryId = entryId.split("\\.")[0];
            openCategoryGUI(player, categoryId);
            return;
        }
        
        for (int glassSlot : GLASS_SLOTS) {
            if (slot == glassSlot) {
                event.setCancelled(true);
                return;
            }
        }
        
        event.setCancelled(true);
    }
    
    private void handleConfirmSubmit(Player player, String entryId, Inventory gui) {
        ItemStack submittedItem = gui.getItem(SUBMIT_SLOT);
        
        if (submittedItem == null || submittedItem.getType() == Material.AIR) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + "请先放入物品！", net.kyori.adventure.text.format.NamedTextColor.RED));
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        
        Optional<CollectionCategory> catOpt = collectionService.getCategory(entryId.split("\\.")[0]);
        if (catOpt.isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + "配置错误！", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        
        CollectionEntry entry = catOpt.get().getEntry(entryId);
        if (entry == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + "配置错误！", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        
        PlayerCollectionData data = collectionService.getPlayerData(player);
        
        if (data.hasCollected(entry.getId())) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + "该物品已收集！", net.kyori.adventure.text.format.NamedTextColor.RED));
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        
        if (!collectionService.matchesEntry(entry, submittedItem)) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + "物品不匹配！需要: " + entry.getName(), net.kyori.adventure.text.format.NamedTextColor.RED));
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        
        submittedItem.setAmount(submittedItem.getAmount() - 1);
        
        data.collectItem(entry.getId());
        
        if (entry.getReward() != null) {
            giveReward(player, entry.getReward());
        }
        
        notifyPlayer(player, entry);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        
        player.sendMessage(net.kyori.adventure.text.Component.text(
            plugin.getConfigManager().getPrefix() + "提交成功！可以继续提交或关闭界面", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        
        updateSubmitGUI(gui);
    }
    
    private void updateSubmitGUI(Inventory gui) {
        ItemStack submittedItem = gui.getItem(SUBMIT_SLOT);
        if (submittedItem == null || submittedItem.getAmount() <= 0) {
            gui.setItem(SUBMIT_SLOT, new ItemStack(Material.AIR));
        }
    }
    
    private void returnItemsToPlayer(Player player, Inventory inventory) {
        ItemStack item = inventory.getItem(SUBMIT_SLOT);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values().forEach(dropItem -> 
                player.getWorld().dropItem(player.getLocation(), dropItem));
        }
    }
    
    private void playSound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }
    
    private void giveReward(Player player, CollectionEntry.EntryReward reward) {
        RPGCore rpgCore = RPGCore.getInstance();
        
        if (reward.getMoney() > 0 && rpgCore != null) {
            ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
            if (externalServices != null && externalServices.isVaultEnabled()) {
                externalServices.deposit(player, reward.getMoney());
            }
        }
        
        if (reward.getPoints() > 0 && rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            PointsService pointsService = registry.getService(PointsService.class);
            if (pointsService != null) {
                pointsService.addBalance(player.getUniqueId(), reward.getPoints(), "图鉴收集奖励");
            }
        }
        
        for (String cmd : reward.getCommands()) {
            String parsedCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        for (String msg : reward.getMessages()) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                msg.replace("{player}", player.getName())));
        }
    }
    
    private void notifyPlayer(Player player, CollectionEntry entry) {
        CollectionCategory category = collectionService.getCategory(entry.getCategoryId()).orElse(null);
        int progress = collectionService.getCategoryProgress(player, entry.getCategoryId());
        int total = category != null ? category.getTotalEntries() : 0;
        
        String message = plugin.getConfigManager().getMessage("collected")
            .replace("{item}", entry.getName())
            .replace("{current}", String.valueOf(progress))
            .replace("{total}", String.valueOf(total));
        
        player.sendMessage(net.kyori.adventure.text.Component.text(
            plugin.getConfigManager().getPrefix() + message));
        
        if (category != null && collectionService.isCategoryComplete(player, entry.getCategoryId())) {
            String completeMsg = plugin.getConfigManager().getMessage("category-complete")
                .replace("{category}", category.getName());
            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getPrefix() + completeMsg)
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        }
    }
    
    public void openMainGUI(Player player) {
        String title = plugin.getConfig().getString("gui.title", "图鉴收集");
        Inventory gui = Bukkit.createInventory(new MainGUIHolder(), 27, title);
        
        for (CollectionSet set : collectionService.getSets().values()) {
            ItemStack icon = createSetIcon(player, set);
            gui.setItem(set.getSlot(), icon);
        }
        
        player.openInventory(gui);
    }
    
    private void openSetGUI(Player player, String setId) {
        Optional<CollectionSet> setOpt = collectionService.getSet(setId);
        if (setOpt.isEmpty()) return;
        
        CollectionSet set = setOpt.get();
        String title = set.getName();
        Inventory gui = Bukkit.createInventory(new SetGUIHolder(setId), 27, title);
        
        for (String categoryId : set.getCategoryIds()) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
            if (catOpt.isPresent()) {
                ItemStack icon = createCategoryIcon(player, catOpt.get());
                gui.setItem(catOpt.get().getSlot(), icon);
            }
        }
        
        ItemStack backBtn = createBackButton("主菜单");
        gui.setItem(26, backBtn);
        
        player.openInventory(gui);
    }
    
    private void openCategoryGUI(Player player, String categoryId) {
        Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
        if (catOpt.isEmpty()) return;
        
        CollectionCategory category = catOpt.get();
        String title = category.getName();
        Inventory gui = Bukkit.createInventory(new CategoryGUIHolder(categoryId), 27, title);
        
        for (CollectionEntry entry : category.getEntries().values()) {
            ItemStack icon = createEntryIcon(player, entry);
            gui.setItem(entry.getSlot(), icon);
        }
        
        ItemStack backBtn = createBackButton("上级菜单");
        gui.setItem(26, backBtn);
        
        player.openInventory(gui);
    }
    
    private void openSubmitGUI(Player player, CollectionEntry entry) {
        String title = "提交: " + entry.getName();
        Inventory gui = Bukkit.createInventory(new SubmitGUIHolder(entry.getId()), 27, title);
        
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(net.kyori.adventure.text.Component.text(" "));
        glass.setItemMeta(glassMeta);
        
        for (int slot : GLASS_SLOTS) {
            gui.setItem(slot, glass);
        }
        
        gui.setItem(SUBMIT_SLOT, new ItemStack(Material.AIR));
        
        ItemStack confirmBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        confirmMeta.displayName(net.kyori.adventure.text.Component.text("确认提交", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        List<net.kyori.adventure.text.Component> confirmLore = new ArrayList<>();
        confirmLore.add(net.kyori.adventure.text.Component.text("点击提交物品", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        confirmMeta.lore(confirmLore);
        confirmBtn.setItemMeta(confirmMeta);
        gui.setItem(CONFIRM_SLOT, confirmBtn);
        
        ItemStack backBtn = createBackButton("返回");
        gui.setItem(BACK_SLOT, backBtn);
        
        playerSubmitEntry.put(player.getUniqueId(), entry.getId());
        player.openInventory(gui);
    }
    
    private ItemStack createBackButton(String text) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(text, net.kyori.adventure.text.format.NamedTextColor.RED));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSetIcon(Player player, CollectionSet set) {
        ItemStack item = new ItemStack(set.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text(set.getName()));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text(set.getDescription())
            .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.empty());
        
        int totalProgress = 0;
        int totalEntries = 0;
        for (String catId : set.getCategoryIds()) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(catId);
            if (catOpt.isPresent()) {
                totalProgress += collectionService.getCategoryProgress(player, catId);
                totalEntries += catOpt.get().getTotalEntries();
            }
        }
        
        boolean complete = totalProgress >= totalEntries;
        lore.add(net.kyori.adventure.text.Component.text("进度: " + totalProgress + "/" + totalEntries)
            .color(complete ? net.kyori.adventure.text.format.NamedTextColor.GREEN : 
                           net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        
        if (complete) {
            lore.add(net.kyori.adventure.text.Component.text("已完成!")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createCategoryIcon(Player player, CollectionCategory category) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text(category.getName()));
        
        int progress = collectionService.getCategoryProgress(player, category.getId());
        int total = category.getTotalEntries();
        boolean complete = progress >= total;
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text(category.getDescription())
            .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text("进度: " + progress + "/" + total)
            .color(complete ? net.kyori.adventure.text.format.NamedTextColor.GREEN : 
                           net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        
        if (complete) {
            lore.add(net.kyori.adventure.text.Component.text("已完成!")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createEntryIcon(Player player, CollectionEntry entry) {
        PlayerCollectionData data = collectionService.getPlayerData(player);
        boolean collected = data.hasCollected(entry.getId());
        
        Material material = collected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        if (entry.isItemEntry() && entry.getMaterial() != null) {
            material = entry.getMaterial();
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (collected) {
            meta.displayName(net.kyori.adventure.text.Component.text(entry.getName()));
            
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("状态: 已收集")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            
            if (entry.getReward() != null && entry.getReward().hasReward()) {
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(net.kyori.adventure.text.Component.text("奖励已领取")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
            
            meta.lore(lore);
        } else {
            meta.displayName(net.kyori.adventure.text.Component.text(entry.getName()));
            
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("状态: 未收集")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            lore.add(net.kyori.adventure.text.Component.text("提示: " + entry.getHint())
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            
            if (entry.getReward() != null && entry.getReward().hasReward()) {
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(net.kyori.adventure.text.Component.text("奖励:")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                
                if (entry.getReward().getMoney() > 0) {
                    lore.add(net.kyori.adventure.text.Component.text(
                        "  金币: " + entry.getReward().getMoney())
                        .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                }
                if (entry.getReward().getPoints() > 0) {
                    lore.add(net.kyori.adventure.text.Component.text(
                        "  点券: " + entry.getReward().getPoints())
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                }
            }
            
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("点击提交")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
            
            meta.lore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public static class MainGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
    
    public static class SetGUIHolder implements InventoryHolder {
        private final String setId;
        
        public SetGUIHolder(String setId) {
            this.setId = setId;
        }
        
        public String getSetId() { return setId; }
        
        @Override
        public Inventory getInventory() { return null; }
    }
    
    public static class CategoryGUIHolder implements InventoryHolder {
        private final String categoryId;
        
        public CategoryGUIHolder(String categoryId) {
            this.categoryId = categoryId;
        }
        
        public String getCategoryId() { return categoryId; }
        
        @Override
        public Inventory getInventory() { return null; }
    }
    
    public static class SubmitGUIHolder implements InventoryHolder {
        private final String entryId;
        
        public SubmitGUIHolder(String entryId) {
            this.entryId = entryId;
        }
        
        public String getEntryId() { return entryId; }
        
        @Override
        public Inventory getInventory() { return null; }
    }
}
