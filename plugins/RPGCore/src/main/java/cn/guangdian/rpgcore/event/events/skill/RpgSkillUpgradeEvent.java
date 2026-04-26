package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能升级事件
 *
 * <p>当玩家升级技能时触发。</p>
 *
 * <p><strong>已废弃</strong>：业务事件应定义在对应的业务插件中，而不是 RPGCore。
 * 请迁移到 GuangDianClass 插件中的 {@code cn.guangdian.classsystem.event.SkillUpgradeEvent}。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 * @deprecated 业务事件已迁移到对应插件。请使用 GuangDianClass 插件中的 SkillUpgradeEvent。
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class RpgSkillUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final Player player;
    private final String skillId;
    private final String skillName;
    private final int oldLevel;
    private final int newLevel;
    private final int upgradeCost;
    private boolean cancelled;

    /**
     * 创建技能升级事件
     * 
     * @param player 升级技能的玩家
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @param upgradeCost 升级消耗
     */
    public RpgSkillUpgradeEvent(Player player, String skillId, String skillName, 
                                 int oldLevel, int newLevel, int upgradeCost) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.playerId = player.getUniqueId();
        this.skillId = skillId;
        this.skillName = skillName;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.upgradeCost = upgradeCost;
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
     * 获取原等级
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * 获取新等级
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * 获取等级提升量
     */
    public int getLevelIncrease() {
        return newLevel - oldLevel;
    }

    /**
     * 获取升级消耗
     */
    public int getUpgradeCost() {
        return upgradeCost;
    }

    /**
     * 检查是否为满级
     */
    public boolean isMaxLevel(int maxLevel) {
        return newLevel >= maxLevel;
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