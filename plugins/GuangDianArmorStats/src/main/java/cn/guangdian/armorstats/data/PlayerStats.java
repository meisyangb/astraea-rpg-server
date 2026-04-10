package cn.guangdian.armorstats.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats {

    private double maxHealth;
    private double minAttack;
    private double maxAttack;
    private double defenseMin;
    private double defenseMax;
    private double critChancePercent;
    private double critDamagePercent;
    private double lifestealPercent;
    private double healthRegen;

    private double dodgePercent;
    private double damageReflectPercent;
    private double reflectPercent;
    private double lifestealResistPercent;
    private double critResistPercent;
    private double critDamageResistPercent;
    private double parryPercent;

    private double pvpMinAttack;
    private double pvpMaxAttack;
    private double pvpDefenseMin;
    private double pvpDefenseMax;

    private double moveSpeedPercent;
    private double poisonPercent;
    private double freezePercent;
    private double blindPercent;
    private double expBonusPercent;
    private double lifestealMultiplier;

    // 护甲与穿透系统
    private double armorPercent;           // 护甲值%
    private double armorStrength;          // 护甲强度%（抵消护甲穿透）
    private double armorPenetration;      // 护甲穿透%
    private double defensePenetration;    // 防御穿透%
    private double damageReductionBonus;  // 额外减伤%（技能/buff等）

    // 躲避反伤系统
    private double dodgeReflectPercent;   // 躲避反伤触发概率%
    private double dodgeReflectRatio;     // 躲避反弹比例%

    // 生命恢复系统
    private double healthRegenPercent;    // 生命恢复%（按最大生命百分比回复）

    // 状态效果属性
    private double burnPercent;           // 燃烧概率%
    private double scorchPercent;         // 灼烧概率%

    // 击退抗性
    private double knockbackResistPercent; // 击退抗性%

    // 环境伤害抗性
    private double fireResistPercent;      // 火焰抗性%
    private double fallResistPercent;      // 摔落抗性%
    private double drowningResistPercent;  // 溺水抗性%
    private double poisonResistPercent;    // 中毒抗性%
    private double witherResistPercent;    // 凋零抗性%
    private double lavaResistPercent;      // 岩浆抗性%
    private double magicResistPercent;     // 魔法抗性%
    private double explosionResistPercent; // 爆炸抗性%
    private double projectileResistPercent; // 弹射物抗性%

    public PlayerStats() {
        reset();
    }

    public void reset() {
        // 以下是装备提供的额外属性，默认值都应该是 0
        // 基础值（如基础攻击1、基础暴击伤害200%）在最终计算时处理
        this.maxHealth = 0.0;
        this.minAttack = 0.0;
        this.maxAttack = 0.0;
        this.defenseMin = 0.0;
        this.defenseMax = 0.0;
        this.critChancePercent = 0.0;
        this.critDamagePercent = 0.0;  // 装备提供的额外暴击伤害，不是总暴击伤害
        this.lifestealPercent = 0.0;
        this.healthRegen = 0.0;

        this.dodgePercent = 0.0;
        this.damageReflectPercent = 0.0;
        this.reflectPercent = 0.0;
        this.lifestealResistPercent = 0.0;
        this.critResistPercent = 0.0;
        this.critDamageResistPercent = 0.0;
        this.parryPercent = 0.0;

        this.pvpMinAttack = 0.0;
        this.pvpMaxAttack = 0.0;
        this.pvpDefenseMin = 0.0;
        this.pvpDefenseMax = 0.0;

        this.moveSpeedPercent = 0.0;
        this.poisonPercent = 0.0;
        this.freezePercent = 0.0;
        this.blindPercent = 0.0;
        this.expBonusPercent = 0.0;
        this.lifestealMultiplier = 0.0;  // 装备提供的额外吸血倍率，不是总倍率

        // 护甲与穿透系统
        this.armorPercent = 0.0;
        this.armorStrength = 0.0;
        this.armorPenetration = 0.0;
        this.defensePenetration = 0.0;
        this.damageReductionBonus = 0.0;

        // 躲避反伤系统
        this.dodgeReflectPercent = 0.0;
        this.dodgeReflectRatio = 0.0;

        // 生命恢复系统
        this.healthRegenPercent = 0.0;

        // 状态效果属性
        this.burnPercent = 0.0;
        this.scorchPercent = 0.0;

        // 击退抗性
        this.knockbackResistPercent = 0.0;

        // 环境伤害抗性
        this.fireResistPercent = 0.0;
        this.fallResistPercent = 0.0;
        this.drowningResistPercent = 0.0;
        this.poisonResistPercent = 0.0;
        this.witherResistPercent = 0.0;
        this.lavaResistPercent = 0.0;
        this.magicResistPercent = 0.0;
        this.explosionResistPercent = 0.0;
        this.projectileResistPercent = 0.0;
    }

    public void addStats(Map<String, AttributeValue> attrs) {
        if (attrs == null) return;

        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            String key = entry.getKey();
            AttributeValue val = entry.getValue();
            if (val instanceof AttributeValue.SingleValue) {
                double v = ((AttributeValue.SingleValue) val).getValue();
                switch (key) {
                    case "生命上限": this.maxHealth += v; break;
                    case "暴击几率": this.critChancePercent += v; break;
                    case "暴击伤害": this.critDamagePercent += v; break;
                    case "吸血几率": this.lifestealPercent += v; break;
                    case "每秒回血": this.healthRegen += v; break;
                    case "闪避": this.dodgePercent += v; break;
                    case "伤害反弹": this.damageReflectPercent += v; break;
                    case "反伤比例": this.reflectPercent += v; break;
                    case "吸血抵抗": this.lifestealResistPercent += v; break;
                    case "暴击抵抗": this.critResistPercent += v; break;
                    case "暴伤抵抗": this.critDamageResistPercent += v; break;
                    case "招架": this.parryPercent += v; break;
                    case "移动速度": this.moveSpeedPercent += v; break;
                    case "中毒": this.poisonPercent += v; break;
                    case "冰冻": this.freezePercent += v; break;
                    case "致盲": this.blindPercent += v; break;
                    case "经验加成": this.expBonusPercent += v; break;
                    case "吸血倍率": this.lifestealMultiplier += v; break;
                    // 护甲与穿透系统
                    case "护甲值": this.armorPercent += v; break;
                    case "护甲强度": this.armorStrength += v; break;
                    case "护甲穿透": this.armorPenetration += v; break;
                    case "防御穿透": this.defensePenetration += v; break;
                    case "减伤": this.damageReductionBonus += v; break;
                    // 躲避反伤系统
                    case "躲避反伤": this.dodgeReflectPercent += v; break;
                    case "躲避反弹比例": this.dodgeReflectRatio += v; break;
                    // 生命恢复系统
                    case "生命恢复": this.healthRegenPercent += v; break;
                    // 状态效果属性
                    case "燃烧": this.burnPercent += v; break;
                    case "灼烧": this.scorchPercent += v; break;
                    // 击退抗性
                    case "击退抗性": this.knockbackResistPercent += v; break;
                    // 环境伤害抗性
                    case "火焰抗性": this.fireResistPercent += v; break;
                    case "摔落抗性": this.fallResistPercent += v; break;
                    case "溺水抗性": this.drowningResistPercent += v; break;
                    case "中毒抗性": this.poisonResistPercent += v; break;
                    case "凋零抗性": this.witherResistPercent += v; break;
                    case "岩浆抗性": this.lavaResistPercent += v; break;
                    case "魔法抗性": this.magicResistPercent += v; break;
                    case "爆炸抗性": this.explosionResistPercent += v; break;
                    case "弹射物抗性": this.projectileResistPercent += v; break;
                    default: break;
                }
            } else if (val instanceof AttributeValue.RangeValue) {
                double min = ((AttributeValue.RangeValue) val).getMin();
                double max = ((AttributeValue.RangeValue) val).getMax();
                switch (key) {
                    case "攻击力":
                        this.minAttack += min;
                        this.maxAttack += max;
                        break;
                    case "防御力":
                        this.defenseMin += min;
                        this.defenseMax += max;
                        break;
                    case "PVP攻击力":
                        this.pvpMinAttack += min;
                        this.pvpMaxAttack += max;
                        break;
                    case "PVP防御力":
                        this.pvpDefenseMin += min;
                        this.pvpDefenseMax += max;
                        break;
                    default: break;
                }
            }
        }
    }

    /**
     * 合并另一个PlayerStats的属性
     */
    public void addPlayerStats(PlayerStats other) {
        if (other == null) return;
        
        this.maxHealth += other.maxHealth;
        this.minAttack += other.minAttack;
        this.maxAttack += other.maxAttack;
        this.defenseMin += other.defenseMin;
        this.defenseMax += other.defenseMax;
        this.critChancePercent += other.critChancePercent;
        this.critDamagePercent += other.critDamagePercent;
        this.lifestealPercent += other.lifestealPercent;
        this.healthRegen += other.healthRegen;
        this.dodgePercent += other.dodgePercent;
        this.damageReflectPercent += other.damageReflectPercent;
        this.reflectPercent += other.reflectPercent;
        this.lifestealResistPercent += other.lifestealResistPercent;
        this.critResistPercent += other.critResistPercent;
        this.critDamageResistPercent += other.critDamageResistPercent;
        this.parryPercent += other.parryPercent;
        this.pvpMinAttack += other.pvpMinAttack;
        this.pvpMaxAttack += other.pvpMaxAttack;
        this.pvpDefenseMin += other.pvpDefenseMin;
        this.pvpDefenseMax += other.pvpDefenseMax;
        this.moveSpeedPercent += other.moveSpeedPercent;
        this.poisonPercent += other.poisonPercent;
        this.freezePercent += other.freezePercent;
        this.blindPercent += other.blindPercent;
        this.expBonusPercent += other.expBonusPercent;
        this.lifestealMultiplier += other.lifestealMultiplier;
        // 护甲与穿透系统
        this.armorPercent += other.armorPercent;
        this.armorStrength += other.armorStrength;
        this.armorPenetration += other.armorPenetration;
        this.defensePenetration += other.defensePenetration;
        this.damageReductionBonus += other.damageReductionBonus;

        // 躲避反伤系统
        this.dodgeReflectPercent += other.dodgeReflectPercent;
        this.dodgeReflectRatio += other.dodgeReflectRatio;

        // 生命恢复系统
        this.healthRegenPercent += other.healthRegenPercent;

        // 状态效果属性
        this.burnPercent += other.burnPercent;
        this.scorchPercent += other.scorchPercent;

        // 击退抗性
        this.knockbackResistPercent += other.knockbackResistPercent;

        // 环境伤害抗性
        this.fireResistPercent += other.fireResistPercent;
        this.fallResistPercent += other.fallResistPercent;
        this.drowningResistPercent += other.drowningResistPercent;
        this.poisonResistPercent += other.poisonResistPercent;
        this.witherResistPercent += other.witherResistPercent;
        this.lavaResistPercent += other.lavaResistPercent;
        this.magicResistPercent += other.magicResistPercent;
        this.explosionResistPercent += other.explosionResistPercent;
        this.projectileResistPercent += other.projectileResistPercent;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("maxHealth", maxHealth);
        map.put("minAttack", minAttack);
        map.put("maxAttack", maxAttack);
        map.put("defenseMin", defenseMin);
        map.put("defenseMax", defenseMax);
        map.put("critChancePercent", critChancePercent);
        map.put("critDamagePercent", critDamagePercent);
        map.put("lifestealPercent", lifestealPercent);
        map.put("healthRegen", healthRegen);
        map.put("dodgePercent", dodgePercent);
        map.put("damageReflectPercent", damageReflectPercent);
        map.put("reflectPercent", reflectPercent);
        map.put("lifestealResistPercent", lifestealResistPercent);
        map.put("critResistPercent", critResistPercent);
        map.put("critDamageResistPercent", critDamageResistPercent);
        map.put("parryPercent", parryPercent);
        map.put("pvpMinAttack", pvpMinAttack);
        map.put("pvpMaxAttack", pvpMaxAttack);
        map.put("pvpDefenseMin", pvpDefenseMin);
        map.put("pvpDefenseMax", pvpDefenseMax);
        map.put("moveSpeedPercent", moveSpeedPercent);
        map.put("poisonPercent", poisonPercent);
        map.put("freezePercent", freezePercent);
        map.put("blindPercent", blindPercent);
        map.put("expBonusPercent", expBonusPercent);
        map.put("lifestealMultiplier", lifestealMultiplier);
        return map;
    }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public double getMinAttack() { return minAttack; }
    public void setMinAttack(double minAttack) { this.minAttack = minAttack; }

    public double getMaxAttack() { return maxAttack; }
    public void setMaxAttack(double maxAttack) { this.maxAttack = maxAttack; }

    public double getDefenseMin() { return defenseMin; }
    public void setDefenseMin(double defenseMin) { this.defenseMin = defenseMin; }

    public double getDefenseMax() { return defenseMax; }
    public void setDefenseMax(double defenseMax) { this.defenseMax = defenseMax; }

    public double getDefenseAverage() {
        return (defenseMin + defenseMax) / 2.0;
    }

    public double getAttackAverage() {
        return (minAttack + maxAttack) / 2.0;
    }

    public double getCritChancePercent() { return critChancePercent; }
    public void setCritChancePercent(double critChancePercent) { this.critChancePercent = critChancePercent; }

    public double getCritDamagePercent() { return critDamagePercent; }
    public void setCritDamagePercent(double critDamagePercent) { this.critDamagePercent = critDamagePercent; }

    public double getLifestealPercent() { return lifestealPercent; }
    public void setLifestealPercent(double lifestealPercent) { this.lifestealPercent = lifestealPercent; }

    public double getHealthRegen() { return healthRegen; }
    public void setHealthRegen(double healthRegen) { this.healthRegen = healthRegen; }

    public double getDodgePercent() { return dodgePercent; }
    public void setDodgePercent(double dodgePercent) { this.dodgePercent = dodgePercent; }

    public double getDamageReflectPercent() { return damageReflectPercent; }
    public void setDamageReflectPercent(double damageReflectPercent) { this.damageReflectPercent = damageReflectPercent; }

    public double getReflectPercent() { return reflectPercent; }
    public void setReflectPercent(double reflectPercent) { this.reflectPercent = reflectPercent; }

    public double getLifestealResistPercent() { return lifestealResistPercent; }
    public void setLifestealResistPercent(double lifestealResistPercent) { this.lifestealResistPercent = lifestealResistPercent; }

    public double getCritResistPercent() { return critResistPercent; }
    public void setCritResistPercent(double critResistPercent) { this.critResistPercent = critResistPercent; }

    public double getCritDamageResistPercent() { return critDamageResistPercent; }
    public void setCritDamageResistPercent(double critDamageResistPercent) { this.critDamageResistPercent = critDamageResistPercent; }

    public double getParryPercent() { return parryPercent; }
    public void setParryPercent(double parryPercent) { this.parryPercent = parryPercent; }

    public double getPvpMinAttack() { return pvpMinAttack; }
    public void setPvpMinAttack(double pvpMinAttack) { this.pvpMinAttack = pvpMinAttack; }

    public double getPvpMaxAttack() { return pvpMaxAttack; }
    public void setPvpMaxAttack(double pvpMaxAttack) { this.pvpMaxAttack = pvpMaxAttack; }

    public double getPvpDefenseMin() { return pvpDefenseMin; }
    public void setPvpDefenseMin(double pvpDefenseMin) { this.pvpDefenseMin = pvpDefenseMin; }

    public double getPvpDefenseMax() { return pvpDefenseMax; }
    public void setPvpDefenseMax(double pvpDefenseMax) { this.pvpDefenseMax = pvpDefenseMax; }

    public double getPvpAttackAverage() {
        return (pvpMinAttack + pvpMaxAttack) / 2.0;
    }

    public double getPvpDefenseAverage() {
        return (pvpDefenseMin + pvpDefenseMax) / 2.0;
    }

    public double getMoveSpeedPercent() { return moveSpeedPercent; }
    public void setMoveSpeedPercent(double moveSpeedPercent) { this.moveSpeedPercent = moveSpeedPercent; }

    public double getPoisonPercent() { return poisonPercent; }
    public void setPoisonPercent(double poisonPercent) { this.poisonPercent = poisonPercent; }

    public double getFreezePercent() { return freezePercent; }
    public void setFreezePercent(double freezePercent) { this.freezePercent = freezePercent; }

    public double getBlindPercent() { return blindPercent; }
    public void setBlindPercent(double blindPercent) { this.blindPercent = blindPercent; }

    public double getExpBonusPercent() { return expBonusPercent; }
    public void setExpBonusPercent(double expBonusPercent) { this.expBonusPercent = expBonusPercent; }

    public double getLifestealMultiplier() { return lifestealMultiplier; }
    public void setLifestealMultiplier(double lifestealMultiplier) { this.lifestealMultiplier = lifestealMultiplier; }

    // 护甲与穿透系统 getter/setter
    public double getArmorPercent() { return armorPercent; }
    public void setArmorPercent(double armorPercent) { this.armorPercent = armorPercent; }

    public double getArmorStrength() { return armorStrength; }
    public void setArmorStrength(double armorStrength) { this.armorStrength = armorStrength; }

    public double getArmorPenetration() { return armorPenetration; }
    public void setArmorPenetration(double armorPenetration) { this.armorPenetration = armorPenetration; }

    public double getDefensePenetration() { return defensePenetration; }
    public void setDefensePenetration(double defensePenetration) { this.defensePenetration = defensePenetration; }

    public double getDamageReductionBonus() { return damageReductionBonus; }
    public void setDamageReductionBonus(double damageReductionBonus) { this.damageReductionBonus = damageReductionBonus; }

    // 躲避反伤系统 getter/setter
    public double getDodgeReflectPercent() { return dodgeReflectPercent; }
    public void setDodgeReflectPercent(double dodgeReflectPercent) { this.dodgeReflectPercent = dodgeReflectPercent; }

    public double getDodgeReflectRatio() { return dodgeReflectRatio; }
    public void setDodgeReflectRatio(double dodgeReflectRatio) { this.dodgeReflectRatio = dodgeReflectRatio; }

    // 生命恢复系统 getter/setter
    public double getHealthRegenPercent() { return healthRegenPercent; }
    public void setHealthRegenPercent(double healthRegenPercent) { this.healthRegenPercent = healthRegenPercent; }

    // 状态效果属性 getter/setter
    public double getBurnPercent() { return burnPercent; }
    public void setBurnPercent(double burnPercent) { this.burnPercent = burnPercent; }

    public double getScorchPercent() { return scorchPercent; }
    public void setScorchPercent(double scorchPercent) { this.scorchPercent = scorchPercent; }

    // 击退抗性 getter/setter
    public double getKnockbackResistPercent() { return knockbackResistPercent; }
    public void setKnockbackResistPercent(double knockbackResistPercent) { this.knockbackResistPercent = knockbackResistPercent; }

    // 环境伤害抗性 getter/setter
    public double getFireResistPercent() { return fireResistPercent; }
    public void setFireResistPercent(double fireResistPercent) { this.fireResistPercent = fireResistPercent; }

    public double getFallResistPercent() { return fallResistPercent; }
    public void setFallResistPercent(double fallResistPercent) { this.fallResistPercent = fallResistPercent; }

    public double getDrowningResistPercent() { return drowningResistPercent; }
    public void setDrowningResistPercent(double drowningResistPercent) { this.drowningResistPercent = drowningResistPercent; }

    public double getPoisonResistPercent() { return poisonResistPercent; }
    public void setPoisonResistPercent(double poisonResistPercent) { this.poisonResistPercent = poisonResistPercent; }

    public double getWitherResistPercent() { return witherResistPercent; }
    public void setWitherResistPercent(double witherResistPercent) { this.witherResistPercent = witherResistPercent; }

    public double getLavaResistPercent() { return lavaResistPercent; }
    public void setLavaResistPercent(double lavaResistPercent) { this.lavaResistPercent = lavaResistPercent; }

    public double getMagicResistPercent() { return magicResistPercent; }
    public void setMagicResistPercent(double magicResistPercent) { this.magicResistPercent = magicResistPercent; }

    public double getExplosionResistPercent() { return explosionResistPercent; }
    public void setExplosionResistPercent(double explosionResistPercent) { this.explosionResistPercent = explosionResistPercent; }

    public double getProjectileResistPercent() { return projectileResistPercent; }
    public void setProjectileResistPercent(double projectileResistPercent) { this.projectileResistPercent = projectileResistPercent; }
}