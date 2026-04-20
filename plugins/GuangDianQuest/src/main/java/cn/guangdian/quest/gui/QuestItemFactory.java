package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.quest.model.QuestReward;
import cn.guangdian.quest.model.QuestType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务物品工厂
 * 负责生成任务相关的 GUI 物品
 */
public class QuestItemFactory {

    private final GuangDianQuest plugin;

    public QuestItemFactory(@NotNull GuangDianQuest plugin) {
        this.plugin = plugin;
    }

    /**
     * 根据任务类型获取材质
     */
    public Material getMaterialByType(@NotNull QuestType type) {
        return switch (type) {
            case MAIN -> Material.NETHER_STAR;
            case SIDE -> Material.BOOK;
            case DAILY -> Material.CLOCK;
            case ACHIEVEMENT -> Material.GOLDEN_APPLE;
        };
    }

    /**
     * 根据任务状态获取材质
     */
    public Material getMaterialByStatus(@NotNull QuestType type, boolean isActive, boolean isCompleted) {
        if (isCompleted) {
            return Material.LIME_DYE;
        }
        if (isActive) {
            return switch (type) {
                case MAIN -> Material.ENCHANTED_BOOK;
                case SIDE -> Material.WRITABLE_BOOK;
                case DAILY -> Material.CLOCK;
                case ACHIEVEMENT -> Material.ENCHANTED_GOLDEN_APPLE;
            };
        }
        return getMaterialByType(type);
    }

