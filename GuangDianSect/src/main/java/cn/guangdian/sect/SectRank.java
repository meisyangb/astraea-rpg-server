package cn.guangdian.sect;

import org.bukkit.configuration.ConfigurationSection;

/**
 * 门派职位类
 */
public class SectRank {
    private final String id;
    private final String name;
    private final int level;
    private final String permission;
    private final int contributionNeeded;
    private final RankBonuses bonuses;
    
    public SectRank(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.level = config.getInt("level", 1);
        this.permission = config.getString("permission", "");
        this.contributionNeeded = config.getInt("contribution_needed", 0);
        
        // 解析职位加成
        ConfigurationSection bonusSection = config.getConfigurationSection("bonuses");
        if (bonusSection != null) {
            this.bonuses = new RankBonuses(bonusSection);
        } else {
            this.bonuses = new RankBonuses();
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public String getPermission() { return permission; }
    public int getContributionNeeded() { return contributionNeeded; }
    public RankBonuses getBonuses() { return bonuses; }
    
    public boolean isHigherThan(SectRank other) {
        return this.level > other.level;
    }
    
    public boolean isAtLeast(SectRank other) {
        return this.level >= other.level;
    }
    
    /**
     * 职位加成数据类
     */
    public static class RankBonuses {
        private final int damageBonus;
        private final int healthBonus;
        private final int expBonus;
        
        public RankBonuses() {
            this.damageBonus = 0;
            this.healthBonus = 0;
            this.expBonus = 0;
        }
        
        public RankBonuses(ConfigurationSection config) {
            this.damageBonus = config.getInt("damage_bonus", 0);
            this.healthBonus = config.getInt("health_bonus", 0);
            this.expBonus = config.getInt("exp_bonus", 0);
        }
        
        public int getDamageBonus() { return damageBonus; }
        public int getHealthBonus() { return healthBonus; }
        public int getExpBonus() { return expBonus; }
    }
}