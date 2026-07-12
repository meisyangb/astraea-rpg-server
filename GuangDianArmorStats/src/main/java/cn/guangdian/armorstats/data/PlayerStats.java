package cn.guangdian.armorstats.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化的玩家属性存储类
 * 
 * 使用 Map<AttributeType, Double> 存储属性值，避免硬编码大量字段
 */
public class PlayerStats {

    private final Map<AttributeType, Double> attributes = new HashMap<>();

    public PlayerStats() {
        reset();
    }

    public void reset() {
        attributes.clear();
        // 所有属性默认值为 0
        for (AttributeType type : AttributeType.values()) {
            attributes.put(type, 0.0);
        }
    }

    /**
     * 获取属性值
     */
    public double get(AttributeType type) {
        return attributes.getOrDefault(type, 0.0);
    }

    /**
     * 设置属性值
     */
    public void set(AttributeType type, double value) {
        attributes.put(type, value);
    }

    /**
     * 增加属性值
     */
    public void add(AttributeType type, double value) {
        double current = get(type);
        set(type, current + value);
    }

    /**
     * 合并另一个 PlayerStats 的属性
     */
    public void addPlayerStats(PlayerStats other) {
        if (other == null) return;
        
        for (AttributeType type : AttributeType.values()) {
            add(type, other.get(type));
        }
    }

