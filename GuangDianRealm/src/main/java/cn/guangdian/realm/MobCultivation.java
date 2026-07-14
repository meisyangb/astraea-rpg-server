package cn.guangdian.realm;

import org.bukkit.configuration.ConfigurationSection;

/**
 * 怪物修为配置类
 */
public class MobCultivation {
    private final String mobName;
    private final int level;
    private final long cultivation;
    private final String realmRequirement;
    
    public MobCultivation(String mobName, ConfigurationSection config) {
        this.mobName = mobName;
        this.level = config.getInt("level", 1);
        this.cultivation = config.getLong("cultivation", 0);
        this.realmRequirement = config.getString("realm_requirement", "");
    }
    
    // Getters
    public String getMobName() { return mobName; }
    public int getLevel() { return level; }
    public long getCultivation() { return cultivation; }
    public String getRealmRequirement() { return realmRequirement; }
    
    public boolean hasRealmRequirement() {
        return realmRequirement != null && !realmRequirement.isEmpty();
    }
}