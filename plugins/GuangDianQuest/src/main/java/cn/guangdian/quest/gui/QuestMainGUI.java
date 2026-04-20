package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 任务主菜单 GUI
 * 任务系统的唯一入口界面
 */
public class QuestMainGUI extends QuestGUI {

    private final QuestItemFactory itemFactory;
    private final QuestNavigation nav;

    public QuestMainGUI(GuangDianQuest plugin, Player player) {
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
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        int activeCount = data.getActiveQuestCount();
        int completedCount = data.getTotalCompletedCount();

        GUIBuilder builder = createBuilder("<gold><bold>任务中心", 5)
            .setFiller(FILLER_MATERIAL);

        // 边框
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // ===== 第一行: 标题装饰 =====
        builder.setItem(4, createItem(Material.NETHER_STAR, "<gold><bold>✦ 任务中心 ✦", List.of(
            "<yellow>欢迎来到任务系统",
            "<gray>在这里你可以管理所有任务"
        )));

        // ===== 第二行: 主要功能入口 =====

        // 进行中任务 (slot 11)
        ItemStack activeQuestsItem = itemFactory.createNavigationItem(
            Material.ENCHANTED_BOOK,
            "<yellow><bold>进行中任务",
            List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>当前进行中的任务: <yellow>" + activeCount + "<gray>/" + plugin.getMaxActiveQuests(),
                "<gray>查看任务进度和完成情况",
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "",
                "<green>⚡ 点击查看详情"
            ),
            Math.max(1, Math.min(activeCount, 64))
        );
        builder.setItem(11, activeQuestsItem, event -> {
            playClickSound();
            new QuestListGUI(plugin, player, QuestListGUI.ListType.ACTIVE).open();
        });

        // 可接取任务 (slot 13)
        List<String> availableQuests = plugin.getQuestManager().getAvailableQuests(player.getUniqueId());
        ItemStack availableItem = itemFactory.createNavigationItem(
            Material.BOOK,
            "<aqua><bold>可接取任务",
            List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>可接取的任务: <yellow>" + availableQuests.size(),
                "<gray>浏览并接取新的任务",
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "",
                "<green>⚡ 点击查看详情"
            ),
            Math.max(1, Math.min(availableQuests.size(), 64))
        );
        builder.setItem(13, availableItem, event -> {
            playClickSound();
            new QuestListGUI(plugin, player, QuestListGUI.ListType.AVAILABLE).open();
        });

        // 每日任务 (slot 15)
        int dailyCompleted = data.getDailyCompletedCount();
        ItemStack dailyItem = itemFactory.createNavigationItem(
            Material.CLOCK,
            "<gold><bold>每日任务",
            List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>今日完成: <yellow>" + dailyCompleted + "<gray>/" + plugin.getDailyQuestLimit(),
                "<gray>查看今日可做的每日任务",
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "",
                "<green>⚡ 点击查看详情"
            ),
            1
        );
        builder.setItem(15, dailyItem, event -> {
            playClickSound();
            new DailyQuestGUI(plugin, player).open();
        });

        // 任务线 (slot 17)
        int questLineCount = plugin.getQuestLineManager().getAllQuestLines().size();
        ItemStack questLineItem = itemFactory.createNavigationItem(
            Material.WRITABLE_BOOK,
            "<light_purple><bold>任务线",
            List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>任务线数量: <yellow>" + questLineCount,
                "<gray>查看任务线进度和剧情",
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "",
                "<green>⚡ 点击查看详情"
            ),
            Math.max(1, questLineCount)
        );
        builder.setItem(17, questLineItem, event -> {
            playClickSound();
            new QuestLineGUI(plugin, player).open();
        });

        // ===== 第三行: 统计信息 =====
        ItemStack statsItem = createItem(Material.PAPER, "<green><bold>任务统计", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>总完成任务: <yellow>" + completedCount,
            "<gray>成就点数: <yellow>" + data.getAchievementPoints(),
            "<gray>当前活跃: <yellow>" + activeCount + "<gray>/" + plugin.getMaxActiveQuests(),
            "<dark_gray>━━━━━━━━━━━━━━━━━━"
        ));
        builder.setItem(22, statsItem, null);

        // ===== 第四行: 快速操作 =====
        // 追踪按钮
        ItemStack trackItem = createItem(Material.COMPASS, "<yellow><bold>任务追踪", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>在聊天栏显示当前任务进度",
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "",
            "<green>⚡ 点击追踪"
        ));
        builder.setItem(29, trackItem, event -> {
            playClickSound();
            close();
            player.performCommand("quest track");
        });

        // 帮助按钮
        ItemStack helpItem = createItem(Material.BOOKSHELF, "<blue><bold>帮助", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>查看任务系统帮助信息",
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "",
            "<green>⚡ 点击查看帮助"
        ));
        builder.setItem(33, helpItem, event -> {
            playClickSound();
            close();
            player.performCommand("quest help");
        });

        // ===== 底部导航栏 =====
        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 填充其余导航栏位置
        for (int i = 45; i < 54; i++) {
            if (i != QuestNavigation.SLOT_CLOSE) {
                builder.setItem(i, nav.createNavFiller());
            }
        }

        gui = builder.build();
    }
}
