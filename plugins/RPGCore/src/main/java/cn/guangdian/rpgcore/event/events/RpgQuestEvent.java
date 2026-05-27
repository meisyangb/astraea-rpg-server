package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;

import java.util.UUID;

/**
 * 任务事件基类
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class RpgQuestEvent extends CoreEvent {

    protected final UUID playerId;
    protected final String questId;
    protected final String questName;
    protected final String questType;

    public RpgQuestEvent(UUID playerId, String questId, String questName, String questType) {
        this.playerId = playerId;
        this.questId = questId;
        this.questName = questName;
        this.questType = questType;
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

    @Override
    public String getEventName() {
        return "RpgQuestEvent";
    }

    /**
     * 任务接取事件
     */
    public static class Accept extends RpgQuestEvent {
        public Accept(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }

        @Override
        public String getEventName() {
            return "RpgQuestAcceptEvent";
        }
    }

    /**
     * 任务完成事件
     */
    public static class Complete extends RpgQuestEvent {
        public Complete(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }

        @Override
        public String getEventName() {
            return "RpgQuestCompleteEvent";
        }
    }

    /**
     * 任务放弃事件
     */
    public static class Abandon extends RpgQuestEvent {
        public Abandon(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }

        @Override
        public String getEventName() {
            return "RpgQuestAbandonEvent";
        }
    }

    /**
     * 任务进度更新事件
     */
    public static class Progress extends RpgQuestEvent {
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

        @Override
        public String getEventName() {
            return "RpgQuestProgressEvent";
        }
    }
}