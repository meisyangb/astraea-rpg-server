package cn.guangdian.quest.service;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 聊天框消息服务
 * 负责所有任务相关消息的格式化和输出
 */
public class ChatMessageService {

    private final GuangDianQuest plugin;

    // 边框宽度常量
    private static final String BORDER_TOP    = "<gold><bold>╔══════════════════════════════════════════════════╗";
    private static final String BORDER_BOTTOM = "<gold><bold>╚══════════════════════════════════════════════════╝";
    private static final String BORDER_MID    = "<gold><bold>╠══════════════════════════════════════════════════╣";
    private static final String SPACER        = "<gold><bold>║                                                  ║";

    // 截断长度常量
    private static final int TRUNCATE_NAME        = 35;
    private static final int TRUNCATE_FULLNAME    = 35;
    private static final int TRUNCATE_DESC        = 45;
    private static final int TRUNCATE_OBJ_SUMMARY = 25;
    private static final int TRUNCATE_OBJ_DETAIL  = 35;
    private static final int TRUNCATE_OBJ_TRACK   = 25;
    private static final int TRUNCATE_REWARD      = 40;
    private static final int TRUNCATE_PREREQ      = 30;

    // 显示数量常量
    private static final int MAX_ACTIVE_SHOWN   = 8;
    private static final int MAX_AVAILABLE_SHOWN = 5;
    private static final int MAX_OBJ_DETAIL     = 8;
    private static final int MAX_OBJ_TRACK      = 5;

    public ChatMessageService(GuangDianQuest plugin) {
        this.plugin = plugin;
    }

    /**
     * 发送任务列表
     */
    public void sendQuestList(Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        int activeCount = data.getActiveQuestCount();
        int maxCount = plugin.getMaxActiveQuests();

        // 标题
        player.sendMessage(color(BORDER_TOP));
        player.sendMessage(color("<gold><bold>║  📋 任务日志                                      ║"));
        player.sendMessage(color(BORDER_MID));

        // 进行中任务
        player.sendMessage(color("<yellow><bold>║  ◐ 进行中: <white>" + activeCount + "/" + maxCount));
        player.sendMessage(color(SPACER));

        if (activeCount == 0) {
            player.sendMessage(color("<gray>║  暂无进行中的任务"));
        } else {
            int shown = 0;
            for (String questId : data.getActiveQuestIds()) {
                if (shown >= MAX_ACTIVE_SHOWN) break;
                Quest quest = plugin.getQuestManager().getQuest(questId);
                if (quest == null) continue;

                sendQuestSummary(player, quest, data);
                shown++;
            }

            if (activeCount > MAX_ACTIVE_SHOWN) {
                player.sendMessage(color("<gray>║  还有 " + (activeCount - MAX_ACTIVE_SHOWN) + " 个任务..."));
            }
        }

        // 可接取任务
        List<String> available = plugin.getQuestManager().getAvailableQuests(player.getUniqueId());
        player.sendMessage(color(SPACER));
        player.sendMessage(color("<aqua><bold>║  ◐ 可接取: <white>" + available.size() + " <aqua><bold>个"));

        if (!available.isEmpty()) {
            int shown = 0;
            for (String questId : available) {
                if (shown >= MAX_AVAILABLE_SHOWN) break;
                Quest quest = plugin.getQuestManager().getQuest(questId);
                if (quest == null) continue;

                String prefix = quest.getType().getPrefix();
                String name = truncate(quest.getName(), TRUNCATE_NAME);
                player.sendMessage(color("<gray>║    " + prefix + " <white>" + name));
                shown++;
            }

            if (available.size() > MAX_AVAILABLE_SHOWN) {
                player.sendMessage(color("<gray>║  还有 " + (available.size() - MAX_AVAILABLE_SHOWN) + " 个可接取..."));
            }
        }

        player.sendMessage(color(BORDER_BOTTOM));
        player.sendMessage(color("<gray>使用 <yellow>/quest info <ID> <gray>查看详情"));
    }

    /**
     * 发送任务摘要
     */
    private void sendQuestSummary(Player player, Quest quest, PlayerQuestData data) {
        String prefix = quest.getType().getPrefix();
        String name = truncate(quest.getName(), TRUNCATE_NAME);
        int[] progress = data.getProgress(quest.getId());
        int percent = calculateProgressPercent(progress, quest);

        player.sendMessage(color("<yellow><bold>║  " + prefix + " <white>" + name));
        player.sendMessage(color("<dark_green>║    " + buildProgressBar(percent) + " " + percent + "%"));

        // 显示第一个目标
        if (quest.getObjectiveCount() > 0 && progress != null) {
            QuestObjective obj = quest.getObjective(0);
            int current = Math.min(progress[0], obj.getAmount());
            String icon = current >= obj.getAmount() ? "<green>✔" : "<gray>○";
            String objDesc = truncate(obj.getDescription(), TRUNCATE_OBJ_SUMMARY);
            player.sendMessage(color("║    " + icon + " <white>" + objDesc + " <gray>" + current + "/" + obj.getAmount()));
        }
    }

