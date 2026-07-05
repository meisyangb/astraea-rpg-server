package cn.guangdian.armorstats.storage;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 玩家数据持久化存储
 * 只保存：血量、防具属性缓存（武器属性不保存，登录时重新解析）
 * 
 * 优化特性:
 * - 异步保存，避免阻塞主线程
 * - 保存失败时的错误处理和数据保留
 * - 服务器关闭时等待所有保存完成
 * 
 * 重构: 使用 RPGCore AsyncExecutor 统一异步执行
 */
public class PlayerDataStorage {

    private final GuangDianArmorStats plugin;
    private final File dataFolder;
    private final AsyncExecutor asyncExecutor;
    private final AsyncExecutorService localAsyncExecutor; // 降级用

    /**
     * 构造函数 - 使用 RPGCore AsyncExecutor
     */
    public PlayerDataStorage(GuangDianArmorStats plugin, AsyncExecutor asyncExecutor) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.asyncExecutor = asyncExecutor;
        this.localAsyncExecutor = null;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        plugin.getLogger().info("PlayerDataStorage 使用 RPGCore AsyncExecutor");
    }
    
    /**
     * 构造函数 - 使用本地 AsyncExecutorService（降级）
     */
    public PlayerDataStorage(GuangDianArmorStats plugin, AsyncExecutorService localExecutor) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.asyncExecutor = null;
        this.localAsyncExecutor = localExecutor;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (localExecutor != null) {
            plugin.getLogger().info("PlayerDataStorage 使用本地 AsyncExecutorService");
        } else {
            plugin.getLogger().info("PlayerDataStorage 使用同步保存");
        }
    }
    
    /**
     * 向后兼容的构造函数（不使用异步保存）
     */
    @Deprecated
    public PlayerDataStorage(GuangDianArmorStats plugin) {
        this(plugin, (AsyncExecutorService) null);
    }

    /**
     * 异步保存玩家数据
     * 只保存防具属性，武器属性不保存
     * 
     * @return CompletableFuture，可用于等待保存完成或处理错误
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID uuid, double health, double maxHealth,
                                                        PlayerStats armorStats,
                                                        List<String> armorSkills,
                                                        List<String> weaponSkills) {
        // 优先使用 RPGCore AsyncExecutor
        if (asyncExecutor != null) {
            return asyncExecutor.submitPlayerSave(uuid, () -> {
                savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills, weaponSkills);
            }).exceptionally(ex -> {
                plugin.getLogger().severe("异步保存玩家数据失败: " + uuid + " - " + ex.getMessage());
                return null;
            });
        }
        
        // 降级使用本地 AsyncExecutorService
        if (localAsyncExecutor != null) {
            return localAsyncExecutor.savePlayerDataAsync(uuid, () -> {
                savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills, weaponSkills);
            }).exceptionally(ex -> {
                plugin.getLogger().severe("异步保存玩家数据失败: " + uuid + " - " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        }
        
        // 同步保存
        savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills, weaponSkills);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 保存玩家数据（同步版本）
     * 只保存防具属性，武器属性不保存
     */
    public void savePlayerData(UUID uuid, double health, double maxHealth,
                                PlayerStats armorStats,
                                List<String> armorSkills,
                                List<String> weaponSkills) {
        // 如果有异步执行器，使用异步保存
        if (asyncExecutor != null || localAsyncExecutor != null) {
            savePlayerDataAsync(uuid, health, maxHealth, armorStats, armorSkills, weaponSkills);
        } else {
            savePlayerDataSync(uuid, health, maxHealth, armorStats, armorSkills, weaponSkills);
        }
    }
    
    /**
     * 同步保存玩家数据（内部方法）
     */
    private void savePlayerDataSync(UUID uuid, double health, double maxHealth,
                                     PlayerStats armorStats,
                                     List<String> armorSkills,
                                     List<String> weaponSkills) {
        File file = getPlayerFile(uuid);
        YamlConfiguration config = new YamlConfiguration();

        // 保存血量
        config.set("health", health);
        config.set("maxHealth", maxHealth);

        // 只保存防具属性（武器属性不保存，登录时重新解析）
        if (armorStats != null) {
            config.set("armorStats", armorStats.toMap());
        }

        // 保存防具技能
        config.set("armorSkills", armorSkills != null ? armorSkills : new ArrayList<>());

        // 保存时间戳
        config.set("lastSave", System.currentTimeMillis());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据: " + uuid + " - " + e.getMessage());
            throw new RuntimeException("保存失败", e);
        }
    }
    
    /**
     * 等待所有保存完成（服务器关闭时调用）
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public void awaitAllSaves(long timeout, TimeUnit unit) {
        if (asyncExecutor != null) {
            asyncExecutor.awaitTermination(timeout, unit);
        } else if (localAsyncExecutor != null) {
            localAsyncExecutor.awaitAllSaves(timeout, unit);
        }
    }

    /**
     * 加载玩家数据
     * @return PlayerData 对象，如果不存在返回null
     */
    public PlayerData loadPlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData();

        // 读取血量
        data.health = config.getDouble("health", -1);
        data.maxHealth = config.getDouble("maxHealth", 20);

        // 读取防具属性
        ConfigurationSection armorSection = config.getConfigurationSection("armorStats");
        if (armorSection != null) {
            data.armorStats = parseStatsFromSection(armorSection);
        }

        // 读取防具技能
        data.armorSkills = config.getStringList("armorSkills");

        data.lastSave = config.getLong("lastSave", 0);

        return data;
    }

    /**
     * 删除玩家数据文件
     */
    public void deletePlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 检查玩家数据是否存在
     */
    public boolean hasPlayerData(UUID uuid) {
        return getPlayerFile(uuid).exists();
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private PlayerStats parseStatsFromSection(ConfigurationSection section) {
        PlayerStats stats = new PlayerStats();
        stats.setMaxHealth(section.getDouble("maxHealth", 20));
        stats.setMinAttack(section.getDouble("minAttack", 1));
        stats.setMaxAttack(section.getDouble("maxAttack", 1));
        stats.setDefenseMin(section.getDouble("defenseMin", 0));
        stats.setDefenseMax(section.getDouble("defenseMax", 0));
        stats.setCritChancePercent(section.getDouble("critChancePercent", 0));
        stats.setCritDamagePercent(section.getDouble("critDamagePercent", 0));
        stats.setLifestealPercent(section.getDouble("lifestealPercent", 0));
        stats.setHealthRegen(section.getDouble("healthRegen", 0));
        stats.setDodgePercent(section.getDouble("dodgePercent", 0));
        stats.setDamageReflectPercent(section.getDouble("damageReflectPercent", 0));
        stats.setReflectPercent(section.getDouble("reflectPercent", 0));
        stats.setLifestealResistPercent(section.getDouble("lifestealResistPercent", 0));
        stats.setCritResistPercent(section.getDouble("critResistPercent", 0));
        stats.setCritDamageResistPercent(section.getDouble("critDamageResistPercent", 0));
        stats.setParryPercent(section.getDouble("parryPercent", 0));
        stats.setPvpMinAttack(section.getDouble("pvpMinAttack", 0));
        stats.setPvpMaxAttack(section.getDouble("pvpMaxAttack", 0));
        stats.setPvpDefenseMin(section.getDouble("pvpDefenseMin", 0));
        stats.setPvpDefenseMax(section.getDouble("pvpDefenseMax", 0));
        stats.setMoveSpeedPercent(section.getDouble("moveSpeedPercent", 0));
        stats.setPoisonPercent(section.getDouble("poisonPercent", 0));
        stats.setFreezePercent(section.getDouble("freezePercent", 0));
        stats.setBlindPercent(section.getDouble("blindPercent", 0));
        stats.setExpBonusPercent(section.getDouble("expBonusPercent", 0));
        stats.setLifestealMultiplier(section.getDouble("lifestealMultiplier", 0));
        return stats;
    }

    /**
     * 玩家数据容器
     * 只包含防具属性（武器属性登录时重新解析）
     */
    public static class PlayerData {
        public double health = -1;
        public double maxHealth = 20;
        public PlayerStats armorStats;
        public List<String> armorSkills = new ArrayList<>();
        public long lastSave = 0;
    }
}