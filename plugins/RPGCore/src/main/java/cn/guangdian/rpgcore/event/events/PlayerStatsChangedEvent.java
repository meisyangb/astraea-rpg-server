package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;
import cn.guangdian.rpgcore.event.EventPriority;

import java.util.UUID;

/**
 * 玩家属性变化事件
 * 
 * <p>当玩家的RPG属性发生变化时触发此事件。
 * 其他插件（如GuangDianName）可以订阅此事件来更新玩家显示。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 发布属性变化事件
 * eventBus.publish(new PlayerStatsChangedEvent(playerId, oldStats, newStats));
 * 
 * // 订阅属性变化事件
 * eventBus.subscribe(PlayerStatsChangedEvent.class, event -> {
 *     // 更新玩家显示
 *     updatePlayerDisplay(event.getPlayerId());
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerStatsChangedEvent extends CoreEvent {

    private final UUID playerId;
    private final String playerName;
    private final double oldHealth;
    private final double newHealth;
    private final double oldAttack;
    private final double newAttack;
    private final double oldDefense;
    private final double newDefense;
    private final long timestamp;

    /**
     * 创建属性变化事件
     * 
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     * @param oldHealth 旧生命值
     * @param newHealth 新生命值
     * @param oldAttack 旧攻击力
     * @param newAttack 新攻击力
     * @param oldDefense 旧防御力
     * @param newDefense 新防御力
     */
    public PlayerStatsChangedEvent(UUID playerId, String playerName,
                                   double oldHealth, double newHealth,
                                   double oldAttack, double newAttack,
                                   double oldDefense, double newDefense) {
        super(false); // 同步事件，确保及时处理
        this.playerId = playerId;
        this.playerName = playerName;
        this.oldHealth = oldHealth;
        this.newHealth = newHealth;
        this.oldAttack = oldAttack;
        this.newAttack = newAttack;
        this.oldDefense = oldDefense;
        this.newDefense = newDefense;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建简化版属性变化事件（仅包含基本信息）
     * 
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     */
    public PlayerStatsChangedEvent(UUID playerId, String playerName) {
        this(playerId, playerName, 0, 0, 0, 0, 0, 0);
    }

    /**
     * 获取玩家UUID
     * 
     * @return 玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取玩家名称
     * 
     * @return 玩家名称
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * 获取旧生命值
     * 
     * @return 旧生命值
     */
    public double getOldHealth() {
        return oldHealth;
    }

    /**
     * 获取新生命值
     * 
     * @return 新生命值
     */
    public double getNewHealth() {
        return newHealth;
    }

    /**
     * 获取旧攻击力
     * 
     * @return 旧攻击力
     */
    public double getOldAttack() {
        return oldAttack;
    }

    /**
     * 获取新攻击力
     * 
     * @return 新攻击力
     */
    public double getNewAttack() {
        return newAttack;
    }

    /**
     * 获取旧防御力
     * 
     * @return 旧防御力
     */
    public double getOldDefense() {
        return oldDefense;
    }

    /**
     * 获取新防御力
     * 
     * @return 新防御力
     */
    public double getNewDefense() {
        return newDefense;
    }

    /**
     * 检查生命值是否发生变化
     * 
     * @return 如果生命值发生变化返回 true
     */
    public boolean healthChanged() {
        return oldHealth != newHealth;
    }

    /**
     * 检查攻击力是否发生变化
     * 
     * @return 如果攻击力发生变化返回 true
     */
    public boolean attackChanged() {
        return oldAttack != newAttack;
    }

    /**
     * 检查防御力是否发生变化
     * 
     * @return 如果防御力发生变化返回 true
     */
    public boolean defenseChanged() {
        return oldDefense != newDefense;
    }

    /**
     * 检查是否有任何属性发生变化
     * 
     * @return 如果有任何属性变化返回 true
     */
    public boolean hasAnyChange() {
        return healthChanged() || attackChanged() || defenseChanged();
    }

    @Override
    public String getEventName() {
        return "PlayerStatsChangedEvent";
    }

    @Override
    public String toString() {
        return String.format("PlayerStatsChangedEvent{player=%s, health=%.0f->%.0f, attack=%.0f->%.0f, defense=%.0f->%.0f}",
            playerName, oldHealth, newHealth, oldAttack, newAttack, oldDefense, newDefense);
    }
}