package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Quest GUI 基类
 * 封装 RPGCore GUI 服务，提供任务系统专用的 GUI 功能
 */
public abstract class QuestGUI {

    protected final GuangDianQuest plugin;
    protected GUI gui;
    protected final Player player;

    // 常用材质定义
    protected static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    protected static final Material BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    protected static final Material CLOSE_MATERIAL = Material.BARRIER;
    protected static final Material BACK_MATERIAL = Material.ARROW;
    protected static final Material NEXT_PAGE_MATERIAL = Material.LIME_STAINED_GLASS_PANE;
    protected static final Material PREV_PAGE_MATERIAL = Material.RED_STAINED_GLASS_PANE;

    // 任务类型材质
    protected static final Material MAIN_QUEST_MATERIAL = Material.NETHER_STAR;
    protected static final Material SIDE_QUEST_MATERIAL = Material.BOOK;
    protected static final Material DAILY_QUEST_MATERIAL = Material.CLOCK;
    protected static final Material ACHIEVEMENT_MATERIAL = Material.GOLDEN_APPLE;
    protected static final Material QUEST_LINE_MATERIAL = Material.WRITABLE_BOOK;

    public QuestGUI(@NotNull GuangDianQuest plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * 创建并打开 GUI
     */
    public abstract void open();

    /**
     * 构建 GUI 内容
     */
    protected abstract void build();

    /**
     * 重新打开 GUI (刷新)
     */
    public void refresh() {
        close();
        open();
    }

    /**
     * 关闭 GUI
     */
    public void close() {
        if (gui != null) {
            gui.close(player);
        }
    }

    /**
     * 创建基础物品
     */
    protected ItemStack createItem(@NotNull Material material, @NotNull String name, @Nullable List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMiniMessageService().colorize(name));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                    .map(line -> plugin.getMiniMessageService().colorize(line))
                    .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 创建带显示名称的物品
     */
    protected ItemStack createItem(@NotNull Material material, @NotNull String name) {
        return createItem(material, name, null);
    }

    /**
     * 创建关闭按钮
     */
    protected ItemStack createCloseButton() {
        return createItem(CLOSE_MATERIAL, "<red>关闭菜单", List.of(
            "<gray>点击关闭"
        ));
    }

    /**
     * 创建返回按钮
     */
    protected ItemStack createBackButton() {
        return createItem(BACK_MATERIAL, "<yellow>返回", List.of(
            "<gray>点击返回上一级"
        ));
    }

    /**
     * 创建上一页按钮
     */
    protected ItemStack createPrevPageButton(int currentPage, int totalPages) {
        return createItem(PREV_PAGE_MATERIAL, "<red>上一页", List.of(
            "<gray>第 " + currentPage + "/" + totalPages + " 页",
            "<gray>点击查看上一页"
        ));
    }

    /**
     * 创建下一页按钮
     */
    protected ItemStack createNextPageButton(int currentPage, int totalPages) {
        return createItem(NEXT_PAGE_MATERIAL, "<green>下一页", List.of(
            "<gray>第 " + currentPage + "/" + totalPages + " 页",
            "<gray>点击查看下一页"
        ));
    }

    /**
     * 创建填充物品
     */
    protected ItemStack createFiller() {
        return createItem(FILLER_MATERIAL, " ");
    }

    /**
     * 创建边框物品
     */
    protected ItemStack createBorder() {
        return createItem(BORDER_MATERIAL, " ");
    }

    /**
     * 获取关闭点击处理器
     */
    protected Consumer<InventoryClickEvent> getCloseHandler() {
        return event -> close();
    }

    /**
     * 获取返回主菜单处理器
     */
    protected Consumer<InventoryClickEvent> getBackToMainHandler() {
        return event -> {
            close();
            new QuestMainGUI(plugin, player).open();
        };
    }

    /**
     * 发送消息给玩家
     */
    protected void sendMessage(@NotNull String message) {
        player.sendMessage(plugin.getMiniMessageService().colorize(message));
    }

    /**
     * 播放点击音效
     */
    protected void playClickSound() {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * 播放成功音效
     */
    protected void playSuccessSound() {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
    }

    /**
     * 播放错误音效
     */
    protected void playErrorSound() {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    /**
     * 获取 GUIBuilder 实例
     */
    protected GUIBuilder createBuilder(@NotNull String title, int rows) {
        return GUIBuilder.create(title, rows);
    }
}
