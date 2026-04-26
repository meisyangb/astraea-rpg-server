package cn.guangdian.armorstats.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ArmorStats 主配置类
 * 使用 Configurate 进行配置管理
 */
@ConfigSerializable
public class ArmorStatsConfig {

    @Setting("defense")
    private DefenseConfig defense = new DefenseConfig();

    @Setting("damage")
    private DamageConfig damage = new DamageConfig();

    @Setting("dodge")
    private DodgeConfig dodge = new DodgeConfig();

    @Setting("crit")
    private CritConfig crit = new CritConfig();

    @Setting("health")
    private HealthConfig health = new HealthConfig();

    @Setting("equipment")
    private EquipmentConfig equipment = new EquipmentConfig();

    @Setting("optimization")
    private OptimizationConfig optimization = new OptimizationConfig();

    @Setting("attributes")
    private Map<String, AttributePatternConfig> attributes = new HashMap<>();

    // Getters
    public DefenseConfig getDefense() { return defense; }
    public DamageConfig getDamage() { return damage; }
    public DodgeConfig getDodge() { return dodge; }
    public CritConfig getCrit() { return crit; }
    public HealthConfig getHealth() { return health; }
    public EquipmentConfig getEquipment() { return equipment; }
    public OptimizationConfig getOptimization() { return optimization; }
    public Map<String, AttributePatternConfig> getAttributes() { return attributes; }

    @ConfigSerializable
    public static class DefenseConfig {
        @Setting("divisor")
        private double divisor = 20.0;

        @Setting("max-reduction")
        private double maxReduction = 0.75;

        public double getDivisor() { return divisor; }
        public double getMaxReduction() { return maxReduction; }
    }

    @ConfigSerializable
    public static class DamageConfig {
        @Setting("min-damage")
        private double minDamage = 1.0;

        @Setting("crit-multiplier")
        private double critMultiplier = 1.5;

        public double getMinDamage() { return minDamage; }
        public double getCritMultiplier() { return critMultiplier; }
    }

    @ConfigSerializable
    public static class DodgeConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("max-chance")
        private double maxChance = 0.5;

        public boolean isEnabled() { return enabled; }
        public double getMaxChance() { return maxChance; }
    }

    @ConfigSerializable
    public static class CritConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("resist-enabled")
        private boolean resistEnabled = true;

        @Setting("resist-reduction")
        private double resistReduction = 0.3;

        public boolean isEnabled() { return enabled; }
        public boolean isResistEnabled() { return resistEnabled; }
        public double getResistReduction() { return resistReduction; }
    }

    @ConfigSerializable
    public static class HealthConfig {
        @Setting("display-scale-enabled")
        private boolean displayScaleEnabled = true;

        @Setting("display-max-rows")
        private int displayMaxRows = 10;

        @Setting("max-health-limit")
        private double maxHealthLimit = 2000000.0;

        public boolean isDisplayScaleEnabled() { return displayScaleEnabled; }
        public int getDisplayMaxRows() { return displayMaxRows; }
        public double getMaxHealthLimit() { return maxHealthLimit; }
    }

    @ConfigSerializable
    public static class EquipmentConfig {
        @Setting("weapon-keywords")
        private List<String> weaponKeywords = new ArrayList<>();

        @Setting("armor-keywords")
        private List<String> armorKeywords = new ArrayList<>();

        @Setting("accessory-keywords")
        private List<String> accessoryKeywords = new ArrayList<>();

        public EquipmentConfig() {
            // 默认武器关键词
            weaponKeywords.add("武器");
            weaponKeywords.add("剑");
            weaponKeywords.add("斧");
            weaponKeywords.add("弓");

            // 默认防具关键词
            armorKeywords.add("头盔");
            armorKeywords.add("胸甲");
            armorKeywords.add("护腿");
            armorKeywords.add("靴子");

            // 默认饰品关键词
            accessoryKeywords.add("项链");
            accessoryKeywords.add("戒指");
            accessoryKeywords.add("饰品");
        }

        public List<String> getWeaponKeywords() { return weaponKeywords; }
        public List<String> getArmorKeywords() { return armorKeywords; }
        public List<String> getAccessoryKeywords() { return accessoryKeywords; }
    }

    @ConfigSerializable
    public static class OptimizationConfig {
        @Setting("async-save")
        private AsyncSaveConfig asyncSave = new AsyncSaveConfig();

        @Setting("equipment-cache")
        private EquipmentCacheConfig equipmentCache = new EquipmentCacheConfig();

        @Setting("bossbar-optimizer")
        private BossBarOptimizerConfig bossBarOptimizer = new BossBarOptimizerConfig();

        public AsyncSaveConfig getAsyncSave() { return asyncSave; }
        public EquipmentCacheConfig getEquipmentCache() { return equipmentCache; }
        public BossBarOptimizerConfig getBossBarOptimizer() { return bossBarOptimizer; }
    }

    @ConfigSerializable
    public static class AsyncSaveConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("thread-pool-size")
        private int threadPoolSize = 2;

        public boolean isEnabled() { return enabled; }
        public int getThreadPoolSize() { return threadPoolSize; }
    }

    @ConfigSerializable
    public static class EquipmentCacheConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("max-size")
        private int maxSize = 1000;

        public boolean isEnabled() { return enabled; }
        public int getMaxSize() { return maxSize; }
    }

    @ConfigSerializable
    public static class BossBarOptimizerConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("min-health-change")
        private double minHealthChange = 0.5;

        @Setting("combat-duration")
        private long combatDuration = 5000;

        @Setting("combat-update-interval")
        private long combatUpdateInterval = 100;

        @Setting("normal-update-interval")
        private long normalUpdateInterval = 1000;

        public boolean isEnabled() { return enabled; }
        public double getMinHealthChange() { return minHealthChange; }
        public long getCombatDuration() { return combatDuration; }
        public long getCombatUpdateInterval() { return combatUpdateInterval; }
        public long getNormalUpdateInterval() { return normalUpdateInterval; }
    }

    @ConfigSerializable
    public static class AttributePatternConfig {
        @Setting("pattern")
        private String pattern = "";

        @Setting("type")
        private String type = "flat";

        @Setting("slot")
        private String slot = "all";

        public String getPattern() { return pattern; }
        public String getType() { return type; }
        public String getSlot() { return slot; }
    }
}
