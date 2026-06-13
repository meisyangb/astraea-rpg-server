package cn.guangdian.rpgcore.event.events.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 技能伤害事件
 * 
 * <p>当技能造成伤害时触发。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @EventHandler
 * public void onSkillDamage(RpgSkillDamageEvent event) {
 *     Player caster = event.getCaster();
 *     LivingEntity target = event.getTarget();
 *     String skillId = event.getSkillId();
 *     
 *     // 修改伤害
 *     if (skillId.equals("fireball")) {
 *         // 火球术额外伤害
 *         event.setDamage(event.getDamage() * 1.5);
 *     }
 *     
 *     // 暴击检测
 *     if (event.isCritical()) {
 *         event.setDamage(event.getDamage() * 2.0);
 *         caster.sendMessage("§c暴击!");
 *     }
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgSkillDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final UUID casterId;
    private final Player caster;
    private final LivingEntity target;
    private final String skillId;
    private final String skillName;
    private final int skillLevel;
    private double damage;
    private double originalDamage;
    private boolean critical;
    private final DamageType damageType;
    private boolean cancelled;

    /**
     * 伤害类型
     */
    public enum DamageType {
        /** 物理伤害 */
        PHYSICAL,
        /** 魔法伤害 */
        MAGICAL,
        /** 真实伤害 */
        TRUE,
        /** 混合伤害 */
        HYBRID
    }

    /**
     * 创建技能伤害事件
     * 
     * @param caster 施法者
     * @param target 目标
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param skillLevel 技能等级
     * @param damage 伤害值
     * @param damageType 伤害类型
     */
    public RpgSkillDamageEvent(Player caster, LivingEntity target, String skillId, 
                                String skillName, int skillLevel, double damage, 
                                DamageType damageType) {
        super(!Bukkit.isPrimaryThread());
        this.caster = caster;
        this.casterId = caster.getUniqueId();
        this.target = target;
        this.skillId = skillId;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.damage = damage;
        this.originalDamage = damage;
        this.critical = false;
        this.damageType = damageType;
        this.cancelled = false;
    }

    /**
     * 获取施法者
     */
    public Player getCaster() {
        return caster;
    }

    /**
     * 获取施法者UUID
     */
    public UUID getCasterId() {
        return casterId;
    }

    /**
     * 获取目标
     */
    public LivingEntity getTarget() {
        return target;
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
     * 获取伤害值
     */
    public double getDamage() {
        return damage;
    }

    /**
     * 设置伤害值
     */
    public void setDamage(double damage) {
        this.damage = damage;
    }

    /**
     * 获取原始伤害值
     */
    public double getOriginalDamage() {
        return originalDamage;
    }

    /**
     * 获取伤害变化量
     */
    public double getDamageChange() {
        return damage - originalDamage;
    }

    /**
     * 获取伤害变化百分比
     */
    public double getDamageChangePercent() {
        return originalDamage == 0 ? 0 : (damage - originalDamage) / originalDamage * 100;
    }

    /**
     * 是否暴击
     */
    public boolean isCritical() {
        return critical;
    }

    /**
     * 设置是否暴击
     */
    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    /**
     * 获取伤害类型
     */
    public DamageType getDamageType() {
        return damageType;
    }

    /**
     * 是否为物理伤害
     */
    public boolean isPhysicalDamage() {
        return damageType == DamageType.PHYSICAL;
    }

    /**
     * 是否为魔法伤害
     */
    public boolean isMagicalDamage() {
        return damageType == DamageType.MAGICAL;
    }

    /**
     * 是否为真实伤害
     */
    public boolean isTrueDamage() {
        return damageType == DamageType.TRUE;
    }

    /**
     * 目标是否为玩家
     */
    public boolean isTargetPlayer() {
        return target instanceof Player;
    }

    /**
     * 获取目标玩家（如果是玩家）
     */
    public Player getTargetPlayer() {
        return target instanceof Player ? (Player) target : null;
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