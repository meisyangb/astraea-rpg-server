package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能释放事件
 *
 * <p>当玩家释放技能时触发。此事件可被取消，取消后技能不会释放。</p>
 *
 * <p><strong>已废弃</strong>：业务事件应定义在对应的业务插件中，而不是 RPGCore。
 * 请迁移到 GuangDianClass 插件中的 {@code cn.guangdian.classsystem.event.SkillCastEvent}。</p>
 *
 * <h3>迁移示例：</h3>
 * <pre>{@code
 * // 旧代码（已废弃）
 * RpgSkillCastEvent event = new RpgSkillCastEvent(player, skillId, skillName, manaCost, cooldownMs);
 *
 * // 新代码（推荐）
 * // 在 GuangDianClass 插件中定义 SkillCastEvent
 * SkillCastEvent event = new SkillCastEvent(player, skillId, skillName, manaCost, cooldownMs);
 * EventPublisher.publish(event);
 * }</pre>
 *
 * @author GuangDian
 * @since 1.0.0
 * @deprecated 业务事件已迁移到对应插件。请使用 GuangDianClass 插件中的 SkillCastEvent。
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class RpgSkillCastEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID playerId;
    private final Player caster;
    private final String skillId;
    private final String skillName;
    private int skillLevel;
    private final int manaCost;
    private long cooldownMs;
    private final CastType castType;
    private final String targetType;
    private boolean cancelled;

    /**
     * 技能释放类型
     */
    public enum CastType {
        /** 主动释放 */
        ACTIVE,
        /** 被动触发 */
        PASSIVE,
        /** 自动释放 */
        AUTO,
        /** 物品触发 */
        ITEM_TRIGGER
    }

    /**
     * 创建技能释放事件
     * 
     * @param caster 施法者
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param manaCost 法力消耗
     * @param cooldownMs 冷却时间（毫秒）
     */
    public RpgSkillCastEvent(Player caster, String skillId, String skillName, 
                              int manaCost, long cooldownMs) {
        this(caster, skillId, skillName, 1, manaCost, cooldownMs, CastType.ACTIVE, "self");
    }

    /**
     * 创建技能释放事件（完整参数）
     * 
     * @param caster 施法者
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param skillLevel 技能等级
     * @param manaCost 法力消耗
     * @param cooldownMs 冷却时间（毫秒）
     * @param castType 释放类型
     * @param targetType 目标类型
     */
    public RpgSkillCastEvent(Player caster, String skillId, String skillName, 
                              int skillLevel, int manaCost, long cooldownMs,
                              CastType castType, String targetType) {
        super(!Bukkit.isPrimaryThread());
        this.caster = caster;
        this.playerId = caster.getUniqueId();
        this.skillId = skillId;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.manaCost = manaCost;
        this.cooldownMs = cooldownMs;
        this.castType = castType;
        this.targetType = targetType;
        this.cancelled = false;
    }

    /**
     * 获取施法者
     */
    public Player getCaster() {
        return caster;
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
     * 获取技能等级
     */
    public int getSkillLevel() {
        return skillLevel;
    }

    /**
     * 设置技能等级
     */
    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    /**
     * 获取法力消耗
     */
    public int getManaCost() {
        return manaCost;
    }

    /**
     * 获取冷却时间（毫秒）
     */
    public long getCooldownMs() {
        return cooldownMs;
    }

    /**
     * 设置冷却时间（毫秒）
     */
    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    /**
     * 获取冷却时间（秒）
     */
    public double getCooldownSeconds() {
        return cooldownMs / 1000.0;
    }

    /**
     * 获取冷却时间（ticks）
     */
    public long getCooldownTicks() {
        return cooldownMs / 50;
    }

    /**
     * 获取释放类型
     */
    public CastType getCastType() {
        return castType;
    }

    /**
     * 是否为主动技能
     */
    public boolean isActiveSkill() {
        return castType == CastType.ACTIVE;
    }

    /**
     * 是否为被动技能
     */
    public boolean isPassiveSkill() {
        return castType == CastType.PASSIVE;
    }

    /**
     * 获取目标类型
     */
    public String getTargetType() {
        return targetType;
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