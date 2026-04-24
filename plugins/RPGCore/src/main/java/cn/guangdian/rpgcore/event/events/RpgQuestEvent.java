package cn.guangdian.rpgcore.event.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 任务事件基类 - Bukkit 原生事件
 *
 * <p>任务相关事件的基类，其他插件可以通过 @EventHandler 监听具体子类事件。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 监听任务完成事件
 * @EventHandler
 * public void onQuestComplete(RpgQuestEvent.Complete event) {
 *     getLogger().info("玩家 " + event.getPlayerId() + " 完成了任务: " + event.getQuestName());
 * }
 *
 * // 发布事件
 * Bukkit.getPluginManager().callEvent(new RpgQuestEvent.Complete(
 *     playerId, questId, questName, questType));
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public abstract class RpgQuestEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * 获取玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取任务ID
     */
    public String getQuestId() {
        return questId;
    }

    /**
     * 获取任务名称
     */
    public String getQuestName() {
        return questName;
    }

    /**
     * 获取任务类型
     */
    public String getQuestType() {
        return questType;
    }

    /**
     * 任务接取事件
     */
    public static class Accept extends RpgQuestEvent {
        public Accept(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }
    }

    /**
     * 任务完成事件
     */
    public static class Complete extends RpgQuestEvent {
        public Complete(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
        }
    }

    /**
     * 任务放弃事件
     */
    public static class Abandon extends RpgQuestEvent {
        public Abandon(UUID playerId, String questId, String questName, String questType) {
            super(playerId, questId, questName, questType);
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

        /**
         * 获取目标索引
         */
        public int getObjectiveIndex() {
            return objectiveIndex;
        }

        /**
         * 获取当前进度
         */
        public int getCurrentProgress() {
            return currentProgress;
        }

        /**
         * 获取所需进度
         */
        public int getRequiredProgress() {
            return requiredProgress;
        }

        /**
         * 检查目标是否完成
         */
        public boolean isObjectiveComplete() {
            return currentProgress >= requiredProgress;
        }
    }
}
