package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.QuestLine;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * 任务线 GUI
 * 显示任务线列表和进度
 * 导航: 主页 -> 任务线 -> 任务线详情
 */
public class QuestLineGUI extends QuestGUI {

    private final QuestItemFactory itemFactory;
    private final QuestNavigation nav;

    public QuestLineGUI(GuangDianQuest plugin, Player player) {
        super(plugin, player);
        this.itemFactory = new QuestItemFactory(plugin);
        this.nav = new QuestNavigation(plugin, player);
    }

    @Override
    public void open() {
        build();
        if (gui != null) {
            gui.open(player);
        }
    }

    @Override
    protected void build() {
        Collection<QuestLine> questLines = plugin.getQuestLineManager().getAllQuestLines();

        GUIBuilder builder = createBuilder("<gold><bold>任务线", 5)
            .setFiller(FILLER_MATERIAL);

        // 边框
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // 面包屑导航 (slot 4)
        builder.setItem(4, createItem(Material.PAPER, "<yellow>任务中心 > 任务线", List.of(
            "<gray>点击返回任务中心主页"
        )), nav.getHomeHandler());

        // 标题信息 (slot 13)
        ItemStack infoItem = createItem(Material.WRITABLE_BOOK, "<gold><bold>任务线列表", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>任务线是串联多个任务的",
            "<gray>剧情线，完成任务线",
            "<gray>可获得丰厚奖励",
            "<dark_gray>━━━━━━━━━━━━━━━━━━"
        ));
        builder.setItem(13, infoItem, null);

        // 填充任务线 (slot 20-26, 29-35)
        int[] slots = {20, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 33, 34, 35};
        int index = 0;
        for (QuestLine questLine : questLines) {
            if (index >= slots.length) break;

            ItemStack lineItem = itemFactory.createQuestLineItem(questLine, player);
            builder.setItem(slots[index], lineItem, event -> {
                playClickSound();
                new QuestLineDetailGUI(plugin, player, questLine, this).open();
            });
            index++;
        }

        // 如果没有任务线
        if (questLines.isEmpty()) {
            ItemStack noLineItem = createItem(Material.BARRIER, "<red><bold>暂无任务线", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>当前没有可用的任务线",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, noLineItem);
        }

        // ===== 底部导航栏 =====
        // 主页按钮
        builder.setItem(QuestNavigation.SLOT_HOME, nav.createHomeButton(), nav.getHomeHandler());

        // 返回按钮
        builder.setItem(QuestNavigation.SLOT_BACK, nav.createBackButton("任务中心"), nav.getHomeHandler());

        // 上一页/空位
        builder.setItem(QuestNavigation.SLOT_PREV, nav.createNavFiller());

        // 当前位置
        builder.setItem(QuestNavigation.SLOT_INFO, nav.createLocationIndicator("任务线", "共 " + questLines.size() + " 条"), null);

        // 下一页/空位
        builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNavFiller());

        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 刷新按钮
        builder.setItem(QuestNavigation.SLOT_REFRESH, nav.createRefreshButton(),
            nav.getRefreshHandler(() -> new QuestLineGUI(plugin, player).open()));

        gui = builder.build();
    }
}
