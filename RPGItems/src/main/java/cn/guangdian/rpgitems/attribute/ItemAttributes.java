package cn.guangdian.rpgitems.attribute;

/**
 * 物品属性数据结构
 * 包含物品的所有属性信息
 */
public record ItemAttributes(
    // 攻击属性
    double attackMin,
    double attackMax,
    
    // 防御属性
    double defenseMin,
    double defenseMax,
    
    // 生命属性
    double maxHealth,
    double healthRegen,
    
    // 暴击属性
    double critChance,
    double critDamage,
    
    // 生命偷取
    double lifestealChance,
    double lifestealMultiplier,
    
    // 闪避与格挡
    double dodgeChance,
    double parryChance,
    
    // 移动速度
    double moveSpeed,
    
    // 减伤
    double damageReduction,
    
    // PVP属性
    double pvpAttackMin,
    double pvpAttackMax,
    double pvpDefenseMin,
    double pvpDefenseMax,
    
    // 暴击抵抗属性
    double critResist,
    double critDamageResist,
    
    // 吸血抵抗
    double lifestealResist,
    
    // 护甲与穿透系统
    double armor,
    double armorStrength,
    double armorPenetration,
    double defensePenetration,
    
    // 伤害反弹
    double damageReflect,
    double reflectRatio,
    
    // 状态效果概率
    double poisonChance,
    double freezeChance,
    double blindChance,
    double burnChance,
    double scorchChance,
    double igniteChance,    // 点燃几率
    double slowChance,      // 减速几率
    
    // 环境抗性
    double fireResist,
    double fallResist,
    double drowningResist,
    double poisonResist,
    double witherResist,
    double lavaResist,
    double magicResist,
    double explosionResist,
    double projectileResist,
    
    // 其他属性
    double knockbackResist,
    double expBonus,
    double healthRegenPercent,
    double dodgeReflectChance,
    double dodgeReflectRatio,
    
    // 装备等级
    int level,
    
    // 需要职业
    String requiredClass
) {
    /**
     * 空属性常量
     */
    public static final ItemAttributes EMPTY = new ItemAttributes(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0,
        0, 0, 0, 0,
        0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, ""
    );
    
    /**
     * 获取平均攻击力
     */
    public double getAverageAttack() {
        return (attackMin + attackMax) / 2;
    }
    
    /**
     * 获取平均防御力
     */
    public double getAverageDefense() {
        return (defenseMin + defenseMax) / 2;
    }
    
    /**
     * 是否有攻击属性
     */
    public boolean hasAttack() {
        return attackMin > 0 || attackMax > 0;
    }
    
    /**
     * 是否有防御属性
     */
    public boolean hasDefense() {
        return defenseMin > 0 || defenseMax > 0;
    }
    
    /**
     * 是否有任何属性
     */
    public boolean hasAnyAttribute() {
        return hasAttack() || hasDefense() || maxHealth > 0 || healthRegen > 0 ||
               critChance > 0 || critDamage > 0 || lifestealChance > 0 ||
               dodgeChance > 0 || parryChance > 0 || moveSpeed > 0 || damageReduction > 0 ||
               pvpAttackMin > 0 || pvpDefenseMin > 0 ||
               critResist > 0 || critDamageResist > 0 || lifestealResist > 0 ||
               armor > 0 || armorStrength > 0 || armorPenetration > 0 || defensePenetration > 0 ||
               damageReflect > 0 || reflectRatio > 0 ||
               poisonChance > 0 || freezeChance > 0 || blindChance > 0 ||
               burnChance > 0 || scorchChance > 0 || igniteChance > 0 || slowChance > 0 ||
               fireResist > 0 || fallResist > 0 || drowningResist > 0 ||
               poisonResist > 0 || witherResist > 0 || lavaResist > 0 ||
               magicResist > 0 || explosionResist > 0 || projectileResist > 0 ||
               knockbackResist > 0 || expBonus > 0 || healthRegenPercent > 0 ||
               dodgeReflectChance > 0 || dodgeReflectRatio > 0;
    }
}
