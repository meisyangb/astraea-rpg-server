package cn.guangdian.armorstats.boss;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.config.DamageDebugConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class BossStatsManager {

    private final GuangDianArmorStats plugin;
    private final Map<String, BossStats> bossStatsMap;
    private final Map<UUID, CacheEntry> entityStatsCache;
    private FileConfiguration bossConfig;
    private File bossConfigFile;

    private Object mythicMobManager;
    private Method isMythicMobMethod;
    private Method getMythicMobInstanceMethod;
    private Method getMythicMobTypeMethod;

    private static final long CACHE_EXPIRE_MS = 30000;
    private static final int MAX_CACHE_SIZE = 500;
    
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong reflectionCalls = new AtomicLong();

    private static class CacheEntry {
        final BossStats stats;
        final long timestamp;
        final String mythicMobId;
        
        CacheEntry(BossStats stats, String mythicMobId) {
            this.stats = stats;
            this.timestamp = System.currentTimeMillis();
            this.mythicMobId = mythicMobId;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }

    public BossStatsManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.bossStatsMap = new ConcurrentHashMap<>(16, 0.75f, 2);
        this.entityStatsCache = new ConcurrentHashMap<>(64, 0.75f, 2);
        
        loadConfig();
        initMythicMobsIntegration();
    }

    private void loadConfig() {
        bossConfigFile = new File(plugin.getDataFolder(), "boss_stats.yml");
        if (!bossConfigFile.exists()) {
            plugin.saveResource("boss_stats.yml", false);
        }
        bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
        loadBossStats();
    }

    private void loadBossStats() {
        bossStatsMap.clear();
        
        ConfigurationSection bossesSection = bossConfig.getConfigurationSection("bosses");
        if (bossesSection == null) {
            plugin.getLogger().warning("No bosses section found in boss_stats.yml");
            return;
        }

        for (String mythicMobId : bossesSection.getKeys(false)) {
            ConfigurationSection bossSection = bossesSection.getConfigurationSection(mythicMobId);
            if (bossSection == null) continue;

            BossStats stats = new BossStats(mythicMobId);
            
            stats.setDisplayName(bossSection.getString("display_name", mythicMobId));
            stats.setMinAttack(bossSection.getDouble("attack.min", 100));
            stats.setMaxAttack(bossSection.getDouble("attack.max", 200));
            stats.setDefense(bossSection.getDouble("defense.base", 5000));
            stats.setArmorPercent(bossSection.getDouble("defense.armor_percent", 20));
            stats.setCritChance(bossSection.getDouble("crit.chance", 0));
            stats.setCritDamage(bossSection.getDouble("crit.damage", 150));
            stats.setDodgeChance(bossSection.getDouble("dodge.chance", 0));
            stats.setParryChance(bossSection.getDouble("parry.chance", 0));
            stats.setDamageReduction(bossSection.getDouble("defense.damage_reduction", 0));
            stats.setLifestealResistPercent(bossSection.getDouble("lifesteal_resist", 0));
            stats.setArmorPenetration(bossSection.getDouble("penetration.armor", 0));
            stats.setDefensePenetration(bossSection.getDouble("penetration.defense", 0));
            stats.setHealthMultiplier(bossSection.getDouble("multiplier.health", 1.0));
            stats.setDamageMultiplier(bossSection.getDouble("multiplier.damage", 1.0));
            
            ConfigurationSection elementalSection = bossSection.getConfigurationSection("elemental");
            if (elementalSection != null) {
                ConfigurationSection damageSection = elementalSection.getConfigurationSection("damage");
                if (damageSection != null) {
                    for (String element : damageSection.getKeys(false)) {
                        stats.addElementalDamage(element, damageSection.getDouble(element, 0));
                    }
                }
                
                ConfigurationSection resistSection = elementalSection.getConfigurationSection("resistance");
                if (resistSection != null) {
                    for (String element : resistSection.getKeys(false)) {
                        stats.addElementalResistance(element, resistSection.getDouble(element, 0));
                    }
                }
            }
            
            ConfigurationSection customSection = bossSection.getConfigurationSection("custom");
            if (customSection != null) {
                for (String key : customSection.getKeys(false)) {
                    stats.setCustomAttribute(key, customSection.get(key));
                }
            }

            bossStatsMap.put(mythicMobId, stats);
        }

        plugin.getLogger().info("Loaded " + bossStatsMap.size() + " boss stats configurations");
    }

    private void initMythicMobsIntegration() {
        try {
            var mythicPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin == null || !mythicPlugin.isEnabled()) {
                plugin.getLogger().info("MythicMobs not found, boss stats integration disabled");
                return;
            }

            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkitInst = instMethod.invoke(null);
            Method getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
            mythicMobManager = getMobManagerMethod.invoke(mythicBukkitInst);

            Class<?> mobManagerClass = mythicMobManager.getClass();
            isMythicMobMethod = mobManagerClass.getMethod("isMythicMob", Entity.class);
            getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", Entity.class);

            plugin.getLogger().info("MythicMobs integration initialized for BossStatsManager");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize MythicMobs integration: " + e.getMessage());
        }
    }

    public boolean isMythicMob(Entity entity) {
        if (mythicMobManager == null || isMythicMobMethod == null) {
            return false;
        }
        try {
            reflectionCalls.incrementAndGet();
            return (boolean) isMythicMobMethod.invoke(mythicMobManager, entity);
        } catch (Exception e) {
            return false;
        }
    }

    public String getMythicMobId(Entity entity) {
        if (getMythicMobInstanceMethod == null) {
            return null;
        }
        try {
            reflectionCalls.incrementAndGet();
            Object mythicMobInstance = getMythicMobInstanceMethod.invoke(mythicMobManager, entity);
            if (mythicMobInstance != null) {
                // 尝试多种方法名以兼容不同版本的 MythicMobs
                String[] methodNames = {"getInternalName", "getMobType", "getType", "getEntityTypeName"};
                for (String methodName : methodNames) {
                    try {
                        Method method = mythicMobInstance.getClass().getMethod(methodName);
                        Object result = method.invoke(mythicMobInstance);
                        if (result instanceof String) {
                            return (String) result;
                        }
                    } catch (NoSuchMethodException ignored) {
                        // 方法不存在，尝试下一个
                    }
                }
                
                // 如果上述方法都不存在，尝试从 MythicMobType 对象获取
                try {
                    Method getMobTypeObjMethod = mythicMobInstance.getClass().getMethod("getMobType");
                    Object mobType = getMobTypeObjMethod.invoke(mythicMobInstance);
                    if (mobType != null) {
                        // 从 MythicMobType 对象获取内部名称
                        Method getInternalNameMethod = mobType.getClass().getMethod("getInternalName");
                        return (String) getInternalNameMethod.invoke(mobType);
                    }
                } catch (NoSuchMethodException e) {
                    // 方法不存在是预期行为，继续尝试下一种方法
                    plugin.getLogger().fine("Method getMobType not found, trying alternative");
                }
                
                // 最后尝试通过 toString 或其他方式
                try {
                    Method getNameMethod = mythicMobInstance.getClass().getMethod("getName");
                    return (String) getNameMethod.invoke(mythicMobInstance);
                } catch (NoSuchMethodException e) {
                    // 所有方法都失败
                    plugin.getLogger().fine("Method getName not found for MythicMob");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MythicMob ID: " + e.getMessage());
        }
        return null;
    }

    public BossStats getBossStats(String mythicMobId) {
        return bossStatsMap.get(mythicMobId);
    }

    public BossStats getBossStats(LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        
        CacheEntry cached = entityStatsCache.get(entityId);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.stats;
        }
        
        cacheMisses.incrementAndGet();
        
        boolean isMythic = isMythicMob(entity);
        DamageDebugConfig debugConfig = DamageDebugConfig.getInstance();
        debugConfig.logBossManager("实体: " + entity.getType().name() + " 是否MythicMob: " + isMythic);
        
        if (!isMythic) {
            return null;
        }

        String mythicMobId = getMythicMobId(entity);
        debugConfig.logBossManager("MythicMob ID: " + mythicMobId);
        
        if (mythicMobId == null) {
            return null;
        }

        BossStats stats = bossStatsMap.get(mythicMobId);
        debugConfig.logBossManager("找到配置: " + (stats != null ? stats.getDisplayName() : "null") +
            " 已配置的BOSS列表: " + bossStatsMap.keySet());
        
        if (stats != null) {
            if (entityStatsCache.size() >= MAX_CACHE_SIZE) {
                cleanupExpiredCache();
            }
            entityStatsCache.put(entityId, new CacheEntry(stats, mythicMobId));
        }
        
        return stats;
    }

    public boolean hasBossStats(LivingEntity entity) {
        return getBossStats(entity) != null;
    }

    public double calculateBossDamage(LivingEntity boss, double baseDamage) {
        BossStats stats = getBossStats(boss);
        if (stats == null) {
            return baseDamage;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double damage = stats.getMinAttack() + 
            random.nextDouble() * (stats.getMaxAttack() - stats.getMinAttack());
        
        damage *= stats.getDamageMultiplier();
        
        if (random.nextDouble() * 100 < stats.getCritChance()) {
            damage *= (1 + stats.getCritDamage() / 100.0);
        }

        return damage;
    }

    public double calculateBossDefense(LivingEntity boss, double incomingDamage) {
        BossStats stats = getBossStats(boss);
        if (stats == null) {
            return incomingDamage;
        }

        double damage = incomingDamage;
        
        double armorReduction = stats.getArmorPercent() / 100.0;
        damage *= (1.0 - Math.min(0.85, armorReduction));
        
        double defense = stats.getDefense();
        double defenseReduction = defense / (defense + 15000.0);
        damage *= (1.0 - Math.min(0.90, defenseReduction));
        
        damage *= (1.0 - stats.getDamageReduction() / 100.0);

        return Math.max(1.0, damage);
    }

    public void reloadConfig() {
        loadConfig();
        entityStatsCache.clear();
    }

    public void clearCache() {
        entityStatsCache.clear();
    }

    public void cleanupExpiredCache() {
        entityStatsCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public Map<String, BossStats> getAllBossStats() {
        return Collections.unmodifiableMap(bossStatsMap);
    }

    public void saveConfig() {
        try {
            bossConfig.save(bossConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save boss_stats.yml: " + e.getMessage());
        }
    }
    
    public String getCacheStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;
        return String.format("BossStats Cache: %d entries, %.1f%% hit rate (%d/%d), %d reflection calls",
            entityStatsCache.size(), hitRate, hits, total, reflectionCalls.get());
    }
}
