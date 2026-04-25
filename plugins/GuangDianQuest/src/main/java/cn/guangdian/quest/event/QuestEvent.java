package cn.guangdian.quest.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 任务事件基类
 *
 * <p>任务相关事件的基类。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class QuestEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    protected final UUID playerId;
    protected final String questId;
    protected final String questName;
    protected final String questType;

    public QuestEvent(UUID playerId, String questId, String questName, String questType) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.questId = questId;
        this.questName = questName;
        this.questType = questType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getQuestId() {
        return questId;
    }

    public String getQuestName() {
        return questName;
    }

    public String getQuestType() {
        return questType;
    }

    /**
     * 任务接取事件
     */
    public static class Accept extends QuestEvent {
        public Accept(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }
    }

    /**
     * 任务完成事件
     */
    public static class Complete extends QuestEvent {
        public Complete(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }
    }

    /**
     * 任务放弃事件
     */
    public static class Abandon extends QuestEvent {
        public Abandon(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }
    }

    /**
     * 任务进度更新事件
     */
    public static class Progress extends QuestEvent {
        private final int objectiveIndex;
        private final int currentProgress;
        private final int requiredProgress;

        public Progress(UUID playerId, String questId, String questName, String questType,
                        int objectiveIndex, int currentProgress, int requiredProgress) {
            super(playerId, questId, questName, questType);
            this.objectiveIndex = objectiveIndex;
            this.currentProgress = currentProgress;
            this.requiredProgress = requiredProgress;
        }

        public int getObjectiveIndex() {
            return objectiveIndex;
        }

        public int getCurrentProgress() {
            return currentProgress;
        }

        public int getRequiredProgress() {
            return requiredProgress;
        }

        public boolean isObjectiveComplete() {
            return currentProgress >= requiredProgress;
        }
    }
}
