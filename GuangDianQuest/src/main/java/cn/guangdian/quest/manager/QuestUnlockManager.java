package cn.guangdian.quest.manager;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 任务解锁管理器
 * 主线任务按order顺序自动解锁，无需额外配置
 */
public class QuestUnlockManager {

    private final GuangDianQuest plugin;

    public QuestUnlockManager(GuangDianQuest plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查任务是否已解锁
     */
    public boolean isQuestUnlocked(UUID playerId, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return false;

        // 主线任务：按order顺序解锁
        if (quest.getType() == QuestType.MAIN) {
            return checkMainQuestUnlock(playerId, quest);
        }

        // 其他任务：检查prerequisites
        return checkPrerequisites(playerId, quest);
    }

    /**
     * 主线任务解锁检查：前一个order的任务必须完成
     */
    private boolean checkMainQuestUnlock(UUID playerId, Quest quest) {
        if (quest.getOrder() <= 1) return true;

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        String questLine = quest.getQuestLine();
        if (questLine == null) return true;

        // 找到同一任务线中order-1的任务
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (questLine.equals(q.getQuestLine()) 
                && q.getType() == QuestType.MAIN 
                && q.getOrder() == quest.getOrder() - 1) {
                return data.isQuestCompleted(q.getId());
            }
        }
        return true; // 没找到前置任务，默认解锁
    }

    /**
     * 普通前置条件检查
     */
    private boolean checkPrerequisites(UUID playerId, Quest quest) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        for (String prereq : quest.getPrerequisites()) {
            if (!data.isQuestCompleted(prereq)) return false;
        }
        return true;
    }

    /**
     * 获取任务交互状态
     */
    public QuestStatus getStatus(UUID playerId, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return QuestStatus.UNKNOWN;

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        if (data.isQuestCompleted(questId)) return QuestStatus.COMPLETED;
        if (data.isQuestActive(questId)) {
            return plugin.getQuestManager().canComplete(playerId, questId) 
                ? QuestStatus.CAN_COMPLETE : QuestStatus.IN_PROGRESS;
        }
        if (!isQuestUnlocked(playerId, questId)) return QuestStatus.LOCKED;
        return QuestStatus.CAN_ACCEPT;
    }

    /**
     * 获取任务线中前一个任务的名称（用于显示解锁条件）
     */
    public String getPreviousQuestName(Quest quest) {
        if (quest.getType() != QuestType.MAIN || quest.getOrder() <= 1) return null;
        String questLine = quest.getQuestLine();
        if (questLine == null) return null;

        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (questLine.equals(q.getQuestLine()) 
                && q.getType() == QuestType.MAIN 
                && q.getOrder() == quest.getOrder() - 1) {
                return q.getName();
            }
        }
        return null;
    }

    /**
     * 任务完成时通知下一个任务解锁
     */
    public void onQuestComplete(Player player, String questId) {
        Quest completedQuest = plugin.getQuestManager().getQuest(questId);
        if (completedQuest == null || completedQuest.getType() != QuestType.MAIN) return;

        String questLine = completedQuest.getQuestLine();
        if (questLine == null) return;

        // 找下一个任务
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (questLine.equals(q.getQuestLine()) 
                && q.getType() == QuestType.MAIN 
                && q.getOrder() == completedQuest.getOrder() + 1) {
                player.sendMessage(GuangDianQuest.color(
                    "<green>✓ 新任务已解锁: <gold>" + q.getName()));
                try {
                    player.playSound(player.getLocation(), 
                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                } catch (Exception ignored) {}
                return;
            }
        }
    }

    /**
     * 任务状态枚举
     */
    public enum QuestStatus {
        LOCKED,       // 未解锁
        CAN_ACCEPT,   // 可接取
        IN_PROGRESS,  // 进行中
        CAN_COMPLETE, // 可完成
        COMPLETED,    // 已完成
        UNKNOWN       // 未知
    }
}
