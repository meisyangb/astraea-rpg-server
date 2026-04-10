package cn.guangdian.armorstats;

import cn.guangdian.armorstats.adapter.ArmorStatsServiceAdapter;
import cn.guangdian.armorstats.boss.BossAnnouncer;
import cn.guangdian.armorstats.debug.DebugLogManager;
import cn.guangdian.armorstats.cache.EquipmentCacheManager;
import cn.guangdian.armorstats.config.ConfigManager;
import cn.guangdian.armorstats.lifecycle.ArmorStatsDataHandler;
import cn.guangdian.armorstats.manager.BossBarOptimizer;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.manager.DamageManager;
import cn.guangdian.armorstats.manager.HealthManager;
import cn.guangdian.armorstats.manager.CombatLogManager;
import cn.guangdian.armorstats.manager.BossBarManager;
import cn.guangdian.armorstats.skill.SkillManager;
import cn.guangdian.armorstats.listener.EventListeners;
import cn.guangdian.armorstats.listener.GuiListener;
import cn.guangdian.armorstats.listener.GemInlayCacheListener;
import cn.guangdian.armorstats.command.ArmorStatsCommand;
import cn.guangdian.armorstats.command.GemCommand;
import cn.guangdian.armorstats.placeholder.ArmorStatsPlaceholderExpansion;
import cn.guangdian.armorstats.storage.AsyncExecutorService;
import cn.guangdian.armorstats.storage.PlayerDataStorage;
import cn.guangdian.armorstats.task.RegenTask;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class GuangDianArmorStats extends JavaPlugin {

    private static GuangDianArmorStats instance;
    private ConfigManager configManager;
    private StatsManager statsManager;
    private DamageManager damageManager;
    private HealthManager healthManager;
    private SkillManager skillManager;
    private CombatLogManager combatLogManager;
    private BossBarManager bossBarManager;
    private RegenTask regenTask;
    private ArmorStatsPlaceholderExpansion placeholderExpansion;
    private BossAnnouncer bossAnnouncer;
    
    // 优化组件
    private AsyncExecutorService asyncExecutor;
    private cn.guangdian.rpgcore.api.AsyncExecutor rpgCoreAsyncExecutor;
    private EquipmentCacheManager equipmentCacheManager;
    private BossBarOptimizer bossBarOptimizer;
    
    // RPGCore 服务适配器
    private ArmorStatsServiceAdapter serviceAdapter;
    private ArmorStatsDataHandler dataHandler;

    @Override
    public void onEnable() {
        instance = this;

        // 加载所有配置文件
        configManager = new ConfigManager(this);
        configManager.loadAll();
        
        // 初始化调试日志管理器
        DebugLogManager.initialize(this);
        
        // 初始化优化组件
        initOptimizationComponents();

        statsManager = new StatsManager(this);
        healthManager = new HealthManager(statsManager);
        skillManager = new SkillManager(statsManager);
        damageManager = new DamageManager(statsManager, skillManager);
        combatLogManager = new CombatLogManager(this);
        bossBarManager = new BossBarManager(this, statsManager);
        regenTask = new RegenTask(this, statsManager, bossBarManager);
        damageManager.setCombatLogManager(combatLogManager);
        skillManager.setCombatLogManager(combatLogManager);
        
        // 集成 RPGCore 框架组件到管理器
        if (rpgCoreAsyncExecutor != null) {
            statsManager.setRPGCoreAsyncExecutor(rpgCoreAsyncExecutor);
        } else if (asyncExecutor != null) {
            statsManager.setAsyncExecutor(asyncExecutor);
        }
        if (equipmentCacheManager != null) {
            statsManager.setEquipmentCacheManager(equipmentCacheManager);
        }
        if (bossBarOptimizer != null) {
            bossBarManager.setBossBarOptimizer(bossBarOptimizer);
        }

        damageManager.initMythicMobs();
        
        // BOSS 公告系统 - 暂时注释，使用 MythicMobs 自带属性
        // if (damageManager.getBossStatsManager() != null) {
        //     bossAnnouncer = new BossAnnouncer(this, damageManager.getBossStatsManager());
        //     getServer().getPluginManager().registerEvents(bossAnnouncer, this);
        //     getLogger().info("BOSS公告系统已启用");
        // }

        getServer().getPluginManager().registerEvents(new EventListeners(this, statsManager, healthManager, skillManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        
        // 注册玩家生命周期处理器
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            dataHandler = new ArmorStatsDataHandler(this, statsManager, healthManager, bossBarManager);
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，使用传统事件监听");
        }

        // 注册宝石镶嵌缓存联动监听器
        if (equipmentCacheManager != null) {
            getServer().getPluginManager().registerEvents(new GemInlayCacheListener(this, equipmentCacheManager), this);
            getLogger().info("宝石镶嵌缓存联动已启用");
        }

        getCommand("armorstats").setExecutor(new ArmorStatsCommand(statsManager, skillManager, this));
        getCommand("gem").setExecutor(new GemCommand());

        bossBarManager.startUpdateTask();
        regenTask.start();

        // PlaceholderAPI 功能
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new ArmorStatsPlaceholderExpansion(this);
            placeholderExpansion.register();
            getLogger().info("Registered PlaceholderAPI expansion: gdrpg");
        }

        // 注册 RPGCore 服务适配器
        serviceAdapter = new ArmorStatsServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        getLogger().info("GuangDianArmorStats Plugin Enabled!");
        getLogger().info("RPG Combat System Architecture Loaded Successfully!");
        
        // 输出优化组件状态
        logOptimizationStatus();
    }
    
    /**
     * 初始化优化组件
     * 优先使用 RPGCore 统一服务，仅在不可用时使用本地实现
     */
    private void initOptimizationComponents() {
        ConfigurationSection optConfig = getConfig().getConfigurationSection("optimization");
        
        if (optConfig == null) {
            getLogger().warning("优化配置未找到，使用默认配置");
            optConfig = getConfig().createSection("optimization");
        }
        
        // 初始化异步执行器 - 优先使用 RPGCore 统一服务
        ConfigurationSection asyncConfig = optConfig.getConfigurationSection("async_save");
        boolean asyncEnabled = asyncConfig != null && asyncConfig.getBoolean("enabled", true);
        
        if (asyncEnabled) {
            boolean rpgCoreAvailable = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
            if (rpgCoreAvailable) {
                try {
                    rpgCoreAsyncExecutor = RPGCore.getInstance().getAsyncExecutor();
                    getLogger().info("使用 RPGCore 统一 AsyncExecutor（推荐）");
                } catch (Exception e) {
                    getLogger().warning("无法获取 RPGCore AsyncExecutor: " + e.getMessage());
                }
            }
            
            // 仅在 RPGCore 不可用时创建本地实例
            if (rpgCoreAsyncExecutor == null) {
                int threadPoolSize = asyncConfig.getInt("thread_pool_size", 2);
                asyncExecutor = new AsyncExecutorService(this, threadPoolSize);
                getLogger().info("使用本地 AsyncExecutor (线程池大小: " + threadPoolSize + ")");
            }
        }
        
        // 初始化装备缓存管理器
        ConfigurationSection cacheConfig = optConfig.getConfigurationSection("equipment_cache");
        if (cacheConfig != null && cacheConfig.getBoolean("enabled", true)) {
            int maxSize = cacheConfig.getInt("max_size", 1000);
            equipmentCacheManager = new EquipmentCacheManager(this, maxSize);
            getLogger().info("装备缓存已启用 (最大缓存: " + maxSize + ")");
        }
        
        // 初始化BossBar优化器
        ConfigurationSection bossBarOptConfig = optConfig.getConfigurationSection("bossbar_optimizer");
        if (bossBarOptConfig != null && bossBarOptConfig.getBoolean("enabled", true)) {
            double minHealthChange = bossBarOptConfig.getDouble("min_health_change", 0.5);
            long combatDuration = bossBarOptConfig.getLong("combat_duration", 5000);
            long combatUpdateInterval = bossBarOptConfig.getLong("combat_update_interval", 100);
            long normalUpdateInterval = bossBarOptConfig.getLong("normal_update_interval", 1000);
            
            bossBarOptimizer = new BossBarOptimizer(
                minHealthChange,
                combatDuration,
                combatUpdateInterval,
                normalUpdateInterval
            );
            getLogger().info("BossBar优化已启用");
        }
    }
    
    /**
     * 输出优化组件状态
     */
    private void logOptimizationStatus() {
        getLogger().info("========== 优化组件状态 ==========");
        getLogger().info("异步执行器: " + (rpgCoreAsyncExecutor != null ? "RPGCore统一" : (asyncExecutor != null ? "本地" : "未启用")));
        getLogger().info("玩家锁管理器: " + (RPGCore.getInstance() != null ? "RPGCore统一" : "本地"));
        getLogger().info("装备缓存: " + (equipmentCacheManager != null ? "已启用" : "未启用"));
        getLogger().info("BossBar优化: " + (bossBarOptimizer != null ? "已启用" : "未启用"));
        getLogger().info("==================================");
    }

    @Override
    public void onDisable() {
        // 注销玩家生命周期处理器
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        // 等待所有异步保存完成
        if (rpgCoreAsyncExecutor != null) {
            getLogger().info("等待 RPGCore 异步任务完成...");
            rpgCoreAsyncExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } else if (asyncExecutor != null) {
            getLogger().info("等待所有异步保存完成...");
            asyncExecutor.awaitAllSaves(30, TimeUnit.SECONDS);
            asyncExecutor.shutdown();
        }
        
        if (bossBarManager != null) {
            bossBarManager.removeAllBossBars();
        }

        if (regenTask != null) {
            regenTask.stop();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion = null;
        }
        
        if (statsManager != null) {
            getServer().getOnlinePlayers().forEach(player -> {
                statsManager.clearPlayerAttributes(player);
                statsManager.removePlayer(player.getUniqueId());
            });
        }
        
        // 输出缓存统计
        if (equipmentCacheManager != null) {
            getLogger().info("装备缓存统计: " + equipmentCacheManager.getStats());
        }
        
        // 关闭调试日志
        DebugLogManager.shutdown();
        
        getLogger().info("GuangDianArmorStats Plugin Disabled!");
    }

    public static GuangDianArmorStats getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public DamageManager getDamageManager() {
        return damageManager;
    }

    public HealthManager getHealthManager() {
        return healthManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public CombatLogManager getCombatLogManager() {
        return combatLogManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public RegenTask getRegenTask() {
        return regenTask;
    }

    public BossAnnouncer getBossAnnouncer() {
        return bossAnnouncer;
    }

    public void reloadAllConfigs() {
        configManager.reloadAll();
        statsManager.loadConfig();
        damageManager.reloadConfig();
        skillManager.loadSkills();
        bossBarManager.reloadConfig();
        // if (bossAnnouncer != null) {
        //     bossAnnouncer.reloadConfig();
        // }
        if (regenTask != null) {
            regenTask.loadConfig();
            regenTask.start();
        }

        getServer().getOnlinePlayers().forEach(player -> {
            statsManager.refreshPlayerStats(player);
            healthManager.syncPlayerHealth(player);
            if (bossBarManager != null && bossBarManager.isEnabled()) {
                bossBarManager.updateBossBar(player);
            }
        });
    }
}