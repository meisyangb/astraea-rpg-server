package cn.guangdian.armorstats.combat;

/**
 * 伤害来源枚举
 */
public enum DamageSource {
    
    ATTACK("普通攻击"),           // 普通攻击
    SKILL("技能伤害"),            // 技能伤害
    MYTHICMOB("怪物攻击"),        // MythicMobs怪物普通攻击
    MYTHICMOB_SKILL("怪物技能"),  // MythicMobs怪物技能
    PROJECTILE("投射物"),         // 箭矢、火球等
    EFFECT("效果伤害"),           // 药水效果、燃烧等
    FALL("摔落伤害"),             // 摔落
    ENVIRONMENT("环境伤害"),      // 岩浆、仙人掌等
    UNKNOWN("未知来源");          // 未知

    private final String displayName;

    DamageSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}