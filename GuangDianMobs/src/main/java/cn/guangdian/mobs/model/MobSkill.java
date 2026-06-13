package cn.guangdian.mobs.model;

import org.bukkit.entity.LivingEntity;

import java.util.*;

/**
 * 怪物技能数据模型
 */
public class MobSkill {

    private String id;                    // 技能ID
    private String displayName;           // 显示名称
    private SkillType type;               // 技能类型
    private double damage;                // 伤害值
    private double healAmount;            // 治疗量
    private int cooldown;                 // 冷却时间（tick）
    private double range;                 // 作用范围
    private double chance;                // 触发几率（0-1）
    private String targetType;            // 目标类型: SELF, TARGET, AOE, RANDOM
    private List<String> effects;         // 效果列表
    private String particle;              // 粒子效果
    private String sound;                 // 音效
    private String message;               // 触发消息

    // 新增：技能条件
    private List<String> conditions;      // 条件列表

    // 新增：元技能
    private List<String> subSkills;       // 子技能列表（用于组合技能）
    private int delay;                    // 延迟执行（tick）

    // 新增：目标选择器配置
    private TargetSelector targetSelector; // 目标选择器

    public enum SkillType {
        DAMAGE,      // 伤害技能
        HEAL,        // 治疗技能
        BUFF,        // 增益技能
        DEBUFF,      // 减益技能
        SUMMON,      // 召唤技能
        TELEPORT,    // 传送技能
        PROJECTILE   // 弹射物技能
    }

    public MobSkill(String id) {
        this.id = id;
        this.type = SkillType.DAMAGE;
        this.damage = 0;
        this.healAmount = 0;
        this.cooldown = 100;
        this.range = 10;
        this.chance = 0.3;
        this.targetType = "TARGET";
        this.effects = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.subSkills = new ArrayList<>();
        this.delay = 0;
        this.targetSelector = new TargetSelector();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public SkillType getType() { return type; }
    public void setType(SkillType type) { this.type = type; }

    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }

    public double getHealAmount() { return healAmount; }
    public void setHealAmount(double healAmount) { this.healAmount = healAmount; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public List<String> getEffects() { return effects; }
    public void setEffects(List<String> effects) { this.effects = effects; }

    public String getParticle() { return particle; }
    public void setParticle(String particle) { this.particle = particle; }

    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }

    public List<String> getSubSkills() { return subSkills; }
    public void setSubSkills(List<String> subSkills) { this.subSkills = subSkills; }

    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    public TargetSelector getTargetSelector() { return targetSelector; }
    public void setTargetSelector(TargetSelector targetSelector) { this.targetSelector = targetSelector; }

    /**
     * 验证技能配置是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && type != null;
    }

    /**
     * 目标选择器配置
     */
    public static class TargetSelector {
        private String type = "TARGET";  // TARGET, SELF, AOE, RANDOM, PLAYERS_IN_RADIUS, LIVING_IN_RADIUS
        private double radius = 10;

        public TargetSelector() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getRadius() { return radius; }
        public void setRadius(double radius) { this.radius = radius; }
    }

    @Override
    public String toString() {
        return "MobSkill{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", damage=" + damage +
                ", cooldown=" + cooldown +
                '}';
    }
}
