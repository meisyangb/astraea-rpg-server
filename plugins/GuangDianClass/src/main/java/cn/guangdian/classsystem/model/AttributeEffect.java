package cn.guangdian.classsystem.model;

import java.util.HashMap;
import java.util.Map;

public class AttributeEffect {
    
    private final AttributeType type;
    private double healthPerPoint;
    private double attackPerPoint;
    private double defensePerPoint;
    private double critChancePerPoint;
    private double critDamagePerPoint;
    private double dodgePerPoint;
    private double parryPerPoint;
    private double armorPerPoint;
    private double magicResistPerPoint;
    private double lifestealPerPoint;
    private double healthRegenPerPoint;
    private double expBonusPerPoint;
    private double moveSpeedPerPoint;
    private double accuracyPerPoint;
    
    public AttributeEffect(AttributeType type) {
        this.type = type;
    }
    
    public static AttributeEffect fromConfig(AttributeType type, Map<String, Object> config) {
        AttributeEffect effect = new AttributeEffect(type);
        if (config == null) return effect;
        
        effect.healthPerPoint = getDouble(config, "health-per-point", 0);
        effect.attackPerPoint = getDouble(config, "attack-per-point", 0);
        effect.defensePerPoint = getDouble(config, "defense-per-point", 0);
        effect.critChancePerPoint = getDouble(config, "crit-chance-per-point", 0);
        effect.critDamagePerPoint = getDouble(config, "crit-damage-per-point", 0);
        effect.dodgePerPoint = getDouble(config, "dodge-per-point", 0);
        effect.parryPerPoint = getDouble(config, "parry-per-point", 0);
        effect.armorPerPoint = getDouble(config, "armor-per-point", 0);
        effect.magicResistPerPoint = getDouble(config, "magic-resist-per-point", 0);
        effect.lifestealPerPoint = getDouble(config, "lifesteal-per-point", 0);
        effect.healthRegenPerPoint = getDouble(config, "health-regen-per-point", 0);
        effect.expBonusPerPoint = getDouble(config, "exp-bonus-per-point", 0);
        effect.moveSpeedPerPoint = getDouble(config, "move-speed-per-point", 0);
        effect.accuracyPerPoint = getDouble(config, "accuracy-per-point", 0);
        
        return effect;
    }
    
    private static double getDouble(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    public Map<String, Double> calculateBonuses(int points) {
        Map<String, Double> bonuses = new HashMap<>();
        if (points <= 0) return bonuses;
        
        if (healthPerPoint > 0) bonuses.put("health", healthPerPoint * points);
        if (attackPerPoint > 0) {
            bonuses.put("attack-min", attackPerPoint * points);
            bonuses.put("attack-max", attackPerPoint * points);
        }
        if (defensePerPoint > 0) {
            bonuses.put("defense-min", defensePerPoint * points);
            bonuses.put("defense-max", defensePerPoint * points);
        }
        if (critChancePerPoint > 0) bonuses.put("crit-chance", critChancePerPoint * points);
        if (critDamagePerPoint > 0) bonuses.put("crit-damage", critDamagePerPoint * points);
        if (dodgePerPoint > 0) bonuses.put("dodge", dodgePerPoint * points);
        if (parryPerPoint > 0) bonuses.put("parry", parryPerPoint * points);
        if (armorPerPoint > 0) bonuses.put("armor", armorPerPoint * points);
        if (magicResistPerPoint > 0) bonuses.put("magic-resist", magicResistPerPoint * points);
        if (lifestealPerPoint > 0) bonuses.put("lifesteal", lifestealPerPoint * points);
        if (healthRegenPerPoint > 0) bonuses.put("health-regen", healthRegenPerPoint * points);
        if (expBonusPerPoint > 0) bonuses.put("exp-bonus", expBonusPerPoint * points);
        if (moveSpeedPerPoint > 0) bonuses.put("move-speed", moveSpeedPerPoint * points);
        if (accuracyPerPoint > 0) bonuses.put("accuracy", accuracyPerPoint * points);
        
        return bonuses;
    }
    
    public String[] getEffectDescriptions(int points) {
        java.util.List<String> descriptions = new java.util.ArrayList<>();
        if (points <= 0) return descriptions.toArray(new String[0]);
        
        if (healthPerPoint > 0) descriptions.add("<red>生命 +" + (int)(healthPerPoint * points));
        if (attackPerPoint > 0) descriptions.add("<aqua>攻击 +" + (int)(attackPerPoint * points));
        if (defensePerPoint > 0) descriptions.add("<green>防御 +" + (int)(defensePerPoint * points));
        if (critChancePerPoint > 0) descriptions.add("<gold>暴击率 +" + String.format("%.1f%%", critChancePerPoint * points));
        if (critDamagePerPoint > 0) descriptions.add("<gold>暴击伤害 +" + String.format("%.1f%%", critDamagePerPoint * points));
        if (dodgePerPoint > 0) descriptions.add("<aqua>闪避 +" + String.format("%.1f%%", dodgePerPoint * points));
        if (parryPerPoint > 0) descriptions.add("<yellow>招架 +" + String.format("%.1f%%", parryPerPoint * points));
        if (armorPerPoint > 0) descriptions.add("<gray>护甲 +" + String.format("%.1f%%", armorPerPoint * points));
        if (magicResistPerPoint > 0) descriptions.add("<light_purple>魔抗 +" + String.format("%.1f%%", magicResistPerPoint * points));
        if (lifestealPerPoint > 0) descriptions.add("<red>吸血 +" + String.format("%.1f%%", lifestealPerPoint * points));
        if (healthRegenPerPoint > 0) descriptions.add("<red>回血 +" + String.format("%.1f", healthRegenPerPoint * points) + "/秒");
        if (expBonusPerPoint > 0) descriptions.add("<yellow>经验加成 +" + String.format("%.1f%%", expBonusPerPoint * points));
        if (moveSpeedPerPoint > 0) descriptions.add("<white>移速 +" + String.format("%.1f%%", moveSpeedPerPoint * points));
        if (accuracyPerPoint > 0) descriptions.add("<white>命中 +" + String.format("%.1f%%", accuracyPerPoint * points));
        
        return descriptions.toArray(new String[0]);
    }
    
    public AttributeType getType() { return type; }
    public double getHealthPerPoint() { return healthPerPoint; }
    public double getAttackPerPoint() { return attackPerPoint; }
    public double getDefensePerPoint() { return defensePerPoint; }
    public double getCritChancePerPoint() { return critChancePerPoint; }
    public double getCritDamagePerPoint() { return critDamagePerPoint; }
    public double getDodgePerPoint() { return dodgePerPoint; }
    public double getParryPerPoint() { return parryPerPoint; }
    public double getArmorPerPoint() { return armorPerPoint; }
    public double getMagicResistPerPoint() { return magicResistPerPoint; }
    public double getLifestealPerPoint() { return lifestealPerPoint; }
    public double getHealthRegenPerPoint() { return healthRegenPerPoint; }
    public double getExpBonusPerPoint() { return expBonusPerPoint; }
    public double getMoveSpeedPerPoint() { return moveSpeedPerPoint; }
    public double getAccuracyPerPoint() { return accuracyPerPoint; }
}