    /**
     * 添加属性值（从 Lore 解析的结果）
     * 
     * 支持单值和范围值：
     * - 单值 "攻击力: 100" -> MIN_ATTACK=100, MAX_ATTACK=100
     * - 范围值 "攻击力: 100-200" -> MIN_ATTACK=100, MAX_ATTACK=200
     */
    public void addAttributes(Map<String, AttributeValue> attrs) {
        if (attrs == null) return;

        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            String key = entry.getKey();
            AttributeValue val = entry.getValue();

            if (val instanceof AttributeValue.SingleValue) {
                double v = ((AttributeValue.SingleValue) val).getValue();
                addSingleAttribute(key, v);
            } else if (val instanceof AttributeValue.RangeValue) {
                double min = ((AttributeValue.RangeValue) val).getMin();
                double max = ((AttributeValue.RangeValue) val).getMax();
                addRangeAttribute(key, min, max);
            }
        }
    }

    /**
     * 兼容性方法 - 添加属性值（从 Lore 解析的结果）
     */
    public void addStats(Map<String, AttributeValue> attrs) {
        addAttributes(attrs);
    }

    /**
     * 添加单值属性
     */
    private void addSingleAttribute(String key, double value) {
        // 直接映射到枚举
        switch (key) {
            case "生命上限":
                add(AttributeType.MAX_HEALTH, value);
                break;
            case "攻击力":
                add(AttributeType.MIN_ATTACK, value);
                add(AttributeType.MAX_ATTACK, value);
                break;
            case "防御力":
                add(AttributeType.DEFENSE_MIN, value);
                add(AttributeType.DEFENSE_MAX, value);
                break;
            case "暴击几率":
                add(AttributeType.CRIT_CHANCE, value);
                break;
            case "暴击伤害":
                add(AttributeType.CRIT_DAMAGE, value);
                break;
            case "吸血几率":
                add(AttributeType.LIFESTEAL_CHANCE, value);
                break;
            case "吸血倍率":
                add(AttributeType.LIFESTEAL_MULTIPLIER, value);
                break;
            case "每秒回血":
            case "生命回复":
                add(AttributeType.HEALTH_REGEN, value);
                break;
            case "闪避":
                add(AttributeType.DODGE, value);
                break;
            case "伤害反弹":
                add(AttributeType.DAMAGE_REFLECT, value);
                break;
            case "反伤比例":
                add(AttributeType.REFLECT_RATIO, value);
                break;
            case "吸血抵抗":
                add(AttributeType.LIFESTEAL_RESIST, value);
                break;
            case "暴击抵抗":
                add(AttributeType.CRIT_RESIST, value);
                break;
            case "暴伤抵抗":
                add(AttributeType.CRIT_DAMAGE_RESIST, value);
                break;
            case "招架":
                add(AttributeType.PARRY, value);
                break;
            case "移动速度":
                add(AttributeType.MOVE_SPEED, value);
                break;
            case "中毒":
                add(AttributeType.POISON, value);
                break;
            case "冰冻":
                add(AttributeType.FREEZE, value);
                break;
            case "致盲":
                add(AttributeType.BLIND, value);
                break;
            case "经验加成":
                add(AttributeType.EXP_BONUS, value);
                break;
            case "护甲值":
                add(AttributeType.ARMOR, value);
                break;
            case "护甲强度":
                add(AttributeType.ARMOR_STRENGTH, value);
                break;
            case "护甲穿透":
                add(AttributeType.ARMOR_PENETRATION, value);
                break;
            case "防御穿透":
                add(AttributeType.DEFENSE_PENETRATION, value);
                break;
            case "减伤":
                add(AttributeType.DAMAGE_REDUCTION, value);
                break;
            case "躲避反伤":
                add(AttributeType.DODGE_REFLECT_CHANCE, value);
                break;
            case "躲避反弹比例":
                add(AttributeType.DODGE_REFLECT_RATIO, value);
                break;
            case "生命恢复":
                add(AttributeType.HEALTH_REGEN_PERCENT, value);
                break;
            case "燃烧":
                add(AttributeType.BURN, value);
                break;
            case "灼烧":
                add(AttributeType.SCORCH, value);
                break;
            case "击退抗性":
                add(AttributeType.KNOCKBACK_RESIST, value);
                break;
            case "火焰抗性":
                add(AttributeType.FIRE_RESIST, value);
                break;
            case "摔落抗性":
                add(AttributeType.FALL_RESIST, value);
                break;
            case "溺水抗性":
                add(AttributeType.DROWNING_RESIST, value);
                break;
            case "中毒抗性":
                add(AttributeType.POISON_RESIST, value);
                break;
            case "凋零抗性":
                add(AttributeType.WITHER_RESIST, value);
                break;
            case "岩浆抗性":
                add(AttributeType.LAVA_RESIST, value);
                break;
            case "魔法抗性":
                add(AttributeType.MAGIC_RESIST, value);
                break;
            case "爆炸抗性":
                add(AttributeType.EXPLOSION_RESIST, value);
                break;
            case "弹射物抗性":
                add(AttributeType.PROJECTILE_RESIST, value);
                break;
            case "PVP攻击力":
                add(AttributeType.PVP_MIN_ATTACK, value);
                add(AttributeType.PVP_MAX_ATTACK, value);
                break;
            case "PVP防御力":
                add(AttributeType.PVP_DEFENSE_MIN, value);
                add(AttributeType.PVP_DEFENSE_MAX, value);
                break;
        }
    }

    /**
     * 添加范围值属性
     */
    private void addRangeAttribute(String key, double min, double max) {
        switch (key) {
            case "攻击力":
                add(AttributeType.MIN_ATTACK, min);
                add(AttributeType.MAX_ATTACK, max);
                break;
            case "防御力":
                add(AttributeType.DEFENSE_MIN, min);
                add(AttributeType.DEFENSE_MAX, max);
                break;
            case "PVP攻击力":
                add(AttributeType.PVP_MIN_ATTACK, min);
                add(AttributeType.PVP_MAX_ATTACK, max);
                break;
            case "PVP防御力":
                add(AttributeType.PVP_DEFENSE_MIN, min);
                add(AttributeType.PVP_DEFENSE_MAX, max);
                break;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (AttributeType type : AttributeType.values()) {
            map.put(type.name(), get(type));
        }
        return map;
    }

    /**
     * 检查是否有任何属性
     */
    public boolean hasAnyStats() {
        for (AttributeType type : AttributeType.values()) {
            if (get(type) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取平均攻击力
     */
    public double getAttackAverage() {
        return (get(AttributeType.MIN_ATTACK) + get(AttributeType.MAX_ATTACK)) / 2.0;
    }

    /**
     * 获取平均防御力
     */
    public double getDefenseAverage() {
        return (get(AttributeType.DEFENSE_MIN) + get(AttributeType.DEFENSE_MAX)) / 2.0;
    }

    /**
     * 获取平均 PVP 攻击力
     */
    public double getPvpAttackAverage() {
        return (get(AttributeType.PVP_MIN_ATTACK) + get(AttributeType.PVP_MAX_ATTACK)) / 2.0;
    }

    /**
     * 获取平均 PVP 防御力
     */
    public double getPvpDefenseAverage() {
        return (get(AttributeType.PVP_DEFENSE_MIN) + get(AttributeType.PVP_DEFENSE_MAX)) / 2.0;
    }

    // ==================== 兼容性 getter 方法 ====================
    // 为了避免大量修改现有代码，提供这些兼容方法

    public double getMaxHealth() { return get(AttributeType.MAX_HEALTH); }
    public double getMinAttack() { return get(AttributeType.MIN_ATTACK); }
    public double getMaxAttack() { return get(AttributeType.MAX_ATTACK); }
    public double getDefenseMin() { return get(AttributeType.DEFENSE_MIN); }
    public double getDefenseMax() { return get(AttributeType.DEFENSE_MAX); }
    public double getCritChancePercent() { return get(AttributeType.CRIT_CHANCE); }
    public double getCritDamagePercent() { return get(AttributeType.CRIT_DAMAGE); }
    public double getLifestealPercent() { return get(AttributeType.LIFESTEAL_CHANCE); }
    public double getHealthRegen() { return get(AttributeType.HEALTH_REGEN); }
    public double getDodgePercent() { return get(AttributeType.DODGE); }
    public double getDamageReflectPercent() { return get(AttributeType.DAMAGE_REFLECT); }
    public double getReflectPercent() { return get(AttributeType.REFLECT_RATIO); }
    public double getLifestealResistPercent() { return get(AttributeType.LIFESTEAL_RESIST); }
    public double getCritResistPercent() { return get(AttributeType.CRIT_RESIST); }
    public double getCritDamageResistPercent() { return get(AttributeType.CRIT_DAMAGE_RESIST); }
    public double getParryPercent() { return get(AttributeType.PARRY); }
    public double getPvpMinAttack() { return get(AttributeType.PVP_MIN_ATTACK); }
    public double getPvpMaxAttack() { return get(AttributeType.PVP_MAX_ATTACK); }
    public double getPvpDefenseMin() { return get(AttributeType.PVP_DEFENSE_MIN); }
    public double getPvpDefenseMax() { return get(AttributeType.PVP_DEFENSE_MAX); }
    public double getMoveSpeedPercent() { return get(AttributeType.MOVE_SPEED); }
    public double getPoisonPercent() { return get(AttributeType.POISON); }
    public double getFreezePercent() { return get(AttributeType.FREEZE); }
    public double getBlindPercent() { return get(AttributeType.BLIND); }
    public double getExpBonusPercent() { return get(AttributeType.EXP_BONUS); }
    public double getLifestealMultiplier() { return get(AttributeType.LIFESTEAL_MULTIPLIER); }
    public double getArmorPercent() { return get(AttributeType.ARMOR); }
    public double getArmorStrength() { return get(AttributeType.ARMOR_STRENGTH); }
    public double getArmorPenetration() { return get(AttributeType.ARMOR_PENETRATION); }
    public double getDefensePenetration() { return get(AttributeType.DEFENSE_PENETRATION); }
    public double getDamageReductionBonus() { return get(AttributeType.DAMAGE_REDUCTION); }
    public double getDodgeReflectPercent() { return get(AttributeType.DODGE_REFLECT_CHANCE); }
    public double getDodgeReflectRatio() { return get(AttributeType.DODGE_REFLECT_RATIO); }
    public double getHealthRegenPercent() { return get(AttributeType.HEALTH_REGEN_PERCENT); }
    public double getBurnPercent() { return get(AttributeType.BURN); }
    public double getScorchPercent() { return get(AttributeType.SCORCH); }
    public double getKnockbackResistPercent() { return get(AttributeType.KNOCKBACK_RESIST); }
    public double getFireResistPercent() { return get(AttributeType.FIRE_RESIST); }
    public double getFallResistPercent() { return get(AttributeType.FALL_RESIST); }
    public double getDrowningResistPercent() { return get(AttributeType.DROWNING_RESIST); }
    public double getPoisonResistPercent() { return get(AttributeType.POISON_RESIST); }
    public double getWitherResistPercent() { return get(AttributeType.WITHER_RESIST); }
    public double getLavaResistPercent() { return get(AttributeType.LAVA_RESIST); }
    public double getMagicResistPercent() { return get(AttributeType.MAGIC_RESIST); }
    public double getExplosionResistPercent() { return get(AttributeType.EXPLOSION_RESIST); }
    public double getProjectileResistPercent() { return get(AttributeType.PROJECTILE_RESIST); }

    // ==================== 兼容性 setter 方法 ====================

    public void setMaxHealth(double v) { set(AttributeType.MAX_HEALTH, v); }
    public void setMinAttack(double v) { set(AttributeType.MIN_ATTACK, v); }
    public void setMaxAttack(double v) { set(AttributeType.MAX_ATTACK, v); }
    public void setDefenseMin(double v) { set(AttributeType.DEFENSE_MIN, v); }
    public void setDefenseMax(double v) { set(AttributeType.DEFENSE_MAX, v); }
    public void setCritChancePercent(double v) { set(AttributeType.CRIT_CHANCE, v); }
    public void setCritDamagePercent(double v) { set(AttributeType.CRIT_DAMAGE, v); }
    public void setLifestealPercent(double v) { set(AttributeType.LIFESTEAL_CHANCE, v); }
    public void setHealthRegen(double v) { set(AttributeType.HEALTH_REGEN, v); }
    public void setDodgePercent(double v) { set(AttributeType.DODGE, v); }
    public void setDamageReflectPercent(double v) { set(AttributeType.DAMAGE_REFLECT, v); }
    public void setReflectPercent(double v) { set(AttributeType.REFLECT_RATIO, v); }
    public void setLifestealResistPercent(double v) { set(AttributeType.LIFESTEAL_RESIST, v); }
    public void setCritResistPercent(double v) { set(AttributeType.CRIT_RESIST, v); }
    public void setCritDamageResistPercent(double v) { set(AttributeType.CRIT_DAMAGE_RESIST, v); }
    public void setParryPercent(double v) { set(AttributeType.PARRY, v); }
    public void setPvpMinAttack(double v) { set(AttributeType.PVP_MIN_ATTACK, v); }
    public void setPvpMaxAttack(double v) { set(AttributeType.PVP_MAX_ATTACK, v); }
    public void setPvpDefenseMin(double v) { set(AttributeType.PVP_DEFENSE_MIN, v); }
    public void setPvpDefenseMax(double v) { set(AttributeType.PVP_DEFENSE_MAX, v); }
    public void setMoveSpeedPercent(double v) { set(AttributeType.MOVE_SPEED, v); }
    public void setPoisonPercent(double v) { set(AttributeType.POISON, v); }
    public void setFreezePercent(double v) { set(AttributeType.FREEZE, v); }
    public void setBlindPercent(double v) { set(AttributeType.BLIND, v); }
    public void setExpBonusPercent(double v) { set(AttributeType.EXP_BONUS, v); }
    public void setLifestealMultiplier(double v) { set(AttributeType.LIFESTEAL_MULTIPLIER, v); }
    public void setArmorPercent(double v) { set(AttributeType.ARMOR, v); }
    public void setArmorStrength(double v) { set(AttributeType.ARMOR_STRENGTH, v); }
    public void setArmorPenetration(double v) { set(AttributeType.ARMOR_PENETRATION, v); }
    public void setDefensePenetration(double v) { set(AttributeType.DEFENSE_PENETRATION, v); }
    public void setDamageReductionBonus(double v) { set(AttributeType.DAMAGE_REDUCTION, v); }
    public void setDodgeReflectPercent(double v) { set(AttributeType.DODGE_REFLECT_CHANCE, v); }
    public void setDodgeReflectRatio(double v) { set(AttributeType.DODGE_REFLECT_RATIO, v); }
    public void setHealthRegenPercent(double v) { set(AttributeType.HEALTH_REGEN_PERCENT, v); }
    public void setBurnPercent(double v) { set(AttributeType.BURN, v); }
    public void setScorchPercent(double v) { set(AttributeType.SCORCH, v); }
    public void setKnockbackResistPercent(double v) { set(AttributeType.KNOCKBACK_RESIST, v); }
    public void setFireResistPercent(double v) { set(AttributeType.FIRE_RESIST, v); }
    public void setFallResistPercent(double v) { set(AttributeType.FALL_RESIST, v); }
    public void setDrowningResistPercent(double v) { set(AttributeType.DROWNING_RESIST, v); }
    public void setPoisonResistPercent(double v) { set(AttributeType.POISON_RESIST, v); }
    public void setWitherResistPercent(double v) { set(AttributeType.WITHER_RESIST, v); }
    public void setLavaResistPercent(double v) { set(AttributeType.LAVA_RESIST, v); }
    public void setMagicResistPercent(double v) { set(AttributeType.MAGIC_RESIST, v); }
    public void setExplosionResistPercent(double v) { set(AttributeType.EXPLOSION_RESIST, v); }
    public void setProjectileResistPercent(double v) { set(AttributeType.PROJECTILE_RESIST, v); }
}