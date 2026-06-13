package cn.guangdian.classsystem.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {
    
    private final Map<UUID, Object> openGUIs = new ConcurrentHashMap<>();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof ClassMainGUI gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            gui.handleClick(slot);
            return;
        }
        
        if (holder instanceof ClassSelectionGUI gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            gui.handleClick(slot);
            return;
        }
        
        if (holder instanceof ClassAdvanceGUI gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            gui.handleClick(slot);
            return;
        }
        
        if (holder instanceof AttributeGUI gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            boolean isLeftClick = event.isLeftClick();
            boolean isShiftClick = event.isShiftClick();
            gui.handleClick(slot, isLeftClick, isShiftClick);
            return;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        openGUIs.remove(player.getUniqueId());
    }
    
    public void registerGUI(Player player, Object gui) {
        openGUIs.put(player.getUniqueId(), gui);
    }
    
    public void unregisterGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }
    
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
    
    public Object getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
}
