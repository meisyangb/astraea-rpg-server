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
    
    private final Map<UUID, AttributeGUI> openGUIs = new ConcurrentHashMap<>();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AttributeGUI gui)) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        
        boolean isLeftClick = event.isLeftClick();
        boolean isShiftClick = event.isShiftClick();
        
        gui.handleClick(slot, isLeftClick, isShiftClick);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        openGUIs.remove(player.getUniqueId());
    }
    
    public void registerGUI(Player player, AttributeGUI gui) {
        openGUIs.put(player.getUniqueId(), gui);
    }
    
    public void unregisterGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }
    
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
}
