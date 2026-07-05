package cn.guangdian.classsystem.model;

import org.bukkit.Material;

import java.util.List;
import java.util.ArrayList;

/**
 * 技能球数据模型
 * 
 * 技能球是技能的可视化表现形式:
 * - 主动技能球: 可放置到快捷栏,右键使用
 * - 被动技能球: 仅在技能空间展示,自动生效
 */
public class SkillOrb {

    private final String skillId;           // 技能ID
    private final String name;              // 技能名称
    private final SkillType type;           // 技能类型
    private final String pathway;           // 所属途径
    private final int sequence;             // 序列等级
    private final Material material;        // 技能球材质
    private final int customModelData;      // 自定义模型数据(用于材质包)
    private final String description;       // 技能描述
    private final double damageMult;        // 伤害倍率
    private final double range;             // 范围
    private final int cooldown;             // 冷却时间(秒)
    private final int manaCost;             // 法力消耗
    private final int requiredTier;         // 所需职业等级
    private final String effect;            // 视觉效果
    private final double triggerChance;     // 触发概率(被动技能)
    private final List<String> statusEffects; // 状态效果
    private boolean unlocked;               // 是否解锁

    public SkillOrb(String skillId, String name, SkillType type, String pathway, int sequence,
                   Material material, int customModelData, String description, double damageMult, 
                   double range, int cooldown, int manaCost, int requiredTier, 
                   String effect, double triggerChance, List<String> statusEffects) {
        this.skillId = skillId;
        this.name = name;
        this.type = type;
        this.pathway = pathway;
        this.sequence = sequence;
        this.material = material;
        this.customModelData = customModelData;
        this.description = description;
        this.damageMult = damageMult;
        this.range = range;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.requiredTier = requiredTier;
        this.effect = effect;
        this.triggerChance = triggerChance;
        this.statusEffects = statusEffects != null ? statusEffects : new ArrayList<>();
        this.unlocked = false;
    }

    // Getters
    public String getSkillId() { return skillId; }
    public String getName() { return name; }
    public SkillType getType() { return type; }
    public String getPathway() { return pathway; }
    public int getSequence() { return sequence; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public String getDescription() { return description; }
    public double getDamageMult() { return damageMult; }
    public double getRange() { return range; }
    public int getCooldown() { return cooldown; }
    public int getManaCost() { return manaCost; }
    public int getRequiredTier() { return requiredTier; }
    public String getEffect() { return effect; }
    public double getTriggerChance() { return triggerChance; }
    public List<String> getStatusEffects() { return statusEffects; }
    public boolean isUnlocked() { return unlocked; }
    
    // Setter
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    /**
     * 是否为主动技能
     */
    public boolean isActive() {
        return type == SkillType.ACTIVE;
    }

    /**
     * 是否为被动技能
     */
    public boolean isPassive() {
        return type == SkillType.PASSIVE;
    }

    /**
     * 技能类型枚举
     */
    public enum SkillType {
        ACTIVE,   // 主动技能
        PASSIVE   // 被动技能
    }
}
