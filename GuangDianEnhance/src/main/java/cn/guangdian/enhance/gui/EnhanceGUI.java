package cn.guangdian.enhance.gui;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.data.EnhanceResult;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnhanceGUI implements InventoryHolder, Listener {

    private static final String TITLE = "强化系统";
    private static final int SIZE = 27;
    
    private final GuangDianEnhance plugin;
    private final EnhanceManager enhanceManager;
    private final EnhanceConfig config;
    private final MiniMessageService miniMessage;
    
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    
    public EnhanceGUI(GuangDianEnhance plugin, EnhanceManager enhanceManager) {
        this.plugin = plugin;
        this.enhanceManager = enhanceManager;
        this.config = plugin.getEnhanceConfig();
        this.miniMessage = plugin.getMiniMessage();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(this, SIZE, miniMessage.colorize("<gold>" + TITLE));
        
        updateInventory(player, inventory);
        
        openInventories.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }
    
    private void updateInventory(Player player, Inventory inventory) {
        inventory.clear();
        
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        
        ItemStack displayItem = createDisplayItem(mainHand);
        inventory.setItem(13, displayItem);
        
        if (config.isEnhanceable(mainHand)) {
            int level = enhanceManager.getLevel(mainHand);
            
            if (level < config.getMaxLevel()) {
                ItemStack enhanceButton = createEnhanceButton(mainHand, level, player);
                inventory.setItem(15, enhanceButton);
            }
            
            ItemStack infoButton = createInfoButton(mainHand, level, player);
            inventory.setItem(11, infoButton);
        }
        
        ItemStack glass = createGlassPane();
        for (int i = 0; i < SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }
    
    private ItemStack createDisplayItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(miniMessage.colorize("<red>请手持装备"));
            List<Component> lore = new ArrayList<>();
            lore.add(miniMessage.colorize("<gray>将装备放在主手"));
            lore.add(miniMessage.colorize("<gray>然后打开此界面"));
            meta.lore(lore);
            empty.setItemMeta(meta);
            return empty;
        }
        
        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        
        int level = enhanceManager.getLevel(item);
        double multiplier = enhanceManager.getAttributeMultiplier(level);
        
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        
        lore.add(miniMessage.colorize(""));
        lore.add(miniMessage.colorize("<gold>=== 强化信息 ==="));
        lore.add(miniMessage.colorize("<yellow>强化等级: <green>+" + level));
        lore.add(miniMessage.colorize("<yellow>属性加成: <aqua>" + String.format("%.1f%%", (multiplier - 1) * 100)));
        
        meta.lore(lore);
        display.setItemMeta(meta);
        
        return display;
    }
    
    private ItemStack createEnhanceButton(ItemStack item, int level, Player player) {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta meta = button.getItemMeta();
        
        double baseRate = enhanceManager.getSuccessRate(level, item);
        int pityCount = enhanceManager.getPityCountForPlayer(player.getUniqueId(), level);
        double pityBonus = enhanceManager.getPityBonusForPlayer(player.getUniqueId(), level);
        double totalRate = Math.min(1.0, baseRate + pityBonus);
        
        meta.displayName(miniMessage.colorize("<green>点击强化"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.colorize("<yellow>当前等级: <white>+" + level));
        lore.add(miniMessage.colorize("<yellow>目标等级: <white>+" + (level + 1)));
        lore.add(miniMessage.colorize(""));
        lore.add(miniMessage.colorize("<yellow>成功率: <green>" + String.format("%.1f%%", totalRate * 100)));
        if (pityCount > 0) {
            lore.add(miniMessage.colorize("<gray>保底加成: +" + String.format("%.1f%%", pityBonus * 100)));
        }
        
        List<EnhanceConfig.MaterialCost> costs = config.getMaterialCostForLevel(level + 1);
        if (!costs.isEmpty()) {
            lore.add(miniMessage.colorize(""));
            lore.add(miniMessage.colorize("<gold>所需材料:"));
            for (EnhanceConfig.MaterialCost cost : costs) {
                lore.add(miniMessage.colorize("<gray>• " + cost.getMaterial().name() + " x" + cost.getAmount()));
            }
        }
        
        lore.add(miniMessage.colorize(""));
        lore.add(miniMessage.colorize("<green><bold>点击强化"));
        
        meta.lore(lore);
        button.setItemMeta(meta);
        
        return button;
    }
    
    private ItemStack createInfoButton(ItemStack item, int level, Player player) {
        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        
        meta.displayName(miniMessage.colorize("<yellow>强化详情"));
        
        double baseRate = enhanceManager.getSuccessRate(level, item);
        double multiplier = enhanceManager.getAttributeMultiplier(level);
        int pityCount = enhanceManager.getPityCountForPlayer(player.getUniqueId(), level);
        
        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.colorize("<yellow>物品: <white>" + item.getType().name()));
        lore.add(miniMessage.colorize("<yellow>等级: <green>+" + level + " <gray>/ " + config.getMaxLevel()));
        lore.add(miniMessage.colorize("<yellow>属性加成: <aqua>" + String.format("%.1f%%", (multiplier - 1) * 100)));
        lore.add(miniMessage.colorize(""));
        lore.add(miniMessage.colorize("<gold>成功率信息:"));
        lore.add(miniMessage.colorize("<gray>基础: " + String.format("%.1f%%", baseRate * 100)));
        if (config.isPityEnabled()) {
            lore.add(miniMessage.colorize("<gray>保底: 失败" + pityCount + "次"));
        }
        
        meta.lore(lore);
        button.setItemMeta(meta);
        
        return button;
    }
    
    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        return glass;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EnhanceGUI)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        if (slot == 15) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            
            if (mainHand == null || mainHand.getType().isAir()) {
                player.sendMessage(miniMessage.colorize("<red>请手持装备进行强化"));
                return;
            }
            
            EnhanceResult result = enhanceManager.enhance(player, mainHand);
            
            if (result == EnhanceResult.SUCCESS) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
            } else if (result.isFailed()) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.8f);
            }
            
            updateInventory(player, event.getInventory());
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnhanceGUI) {
            openInventories.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EnhanceGUI) {
            event.setCancelled(true);
        }
    }
    
    public void closeAll() {
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openInventories.clear();
    }
    
    @Override
    public Inventory getInventory() {
        return null;
    }
}
