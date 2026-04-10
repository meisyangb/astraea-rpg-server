package cn.guangdian.decompose.listener;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.gui.DecomposeGUI;
import cn.guangdian.decompose.manager.DecomposeManager;
import net.kyori.adventure.text.Component;
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

public class DecomposeListener implements Listener {

    private final GuangDianDecompose plugin;

    public DecomposeListener(GuangDianDecompose plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof DecomposeGUI)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        DecomposeGUI gui = plugin.getDecomposeGUI();

        if (gui.isCloseButton(slot)) {
            player.closeInventory();
            return;
        }

        if (gui.isDecomposeButton(slot)) {
            DecomposeManager.DecomposeResult result = gui.handleDecompose(player, event.getInventory());
            if (result.isSuccess()) {
                updateGuiAfterDecompose(player, event.getInventory());
            }
            return;
        }

        if (gui.isInputSlot(slot)) {
            handleInputSlotClick(player, event);
        }
    }

    private void handleInputSlotClick(Player player, InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (clickedInv.equals(player.getOpenInventory().getTopInventory())) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                ItemStack currentItem = event.getInventory().getItem(13);
                if (currentItem != null && !currentItem.getType().isAir()) {
                    player.getInventory().addItem(currentItem);
                }
                event.getInventory().setItem(13, cursor.clone());
                event.setCursor(null);
                plugin.getDecomposeGUI().handleInput(player, event.getInventory(), cursor);
            } else {
                ItemStack currentItem = event.getInventory().getItem(13);
                if (currentItem != null && !currentItem.getType().isAir()) {
                    player.getInventory().addItem(currentItem);
                    event.getInventory().setItem(13, null);
                    plugin.getDecomposeGUI().handleInput(player, event.getInventory(), null);
                }
            }
        }
    }

    private void updateGuiAfterDecompose(Player player, Inventory gui) {
        ItemStack remaining = gui.getItem(13);
        if (remaining == null || remaining.getType().isAir()) {
            plugin.getDecomposeGUI().handleInput(player, gui, null);
        } else {
            plugin.getDecomposeGUI().handleInput(player, gui, remaining);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DecomposeGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof DecomposeGUI) {
            ItemStack inputItem = event.getInventory().getItem(13);
            if (inputItem != null && !inputItem.getType().isAir()) {
                player.getInventory().addItem(inputItem);
            }
        }
    }
}
