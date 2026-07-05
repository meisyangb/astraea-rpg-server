package cn.guangdian.enhance.listener;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.gui.EnhanceGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 强化系统监听器 — 完全参考 GuangDianSocket 的 SocketListener 风格
 * 只做委托，所有逻辑由 EnhanceGUI 自身处理
 */
public class EnhanceListener implements Listener {

    private final GuangDianEnhance plugin;

    public EnhanceListener(GuangDianEnhance plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof EnhanceGUI gui) {
            gui.handleInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof EnhanceGUI gui) {
            gui.handleInventoryDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof EnhanceGUI gui) {
            gui.handleInventoryClose();
        }
    }
}
