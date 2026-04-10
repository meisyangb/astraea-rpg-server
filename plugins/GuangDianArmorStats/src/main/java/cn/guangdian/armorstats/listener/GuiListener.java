package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.gui.GemInlayGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof GemInlayGUI) {
            GemInlayGUI gui = (GemInlayGUI) inventory.getHolder();
            gui.handleInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GemInlayGUI gui) {
            gui.handleInventoryDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GemInlayGUI gui) {
            gui.handleInventoryClose();
        }
    }
}
