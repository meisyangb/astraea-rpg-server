package cn.guangdian.collection.listener;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class ItemCollectListener implements Listener {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    
    public ItemCollectListener(GuangDianCollection plugin, CollectionService collectionService) {
        this.plugin = plugin;
        this.collectionService = collectionService;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getConfigManager().isCollectOnPickup()) return;
        
        ItemStack item = event.getItem().getItemStack();
        checkAndCollect(player, item);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getConfigManager().isCollectOnCraft()) return;
        
        ItemStack item = event.getRecipe().getResult();
        checkAndCollect(player, item);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.AIR) {
            checkAndCollect(player, item);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.AIR) {
            checkAndCollect(player, item);
        }
    }
    
    public void checkAndCollect(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        String mythicId = getMythicMobsId(item);
        
        for (CollectionCategory category : collectionService.getCategories().values()) {
            if (category.getType() == CollectionCategory.CategoryType.MOB_KILL) continue;
            
            for (CollectionEntry entry : category.getEntries().values()) {
                if (collectionService.getPlayerData(player).hasCollected(entry.getId())) continue;
                
                if (matchesEntry(entry, item, mythicId)) {
                    collectionService.collectItem(player, entry);
                }
            }
        }
    }
    
    private boolean matchesEntry(CollectionEntry entry, ItemStack item, String mythicId) {
        switch (entry.getType()) {
            case VANILLA_ITEM:
                return entry.getMaterial() == item.getType();
            case MYTHICMOBS_ITEM:
                return mythicId != null && mythicId.equals(entry.getMythicId());
            default:
                return false;
        }
    }
    
    private String getMythicMobsId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        
        if (typeId != null) return typeId;
        
        NamespacedKey oldKey = new NamespacedKey("mythicmobs", "item");
        return meta.getPersistentDataContainer().get(oldKey, PersistentDataType.STRING);
    }
}
