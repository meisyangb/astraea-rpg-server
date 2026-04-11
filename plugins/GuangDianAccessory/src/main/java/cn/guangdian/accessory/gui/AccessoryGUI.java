package cn.guangdian.accessory.gui;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.service.AccessoryServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AccessoryGUI implements Listener {
    
    private static final int GUI_SIZE = 27;
    private static final int BADGE_SLOT = 10;
    private static final int MEDAL_SLOT = 13;
    private static final int RELIC_SLOT = 16;
    
    private final GuangDianAccessory plugin;
    private final Map<AccessorySlot, Integer> slotPositions;
    private final String guiTitle = "饰品栏";
    private final Set<Integer> backgroundSlots;
    
    public AccessoryGUI(GuangDianAccessory plugin) {
        this.plugin = plugin;
        this.slotPositions = new EnumMap<>(AccessorySlot.class);
        this.backgroundSlots = new HashSet<>();
        
        slotPositions.put(AccessorySlot.BADGE, BADGE_SLOT);
        slotPositions.put(AccessorySlot.MEDAL, MEDAL_SLOT);
        slotPositions.put(AccessorySlot.RELIC, RELIC_SLOT);
        
        for (int i = 0; i < GUI_SIZE; i++) {
            if (!slotPositions.containsValue(i)) {
                backgroundSlots.add(i);
            }
        }
    }
    
    public void openGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(new AccessoryHolder(), GUI_SIZE, Component.text(guiTitle));
        
        fillBackground(inventory);
        
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        
        for (Map.Entry<AccessorySlot, Integer> entry : slotPositions.entrySet()) {
            AccessorySlot slot = entry.getKey();
            int position = entry.getValue();
            
            Accessory equipped = service.getEquippedAccessory(player, slot).orElse(null);
            
            if (equipped != null) {
                ItemStack displayItem = equipped.getMythicItem(1);
                if (displayItem != null) {
                    inventory.setItem(position, displayItem);
                } else {
                    inventory.setItem(position, createEmptySlotItem(slot));
                }
            } else {
                inventory.setItem(position, createEmptySlotItem(slot));
            }
        }
        
        player.openInventory(inventory);
    }
    
    private void fillBackground(Inventory inventory) {
        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            background.setItemMeta(meta);
        }
        
        for (int slot : backgroundSlots) {
            inventory.setItem(slot, background);
        }
    }
    
    private ItemStack createEmptySlotItem(AccessorySlot slot) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(slot.getDisplayName() + "槽位 - 空闲").color(NamedTextColor.GRAY));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AccessoryHolder)) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        int clickedSlot = event.getRawSlot();
        InventoryAction action = event.getAction();
        
        if (clickedSlot < 0 || clickedSlot >= GUI_SIZE) {
            return;
        }
        
        if (backgroundSlots.contains(clickedSlot)) {
            event.setCancelled(true);
            return;
        }
        
        AccessorySlot targetSlot = getSlotByPosition(clickedSlot);
        if (targetSlot == null) {
            event.setCancelled(true);
            return;
        }
        
        event.setCancelled(true);
        
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        
        if (cursor != null && cursor.getType() != Material.AIR) {
            Accessory accessory = service.getAccessoryFromItem(cursor);
            
            if (accessory != null) {
                if (accessory.getSlot() != targetSlot) {
                    player.sendMessage(Component.text("此饰品需要放入 " + accessory.getSlot().getDisplayName() + " 槽位").color(NamedTextColor.RED));
                    return;
                }
                
                Accessory equipped = service.getEquippedAccessory(player, targetSlot).orElse(null);
                
                if (equipped != null) {
                    service.unequipAccessory(player, targetSlot);
                    service.giveAccessory(player, equipped.getId(), 1);
                }
                
                boolean success = service.equipAccessory(player, targetSlot, accessory.getId());
                
                if (success) {
                    int newAmount = cursor.getAmount() - 1;
                    if (newAmount <= 0) {
                        event.setCursor(null);
                    } else {
                        cursor.setAmount(newAmount);
                    }
                    player.sendMessage(Component.text("已装备 " + accessory.getName()).color(NamedTextColor.GREEN));
                    refreshGUI(player);
                } else {
                    player.sendMessage(Component.text("装备失败，请重试").color(NamedTextColor.RED));
                }
            } else {
                player.sendMessage(Component.text("这不是有效的饰品").color(NamedTextColor.RED));
            }
        } else if (currentItem != null && currentItem.getType() != Material.AIR && 
                   currentItem.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            Accessory equipped = service.getEquippedAccessory(player, targetSlot).orElse(null);
            if (equipped != null) {
                service.unequipAccessory(player, targetSlot);
                service.giveAccessory(player, equipped.getId(), 1);
                player.sendMessage(Component.text("已卸下 " + equipped.getName()).color(NamedTextColor.GREEN));
                refreshGUI(player);
            }
        }
    }
    
    private void refreshGUI(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> openGUI(player), 1L);
        }
    }
    
    private AccessorySlot getSlotByPosition(int position) {
        for (Map.Entry<AccessorySlot, Integer> entry : slotPositions.entrySet()) {
            if (entry.getValue() == position) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof AccessoryHolder)) {
            return;
        }
        
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    public static class AccessoryHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
