package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * GUI 导航系统
 * 提供统一的导航栏和面包屑功能
 */
public class QuestNavigation {

    private final GuangDianQuest plugin;
    private final Player player;

    // 导航槽位定义 (底部行)
    public static final int SLOT_HOME = 45;      // 主页
    public static final int SLOT_BACK = 46;      // 返回
    public static final int SLOT_PREV = 48;      // 上一页
    public static final int SLOT_INFO = 49;      // 信息/当前位置
    public static final int SLOT_NEXT = 50;      // 下一页
    public static final int SLOT_CLOSE = 52;     // 关闭
    public static final int SLOT_REFRESH = 53;   // 刷新

    public QuestNavigation(GuangDianQuest plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * 创建主页按钮
     */
    public ItemStack createHomeButton() {
        return createItem(Material.NETHER_STAR, "<gold><bold>主页", List.of(
            "<gray>点击返回任务中心主页"
        ));
    }

    /**
     * 创建返回按钮
     */
    public ItemStack createBackButton(String destination) {
        return createItem(Material.ARROW, "<yellow>返回", List.of(
            "<gray>返回到: " + destination
        ));
    }

    /**
     * 创建关闭按钮
     */
    public ItemStack createCloseButton() {
        return createItem(Material.BARRIER, "<red>关闭", List.of(
            "<gray>点击关闭菜单"
        ));
    }

    /**
     * 创建刷新按钮
     */
    public ItemStack createRefreshButton() {
        return createItem(Material.CLOCK, "<aqua>刷新", List.of(
            "<gray>点击刷新界面"
        ));
    }

    /**
     * 创建上一页按钮
     */
    public ItemStack createPrevPageButton(int current, int total) {
        return createItem(Material.RED_STAINED_GLASS_PANE, "<red>上一页", List.of(
            "<gray>第 " + current + "/" + total + " 页"
        ));
    }

    /**
     * 创建下一页按钮
     */
    public ItemStack createNextPageButton(int current, int total) {
        return createItem(Material.LIME_STAINED_GLASS_PANE, "<green>下一页", List.of(
            "<gray>第 " + current + "/" + total + " 页"
        ));
    }

    /**
     * 创建当前位置指示器
     */
    public ItemStack createLocationIndicator(String location, String description) {
        return createItem(Material.PAPER, "<yellow>" + location, List.of(
            "<gray>" + description
        ));
    }

    /**
     * 创建导航栏填充
     */
    public ItemStack createNavFiller() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }

    /**
     * 创建基础物品
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
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
     * 获取返回主页处理器
     */
    public Consumer<org.bukkit.event.inventory.InventoryClickEvent> getHomeHandler() {
        return event -> {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new QuestMainGUI(plugin, player).open();
        };
    }

    /**
     * 获取关闭处理器
     */
    public Consumer<org.bukkit.event.inventory.InventoryClickEvent> getCloseHandler() {
        return event -> player.closeInventory();
    }

    /**
     * 获取刷新处理器
     */
    public Consumer<org.bukkit.event.inventory.InventoryClickEvent> getRefreshHandler(Runnable refreshAction) {
        return event -> {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            refreshAction.run();
        };
    }
}