    /**
     * 发送任务详情
     */
    public void sendQuestDetail(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) {
            player.sendMessage(color("<red>任务不存在！"));
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(questId);
        boolean isCompleted = data.isQuestCompleted(questId);
        int[] progress = isActive ? data.getProgress(questId) : null;

        // 标题
        player.sendMessage(color(BORDER_TOP));
        player.sendMessage(color("<gold><bold>║  " + quest.getType().getPrefix() + " " + truncate(quest.getFullName(), TRUNCATE_FULLNAME)));
        player.sendMessage(color(BORDER_MID));

        // 基本信息
        player.sendMessage(color("<gray>║  类型: <aqua>" + quest.getType().getDisplayName()));
        if (quest.getRequiredLevel() > 0) {
            player.sendMessage(color("<gray>║  等级: <red>Lv." + quest.getRequiredLevel()));
        }
        if (!quest.getPrerequisites().isEmpty()) {
            String preReq = truncate(String.join(", ", quest.getPrerequisites()), TRUNCATE_PREREQ);
            player.sendMessage(color("<gray>║  前置: <dark_aqua>" + preReq));
        }

        // 描述
        player.sendMessage(color(SPACER));
        for (String line : quest.getDescription()) {
            player.sendMessage(color("<white>║  " + truncate(line, TRUNCATE_DESC)));
        }

        // 目标
        player.sendMessage(color(SPACER));
        player.sendMessage(color("<green><bold>║  ◐ 目标"));

        int maxObj = Math.min(quest.getObjectiveCount(), MAX_OBJ_DETAIL);
        for (int i = 0; i < maxObj; i++) {
            QuestObjective obj = quest.getObjective(i);
            int current = (progress != null && i < progress.length) ? progress[i] : 0;
            boolean done = current >= obj.getAmount();
            String icon = done ? "<green>✔" : "<gray>○";
            String objDesc = truncate(obj.getDescription(), TRUNCATE_OBJ_DETAIL);
            player.sendMessage(color("║    " + icon + " <white>" + objDesc + " <gray>" + current + "/" + obj.getAmount()));
        }

        if (quest.getObjectiveCount() > MAX_OBJ_DETAIL) {
            player.sendMessage(color("<gray>║  还有 " + (quest.getObjectiveCount() - MAX_OBJ_DETAIL) + " 个目标..."));
        }

        // 奖励
        if (quest.getReward().hasRewards()) {
            player.sendMessage(color(SPACER));
            player.sendMessage(color("<yellow><bold>║  ◐ 奖励"));
            String rewardSummary = truncate(quest.getReward().getSummary(), TRUNCATE_REWARD);
            player.sendMessage(color("<white>║    " + rewardSummary));
        }

        // 状态和操作
        player.sendMessage(color(SPACER));
        if (isCompleted) {
            player.sendMessage(color("<green><bold>║  ✔ 已完成"));
        } else if (isActive) {
            boolean allDone = true;
            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                int current = (progress != null && i < progress.length) ? progress[i] : 0;
                if (current < obj.getAmount()) {
                    allDone = false;
                    break;
                }
            }

            if (allDone) {
                player.sendMessage(color("<green><bold>║  所有目标已完成！"));
                player.sendMessage(color("<gray>║  使用 <yellow>/quest complete " + questId + " <gray>提交"));
            } else {
                player.sendMessage(color("<yellow><bold>║  进行中..."));
            }
            player.sendMessage(color("<gray>║  使用 <yellow>/quest abandon " + questId + " <gray>放弃"));
        } else {
            player.sendMessage(color("<gray>║  使用 <yellow>/quest accept " + questId + " <gray>接取"));
        }