    /**
     * 创建任务物品
     */
    public ItemStack createQuestItem(@NotNull Quest quest, @NotNull Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        Material material = getMaterialByStatus(quest.getType(), isActive, isCompleted);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 设置名称
            String statusPrefix = isCompleted ? "<green>[已完成] " :
                canComplete ? "<gold>[可完成] " :
                isActive ? "<yellow>[进行中] " : "<gray>[可接取] ";
            meta.displayName(plugin.getMiniMessageService().colorize(statusPrefix + quest.getFullName()));

            // 设置 Lore
            List<Component> lore = new ArrayList<>();

            // 描述
            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            for (String desc : quest.getDescription()) {
                lore.add(plugin.getMiniMessageService().colorize("<gray>" + desc));
            }
            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));

            // 目标
            if (isActive) {
                lore.add(plugin.getMiniMessageService().colorize("<yellow>任务目标:"));
                int[] progress = data.getProgress(quest.getId());
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    QuestObjective obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    boolean objComplete = current >= obj.getAmount();
                    String status = objComplete ? "<green>✔" : "<gray>○";
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  " + status + " <white>" + obj.getProgressText(current)));
                }
                lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            } else if (!isCompleted) {
                lore.add(plugin.getMiniMessageService().colorize("<yellow>任务目标:"));
                for (QuestObjective obj : quest.getObjectives()) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  <gray>○ <white>" + obj.getDescription()));
                }
                lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            }

            // 奖励
            QuestReward reward = quest.getReward();
            if (reward != null && reward.hasRewards()) {
                lore.add(plugin.getMiniMessageService().colorize("<gold>任务奖励:"));
                if (reward.getExperience() > 0) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  <gray>• <aqua>经验: <white>" + reward.getExperience()));
                }
                if (reward.getPoints() > 0) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  <gray>• <yellow>点券: <white>" + reward.getPoints()));
                }
                if (!reward.getItems().isEmpty()) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  <gray>• <green>物品奖励"));
                }
                lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            }

            // 要求
            if (!isActive && !isCompleted) {
                if (quest.getRequiredLevel() > 0) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "<red>等级要求: Lv." + quest.getRequiredLevel()));
                }
                if (quest.hasPrerequisites()) {
                    lore.add(plugin.getMiniMessageService().colorize(
                        "<red>前置任务: " + String.join(", ", quest.getPrerequisites())));
                }
            }

            // 操作提示
            lore.add(plugin.getMiniMessageService().colorize(""));
            if (isCompleted) {
                lore.add(plugin.getMiniMessageService().colorize("<green>✔ 已完成此任务"));
            } else if (canComplete) {
                lore.add(plugin.getMiniMessageService().colorize("<gold>⚡ 点击完成任务"));
            } else if (isActive) {
                lore.add(plugin.getMiniMessageService().colorize("<yellow>⚡ 点击查看详情"));
            } else {
                lore.add(plugin.getMiniMessageService().colorize("<green>⚡ 点击接取任务"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 创建每日任务物品
     */
    public ItemStack createDailyQuestItem(@NotNull Quest quest, @NotNull Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        Material material = isCompleted ? Material.LIME_DYE :
            canComplete ? Material.ENCHANTED_BOOK :
            isActive ? Material.CLOCK : Material.CLOCK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String statusPrefix = isCompleted ? "<green>[已完成] " :
                canComplete ? "<gold>[可完成] " :
                isActive ? "<yellow>[进行中] " : "<gray>[可接取] ";
            meta.displayName(plugin.getMiniMessageService().colorize(statusPrefix + quest.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));

            // 描述
            for (String desc : quest.getDescription()) {
                lore.add(plugin.getMiniMessageService().colorize("<gray>" + desc));
            }
            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));

            // 目标进度
            if (isActive) {
                lore.add(plugin.getMiniMessageService().colorize("<yellow>当前进度:"));
                int[] progress = data.getProgress(quest.getId());
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    QuestObjective obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    boolean objComplete = current >= obj.getAmount();
                    String status = objComplete ? "<green>✔" : "<gray>○";
                    lore.add(plugin.getMiniMessageService().colorize(
                        "  " + status + " <white>" + obj.getProgressText(current)));
                }
            }

            // 奖励
            QuestReward reward = quest.getReward();
            if (reward != null && reward.hasRewards()) {
                lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
                lore.add(plugin.getMiniMessageService().colorize("<gold>奖励: <yellow>" + reward.getSummary()));
            }

            lore.add(plugin.getMiniMessageService().colorize(""));
            if (isCompleted) {
                lore.add(plugin.getMiniMessageService().colorize("<green>✔ 今日已完成"));
            } else if (canComplete) {
                lore.add(plugin.getMiniMessageService().colorize("<gold>⚡ 点击完成任务"));
            } else if (isActive) {
                lore.add(plugin.getMiniMessageService().colorize("<yellow>⚡ 点击查看详情"));
            } else {
                lore.add(plugin.getMiniMessageService().colorize("<green>⚡ 点击接取任务"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 创建任务线物品
     */
    public ItemStack createQuestLineItem(@NotNull cn.guangdian.quest.model.QuestLine questLine,
                                          @NotNull Player player) {
        int progress = plugin.getQuestLineManager().getQuestLineProgress(player.getUniqueId(), questLine.getId());
        int percent = questLine.getProgressPercent(progress);
        boolean isComplete = progress >= questLine.getLength() - 1;

        Material material = isComplete ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String statusPrefix = isComplete ? "<gold>[已完成] " : "<yellow>";
            meta.displayName(plugin.getMiniMessageService().colorize(statusPrefix + questLine.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));

            // 描述
            if (questLine.getDescription() != null && !questLine.getDescription().isEmpty()) {
                lore.add(plugin.getMiniMessageService().colorize("<gray>" + questLine.getDescription()));
                lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            }

            // 进度条
            lore.add(plugin.getMiniMessageService().colorize("<yellow>进度: " + percent + "%"));
            lore.add(plugin.getMiniMessageService().colorize(
                "<gray>已完成: <white>" + (progress + 1) + "<gray>/<white>" + questLine.getLength()));

            // 进度条可视化
            int filled = percent / 10;
            StringBuilder bar = new StringBuilder("<green>");
            for (int i = 0; i < 10; i++) {
                bar.append(i < filled ? "█" : "<dark_gray>█");
            }
            lore.add(plugin.getMiniMessageService().colorize(bar.toString()));

            lore.add(plugin.getMiniMessageService().colorize("<dark_gray>━━━━━━━━━━━━━━━━━━"));
            lore.add(plugin.getMiniMessageService().colorize(""));
            lore.add(plugin.getMiniMessageService().colorize("<yellow>⚡ 点击查看详情"));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 创建主菜单导航物品
     */
    public ItemStack createNavigationItem(@NotNull Material material, @NotNull String name,
                                          @NotNull List<String> lore, int count) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(count, 64)));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(plugin.getMiniMessageService().colorize(name));
            meta.lore(lore.stream()
                .map(line -> plugin.getMiniMessageService().colorize(line))
                .toList());
            item.setItemMeta(meta);
        }

        return item;
    }
}
