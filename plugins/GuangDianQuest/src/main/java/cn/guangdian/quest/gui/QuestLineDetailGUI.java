package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestLine;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务线详情 GUI
 * 显示单个任务线的详细信息和任务列表
 * 导航: 主页 -> 任务线 -> 任务线详情
 */
public class QuestLineDetailGUI extends QuestGUI {

    private final QuestLine questLine;
    private final QuestGUI returnGUI;
    private final QuestItemFactory itemFactory;
    private final QuestNavigation nav;

    public QuestLineDetailGUI(GuangDianQuest plugin, Player player, QuestLine questLine, QuestGUI returnGUI) {
        super(plugin, player);
        this.questLine = questLine;
        this.returnGUI = returnGUI;
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
        int progress = plugin.getQuestLineManager().getQuestLineProgress(player.getUniqueId(), questLine.getId());
        int percent = questLine.getProgressPercent(progress);
        boolean isComplete = progress >= questLine.getLength() - 1;

        GUIBuilder builder = createBuilder("<gold>" + questLine.getName(), 6)
            .setFiller(FILLER_MATERIAL);

        // 边框
        for (int i = 0; i < 54; i++) {
            if (i < 9 || (i >= 36 && i < 45) || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // 面包屑导航 (slot 4)
        builder.setItem(4, createItem(Material.PAPER, "<yellow>任务中心 > 任务线 > " + questLine.getName(), List.of(
            "<gray>点击返回任务线列表"
        )), event -> {
            playClickSound();
            if (returnGUI != null) {
                returnGUI.open();
            } else {
                new QuestLineGUI(plugin, player).open();
            }
        });

        // 任务线信息 (slot 13)
        ItemStack infoItem = createQuestLineInfoItem(progress, percent, isComplete);
        builder.setItem(13, infoItem);

        // 进度显示 (slot 15)
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder("<green>");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "<dark_gray>█");
        }
        ItemStack progressItem = createItem(Material.EXPERIENCE_BOTTLE, "<yellow><bold>进度 " + percent + "%", List.of(
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            bar.toString(),
            "<dark_gray>━━━━━━━━━━━━━━━━━━",
            "<gray>已完成: <yellow>" + (progress + 1) + "<gray>/<yellow>" + questLine.getLength()
        ));
        builder.setItem(15, progressItem);

        // 填充任务 (slot 20-26, 29-35)
        List<String> questIds = questLine.getQuestIds();
        int[] slots = {20, 21, 22, 23, 24, 25, 26, 29, 30, 31, 32, 33, 34, 35};
        for (int i = 0; i < questIds.size() && i < slots.length; i++) {
            String questId = questIds.get(i);
            Quest quest = plugin.getQuestManager().getQuest(questId);

            if (quest != null) {
                boolean isQuestActive = data.isQuestActive(questId);
                boolean isQuestCompleted = data.isQuestCompleted(questId);
                boolean canComplete = isQuestActive && plugin.getQuestManager().canComplete(player.getUniqueId(), questId);
                boolean isCurrent = i == progress + 1;
                boolean isLocked = i > progress + 1 && !isQuestCompleted;

                ItemStack questItem = createQuestLineQuestItem(quest, i + 1, isQuestActive, isQuestCompleted,
                    canComplete, isCurrent, isLocked);
                builder.setItem(slots[i], questItem, event -> handleQuestClick(quest, isLocked));
            }
        }

        // 总奖励 (slot 40)
        if (isComplete) {
            ItemStack rewardItem = createItem(Material.GOLD_BLOCK, "<gold><bold>领取最终奖励", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<green>恭喜完成整个任务线！",
                "<yellow>点击领取最终奖励",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(40, rewardItem, event -> {
                playClickSound();
                // TODO: 发放任务线完成奖励
                sendMessage("<green>✔ 已领取任务线完成奖励！");
                playSuccessSound();
            });
        } else {
            ItemStack rewardItem = createItem(Material.CHEST, "<yellow><bold>任务线奖励", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>完成整个任务线可获得:",
                "<yellow>• 丰厚经验奖励",
                "<yellow>• 稀有物品",
                "<yellow>• 特殊称号",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(40, rewardItem);
        }

        // ===== 底部导航栏 =====
        // 主页按钮
        builder.setItem(QuestNavigation.SLOT_HOME, nav.createHomeButton(), nav.getHomeHandler());

        // 返回按钮
        builder.setItem(QuestNavigation.SLOT_BACK, nav.createBackButton("任务线列表"), event -> {
            playClickSound();
            if (returnGUI != null) {
                returnGUI.open();
            } else {
                new QuestLineGUI(plugin, player).open();
            }
        });

        // 上一页/空位
        builder.setItem(QuestNavigation.SLOT_PREV, nav.createNavFiller());

        // 当前位置
        builder.setItem(QuestNavigation.SLOT_INFO, nav.createLocationIndicator(questLine.getName(), "进度 " + percent + "%"));

        // 下一页/空位
        builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNavFiller());

        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 刷新按钮
        builder.setItem(QuestNavigation.SLOT_REFRESH, nav.createRefreshButton(),
            nav.getRefreshHandler(() -> new QuestLineDetailGUI(plugin, player, questLine, returnGUI).open()));

        gui = builder.build();
    }

    /**
     * 创建任务线信息物品
     */
    private ItemStack createQuestLineInfoItem(int progress, int percent, boolean isComplete) {
        Material material = isComplete ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK;

        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        // 描述
        if (questLine.getDescription() != null && !questLine.getDescription().isEmpty()) {
            lore.add("<gray>" + questLine.getDescription());
            lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
        }

        // 进度
        lore.add("<yellow>进度: " + percent + "%");
        lore.add("<gray>已完成: <white>" + (progress + 1) + "<gray>/<white>" + questLine.getLength());

        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        if (isComplete) {
            lore.add("");
            lore.add("<gold>✔ 任务线已完成！");
        }

        String name = isComplete ? "<gold><bold>[已完成] " + questLine.getName() : "<gold><bold>" + questLine.getName();
        return createItem(material, name, lore);
    }

    /**
     * 创建任务线中的任务物品
     */
    private ItemStack createQuestLineQuestItem(Quest quest, int order, boolean isActive,
                                                boolean isCompleted, boolean canComplete,
                                                boolean isCurrent, boolean isLocked) {
        Material material;
        String statusPrefix;

        if (isCompleted) {
            material = Material.LIME_DYE;
            statusPrefix = "<green>[已完成] ";
        } else if (canComplete) {
            material = Material.GOLD_INGOT;
            statusPrefix = "<gold>[可完成] ";
        } else if (isActive) {
            material = Material.ENCHANTED_BOOK;
            statusPrefix = "<yellow>[进行中] ";
        } else if (isCurrent) {
            material = Material.BOOK;
            statusPrefix = "<aqua>[当前] ";
        } else if (isLocked) {
            material = Material.BARRIER;
            statusPrefix = "<dark_gray>[锁定] ";
        } else {
            material = Material.BOOK;
            statusPrefix = "<gray>[未开始] ";
        }

        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
        lore.add("<gray>第 " + order + " 个任务");

        if (!isLocked) {
            // 描述
            for (String desc : quest.getDescription()) {
                lore.add("<dark_gray>" + desc);
            }

            // 目标
            if (isActive) {
                PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
                int[] progress = data.getProgress(quest.getId());
                lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
                lore.add("<yellow>当前进度:");
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    var obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    String status = current >= obj.getAmount() ? "<green>✔" : "<gray>○";
                    lore.add("  " + status + " <white>" + obj.getProgressText(current));
                }
            }

            lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
            lore.add("");

            if (isCompleted) {
                lore.add("<green>✔ 已完成");
            } else if (canComplete) {
                lore.add("<gold>⚡ 点击完成任务");
            } else if (isActive) {
                lore.add("<yellow>⚡ 点击查看详情");
            } else if (isCurrent) {
                lore.add("<green>⚡ 点击接取任务");
            }
        } else {
            lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
            lore.add("<red>完成前置任务后解锁");
        }

        return createItem(material, statusPrefix + quest.getName(), lore);
    }

    /**
     * 处理任务点击
     */
    private void handleQuestClick(Quest quest, boolean isLocked) {
        if (isLocked) {
            playErrorSound();
            sendMessage("<red>此任务尚未解锁！请先完成前置任务。");
            return;
        }

        playClickSound();

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        if (isCompleted) {
            sendMessage("<gray>此任务已完成");
        } else if (canComplete) {
            if (plugin.getQuestManager().completeQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 任务完成: " + quest.getName());
                refresh();
            } else {
                playErrorSound();
                sendMessage("<red>任务完成失败！");
            }
        } else if (isActive) {
            new QuestDetailGUI(plugin, player, quest, this).open();
        } else {
            if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 已接取任务: " + quest.getName());
                refresh();
            } else {
                playErrorSound();
                sendMessage("<red>无法接取任务！");
            }
        }
    }
}
