package cn.guangdian.armorstats.skill;

import java.util.List;
import java.util.ArrayList;

public class Skill {

    private String name;
    private String type;
    private double triggerChance;
    private double range;
    private double damageMultiplier;
    private long cooldown;
    private String effect;
    private int duration;
    private List<String> statusEffects;
    private double healPercent;
    private double manaCost;
    private boolean trueDamage;
    private boolean pvpOnly;
    private boolean dot;

    public Skill(String name, String type, double triggerChance, double range, double damageMultiplier, long cooldown, String effect) {
        this.name = name;
        this.type = type;
        this.triggerChance = triggerChance;
        this.range = range;
        this.damageMultiplier = damageMultiplier;
        this.cooldown = cooldown;
        this.effect = effect;
        this.duration = 0;
        this.statusEffects = new ArrayList<>();
        this.healPercent = 0;
        this.manaCost = 0;
        this.trueDamage = false;
        this.pvpOnly = false;
        this.dot = false;
    }

    public Skill(String name, String type, double triggerChance, double range, double damageMultiplier, 
                 long cooldown, String effect, int duration, List<String> statusEffects, 
                 double healPercent, double manaCost) {
        this.name = name;
        this.type = type;
        this.triggerChance = triggerChance;
        this.range = range;
        this.damageMultiplier = damageMultiplier;
        this.cooldown = cooldown;
        this.effect = effect;
        this.duration = duration;
        this.statusEffects = statusEffects != null ? statusEffects : new ArrayList<>();
        this.healPercent = healPercent;
        this.manaCost = manaCost;
        this.trueDamage = false;
        this.pvpOnly = false;
        this.dot = false;
    }

    public Skill(String name, String type, double triggerChance, double range, double damageMultiplier, 
                 long cooldown, String effect, int duration, List<String> statusEffects, 
                 double healPercent, double manaCost, boolean trueDamage, boolean pvpOnly, boolean dot) {
        this.name = name;
        this.type = type;
        this.triggerChance = triggerChance;
        this.range = range;
        this.damageMultiplier = damageMultiplier;
        this.cooldown = cooldown;
        this.effect = effect;
        this.duration = duration;
        this.statusEffects = statusEffects != null ? statusEffects : new ArrayList<>();
        this.healPercent = healPercent;
        this.manaCost = manaCost;
        this.trueDamage = trueDamage;
        this.pvpOnly = pvpOnly;
        this.dot = dot;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public double getTriggerChance() { return triggerChance; }
    public double getRange() { return range; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public long getCooldown() { return cooldown; }
    public String getEffect() { return effect; }
    public int getDuration() { return duration; }
    public List<String> getStatusEffects() { return statusEffects; }
    public double getHealPercent() { return healPercent; }
    public double getManaCost() { return manaCost; }
    public boolean isTrueDamage() { return trueDamage; }
    public boolean isPvpOnly() { return pvpOnly; }
    public boolean isDot() { return dot; }
    
    public boolean isPassive() {
        return "damage_trigger".equals(type) || "passive".equals(type);
    }
    
    public boolean isActive() {
        return "active".equals(type);
    }

    public boolean isOnHit() {
        return "on_hit".equals(type);
    }

    public boolean hasStatusEffects() {
        return statusEffects != null && !statusEffects.isEmpty();
    }
}
