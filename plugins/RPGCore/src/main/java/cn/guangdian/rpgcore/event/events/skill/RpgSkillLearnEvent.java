package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能学习事件
 *
 * <p>当玩家学习新技能时触发。</p>
 *
 * <p><strong>已废弃</strong>：业务事件应定义在对应的业务插件中，而不是 RPGCore。
 * 请迁移到 GuangDianClass 插件中的 {@code cn.guangdian.classsystem.event.SkillLearnEvent}。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 * @deprecated 业务事件已迁移到对应插件。请使用 GuangDianClass 插件中的 SkillLearnEvent。
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class RpgSkillLearnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final Player player;
    private final String skillId;
    private final String skillName;
    private final int learnCost;
    private boolean cancelled;

    /**
     * 创建技能学习事件
     * 
     * @param player 学习技能的玩家
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param learnCost 学习消耗（如技能点）
     */
    public RpgSkillLearnEvent(Player player, String skillId, String skillName, int learnCost) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.playerId = player.getUniqueId();
        this.skillId = skillId;
        this.skillName = skillName;
        this.learnCost = learnCost;
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
     * 获取技能ID
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 获取技能名称
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * 获取学习消耗
     */
    public int getLearnCost() {
        return learnCost;
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