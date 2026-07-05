package cn.guangdian.enhance.config;

import cn.guangdian.enhance.GuangDianEnhance;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class EnhanceConfig {

    private final GuangDianEnhance plugin;
    
    private int maxLevel;
    private Map<Integer, Integer> tierMaxLevel;  // Tier → 最高强化等级
    private double baseSuccessRate;
    private double decayFactor;
    private double minSuccessRate;
    private String failureType;
    private double degradeChance;
    private double destroyChance;
    private double attributeBonusPerLevel;
    private String attributeBonusType;
    private boolean protectionCharmEnabled;
    private Material protectionCharmItem;
    private boolean protectionCharmConsume;
    private Map<String, List<MaterialCost>> materialCosts;
    private double moneyCostBase;
    private double moneyCostMultiplier;
    private double[] moneyCostCache;
    private long cooldownMs;
    
    private boolean pityEnabled;
    private int pityThreshold;
    private double pityBonusPerFail;
    private double pityMaxBonus;

    public EnhanceConfig(GuangDianEnhance plugin) {
        this.plugin = plugin;
        setDefaults();
    }

    private void setDefaults() {
        maxLevel = 15;
        tierMaxLevel = new HashMap<>();
        baseSuccessRate = 1.0;
        decayFactor = 0.05;
        minSuccessRate = 0.01;
        failureType = "degrade";
        degradeChance = 0.5;
        destroyChance = 0.0;
        attributeBonusPerLevel = 0.1;
        attributeBonusType = "multiplier";
        protectionCharmEnabled = true;
        protectionCharmItem = Material.NETHER_STAR;
        protectionCharmConsume = true;
        materialCosts = new HashMap<>();
        moneyCostBase = 0;
        moneyCostMultiplier = 1.0;
        cooldownMs = 500;
        
        pityEnabled = true;
        pityThreshold = 10;
        pityBonusPerFail = 0.05;
        pityMaxBonus = 0.5;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        
        maxLevel = config.getInt("enhance.max_level", 15);
        
        // 各阶位最高强化等级
        tierMaxLevel = new HashMap<>();
        ConfigurationSection tierSection = config.getConfigurationSection("enhance.tier_max_level");
        if (tierSection != null) {
            for (String key : tierSection.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(key);
                    int tlMax = tierSection.getInt(key);
                    tierMaxLevel.put(tier, tlMax);
                } catch (NumberFormatException ignored) {}
            }
        }
        
        ConfigurationSection successSection = config.getConfigurationSection("enhance.success_rate");
        if (successSection != null) {
            baseSuccessRate = successSection.getDouble("base", 1.0);
            decayFactor = successSection.getDouble("decay_factor", 0.05);
            minSuccessRate = successSection.getDouble("min_rate", 0.01);
        }
        
        ConfigurationSection failureSection = config.getConfigurationSection("enhance.failure");
        if (failureSection != null) {
            failureType = failureSection.getString("type", "degrade");
            degradeChance = failureSection.getDouble("degrade_chance", 0.5);
            destroyChance = failureSection.getDouble("destroy_chance", 0.0);
        }
        
        ConfigurationSection bonusSection = config.getConfigurationSection("enhance.attribute_bonus");
        if (bonusSection != null) {
            attributeBonusPerLevel = bonusSection.getDouble("per_level", 0.1);
            attributeBonusType = bonusSection.getString("type", "multiplier");
        }
        
        ConfigurationSection protectionSection = config.getConfigurationSection("enhance.protection_charm");
        if (protectionSection != null) {
            protectionCharmEnabled = protectionSection.getBoolean("enabled", true);
            String materialName = protectionSection.getString("item", "NETHER_STAR");
            try {
                protectionCharmItem = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                protectionCharmItem = Material.NETHER_STAR;
            }
            protectionCharmConsume = protectionSection.getBoolean("consume_on_fail", true);
        }
        
        loadMaterialCosts(config);
        
        ConfigurationSection moneySection = config.getConfigurationSection("enhance.money_cost");
        if (moneySection != null) {
            moneyCostBase = moneySection.getDouble("base", 0);
            moneyCostMultiplier = moneySection.getDouble("multiplier", 1.0);
        }
        
        cooldownMs = config.getLong("enhance.cooldown_ms", 500);
        
        ConfigurationSection pitySection = config.getConfigurationSection("enhance.pity");
        if (pitySection != null) {
            pityEnabled = pitySection.getBoolean("enabled", true);
            pityThreshold = pitySection.getInt("threshold", 10);
            pityBonusPerFail = pitySection.getDouble("bonus_per_fail", 0.05);
            pityMaxBonus = pitySection.getDouble("max_bonus", 0.5);
        }
        
        plugin.getLogger().info("强化配置加载完成:");
        plugin.getLogger().info("  最高等级: " + maxLevel);
        plugin.getLogger().info("  基础成功率: " + (baseSuccessRate * 100) + "%");
        plugin.getLogger().info("  失败类型: " + failureType);
        plugin.getLogger().info("  保底系统: " + (pityEnabled ? "启用" : "禁用"));

        // 预计算各等级金钱消耗
        moneyCostCache = new double[maxLevel + 1];
        for (int i = 0; i <= maxLevel; i++) {
            moneyCostCache[i] = moneyCostBase * Math.pow(moneyCostMultiplier, i);
        }
    }

    private void loadMaterialCosts(FileConfiguration config) {
        materialCosts = new HashMap<>();
        
        ConfigurationSection materialsSection = config.getConfigurationSection("enhance.materials");
        if (materialsSection == null) {
            return;
        }
        
        for (String levelRange : materialsSection.getKeys(false)) {
            List<?> costList = materialsSection.getList(levelRange);
            if (costList == null) continue;
            
            List<MaterialCost> costs = new ArrayList<>();
            for (Object obj : costList) {
                if (obj instanceof String) {
                    MaterialCost cost = parseMaterialCost((String) obj);
                    if (cost != null) {
                        costs.add(cost);
                    }
                }
            }
            
            if (!costs.isEmpty()) {
                materialCosts.put(levelRange, costs);
            }
        }
    }

    private MaterialCost parseMaterialCost(String str) {
        String[] parts = str.split(":");
        if (parts.length < 2) return null;
        
        try {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);
            return new MaterialCost(material, amount);
        } catch (Exception e) {
            return null;
        }
    }

    public List<MaterialCost> getMaterialCostForLevel(int level) {
        for (Map.Entry<String, List<MaterialCost>> entry : materialCosts.entrySet()) {
            String range = entry.getKey();
            if (matchesLevelRange(range, level)) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }

    private boolean matchesLevelRange(String range, int level) {
        if (range.contains("-")) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return level >= min && level <= max;
                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            try {
                int target = Integer.parseInt(range.trim());
                return level == target;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    public double getMoneyCostForLevel(int level) {
        if (moneyCostBase <= 0) return 0;
        if (level >= 0 && level < moneyCostCache.length) return moneyCostCache[level];
        return moneyCostBase * Math.pow(moneyCostMultiplier, level);
    }

    public int getMaxLevel() { return maxLevel; }
    
    /** 获取指定Tier的最高强化等级，未配置则返回全局默认 */
    public int getMaxLevelForTier(int tier) {
        return tierMaxLevel.getOrDefault(tier, maxLevel);
    }
    
    public double getBaseSuccessRate() { return baseSuccessRate; }
    public double getDecayFactor() { return decayFactor; }
    public double getMinSuccessRate() { return minSuccessRate; }
    public String getFailureType() { return failureType; }
    public double getDegradeChance() { return degradeChance; }
    public double getDestroyChance() { return destroyChance; }
    public double getAttributeBonusPerLevel() { return attributeBonusPerLevel; }
    public String getAttributeBonusType() { return attributeBonusType; }
    public boolean isProtectionCharmEnabled() { return protectionCharmEnabled; }
    public Material getProtectionCharmItem() { return protectionCharmItem; }
    public boolean isProtectionCharmConsume() { return protectionCharmConsume; }
    public long getCooldownMs() { return cooldownMs; }
    
    public boolean isPityEnabled() { return pityEnabled; }
    public int getPityThreshold() { return pityThreshold; }
    public double getPityBonusPerFail() { return pityBonusPerFail; }
    public double getPityMaxBonus() { return pityMaxBonus; }

    public static class MaterialCost {
        private final Material material;
        private final int amount;
        
        public MaterialCost(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
        
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
    }
}
