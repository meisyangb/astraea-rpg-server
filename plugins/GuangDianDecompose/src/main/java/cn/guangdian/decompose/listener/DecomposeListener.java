package cn.guangdian.decompose.listener;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.gui.DecomposeGUI;
import cn.guangdian.decompose.manager.DecomposeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
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

        int slot = event.getRawSlot();
        DecomposeGUI gui = plugin.getDecomposeGUI();
        int inputSlot = gui.getInputSlot();

        if (gui.isCloseButton(slot)) {
            event.setCancelled(true);
            returnItemsToPlayer(player, event.getInventory(), inputSlot);
            player.closeInventory();
            return;
        }

        if (gui.isDecomposeButton(slot)) {
            event.setCancelled(true);
            DecomposeManager.DecomposeResult result = gui.handleDecompose(player, event.getInventory());
            if (result.isSuccess()) {
                updateGuiAfterDecompose(player, event.getInventory());
            }
            return;
        }

        if (gui.isInputSlot(slot)) {
            handleInputSlotClick(player, event, inputSlot);
            return;
        }

        if (slot >= 0 && slot < event.getInventory().getSize()) {
            event.setCancelled(true);
            return;
        }

        InventoryAction action = event.getAction();
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack movedItem = event.getCurrentItem();
            if (movedItem != null && !movedItem.getType().isAir()) {
                ItemStack currentInput = event.getInventory().getItem(inputSlot);
                if (currentInput != null && !currentInput.getType().isAir()) {
                    return;
                }
                event.setCancelled(true);
                event.getInventory().setItem(inputSlot, movedItem.clone());
                event.setCurrentItem(null);
                plugin.getDecomposeGUI().handleInput(player, event.getInventory(), movedItem);
            } else {
                event.setCancelled(true);
            }
        }
    }

    private void handleInputSlotClick(Player player, InventoryClickEvent event, int inputSlot) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        InventoryAction action = event.getAction();
        Inventory topInv = event.getInventory();
        ItemStack cursor = event.getCursor();
        ItemStack currentInput = topInv.getItem(inputSlot);

        if (action == InventoryAction.PLACE_ALL || 
            action == InventoryAction.PLACE_ONE || 
            action == InventoryAction.PLACE_SOME ||
            action == InventoryAction.SWAP_WITH_CURSOR) {
            
            if (cursor != null && !cursor.getType().isAir()) {
                event.setCancelled(true);
                
                if (currentInput != null && !currentInput.getType().isAir()) {
                    player.getInventory().addItem(currentInput);
                }
                
                topInv.setItem(inputSlot, cursor.clone());
                event.setCursor(null);
                plugin.getDecomposeGUI().handleInput(player, topInv, cursor);
            }
        } else if (action == InventoryAction.PICKUP_ALL || 
                   action == InventoryAction.PICKUP_SOME ||
                   action == InventoryAction.PICKUP_HALF) {
            
            if (currentInput != null && !currentInput.getType().isAir()) {
                event.setCancelled(true);
                player.getInventory().addItem(currentInput);
                topInv.setItem(inputSlot, null);
                plugin.getDecomposeGUI().handleInput(player, topInv, null);
            }
        } else if (action == InventoryAction.DROP_ALL_SLOT ||
                   action == InventoryAction.DROP_ONE_SLOT) {
            event.setCancelled(true);
            if (currentInput != null && !currentInput.getType().isAir()) {
                player.getInventory().addItem(currentInput);
                topInv.setItem(inputSlot, null);
                plugin.getDecomposeGUI().handleInput(player, topInv, null);
            }
        } else {
            event.setCancelled(true);
        }
    }

    private void updateGuiAfterDecompose(Player player, Inventory gui) {
        int inputSlot = plugin.getDecomposeGUI().getInputSlot();
        ItemStack remaining = gui.getItem(inputSlot);
        if (remaining == null || remaining.getType().isAir()) {
            plugin.getDecomposeGUI().handleInput(player, gui, null);
        } else {
            plugin.getDecomposeGUI().handleInput(player, gui, remaining);
        }
    }

    private void returnItemsToPlayer(Player player, Inventory gui, int inputSlot) {
        ItemStack inputItem = gui.getItem(inputSlot);
        if (inputItem != null && !inputItem.getType().isAir()) {
            player.getInventory().addItem(inputItem);
            gui.setItem(inputSlot, null);
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
            int inputSlot = plugin.getDecomposeGUI().getInputSlot();
            ItemStack inputItem = event.getInventory().getItem(inputSlot);
            if (inputItem != null && !inputItem.getType().isAir()) {
                player.getInventory().addItem(inputItem);
                event.getInventory().setItem(inputSlot, null);
            }
        }
    }
}
