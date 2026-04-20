package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.quest.model.QuestReward;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务详情 GUI
 * 显示单个任务的详细信息
 * 导航: 主页 -> 任务列表 -> 任务详情
 */
public class QuestDetailGUI extends QuestGUI {

    private final Quest quest;
    private final QuestGUI returnGUI;
    private final QuestNavigation nav;

    public QuestDetailGUI(GuangDianQuest plugin, Player player, Quest quest, QuestGUI returnGUI) {
        super(plugin, player);
        this.quest = quest;
        this.returnGUI = returnGUI;
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
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        String statusText = isCompleted ? "<green>[已完成]" :
            canComplete ? "<gold>[可完成]" :
            isActive ? "<yellow>[进行中]" : "<gray>[未接取]";

        GUIBuilder builder = createBuilder("<gold>任务详情 " + statusText, 5)
            .setFiller(FILLER_MATERIAL);

        // 边框
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // 面包屑导航 (slot 4)
        String breadcrumb = "任务中心 > " + (isActive ? "进行中任务" : isCompleted ? "已完成任务" : "可接取任务") + " > " + quest.getName();
        builder.setItem(4, createItem(Material.PAPER, "<yellow>" + breadcrumb, List.of(
            "<gray>点击返回任务列表"
        )), event -> {
            playClickSound();
            if (returnGUI != null) {
                returnGUI.open();
            } else {
                new QuestMainGUI(plugin, player).open();
            }
        });

        // 任务信息 (slot 11)
        ItemStack infoItem = createQuestInfoItem(isActive, isCompleted, canComplete);
        builder.setItem(11, infoItem, null);

        // 任务描述 (slot 13)
        ItemStack descItem = createDescriptionItem();
        builder.setItem(13, descItem, null);

        // 任务目标 (slot 15)
        ItemStack objectiveItem = createObjectiveItem(isActive, data);
        builder.setItem(15, objectiveItem, null);

        // 任务奖励 (slot 17)
        ItemStack rewardItem = createRewardItem();
        builder.setItem(17, rewardItem, null);

        // 操作按钮 (slot 22)
        if (isCompleted) {
            // 已完成 - 显示完成状态
            ItemStack completedItem = createItem(Material.LIME_DYE, "<green><bold>✔ 已完成", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<gray>此任务已完成",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, completedItem, null);
        } else if (canComplete) {
            // 可完成 - 显示完成按钮
            ItemStack completeItem = createItem(Material.GOLD_INGOT, "<gold><bold>⚡ 完成任务", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<yellow>所有目标已完成！",
                "<green>点击领取奖励",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, completeItem, event -> {
                playClickSound();
                if (plugin.getQuestManager().completeQuest(player.getUniqueId(), quest.getId())) {
                    playSuccessSound();
                    sendMessage("<green>✔ 任务完成: " + quest.getName());
                    // 完成后返回列表
                    if (returnGUI != null) {
                        returnGUI.open();
                    } else {
                        new QuestMainGUI(plugin, player).open();
                    }
                } else {
                    playErrorSound();
                    sendMessage("<red>任务完成失败！");
                }
            });
        } else if (isActive) {
            // 进行中 - 显示放弃按钮
            ItemStack abandonItem = createItem(Material.REDSTONE, "<red><bold>放弃任务", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<red>⚠ 放弃后进度将丢失",
                "<gray>点击放弃此任务",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, abandonItem, event -> {
                playClickSound();
                if (plugin.getQuestManager().abandonQuest(player.getUniqueId(), quest.getId())) {
                    playSuccessSound();
                    sendMessage("<yellow>已放弃任务: " + quest.getName());
                    // 放弃后返回列表
                    if (returnGUI != null) {
                        returnGUI.open();
                    } else {
                        new QuestMainGUI(plugin, player).open();
                    }
                } else {
                    playErrorSound();
                    sendMessage("<red>放弃任务失败！");
                }
            });
        } else {
            // 可接取 - 显示接取按钮
            ItemStack acceptItem = createItem(Material.EMERALD, "<green><bold>⚡ 接取任务", List.of(
                "<dark_gray>━━━━━━━━━━━━━━━━━━",
                "<green>点击接取此任务",
                "<dark_gray>━━━━━━━━━━━━━━━━━━"
            ));
            builder.setItem(22, acceptItem, event -> {
                playClickSound();
                if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), quest.getId())) {
                    playSuccessSound();
                    sendMessage("<green>✔ 已接取任务: " + quest.getName());
                    // 接取后返回列表
                    if (returnGUI != null) {
                        returnGUI.open();
                    } else {
                        new QuestMainGUI(plugin, player).open();
                    }
                } else {
                    playErrorSound();
                    sendMessage("<red>无法接取任务！请检查前置条件。");
                }
            });
        }

        // ===== 底部导航栏 =====
        // 主页按钮
        builder.setItem(QuestNavigation.SLOT_HOME, nav.createHomeButton(), nav.getHomeHandler());

        // 返回按钮
        builder.setItem(QuestNavigation.SLOT_BACK, nav.createBackButton("任务列表"), event -> {
            playClickSound();
            if (returnGUI != null) {
                returnGUI.open();
            } else {
                new QuestMainGUI(plugin, player).open();
            }
        });

        // 上一页/空位
        builder.setItem(QuestNavigation.SLOT_PREV, nav.createNavFiller());

        // 当前位置
        builder.setItem(QuestNavigation.SLOT_INFO, nav.createLocationIndicator(quest.getName(), "任务详情"), null);

        // 下一页/空位
        builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNavFiller());

        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 刷新按钮
        builder.setItem(QuestNavigation.SLOT_REFRESH, nav.createRefreshButton(),
            nav.getRefreshHandler(() -> new QuestDetailGUI(plugin, player, quest, returnGUI).open()));

        gui = builder.build();
    }

    /**
     * 创建任务信息物品
     */
    private ItemStack createQuestInfoItem(boolean isActive, boolean isCompleted, boolean canComplete) {
        Material material = isCompleted ? Material.LIME_DYE :
            canComplete ? Material.GOLD_INGOT :
            isActive ? Material.ENCHANTED_BOOK : Material.BOOK;

        String status = isCompleted ? "<green>[已完成]" :
            canComplete ? "<gold>[可完成]" :
            isActive ? "<yellow>[进行中]" : "<gray>[未接取]";

        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
        lore.add("<gray>类型: <white>" + quest.getType().getDisplayName());

        if (quest.getRequiredLevel() > 0) {
            lore.add("<gray>等级要求: <yellow>Lv." + quest.getRequiredLevel());
        }

        if (quest.hasPrerequisites()) {
            lore.add("<gray>前置任务: <white>" + String.join(", ", quest.getPrerequisites()));
        }

        if (quest.isRepeatable()) {
            lore.add("<gray>可重复: <green>是");
            if (quest.getCooldown() > 0) {
                lore.add("<gray>冷却时间: <yellow>" + formatCooldown(quest.getCooldown()));
            }
        }

        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");
        lore.add("");
        lore.add("<yellow>状态: " + status);

        return createItem(material, quest.getFullName(), lore);
    }

    /**
     * 创建描述物品
     */
    private ItemStack createDescriptionItem() {
        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        for (String desc : quest.getDescription()) {
            lore.add("<gray>" + desc);
        }

        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        return createItem(Material.PAPER, "<yellow><bold>任务描述", lore);
    }

    /**
     * 创建目标物品
     */
    private ItemStack createObjectiveItem(boolean isActive, PlayerQuestData data) {
        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        int[] progress = isActive ? data.getProgress(quest.getId()) : null;

        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            QuestObjective obj = quest.getObjective(i);

            if (isActive && progress != null && i < progress.length) {
                int current = progress[i];
                boolean objComplete = current >= obj.getAmount();
                String status = objComplete ? "<green>✔" : "<gray>○";
                lore.add("  " + status + " <white>" + obj.getProgressText(current));
            } else {
                lore.add("  <gray>○ <white>" + obj.getDescription());
            }
        }

        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        return createItem(Material.COMPASS, "<yellow><bold>任务目标", lore);
    }

    /**
     * 创建奖励物品
     */
    private ItemStack createRewardItem() {
        QuestReward reward = quest.getReward();
        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        if (reward != null && reward.hasRewards()) {
            if (reward.getExperience() > 0) {
                lore.add("<gray>• <aqua>经验值: <white>" + reward.getExperience());
            }
            if (reward.getPoints() > 0) {
                lore.add("<gray>• <yellow>点券: <white>" + reward.getPoints());
            }
            if (!reward.getItems().isEmpty()) {
                lore.add("<gray>• <green>物品奖励:");
                for (String itemKey : reward.getItems().keySet()) {
                    int amount = reward.getItems().get(itemKey);
                    lore.add("  <dark_gray>- <white>" + itemKey + " x" + amount);
                }
            }
            if (!reward.getCommands().isEmpty()) {
                lore.add("<gray>• <light_purple>特殊奖励");
            }
        } else {
            lore.add("<gray>无奖励");
        }

        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━");

        return createItem(Material.GOLD_NUGGET, "<gold><bold>任务奖励", lore);
    }

    /**
     * 格式化冷却时间
     */
    private String formatCooldown(int seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }
}
