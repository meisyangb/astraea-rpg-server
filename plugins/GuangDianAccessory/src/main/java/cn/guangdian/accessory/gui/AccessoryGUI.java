package cn.guangdian.accessory.gui;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.service.AccessoryServiceAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessoryGUI implements Listener {
    
    private final GuangDianAccessory plugin;
    private final Map<AccessorySlot, Integer> slotPositions;
    private final String guiTitle = "饰品栏";
    
    public AccessoryGUI(GuangDianAccessory plugin) {
        this.plugin = plugin;
        this.slotPositions = new HashMap<>();
        slotPositions.put(AccessorySlot.BADGE, 10);
        slotPositions.put(AccessorySlot.MEDAL, 13);
        slotPositions.put(AccessorySlot.RELIC, 16);
    }
    
    public void openGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(new AccessoryHolder(), 27, Component.text(guiTitle));
        
        for (Map.Entry<AccessorySlot, Integer> entry : slotPositions.entrySet()) {
            AccessorySlot slot = entry.getKey();
            int position = entry.getValue();
            
            AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
            Accessory equipped = service.getEquippedAccessory(player, slot).orElse(null);
            
            if (equipped != null) {
                ItemStack displayItem = createDisplayItem(equipped);
                inventory.setItem(position, displayItem);
            } else {
                ItemStack emptySlot = createEmptySlotItem(slot);
                inventory.setItem(position, emptySlot);
            }
        }
        
        player.openInventory(inventory);
    }
    
    private ItemStack createDisplayItem(Accessory accessory) {
        ItemStack item = accessory.getItem();
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("槽位: " + accessory.getSlot().getDisplayName()).color(NamedTextColor.GRAY));
            
            if (!accessory.getAttributes().isEmpty()) {
                lore.add(Component.text("属性加成:").color(NamedTextColor.YELLOW));
                for (Map.Entry<String, Double> attr : accessory.getAttributes().entrySet()) {
                    lore.add(Component.text("  " + attr.getKey() + ": +" + attr.getValue())
                        .color(NamedTextColor.GREEN));
                }
            }
            
            if (!accessory.getDescription().isEmpty()) {
                lore.add(Component.text(""));
                lore.add(Component.text(accessory.getDescription()).color(NamedTextColor.DARK_GRAY));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("点击卸下").color(NamedTextColor.RED));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createEmptySlotItem(AccessorySlot slot) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(slot.getDisplayName() + "槽位").color(NamedTextColor.GRAY));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("将饰品放入此槽位").color(NamedTextColor.DARK_GRAY));
            lore.add(Component.text("以装备饰品").color(NamedTextColor.DARK_GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AccessoryHolder)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        int clickedSlot = event.getRawSlot();
        
        AccessorySlot clickedAccessorySlot = null;
        for (Map.Entry<AccessorySlot, Integer> entry : slotPositions.entrySet()) {
            if (entry.getValue() == clickedSlot) {
                clickedAccessorySlot = entry.getKey();
                break;
            }
        }
        
        if (clickedAccessorySlot == null) {
            return;
        }
        
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        Accessory equipped = service.getEquippedAccessory(player, clickedAccessorySlot).orElse(null);
        
        if (equipped != null) {
            if (service.unequipAccessory(player, clickedAccessorySlot)) {
                service.giveAccessory(player, equipped.getId(), 1);
                player.sendMessage(Component.text("已卸下 " + equipped.getName()).color(NamedTextColor.GREEN));
                openGUI(player);
            }
        } else {
            ItemStack cursor = event.getCursor();
            String accessoryId = service.getAccessoryIdFromItem(cursor);
            
            if (accessoryId != null) {
                Accessory accessory = service.getAccessory(accessoryId).orElse(null);
                if (accessory != null && accessory.getSlot() == clickedAccessorySlot) {
                    if (service.equipAccessory(player, clickedAccessorySlot, accessoryId)) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        player.sendMessage(Component.text("已装备 " + accessory.getName()).color(NamedTextColor.GREEN));
                        openGUI(player);
                    }
                } else {
                    player.sendMessage(Component.text("此饰品无法放入该槽位").color(NamedTextColor.RED));
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }
    
    public static class AccessoryHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
