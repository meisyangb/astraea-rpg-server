package cn.guangdian.armorstats.boss;

import java.util.HashMap;
import java.util.Map;

public class BossStats {

    private final String mythicMobId;
    private String displayName;
    private double minAttack;
    private double maxAttack;
    private double defense;
    private double armorPercent;
    private double critChance;
    private double critDamage;
    private double dodgeChance;
    private double parryChance;
    private double damageReduction;
    private double armorPenetration;
    private double defensePenetration;
    private double healthMultiplier;
    private double damageMultiplier;
    private Map<String, Double> elementalDamage;
    private Map<String, Double> elementalResistance;
    private Map<String, Object> customAttributes;

    public BossStats(String mythicMobId) {
        this.mythicMobId = mythicMobId;
        this.elementalDamage = new HashMap<>();
        this.elementalResistance = new HashMap<>();
        this.customAttributes = new HashMap<>();
        this.healthMultiplier = 1.0;
        this.damageMultiplier = 1.0;
    }

    public String getMythicMobId() {
        return mythicMobId;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : mythicMobId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getMinAttack() {
        return minAttack;
    }

    public void setMinAttack(double minAttack) {
        this.minAttack = minAttack;
    }

    public double getMaxAttack() {
        return maxAttack;
    }

    public void setMaxAttack(double maxAttack) {
        this.maxAttack = maxAttack;
    }

    public double getAttackAverage() {
        return (minAttack + maxAttack) / 2.0;
    }

    public double getDefense() {
        return defense;
    }

    public void setDefense(double defense) {
        this.defense = defense;
    }

    public double getArmorPercent() {
        return armorPercent;
    }

    public void setArmorPercent(double armorPercent) {
        this.armorPercent = armorPercent;
    }

    public double getCritChance() {
        return critChance;
    }

    public void setCritChance(double critChance) {
        this.critChance = critChance;
    }

    public double getCritDamage() {
        return critDamage;
    }

    public void setCritDamage(double critDamage) {
        this.critDamage = critDamage;
    }

    public double getDodgeChance() {
        return dodgeChance;
    }

    public void setDodgeChance(double dodgeChance) {
        this.dodgeChance = dodgeChance;
    }

    public double getParryChance() {
        return parryChance;
    }

    public void setParryChance(double parryChance) {
        this.parryChance = parryChance;
    }

    public double getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(double damageReduction) {
        this.damageReduction = damageReduction;
    }

    public double getArmorPenetration() {
        return armorPenetration;
    }

    public void setArmorPenetration(double armorPenetration) {
        this.armorPenetration = armorPenetration;
    }

    public double getDefensePenetration() {
        return defensePenetration;
    }

    public void setDefensePenetration(double defensePenetration) {
        this.defensePenetration = defensePenetration;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public void setHealthMultiplier(double healthMultiplier) {
        this.healthMultiplier = healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public Map<String, Double> getElementalDamage() {
        return elementalDamage;
    }

    public void setElementalDamage(Map<String, Double> elementalDamage) {
        this.elementalDamage = elementalDamage != null ? elementalDamage : new HashMap<>();
    }

    public void addElementalDamage(String element, double damage) {
        elementalDamage.put(element, damage);
    }

    public double getElementalDamage(String element) {
        return elementalDamage.getOrDefault(element, 0.0);
    }

    public Map<String, Double> getElementalResistance() {
        return elementalResistance;
    }

    public void setElementalResistance(Map<String, Double> elementalResistance) {
        this.elementalResistance = elementalResistance != null ? elementalResistance : new HashMap<>();
    }

    public void addElementalResistance(String element, double resistance) {
        elementalResistance.put(element, resistance);
    }

    public double getElementalResistance(String element) {
        return elementalResistance.getOrDefault(element, 0.0);
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes != null ? customAttributes : new HashMap<>();
    }

    public void setCustomAttribute(String key, Object value) {
        customAttributes.put(key, value);
    }

    public Object getCustomAttribute(String key) {
        return customAttributes.get(key);
    }

    public boolean hasCustomAttributes() {
        return !customAttributes.isEmpty();
    }
}
