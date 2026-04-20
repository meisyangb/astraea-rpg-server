package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 每日任务 GUI
 * 显示今日可做的每日任务
 * 导航: 主页 -> 每日任务
 */
public class DailyQuestGUI extends QuestGUI {

    private final QuestItemFactory itemFactory;
    private final QuestNavigation nav;

    public DailyQuestGUI(GuangDianQuest plugin, Player player) {
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
        int dailyCompleted = data.getDailyCompletedCount();
        int dailyLimit = plugin.getDailyQuestLimit();
        List<String> dailyQuests = plugin.getDailyManager().getDailyQuests(player.getUniqueId());

        GUIBuilder builder = createBuilder("<gold><bold>每日任务", 5)
            .setFiller(FILLER_MATERIAL);

        // 边框
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // 面包屑导航 (slot 4)
        builder.setItem(4, createItem(Material.PAPER, "<yellow>任务中心 > 每日任务", List.of(
            "<gray>点击返回任务中心主页"
        )), nav.getHomeHandler());

        // 标题信息 (slot 13)
        ItemStack infoItem = createItem(Material.CLOCK, "<gold><bold>今日任务", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>今日完成: <yellow>" + dailyCompleted + "<gray>/" + dailyLimit,
            "<gray>可用任务: <yellow>" + dailyQuests.size(),
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "",
            "<yellow>每日任务会在每天",
            "<yellow>凌晨自动刷新"
        ));
        builder.setItem(13, infoItem, null);

        // 进度显示 (slot 15)
        int percent = dailyLimit > 0 ? (dailyCompleted * 100 / dailyLimit) : 0;
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder("<green>");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "<dark_gray>█");
        }
        ItemStack progressItem = createItem(Material.EXPERIENCE_BOTTLE, "<yellow><bold>完成进度 " + percent + "%", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            bar.toString(),
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>已完成: <yellow>" + dailyCompleted + "<gray>/<yellow>" + dailyLimit
        ));
        builder.setItem(15, progressItem, null);

        // 填充每日任务 (slot 20-26, 29-35)
        int[] slots = {20, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 33, 34, 35};
        int index = 0;
        for (String questId : dailyQuests) {
            if (index >= slots.length) break;

            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest != null) {
                ItemStack questItem = itemFactory.createDailyQuestItem(quest, player);
                builder.setItem(slots[index], questItem, event -> handleDailyQuestClick(quest));
                index++;
            }
        }

        // 如果没有每日任务
        if (dailyQuests.isEmpty()) {
            ItemStack noQuestItem = createItem(Material.BARRIER, "<red><bold>暂无每日任务", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>今日没有可用的每日任务",
                "<gray>请明天再来查看",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, noQuestItem);
        }

        // ===== 底部导航栏 =====
        // 主页按钮
        builder.setItem(QuestNavigation.SLOT_HOME, nav.createHomeButton(), nav.getHomeHandler());

        // 返回按钮
        builder.setItem(QuestNavigation.SLOT_BACK, nav.createBackButton("任务中心"), nav.getHomeHandler());

        // 上一页/空位
        builder.setItem(QuestNavigation.SLOT_PREV, nav.createNavFiller());

        // 当前位置
        builder.setItem(QuestNavigation.SLOT_INFO, nav.createLocationIndicator("每日任务", "今日完成 " + dailyCompleted + "/" + dailyLimit), null);

        // 下一页/空位
        builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNavFiller());

        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 刷新按钮
        builder.setItem(QuestNavigation.SLOT_REFRESH, nav.createRefreshButton(),
            nav.getRefreshHandler(() -> new DailyQuestGUI(plugin, player).open()));

        gui = builder.build();
    }

    /**
     * 处理每日任务点击
     */
    private void handleDailyQuestClick(Quest quest) {
        playClickSound();

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        if (isCompleted) {
            sendMessage("<gray>此任务今日已完成");
        } else if (canComplete) {
            // 可完成，尝试完成
            if (plugin.getQuestManager().completeQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 每日任务完成: " + quest.getName());
                refresh();
            } else {
                playErrorSound();
                sendMessage("<red>任务完成失败！");
            }
        } else if (isActive) {
            // 进行中，显示详情
            new QuestDetailGUI(plugin, player, quest, this).open();
        } else {
            // 可接取，尝试接取
            if (data.getDailyCompletedCount() >= plugin.getDailyQuestLimit()) {
                playErrorSound();
                sendMessage("<red>今日每日任务已完成上限！");
                return;
            }

            if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 已接取每日任务: " + quest.getName());
                refresh();
            } else {
                playErrorSound();
                sendMessage("<red>无法接取任务！");
            }
        }
    }
}
