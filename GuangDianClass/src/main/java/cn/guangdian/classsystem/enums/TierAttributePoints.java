package cn.guangdian.classsystem.enums;

import cn.guangdian.classsystem.GuangDianClass;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 阶位属性点 - 从配置文件读取每个阶位的属性点数量
 * 
 * 属性点获取规则：
 * - 阶位提升时获得属性点（从 config.yml 读取）
 * - 不通过转职获得属性点
 * - 阶位越高，属性点越多
 */
public enum TierAttributePoints {
    
    TIER_1(1),
    TIER_2(2),
    TIER_3(3),
    TIER_4(4),
    TIER_5(5),
    TIER_6(6),
    TIER_7(7),
    TIER_8(8),
    TIER_9(9);
    
    private final int tier;
    private int points;
    
    TierAttributePoints(int tier) {
        this.tier = tier;
        this.points = getDefaultPoints(tier);
    }
    
    private static int getDefaultPoints(int tier) {
        switch (tier) {
            case 1: return 10;
            case 2: return 15;
            case 3: return 20;
            case 4: return 30;
            case 5: return 40;
            case 6: return 50;
            case 7: return 60;
            case 8: return 80;
            case 9: return 100;
            default: return 10;
        }
    }
    
    public int getTier() {
        return tier;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    /**
     * 从配置文件加载阶位属性点
     */
    public static void loadFromConfig(GuangDianClass plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("attributes.tier-points");
        if (section == null) return;
        
        for (TierAttributePoints tap : values()) {
            String key = String.valueOf(tap.tier);
            if (section.contains(key)) {
                tap.points = section.getInt(key, tap.points);
            }
        }
    }
    
    /**
     * 根据阶位获取属性点数量
     */
    public static int getPointsByTier(int tier) {
        switch (tier) {
            case 1: return TIER_1.points;
            case 2: return TIER_2.points;
            case 3: return TIER_3.points;
            case 4: return TIER_4.points;
            case 5: return TIER_5.points;
            case 6: return TIER_6.points;
            case 7: return TIER_7.points;
            case 8: return TIER_8.points;
            case 9: return TIER_9.points;
            default: return 10;
        }
    }
    
    /**
     * 计算从当前阶位到目标阶位总共获得的属性点
     */
    public static int getTotalPointsFromTier(int fromTier, int toTier) {
        int total = 0;
        for (int t = fromTier; t <= toTier; t++) {
            total += getPointsByTier(t);
        }
        return total;
    }
    
    /**
     * 计算玩家当前应有的总属性点（从1阶到当前阶位）
     */
    public static int getTotalPointsForTier(int currentTier) {
        return getTotalPointsFromTier(1, currentTier);
    }
}