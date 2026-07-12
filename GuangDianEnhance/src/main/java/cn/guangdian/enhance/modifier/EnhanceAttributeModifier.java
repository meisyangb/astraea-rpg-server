package cn.guangdian.enhance.modifier;

import cn.guangdian.enhance.config.EnhanceConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 【枚举法】属性修改器
 * 直接使用配置中定义的固定倍率，不再使用公式计算
 */
public class EnhanceAttributeModifier {

    private final EnhanceConfig config;

    public EnhanceAttributeModifier(EnhanceConfig config) {
        this.config = config;
    }

    /** 【枚举法】获取指定等级的固定属性倍率 */
    public double getMultiplier(int level) {
        return config.getMultiplierForLevel(level);
    }

    /** 应用倍率到基础属性值 */
    public double applyBonus(double baseValue, int level) {
        double multiplier = getMultiplier(level);
        return baseValue * multiplier;
    }

    /** 应用倍率到所有属性 */
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
            enhanced.put(attrName, baseValue * multiplier);
        }
        
        return enhanced;
    }

    /** 格式化显示倍率 */
    public String formatBonus(int level) {
        if (level <= 0) {
            return "0%";
        }
        
        double multiplier = getMultiplier(level);
        double bonus = (multiplier - 1.0) * 100;
        return String.format("+%.0f%%", bonus);
    }

    /** 计算有效等级（反向计算，用于验证） */
    public int calculateEffectiveLevel(double targetMultiplier) {
        // 【枚举法】遍历倍率表找到对应等级
        Map<Integer, Double> multipliers = config.getLevelMultipliers();
        for (Map.Entry<Integer, Double> entry : multipliers.entrySet()) {
            if (Math.abs(entry.getValue() - targetMultiplier) < 0.01) {
                return entry.getKey();
            }
        }
        return 0;
    }
}