package cn.guangdian.realm;

import org.bukkit.configuration.ConfigurationSection;

/**
 * 境界数据类
 */
public class Realm {
    private final String id;
    private final String name;
    private final String realmType;
    private final int stage;
    private final long requiredCultivation;
    private final RealmBonuses bonuses;
    private final String description;
    
    public Realm(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.realmType = config.getString("realm_type", "lianqi");
        this.stage = config.getInt("stage", 1);
        this.requiredCultivation = config.getLong("required_cultivation", 0);
        this.description = config.getString("description", "");
        
        ConfigurationSection bonusSection = config.getConfigurationSection("bonuses");
        if (bonusSection != null) {
            this.bonuses = new RealmBonuses(bonusSection);
        } else {
            this.bonuses = new RealmBonuses();
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRealmType() { return realmType; }
    public int getStage() { return stage; }
    public long getRequiredCultivation() { return requiredCultivation; }
    public RealmBonuses getBonuses() { return bonuses; }
    public String getDescription() { return description; }
    
    /**
     * 获取境界类型的中文名
     */
    public String getRealmTypeName() {
        return switch (realmType) {
            case "lianqi" -> "练气期";
            case "zhuji" -> "筑基期";
            case "jiadan" -> "假丹期";
            case "jindan" -> "金丹期";
            case "yuanying" -> "元婴期";
            case "huashen" -> "化神期";
            default -> "未知";
        };
    }
    
    /**
     * 境界加成数据类
     */
    public static class RealmBonuses {
        private final int maxHealth;
        private final int attackDamage;
        private final int defense;
        
        public RealmBonuses() {
            this.maxHealth = 0;
            this.attackDamage = 0;
            this.defense = 0;
        }
        
        public RealmBonuses(ConfigurationSection config) {
            this.maxHealth = config.getInt("max_health", 0);
            this.attackDamage = config.getInt("attack_damage", 0);
            this.defense = config.getInt("defense", 0);
        }
        
        public int getMaxHealth() { return maxHealth; }
        public int getAttackDamage() { return attackDamage; }
        public int getDefense() { return defense; }
    }
}