        player.sendMessage(color(BORDER_BOTTOM));
    }

    /**
     * 发送每日任务列表
     */
    public void sendDailyQuests(Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        List<String> dailyQuests = plugin.getDailyManager().getDailyQuests(player.getUniqueId());

        player.sendMessage(color(BORDER_TOP));
        player.sendMessage(color("<gold><bold>║  📅 每日任务                                      ║"));
        player.sendMessage(color(BORDER_MID));
        player.sendMessage(color("<gray>║  今日完成: <yellow>" + data.getDailyCompletedCount() + "/" + plugin.getDailyQuestLimit()));
        player.sendMessage(color(SPACER));

        if (dailyQuests.isEmpty()) {
            player.sendMessage(color("<gray>║  今日暂无每日任务"));
        } else {
            for (String questId : dailyQuests) {
                Quest quest = plugin.getQuestManager().getQuest(questId);
                if (quest == null) continue;

                boolean isActive = data.isQuestActive(questId);
                boolean isCompleted = data.isQuestCompleted(questId);
                String name = truncate(quest.getName(), TRUNCATE_NAME);

                if (isCompleted) {
                    player.sendMessage(color("<green>║  ✔ " + name + " <gray>已完成"));
                } else if (isActive) {
                    int[] progress = data.getProgress(questId);
                    if (quest.getObjectiveCount() > 0 && progress != null) {
                        QuestObjective obj = quest.getObjective(0);
                        int current = Math.min(progress[0], obj.getAmount());
                        player.sendMessage(color("<yellow>║  ◐ " + name + " <gray>" + current + "/" + obj.getAmount()));
                    } else {
                        player.sendMessage(color("<yellow>║  ◐ " + name));
                    }
                } else {
                    player.sendMessage(color("<white>║  ○ " + name + " <gray>可接取"));
                }
            }
        }

        player.sendMessage(color(BORDER_BOTTOM));
    }

    /**
     * 发送任务追踪
     */
    public void sendQuestTrack(Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        if (data.getActiveQuestCount() == 0) {
            player.sendMessage(color("<gray>当前没有进行中的任务"));
            return;
        }

        player.sendMessage(color(BORDER_TOP));
        player.sendMessage(color("<gold><bold>║  🎯 任务追踪                                      ║"));
        player.sendMessage(color(BORDER_MID));

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            String prefix = quest.getType().getPrefix();
            String name = truncate(quest.getName(), TRUNCATE_NAME);
            int[] progress = data.getProgress(questId);

            player.sendMessage(color("<yellow><bold>║  " + prefix + " <white>" + name));

            int maxObj = Math.min(quest.getObjectiveCount(), MAX_OBJ_TRACK);
            for (int i = 0; i < maxObj; i++) {
                QuestObjective obj = quest.getObjective(i);
                int current = (progress != null && i < progress.length) ? progress[i] : 0;
                String icon = current >= obj.getAmount() ? "<green>✔" : "<gray>○";
                String objDesc = truncate(obj.getDescription(), TRUNCATE_OBJ_TRACK);
                player.sendMessage(color("║    " + icon + " <white>" + objDesc + " <gray>" + current + "/" + obj.getAmount()));
            }

            player.sendMessage(color(SPACER));
        }

        player.sendMessage(color(BORDER_BOTTOM));
    }

    /**
     * 发送任务线列表
     */
    public void sendQuestLines(Player player) {
        player.sendMessage(color(BORDER_TOP));
        player.sendMessage(color("<gold><bold>║  📚 任务线                                        ║"));
        player.sendMessage(color(BORDER_MID));

        for (cn.guangdian.quest.model.QuestLine line : plugin.getQuestLineManager().getAllQuestLines()) {
            int progress = plugin.getQuestLineManager().getQuestLineProgress(player.getUniqueId(), line.getId());
            int percent = line.getProgressPercent(progress);
            String name = truncate(line.getName(), TRUNCATE_NAME);

            player.sendMessage(color("<yellow>║  " + name));
            player.sendMessage(color("<dark_green>║    " + buildProgressBar(percent) + " " + percent + "% <gray>(" + (progress + 1) + "/" + line.getLength() + ")"));
        }

        player.sendMessage(color(BORDER_BOTTOM));
    }

    /**
     * 发送成功消息
     */
    public void sendSuccess(Player player, String message) {
        player.sendMessage(color("<green><bold>✔ <reset><green>" + message));
    }

    /**
     * 发送错误消息
     */
    public void sendError(Player player, String message) {
        player.sendMessage(color("<red><bold>✖ <reset><red>" + message));
    }

    /**
     * 发送警告消息
     */
    public void sendWarning(Player player, String message) {
        player.sendMessage(color("<yellow><bold>⚠ <reset><yellow>" + message));
    }

    /**
     * 发送信息消息
     */
    public void sendInfo(Player player, String message) {
        player.sendMessage(color("<aqua><bold>ℹ <reset><aqua>" + message));
    }

    // ==================== 工具方法 ====================

    private Component color(String text) {
        return GuangDianQuest.color(text);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    private String buildProgressBar(int percent) {
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? '█' : '░');
        }
        return bar.toString();
    }

    private int calculateProgressPercent(int[] progress, Quest quest) {
        if (progress == null || quest.getObjectiveCount() == 0) return 0;

        int total = 0;
        int completed = 0;
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            total += quest.getObjective(i).getAmount();
            completed += Math.min(progress[i], quest.getObjective(i).getAmount());
        }

        return total > 0 ? (completed * 100 / total) : 0;
    }
}
