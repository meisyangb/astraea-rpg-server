package cn.guangdian.classsystem.effect;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.AdvancementLevel;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassiveEffectManager {
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final AttributeManager attributeManager;
    private final Map<UUID, AppliedEffects> appliedEffects;
    
    public PassiveEffectManager(GuangDianClass plugin, ClassManager classManager, AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.attributeManager = attributeManager;
        this.appliedEffects = new HashMap<>();
    }
    
    public void applyEffects(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return;
        
        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) return;
        
        AppliedEffects effects = appliedEffects.computeIfAbsent(
            player.getUniqueId(), 
            uuid -> new AppliedEffects()
        );
        
        effects.reset();
        
        applyClassStats(player, gameClass, effects);
        
        applyAttributeBonuses(player, effects);
        
        applyTierBonuses(player, data, effects);
        
        applyAdvancementBonuses(player, data, effects);
    }
    
    private void applyClassStats(Player player, GameClass gameClass, AppliedEffects effects) {
        Map<String, Double> stats = gameClass.getStats();
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        Double healthVal = stats.get("health");
        if (healthAttr != null && healthVal != null) {
            effects.healthBonus = healthVal;
            healthAttr.setBaseValue(20 + healthVal);
        }
        
        AttributeInstance manaAttr = player.getAttribute(Attribute.MAX_ABSORPTION);
        Double manaVal = stats.get("mana");
        if (manaAttr != null && manaVal != null) {
            effects.manaBonus = manaVal;
        }
    }
    
    private void applyAttributeBonuses(Player player, AppliedEffects effects) {
        AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player);
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && bonus.health > 0) {
            healthAttr.setBaseValue(healthAttr.getBaseValue() + bonus.health);
            effects.healthBonus += bonus.health;
        }
        
        effects.attackBonus = bonus.attack;
        effects.defenseBonus = bonus.defense;
        effects.critChance = bonus.critChance;
        effects.critDamage = bonus.critDamage;
        effects.dodgeChance = bonus.dodge;
        effects.manaBonus += bonus.mana;
    }
    
    private void applyTierBonuses(Player player, PlayerClassData data, AppliedEffects effects) {
        int tier = data.getTier();
        double tierMultiplier = 1.0 + (tier - 1) * 0.1;
        
        effects.tierMultiplier = tierMultiplier;
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double tierHealth = (tier - 1) * 10;
            healthAttr.setBaseValue(healthAttr.getBaseValue() + tierHealth);
            effects.healthBonus += tierHealth;
        }
    }
    
    private void applyAdvancementBonuses(Player player, PlayerClassData data, AppliedEffects effects) {
        int advancement = data.getAdvancementLevel();
        
        double advancementMultiplier = AdvancementLevel.getMultiplier(advancement);
        
        effects.advancementMultiplier = advancementMultiplier;
    }
    
    public void removeEffects(Player player) {
        AppliedEffects effects = appliedEffects.remove(player.getUniqueId());
        if (effects == null) return;
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20);
        }
    }
    
    public void refreshEffects(Player player) {
        removeEffects(player);
        applyEffects(player);
    }
    
    public AppliedEffects getAppliedEffects(Player player) {
        return appliedEffects.get(player.getUniqueId());
    }
    
    public double getTotalAttackBonus(Player player) {
        AppliedEffects effects = appliedEffects.get(player.getUniqueId());
        if (effects == null) return 0;
        return effects.attackBonus * effects.tierMultiplier * effects.advancementMultiplier;
    }
    
    public double getTotalDefenseBonus(Player player) {
        AppliedEffects effects = appliedEffects.get(player.getUniqueId());
        if (effects == null) return 0;
        return effects.defenseBonus * effects.tierMultiplier * effects.advancementMultiplier;
    }
    
    public double getCritChance(Player player) {
        AppliedEffects effects = appliedEffects.get(player.getUniqueId());
        if (effects == null) return 0;
        return Math.min(100, effects.critChance);
    }
    
    public double getCritDamage(Player player) {
        AppliedEffects effects = appliedEffects.get(player.getUniqueId());
        if (effects == null) return 1.5;
        return 1.5 + (effects.critDamage / 100);
    }
    
    public double getDodgeChance(Player player) {
        AppliedEffects effects = appliedEffects.get(player.getUniqueId());
        if (effects == null) return 0;
        return Math.min(50, effects.dodgeChance);
    }
    
    public static class AppliedEffects {
        public double healthBonus = 0;
        public double manaBonus = 0;
        public double attackBonus = 0;
        public double defenseBonus = 0;
        public double critChance = 0;
        public double critDamage = 0;
        public double dodgeChance = 0;
        public double tierMultiplier = 1.0;
        public double advancementMultiplier = 1.0;
        
        public void reset() {
            healthBonus = 0;
            manaBonus = 0;
            attackBonus = 0;
            defenseBonus = 0;
            critChance = 0;
            critDamage = 0;
            dodgeChance = 0;
            tierMultiplier = 1.0;
            advancementMultiplier = 1.0;
        }
    }
}
