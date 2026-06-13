package cn.guangdian.rpgitems.listener;

import cn.guangdian.rpgcore.event.events.AttributeChangeEvent;
import cn.guangdian.rpgitems.api.ItemAttributeAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * RPGItems 事件监听器
 * 监听装备变更，触发属性变更事件
 */
public class RPGItemsEventListener implements Listener {

    private final ItemAttributeAPI itemAPI;
    
    public RPGItemsEventListener(ItemAttributeAPI itemAPI) {
        this.itemAPI = itemAPI;
    }
    
    /**
     * 监听背包点击事件（装备变更）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // 检查是否是装备槽位
        int slot = event.getRawSlot();
        if (!isEquipmentSlot(slot)) {
            return;
        }
        
        ItemStack newItem = event.getCursor();
        ItemStack oldItem = event.getCurrentItem();
        
        // 检查是否涉及 RPGItems 物品
        boolean isNewRPGItem = itemAPI.isRPGItem(newItem);
        boolean isOldRPGItem = itemAPI.isRPGItem(oldItem);
        
        if (isNewRPGItem || isOldRPGItem) {
            // 延迟 1 tick 触发事件（让物品栏先更新）
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("RPGItems"),
                () -> {
                    if (player.isOnline()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("slot", slot);
                        data.put("isNewRPGItem", isNewRPGItem);
                        data.put("isOldRPGItem", isOldRPGItem);
                        
                        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
                            player,
                            AttributeChangeEvent.Type.EQUIPMENT_CHANGE,
                            "RPGItems",
                            data
                        );
                        Bukkit.getPluginManager().callEvent(attrEvent);
                    }
                },
                1L
            );
        }
    }
    
    /**
     * 监听快捷栏切换事件（武器变更）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int newSlot = event.getNewSlot();
        
        // 延迟 1 tick 获取新物品（让物品栏先更新）
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("RPGItems"),
            () -> {
                if (player.isOnline()) {
                    ItemStack newItem = player.getInventory().getItem(newSlot);
                    
                    // 检查是否是 RPGItems 物品
                    if (itemAPI.isRPGItem(newItem)) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("newSlot", newSlot);
                        data.put("itemId", itemAPI.getItemId(newItem));
                        
                        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
                            player,
                            AttributeChangeEvent.Type.WEAPON_CHANGE,
                            "RPGItems",
                            data
                        );
                        Bukkit.getPluginManager().callEvent(attrEvent);
                    }
                }
            },
            1L
        );
    }
    
    /**
     * 监听主副手交换事件（武器变更）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        ItemStack newMainHand = event.getMainHandItem();
        ItemStack newOffHand = event.getOffHandItem();
        
        // 检查是否涉及 RPGItems 物品
        boolean isMainRPGItem = itemAPI.isRPGItem(newMainHand);
        boolean isOffRPGItem = itemAPI.isRPGItem(newOffHand);
        
        if (isMainRPGItem || isOffRPGItem) {
            // 延迟 1 tick 触发事件（让物品栏先更新）
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("RPGItems"),
                () -> {
                    if (player.isOnline()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("isMainRPGItem", isMainRPGItem);
                        data.put("isOffRPGItem", isOffRPGItem);
                        
                        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
                            player,
                            AttributeChangeEvent.Type.WEAPON_CHANGE,
                            "RPGItems",
                            data
                        );
                        Bukkit.getPluginManager().callEvent(attrEvent);
                    }
                },
                1L
            );
        }
    }
    
    /**
     * 检查是否是装备槽位
     */
    private boolean isEquipmentSlot(int slot) {
        // 5: 头盔, 6: 胸甲, 7: 护腿, 8: 靴子
        return slot >= 5 && slot <= 8;
    }
}
