package cn.guangdian.armorstats.combat;

import cn.guangdian.armorstats.boss.BossStats;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 伤害上下文
 * 包含一次伤害事件的所有相关信息
 */
public class DamageContext {

    // 原始事件
    private final EntityDamageByEntityEvent event;

    // 攻击者信息
    private final LivingEntity attacker;
    private final Player attackerPlayer;
    private final PlayerStats attackerStats;
    private BossStats attackerBossStats;

    // 目标信息
    private final LivingEntity target;
    private final Player targetPlayer;
    private final PlayerStats targetStats;
    private BossStats targetBossStats;

    // 伤害数据
    private double baseDamage;           // 基础伤害
    private double modifiedDamage;       // 修饰后伤害
    private double finalDamage;          // 最终伤害

    // 伤害类型
    private DamageType damageType;

    // 伤害标记
    private boolean isCritical;          // 是否暴击
    private boolean isDodged;            // 是否闪避
    private boolean isParried;           // 是否招架
    private boolean isBlocked;           // 是否被格挡
    private boolean isCancelled;         // 是否被取消
    private boolean isSkillDamage;       // 是否是技能伤害（跳过攻击力计算）
    private boolean isBossAttack;        // 是否是BOSS攻击
    private boolean isVanillaAttack;     // 是否是原版物品攻击（无RPG属性）
    private boolean isNPC;               // 目标是否是NPC

    // PVP标记
    private final boolean isPVP;

    // 伤害来源
    private DamageSource damageSource;

    public DamageContext(EntityDamageByEntityEvent event,
                         LivingEntity attacker, LivingEntity target,
                         Player attackerPlayer, Player targetPlayer,
                         PlayerStats attackerStats, PlayerStats targetStats,
                         boolean isPVP, double baseDamage) {
        this.event = event;
        this.attacker = attacker;
        this.target = target;
        this.attackerPlayer = attackerPlayer;
        this.targetPlayer = targetPlayer;
        this.attackerStats = attackerStats;
        this.targetStats = targetStats;
        this.isPVP = isPVP;
        this.baseDamage = baseDamage;
        this.modifiedDamage = baseDamage;
        this.finalDamage = baseDamage;
        this.damageType = DamageType.PHYSICAL;
        this.damageSource = DamageSource.ATTACK;
    }

    // Getters
    public EntityDamageByEntityEvent getEvent() { return event; }
    public LivingEntity getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }
    public Player getAttackerPlayer() { return attackerPlayer; }
    public Player getTargetPlayer() { return targetPlayer; }
    public PlayerStats getAttackerStats() { return attackerStats; }
    public PlayerStats getTargetStats() { return targetStats; }
    public double getBaseDamage() { return baseDamage; }
    public double getModifiedDamage() { return modifiedDamage; }
    public double getFinalDamage() { return finalDamage; }
    public DamageType getDamageType() { return damageType; }
    public DamageSource getDamageSource() { return damageSource; }
    public boolean isCritical() { return isCritical; }
    public boolean isDodged() { return isDodged; }
    public boolean isParried() { return isParried; }
    public boolean isBlocked() { return isBlocked; }
    public boolean isCancelled() { return isCancelled; }
    public boolean isPVP() { return isPVP; }
    public boolean isSkillDamage() { return isSkillDamage; }
    public boolean isBossAttack() { return isBossAttack; }
    public boolean isVanillaAttack() { return isVanillaAttack; }
    public boolean isNPC() { return isNPC; }
    public BossStats getAttackerBossStats() { return attackerBossStats; }
    public BossStats getTargetBossStats() { return targetBossStats; }

    // Setters
    public void setBaseDamage(double baseDamage) { this.baseDamage = baseDamage; }
    public void setModifiedDamage(double modifiedDamage) { this.modifiedDamage = modifiedDamage; }
    public void setFinalDamage(double finalDamage) { this.finalDamage = finalDamage; }
    public void setDamageType(DamageType damageType) { this.damageType = damageType; }
    public void setDamageSource(DamageSource damageSource) { this.damageSource = damageSource; }
    public void setCritical(boolean critical) { isCritical = critical; }
    public void setDodged(boolean dodged) { isDodged = dodged; }
    public void setParried(boolean parried) { isParried = parried; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
    public void setSkillDamage(boolean skillDamage) { isSkillDamage = skillDamage; }
    public void setBossAttack(boolean bossAttack) { isBossAttack = bossAttack; }
    public void setVanillaAttack(boolean vanillaAttack) { isVanillaAttack = vanillaAttack; }
    public void setNPC(boolean NPC) { isNPC = NPC; }
    public void setAttackerBossStats(BossStats bossStats) { this.attackerBossStats = bossStats; }
    public void setTargetBossStats(BossStats bossStats) { this.targetBossStats = bossStats; }

    // 伤害修饰方法
    public void multiplyDamage(double multiplier) {
        this.modifiedDamage *= multiplier;
        this.finalDamage = this.modifiedDamage;
    }

    public void addDamage(double amount) {
        this.modifiedDamage += amount;
        this.finalDamage = this.modifiedDamage;
    }

    public void reduceDamage(double amount) {
        this.modifiedDamage = Math.max(0, this.modifiedDamage - amount);
        this.finalDamage = this.modifiedDamage;
    }

    public void reduceDamagePercent(double percent) {
        this.modifiedDamage *= (1.0 - Math.min(1.0, percent));
        this.finalDamage = this.modifiedDamage;
    }

    // 工具方法
    public boolean isAttackerPlayer() { return attackerPlayer != null; }
    public boolean isTargetPlayer() { return targetPlayer != null; }
    public boolean hasAttackerStats() { return attackerStats != null; }
    public boolean hasTargetStats() { return targetStats != null; }
    public boolean hasAttackerBossStats() { return attackerBossStats != null; }
    public boolean hasTargetBossStats() { return targetBossStats != null; }
    public boolean isAttackerBoss() { return attackerBossStats != null; }
    public boolean isTargetBoss() { return targetBossStats != null; }
}