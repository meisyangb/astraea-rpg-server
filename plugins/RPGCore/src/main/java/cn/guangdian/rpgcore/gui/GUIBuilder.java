package cn.guangdian.rpgcore.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI 构建器 - RPGCore GUI 框架
 *
 * <p>提供流式 API 快速构建 GUI 界面。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * GUI gui = GUIBuilder.create("<gold>我的菜单", 54)
 *     .setItem(0, item, click -> {
 *         // 点击处理
 *     })
 *     .setFiller(Material.GRAY_STAINED_GLASS_PANE)
 *     .build();
 *
 * gui.open(player);
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class GUIBuilder {

    private final String title;
    private final int size;
    private final Map<Integer, ItemStack> items;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;
    private ItemStack fillerItem;
    private Consumer<org.bukkit.entity.Player> openHandler;
    private Consumer<org.bukkit.entity.Player> closeHandler;

    private GUIBuilder(@NotNull String title, int size) {
        this.title = title;
        this.size = validateSize(size);
        this.items = new HashMap<>();
        this.clickHandlers = new HashMap<>();
    }

    /**
     * 创建 GUI 构建器
     *
     * @param title GUI 标题 (支持 MiniMessage 格式)
     * @param rows 行数 (1-6)
     * @return GUIBuilder 实例
     */
    public static @NotNull GUIBuilder create(@NotNull String title, int rows) {
        return new GUIBuilder(title, rows * 9);
    }

    /**
     * 创建 GUI 构建器 (直接指定大小)
     *
     * @param title GUI 标题
     * @param size GUI 大小 (必须是 9 的倍数)
     * @return GUIBuilder 实例
     */
    public static @NotNull GUIBuilder createCustom(@NotNull String title, int size) {
        return new GUIBuilder(title, size);
    }

    /**
     * 设置物品到指定槽位
     *
     * @param slot 槽位 (0-53)
     * @param item 物品
     * @return 当前构建器
     */
    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item) {
        if (slot >= 0 && slot < size) {
            items.put(slot, item);
        }
        return this;
    }

    /**
     * 设置物品并绑定点击事件
     *
     * @param slot 槽位
     * @param item 物品
     * @param handler 点击处理器
     * @return 当前构建器
     */
    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> handler) {
        setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
        return this;
    }

    /**
     * 批量设置物品
     *
     * @param slotItems 槽位-物品映射
     * @return 当前构建器
     */
    public @NotNull GUIBuilder setItems(@NotNull Map<Integer, ItemStack> slotItems) {
        items.putAll(slotItems);
        return this;
    }

    /**
     * 设置填充物品 (用于空槽位)
     *
     * @param material 填充物品材质
     * @return 当前构建器
     */
    public @NotNull GUIBuilder setFiller(@NotNull Material material) {
        this.fillerItem = new ItemStack(material);
        return this;
    }

    /**
     * 设置填充物品 (自定义)
     *
     * @param filler 填充物品
     * @return 当前构建器
     */
    public @NotNull GUIBuilder setFillerItem(@NotNull ItemStack filler) {
        this.fillerItem = filler;
        return this;
    }

    /**
     * 填充指定范围
     *
     * @param start 起始槽位
     * @param end 结束槽位
     * @param item 填充物品
     * @return 当前构建器
     */
    public @NotNull GUIBuilder fillRange(int start, int end, @NotNull ItemStack item) {
        for (int i = start; i <= end && i < size; i++) {
            if (!items.containsKey(i)) {
                items.put(i, item);
            }
        }
        return this;
    }

    /**
     * 填充边框
     *
     * @param item 边框物品
     * @return 当前构建器
     */
    public @NotNull GUIBuilder fillBorder(@NotNull ItemStack item) {
        // 顶部和底部行
        for (int i = 0; i < 9; i++) {
            items.putIfAbsent(i, item); // 顶部
            items.putIfAbsent(size - 9 + i, item); // 底部
        }
        // 左右列
        for (int row = 1; row < (size / 9) - 1; row++) {
            items.putIfAbsent(row * 9, item); // 左列
            items.putIfAbsent(row * 9 + 8, item); // 右列
        }
        return this;
    }

    /**
     * 设置打开时的处理器
     */
    public @NotNull GUIBuilder onUpdateOpen(@NotNull Consumer<org.bukkit.entity.Player> handler) {
        this.openHandler = handler;
        return this;
    }

    /**
     * 设置关闭时的处理器
     */
    public @NotNull GUIBuilder onClose(@NotNull Consumer<org.bukkit.entity.Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    /**
     * 构建 GUI
     *
     * @return 构建完成的 GUI
     */
    public @NotNull GUI build() {
        GUI gui = new GUI(title, size);

        // 设置所有物品
        items.forEach(gui::setItem);

        // 设置点击处理器
        clickHandlers.forEach((slot, handler) -> {
            // 通过反射或重新设计来设置 clickHandlers
            // 这里简化处理，实际使用时需要在 GUI 类中暴露方法
        });

        // 填充空槽位
        if (fillerItem != null) {
            gui.fillEmptySlots(fillerItem);
        }

        // 设置打开/关闭处理器
        if (openHandler != null) {
            gui.onUpdateOpen(openHandler);
        }
        if (closeHandler != null) {
            gui.onClose(closeHandler);
        }

        return gui;
    }

    /**
     * 验证 GUI 大小
     */
    private int validateSize(int size) {
        if (size % 9 != 0) {
            throw new IllegalArgumentException("GUI size must be a multiple of 9");
        }
        if (size < 9 || size > 54) {
            throw new IllegalArgumentException("GUI size must be between 9 and 54");
        }
        return size;
    }
}
