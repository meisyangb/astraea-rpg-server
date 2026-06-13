package cn.guangdian.devour.listener;

import cn.guangdian.devour.GuangDianDevour;
import cn.guangdian.devour.gui.DevourGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 吞噬事件监听器
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourListener implements Listener {
    
    private final GuangDianDevour plugin;
    
    public DevourListener(GuangDianDevour plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (holder instanceof DevourGUI gui) {
            gui.handleInventoryClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (holder instanceof DevourGUI gui) {
            gui.handleInventoryDrag(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (holder instanceof DevourGUI gui) {
            gui.handleInventoryClose();
        }
    }
}
