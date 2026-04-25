package cn.guangdian.rpgcore.gui;

import cn.guangdian.rpgcore.message.MessageServiceImpl;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI 界面 - RPGCore GUI 构建器核心类
 *
 * <p>封装 Bukkit Inventory，提供统一的点击事件处理和自动更新。</p>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public class GUI implements InventoryHolder {

    private final String title;
    private final int size;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;
    private final MessageServiceImpl msg;

    private boolean updateOnOpen = false;
    private Consumer<Player> openHandler;
    private Consumer<Player> closeHandler;

    GUI(@NotNull String title, int size) {
        this.title = title;
        this.size = size;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        this.clickHandlers = new HashMap<>();
        this.msg = MessageServiceImpl.getInstance();
    }

    GUI(@NotNull String title, int size, @NotNull Inventory externalInventory) {
        this.title = title;
        this.size = size;
        this.inventory = externalInventory;
        this.clickHandlers = new HashMap<>();
        this.msg = MessageServiceImpl.getInstance();
    }

    /**
     * 设置物品到指定槽位
     */
    public void setItem(int slot, @Nullable ItemStack item) {
        inventory.setItem(slot, item);
    }

    /**
     * 设置物品并绑定点击事件
     */
    public void setItem(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> handler) {
        setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    /**
     * 批量设置物品
     */
    public void setItems(@NotNull Map<Integer, ItemStack> items) {
        items.forEach(this::setItem);
    }

    /**
     * 填充所有空槽位
     */
    public void fillEmptySlots(@NotNull ItemStack filler) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * 填充指定槽位范围
     */
    public void fillRange(int start, int end, @NotNull ItemStack item) {
        for (int i = start; i <= end && i < size; i++) {
            inventory.setItem(i, item);
        }
    }

    /**
     * 获取物品
     */
    public @Nullable ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    /**
     * 清空所有物品
     */
    public void clear() {
        inventory.clear();
        clickHandlers.clear();
    }

    /**
     * 打开 GUI
     */
    public void open(@NotNull Player player) {
        if (updateOnOpen && openHandler != null) {
            openHandler.accept(player);
        }

        // 注册到 GUIListener
        GUIManager guiManager = GUIManager.getInstance();
        if (guiManager.isInitialized()) {
            GUIListener listener = (GUIListener) guiManager.getListener();
            listener.registerPlayerGUI(player, this);
        }

        player.openInventory(inventory);
    }

    /**
     * 关闭 GUI
     */
    public void close(@NotNull Player player) {
        player.closeInventory();
        if (closeHandler != null) {
            closeHandler.accept(player);
        }
    }

    /**
     * 处理点击事件
     */
    public void handleClick(@NotNull InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) {
            return; // 点击了玩家背包
        }

        // 取消所有在 GUI 内的点击操作（防止物品移动）
        event.setCancelled(true);

        // 如果有点击处理器，执行它
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(event);
        }
    }

    /**
     * 设置点击处理器
     */
    public void setClickHandler(int slot, @Nullable Consumer<InventoryClickEvent> handler) {
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    /**
     * 刷新 GUI (重新打开)
     */
    public void refresh(@NotNull Player player) {
        close(player);
        open(player);
    }

    // ==================== Getters ====================

    public @NotNull String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public @NotNull Map<Integer, Consumer<InventoryClickEvent>> getClickHandlers() {
        return clickHandlers;
    }

    // ==================== 配置方法 ====================

    /**
     * 设置打开时的更新处理器
     */
    public GUI onUpdateOpen(@NotNull Consumer<Player> handler) {
        this.updateOnOpen = true;
        this.openHandler = handler;
        return this;
    }

    /**
     * 设置关闭处理器
     */
    public GUI onClose(@NotNull Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }
}
