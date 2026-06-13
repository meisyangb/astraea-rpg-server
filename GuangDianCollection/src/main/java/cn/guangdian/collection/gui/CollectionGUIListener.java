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
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.PointsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CollectionGUIListener implements Listener {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    private final Map<UUID, String> playerSubmitEntry = new HashMap<>();
    private final Map<UUID, String> playerCurrentCategory = new HashMap<>();
    
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
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerSubmitEntry.remove(uuid);
        playerCurrentCategory.remove(uuid);
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
                    playerCurrentCategory.put(player.getUniqueId(), categoryId);
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
        MiniMessageService mm = plugin.getMiniMessage();
        ItemStack submittedItem = gui.getItem(SUBMIT_SLOT);
        
        if (submittedItem == null || submittedItem.getType() == Material.AIR) {
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<red>请先放入物品！");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        
        Optional<CollectionCategory> catOpt = collectionService.getCategory(entryId.split("\\.")[0]);
        if (catOpt.isEmpty()) {
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<red>配置错误！");
            return;
        }
        
        CollectionEntry entry = catOpt.get().getEntry(entryId);
        if (entry == null) {
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<red>配置错误！");
            return;
        }
        
        // 使用 CollectionService 的 submitItem 方法（已包含直接保存）
        boolean success = collectionService.submitItem(player, entry, submittedItem);
        
        if (!success) {
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<green>提交成功！可以继续提交或关闭界面");
        
        updateSubmitGUI(gui);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String categoryId = playerCurrentCategory.get(player.getUniqueId());
            if (categoryId != null) {
                refreshCategoryGUI(player, categoryId);
            }
        }, 1L);
    }
    
    private void refreshCategoryGUI(Player player, String categoryId) {
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openCategoryGUI(player, categoryId);
        }, 1L);
    }
    
    private void sendMessage(Player player, MiniMessageService mm, String message) {
        if (mm != null) {
            player.sendMessage(mm.colorize(message));
        } else {
            player.sendMessage(Component.text(message));
        }
    }
    
    private String toPlainText(String miniMessageText, MiniMessageService mm) {
        if (mm != null) {
            Component component = mm.colorize(miniMessageText);
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return miniMessageText;
    }
    
    private Component parseMiniMessage(String text, MiniMessageService mm) {
        if (mm != null) {
            return mm.colorize(text);
        }
        return Component.text(text);
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
        MiniMessageService mm = plugin.getMiniMessage();
        
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
            sendMessage(player, mm, msg.replace("{player}", player.getName()));
        }
    }
    
    private void notifyPlayer(Player player, CollectionEntry entry) {
        MiniMessageService mm = plugin.getMiniMessage();
        CollectionCategory category = collectionService.getCategory(entry.getCategoryId()).orElse(null);
        int progress = collectionService.getCategoryProgress(player, entry.getCategoryId());
        int total = category != null ? category.getTotalEntries() : 0;
        
        String entryName = toPlainText(entry.getName(), mm);
        String message = plugin.getConfigManager().getMessage("collected")
            .replace("{item}", entryName)
            .replace("{current}", String.valueOf(progress))
            .replace("{total}", String.valueOf(total));
        
        sendMessage(player, mm, plugin.getConfigManager().getPrefix() + message);
        
        if (category != null && collectionService.isCategoryComplete(player, entry.getCategoryId())) {
            String categoryName = toPlainText(category.getName(), mm);
            String completeMsg = plugin.getConfigManager().getMessage("category-complete")
                .replace("{category}", categoryName);
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + completeMsg);
        }
    }
    
    public void openMainGUI(Player player) {
        MiniMessageService mm = plugin.getMiniMessage();
        String titleRaw = plugin.getConfig().getString("gui.title", "图鉴收集");
        String title = toPlainText(titleRaw, mm);
        Inventory gui = Bukkit.createInventory(new MainGUIHolder(), 27, title);
        
        for (CollectionSet set : collectionService.getSets().values()) {
            ItemStack icon = createSetIcon(player, set, mm);
            gui.setItem(set.getSlot(), icon);
        }
        
        player.openInventory(gui);
    }
    
    private void openSetGUI(Player player, String setId) {
        MiniMessageService mm = plugin.getMiniMessage();
        Optional<CollectionSet> setOpt = collectionService.getSet(setId);
        if (setOpt.isEmpty()) return;
        
        CollectionSet set = setOpt.get();
        String title = toPlainText(set.getName(), mm);
        Inventory gui = Bukkit.createInventory(new SetGUIHolder(setId), 27, title);
        
        for (String categoryId : set.getCategoryIds()) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
            if (catOpt.isPresent()) {
                ItemStack icon = createCategoryIcon(player, catOpt.get(), mm);
                gui.setItem(catOpt.get().getSlot(), icon);
            }
        }
        
        ItemStack backBtn = createBackButton("<red>主菜单", mm);
        gui.setItem(26, backBtn);
        
        player.openInventory(gui);
    }
    
    private void openCategoryGUI(Player player, String categoryId) {
        MiniMessageService mm = plugin.getMiniMessage();
        Optional<CollectionCategory> catOpt = collectionService.getCategory(categoryId);
        if (catOpt.isEmpty()) return;
        
        CollectionCategory category = catOpt.get();
        String title = toPlainText(category.getName(), mm);
        Inventory gui = Bukkit.createInventory(new CategoryGUIHolder(categoryId), 27, title);
        
        for (CollectionEntry entry : category.getEntries().values()) {
            ItemStack icon = createEntryIcon(player, entry, mm);
            gui.setItem(entry.getSlot(), icon);
        }
        
        ItemStack backBtn = createBackButton("<red>上级菜单", mm);
        gui.setItem(26, backBtn);
        
        playerCurrentCategory.put(player.getUniqueId(), categoryId);
        player.openInventory(gui);
    }
    
    private void openSubmitGUI(Player player, CollectionEntry entry) {
        MiniMessageService mm = plugin.getMiniMessage();
        String entryName = toPlainText(entry.getName(), mm);
        String title = "提交: " + entryName;
        Inventory gui = Bukkit.createInventory(new SubmitGUIHolder(entry.getId()), 27, title);
        
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        
        for (int slot : GLASS_SLOTS) {
            gui.setItem(slot, glass);
        }
        
        gui.setItem(SUBMIT_SLOT, new ItemStack(Material.AIR));
        
        ItemStack confirmBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        confirmMeta.displayName(parseMiniMessage("<green>确认提交", mm));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(parseMiniMessage("<gray>点击提交物品", mm));
        confirmMeta.lore(confirmLore);
        confirmBtn.setItemMeta(confirmMeta);
        gui.setItem(CONFIRM_SLOT, confirmBtn);
        
        ItemStack backBtn = createBackButton("<red>返回", mm);
        gui.setItem(BACK_SLOT, backBtn);
        
        playerSubmitEntry.put(player.getUniqueId(), entry.getId());
        player.openInventory(gui);
    }
    
    private ItemStack createBackButton(String text, MiniMessageService mm) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(parseMiniMessage(text, mm));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSetIcon(Player player, CollectionSet set, MiniMessageService mm) {
        ItemStack item = new ItemStack(set.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(parseMiniMessage(set.getName(), mm));
        
        List<Component> lore = new ArrayList<>();
        lore.add(parseMiniMessage("<gray>" + set.getDescription(), mm));
        lore.add(Component.empty());
        
        int totalProgress = 0;
        int totalEntries = 0;
        for (String catId : set.getCategoryIds()) {
            Optional<CollectionCategory> catOpt = collectionService.getCategory(catId);
            if (catOpt.isPresent()) {
                totalProgress += collectionService.getCategoryProgress(player, catId);
                totalEntries += catOpt.get().getTotalEntries();
            }
        }
        
        boolean complete = totalProgress >= totalEntries && totalEntries > 0;
        if (complete) {
            lore.add(parseMiniMessage("<green>进度: " + totalProgress + "/" + totalEntries + " <gold>已完成!", mm));
        } else {
            lore.add(parseMiniMessage("<yellow>进度: " + totalProgress + "/" + totalEntries, mm));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createCategoryIcon(Player player, CollectionCategory category, MiniMessageService mm) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(parseMiniMessage(category.getName(), mm));
        
        int progress = collectionService.getCategoryProgress(player, category.getId());
        int total = category.getTotalEntries();
        boolean complete = progress >= total && total > 0;
        
        List<Component> lore = new ArrayList<>();
        lore.add(parseMiniMessage("<gray>" + category.getDescription(), mm));
        lore.add(Component.empty());
        
        if (complete) {
            lore.add(parseMiniMessage("<green>进度: " + progress + "/" + total + " <gold>已完成!", mm));
        } else {
            lore.add(parseMiniMessage("<yellow>进度: " + progress + "/" + total, mm));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createEntryIcon(Player player, CollectionEntry entry, MiniMessageService mm) {
        PlayerCollectionData data = collectionService.getPlayerData(player);
        boolean collected = data.hasCollected(entry.getId());
        
        Material material = collected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        if (entry.isItemEntry() && entry.getMaterial() != null) {
            material = entry.getMaterial();
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(parseMiniMessage(entry.getName(), mm));
        
        List<Component> lore = new ArrayList<>();
        
        if (collected) {
            lore.add(parseMiniMessage("<green>状态: 已收集", mm));
            
            if (entry.getReward() != null && entry.getReward().hasReward()) {
                lore.add(Component.empty());
                lore.add(parseMiniMessage("<gray>奖励已领取", mm));
            }
        } else {
            lore.add(parseMiniMessage("<red>状态: 未收集", mm));
            lore.add(parseMiniMessage("<gray>提示: " + entry.getHint(), mm));
            
            if (entry.getReward() != null && entry.getReward().hasReward()) {
                lore.add(Component.empty());
                lore.add(parseMiniMessage("<gold>奖励:", mm));
                
                if (entry.getReward().getMoney() > 0) {
                    lore.add(parseMiniMessage("<yellow>  金币: " + entry.getReward().getMoney(), mm));
                }
                if (entry.getReward().getPoints() > 0) {
                    lore.add(parseMiniMessage("<aqua>  点券: " + entry.getReward().getPoints(), mm));
                }
            }
            
            lore.add(Component.empty());
            lore.add(parseMiniMessage("<green>点击提交", mm));
        }
        
        meta.lore(lore);
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
