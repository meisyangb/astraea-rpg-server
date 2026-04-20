package cn.guangdian.rpgcore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI 事件监听器 - RPGCore GUI 框架
 *
 * <p>自动处理所有 GUI 的点击、打开、关闭事件。</p>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class GUIListener implements Listener {

    /**
     * 玩家 -> GUI 映射 (用于追踪当前打开的 GUI)
     */
    private final Map<UUID, GUI> playerGUIs = new ConcurrentHashMap<>();

    /**
     * 注册 GUI 到玩家
     */
    public void registerPlayerGUI(@NotNull Player player, @NotNull GUI gui) {
        playerGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * 注销玩家的 GUI
     */
    public void unregisterPlayerGUI(@NotNull Player player) {
        playerGUIs.remove(player.getUniqueId());
    }

    /**
     * 获取玩家当前打开的 GUI
     */
    public GUI getPlayerGUI(@NotNull Player player) {
        return playerGUIs.get(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GUI gui = playerGUIs.get(player.getUniqueId());
        if (gui != null) {
            // 检查是否是同一个 inventory
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory != null && clickedInventory.equals(gui.getInventory())) {
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // 可以在这里添加打开时的逻辑
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        GUI gui = playerGUIs.remove(player.getUniqueId());
        if (gui != null) {
            // 触发关闭处理器 (已在 GUI.close 中处理，这里是备用)
        }
    }
}
