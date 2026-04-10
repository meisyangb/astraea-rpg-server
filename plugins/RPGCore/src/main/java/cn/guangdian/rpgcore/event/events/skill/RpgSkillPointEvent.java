package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能点变化事件
 * 
 * <p>当玩家的技能点发生变化时触发。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @EventHandler
 * public void onSkillPointChange(RpgSkillPointEvent event) {
 *     Player player = event.getPlayer();
 *     int oldPoints = event.getOldPoints();
 *     int newPoints = event.getNewPoints();
 *     
 *     // 显示技能点变化
 *     player.sendMessage("§a技能点: " + oldPoints + " → " + newPoints);
 *     
 *     // 广播技能点成就
 *     if (newPoints >= 100) {
 *         Bukkit.broadcastMessage("§6" + player.getName() + " 达到了 100 技能点!");
 *     }
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgSkillPointEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final Player player;
    private final int oldPoints;
    private final int newPoints;
    private final int change;
    private final ChangeReason reason;
    private final String source;
    private boolean cancelled;

    /**
     * 变化原因
     */
    public enum ChangeReason {
        /** 升级获得 */
        LEVEL_UP,
        /** 任务奖励 */
        QUEST_REWARD,
        /** 消耗 */
        SPEND,
        /** 重置返还 */
        RESET,
        /** 管理员给予 */
        ADMIN_GIVE,
        /** 管理员扣除 */
        ADMIN_TAKE,
        /** 其他 */
        OTHER
    }

    /**
     * 创建技能点变化事件
     * 
     * @param player 玩家
     * @param oldPoints 原技能点
     * @param newPoints 新技能点
     * @param reason 变化原因
     */
    public RpgSkillPointEvent(Player player, int oldPoints, int newPoints, ChangeReason reason) {
        this(player, oldPoints, newPoints, reason, "");
    }

    /**
     * 创建技能点变化事件
     * 
     * @param player 玩家
     * @param oldPoints 原技能点
     * @param newPoints 新技能点
     * @param reason 变化原因
     * @param source 来源描述
     */
    public RpgSkillPointEvent(Player player, int oldPoints, int newPoints, 
                               ChangeReason reason, String source) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.playerId = player.getUniqueId();
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.change = newPoints - oldPoints;
        this.reason = reason;
        this.source = source;
        this.cancelled = false;
    }

    /**
     * 获取玩家
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 获取玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取原技能点
     */
    public int getOldPoints() {
        return oldPoints;
    }

    /**
     * 获取新技能点
     */
    public int getNewPoints() {
        return newPoints;
    }

    /**
     * 获取变化量
     */
    public int getChange() {
        return change;
    }

    /**
     * 是否为增加
     */
    public boolean isIncrease() {
        return change > 0;
    }

    /**
     * 是否为减少
     */
    public boolean isDecrease() {
        return change < 0;
    }

    /**
     * 获取变化原因
     */
    public ChangeReason getReason() {
        return reason;
    }

    /**
     * 获取来源描述
     */
    public String getSource() {
        return source;
    }

    /**
     * 是否为玩家主动消耗
     */
    public boolean isPlayerSpend() {
        return reason == ChangeReason.SPEND;
    }

    /**
     * 是否为系统奖励
     */
    public boolean isSystemReward() {
        return reason == ChangeReason.LEVEL_UP || reason == ChangeReason.QUEST_REWARD;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}