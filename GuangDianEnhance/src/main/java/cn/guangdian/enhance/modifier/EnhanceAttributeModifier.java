package cn.guangdian.enhance.modifier;

import cn.guangdian.enhance.config.EnhanceConfig;

import java.util.HashMap;
import java.util.Map;

public class EnhanceAttributeModifier {

    private final EnhanceConfig config;

    public EnhanceAttributeModifier(EnhanceConfig config) {
        this.config = config;
    }

    public double getMultiplier(int level) {
        if (level <= 0) {
            return 1.0;
        }
        
        return 1.0 + level * config.getAttributeBonusPerLevel();
    }

    public double applyBonus(double baseValue, int level) {
        double multiplier = getMultiplier(level);
        
        if ("multiplier".equals(config.getAttributeBonusType())) {
            return baseValue * multiplier;
        } else {
            return baseValue + (level * config.getAttributeBonusPerLevel() * 10);
        }
    }

    public Map<String, Double> applyBonusToAttributes(
            Map<String, Double> baseAttributes, int level) {
        
        if (level <= 0) {
            return baseAttributes;
        }
        
        Map<String, Double> enhanced = new HashMap<>();
        double multiplier = getMultiplier(level);
        
        for (Map.Entry<String, Double> entry : baseAttributes.entrySet()) {
            String attrName = entry.getKey();
            double baseValue = entry.getValue();
            
            double enhancedValue;
            if ("multiplier".equals(config.getAttributeBonusType())) {
                enhancedValue = baseValue * multiplier;
            } else {
                enhancedValue = baseValue + (level * config.getAttributeBonusPerLevel() * getAttributeWeight(attrName));
            }
            
            enhanced.put(attrName, enhancedValue);
        }
        
        return enhanced;
    }

    private double getAttributeWeight(String attrName) {
        return switch (attrName) {
            case "攻击力", "防御力" -> 10.0;
            case "生命上限" -> 100.0;
            case "暴击几率", "暴击伤害" -> 5.0;
            default -> 1.0;
        };
    }

    public String formatBonus(int level) {
        if (level <= 0) {
            return "0%";
        }
        
        double bonus = level * config.getAttributeBonusPerLevel() * 100;
        return String.format("+%.0f%%", bonus);
    }

    public int calculateEffectiveLevel(int currentLevel, double targetMultiplier) {
        if (targetMultiplier <= 1.0) {
            return 0;
        }
        
        double bonusPerLevel = config.getAttributeBonusPerLevel();
        if (bonusPerLevel <= 0) {
            return 0;
        }
        
        int targetLevel = (int) Math.round((targetMultiplier - 1.0) / bonusPerLevel);
        return Math.max(0, Math.min(targetLevel, config.getMaxLevel()));
    }
}
