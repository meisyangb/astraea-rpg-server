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
import cn.guangdian.armorstats.listener.SkillTriggerListener;
import cn.guangdian.armorstats.command.ArmorStatsCommand;
import cn.guangdian.armorstats.placeholder.ArmorStatsPlaceholderExpansion;
import cn.guangdian.armorstats.storage.AsyncExecutorService;
import cn.guangdian.armorstats.storage.PlayerDataStorage;
import cn.guangdian.armorstats.task.RegenTask;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.TimeUnit;

public final class GuangDianArmorStats extends AbstractRPGPlugin {

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
    
    // 优化组件 - 优先使用 RPGCore 服务，本地实现作为降级方案
    private AsyncExecutorService asyncExecutor;  // 本地异步执行器（降级用）
    private cn.guangdian.rpgcore.api.AsyncExecutor rpgCoreAsyncExecutor;  // RPGCore 统一异步执行器
    private EquipmentCacheManager equipmentCacheManager;  // 本地装备缓存
    private BossBarOptimizer bossBarOptimizer;
    
    // RPGCore 核心服务引用 - 优先使用 RPGCore，本地实现作为降级
    private RPGCore rpgCore;
    private SyncScheduler rpgCoreScheduler;
    private ExternalServiceIntegration externalServices;
    private MiniMessageService miniMessage;
    private SoundService soundService;
    private CacheProvider cacheProvider;
    
    // RPGCore 服务适配器
    private ArmorStatsServiceAdapter serviceAdapter;
    private ArmorStatsDataHandler dataHandler;
    
    // 增量属性管理器（核心）
    private cn.guangdian.armorstats.manager.IncrementalStatsManager incrementalStatsManager;
    
    // 玩家数据存储
    private PlayerDataStorage playerDataStorage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 核心服务（优先使用 RPGCore，本地实现作为降级）
        initRPGCoreServices();

        // 加载所有配置文件
        configManager = new ConfigManager(this);
        configManager.loadAll();
        
        // 初始化调试日志管理器
        DebugLogManager.initialize(this);
        
        // 初始化优化组件
        initOptimizationComponents();

        // 初始化增量属性管理器（核心系统）
        incrementalStatsManager = new cn.guangdian.armorstats.manager.IncrementalStatsManager(this);
        
        // 初始化管理器
        statsManager = new StatsManager(this);
        healthManager = new HealthManager(this, statsManager);
        skillManager = new SkillManager(statsManager);
        damageManager = new DamageManager(statsManager, skillManager);
        combatLogManager = new CombatLogManager(this);
        bossBarManager = new BossBarManager(this, statsManager);
        regenTask = new RegenTask(this, statsManager, bossBarManager);
        damageManager.setCombatLogManager(combatLogManager);
        skillManager.setCombatLogManager(combatLogManager);
        
        // 集成 RPGCore 框架组件到管理器
        if (bossBarOptimizer != null) {
            bossBarManager.setBossBarOptimizer(bossBarOptimizer);
        }

        damageManager.initMythicMobs();
        
        // 注册增量属性更新监听器
        getServer().getPluginManager().registerEvents(
            new cn.guangdian.armorstats.listener.IncrementalStatsListener(this, incrementalStatsManager),
            this
        );
        getLogger().info("增量属性更新监听器已启用");
        
        // 启动速度监测任务
        incrementalStatsManager.getApplier().startSpeedMonitor();
        
        // 注册战斗事件监听器（伤害、回血、死亡、重生等）
        getServer().getPluginManager().registerEvents(
            new cn.guangdian.armorstats.listener.CombatListener(this, incrementalStatsManager),
            this
        );
        getLogger().info("战斗事件监听器已启用");
        
        // 注册技能触发监听器（直接右键触发技能）
        getServer().getPluginManager().registerEvents(new SkillTriggerListener(this, skillManager), this);
        getLogger().info("技能触发监听器已启用");

        getCommand("armorstats").setExecutor(new ArmorStatsCommand(statsManager, skillManager, this));

        bossBarManager.startUpdateTask();
        regenTask.start();

        // PlaceholderAPI 功能
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new ArmorStatsPlaceholderExpansion(this);
            placeholderExpansion.register();
            getLogger().info("Registered PlaceholderAPI expansion: gdrpg");
        }

        getLogger().info("GuangDianArmorStats Plugin Enabled!");
        getLogger().info("PDC 属性系统已加载!");
        
        // 输出优化组件状态
        logOptimizationStatus();
    }
    
    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    rpgCoreScheduler = rpgCore.getScheduler();
                    externalServices = rpgCore.getExternalServices();
                    miniMessage = rpgCore.getMiniMessageService();
                    soundService = rpgCore.getSoundService();
                    cacheProvider = rpgCore.getCacheProvider();
                    rpgCoreAsyncExecutor = rpgCore.getAsyncExecutor();
                    getLogger().info("已连接到 RPGCore 核心服务");
                }
            } catch (Exception e) {
                getLogger().warning("连接 RPGCore 服务失败: " + e.getMessage());
            }
        }
        
        // 如果 RPGCore 服务不可用，初始化本地降级服务
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            getLogger().info("使用本地 MiniMessageService（降级）");
        }
        if (soundService == null) {
            soundService = SoundService.getInstance();
            getLogger().info("使用本地 SoundService（降级）");
        }
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
        
        // 初始化玩家数据存储
        if (rpgCoreAsyncExecutor != null) {
            playerDataStorage = new PlayerDataStorage(this, rpgCoreAsyncExecutor);
        } else if (asyncExecutor != null) {
            playerDataStorage = new PlayerDataStorage(this, asyncExecutor);
        } else {
            playerDataStorage = new PlayerDataStorage(this);
        }
        getLogger().info("玩家数据存储已初始化");
        
        // 初始化异步执行器 - 优先使用 RPGCore 统一服务
        ConfigurationSection asyncConfig = optConfig.getConfigurationSection("async_save");
        boolean asyncEnabled = asyncConfig != null && asyncConfig.getBoolean("enabled", true);
        
        if (asyncEnabled) {
            // RPGCore AsyncExecutor 已在 initRPGCoreServices() 中初始化
            if (rpgCoreAsyncExecutor != null) {
                getLogger().info("使用 RPGCore 统一 AsyncExecutor（推荐）");
            } else {
                // 仅在 RPGCore 不可用时创建本地实例
                int threadPoolSize = asyncConfig.getInt("thread_pool_size", 2);
                asyncExecutor = new AsyncExecutorService(this, threadPoolSize);
                getLogger().info("使用本地 AsyncExecutor (线程池大小: " + threadPoolSize + ")");
            }
        }
        
        // 初始化装备缓存管理器 - 优先使用 RPGCore CacheProvider
        ConfigurationSection cacheConfig = optConfig.getConfigurationSection("equipment_cache");
        if (cacheConfig != null && cacheConfig.getBoolean("enabled", true)) {
            int maxSize = cacheConfig.getInt("max_size", 1000);
            // 如果 RPGCore CacheProvider 可用，使用统一缓存；否则使用本地实现
            if (cacheProvider != null) {
                getLogger().info("使用 RPGCore 统一 CacheProvider（推荐）");
            }
            // 本地装备缓存管理器仍然需要，用于装备解析逻辑
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
        getLogger().info("========== RPGCore 服务集成状态 ==========");
        getLogger().info("RPGCore 核心: " + (rpgCore != null ? "已连接" : "未连接"));
        getLogger().info("MiniMessage服务: " + (rpgCore != null && rpgCore.getMiniMessageService() != null ? "RPGCore统一" : "本地降级"));
        getLogger().info("Sound服务: " + (rpgCore != null && rpgCore.getSoundService() != null ? "RPGCore统一" : "本地降级"));
        getLogger().info("CacheProvider: " + (cacheProvider != null ? "RPGCore统一" : "本地降级"));
        getLogger().info("异步执行器: " + (rpgCoreAsyncExecutor != null ? "RPGCore统一" : (asyncExecutor != null ? "本地" : "未启用")));
        getLogger().info("SyncScheduler: " + (rpgCoreScheduler != null ? "RPGCore统一" : "本地"));
        getLogger().info("ExternalServices: " + (externalServices != null ? "RPGCore统一" : "未启用"));
        getLogger().info("装备缓存: " + (equipmentCacheManager != null ? "已启用" : "未启用"));
        getLogger().info("BossBar优化: " + (bossBarOptimizer != null ? "已启用" : "未启用"));
        getLogger().info("==========================================");
    }
    
    @Override
    protected void onPluginDisable() {
        // 停止速度监测
        if (incrementalStatsManager != null) {
            incrementalStatsManager.getApplier().stopSpeedMonitor();
        }
        
        // 注销玩家生命周期处理器
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
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
            try {
                // 使用反射调用 unregister()，兼容不同版本 PlaceholderAPI
                java.lang.reflect.Method unregisterMethod = placeholderExpansion.getClass().getMethod("unregister");
                unregisterMethod.invoke(placeholderExpansion);
            } catch (Exception e) {
                // 旧版 PlaceholderAPI 没有 unregister 方法，忽略
            }
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
    
    @Override
    protected String getPluginName() {
        return "GuangDianArmorStats";
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
    
    public PlayerDataStorage getPlayerDataStorage() {
        return playerDataStorage;
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

    // ==================== RPGCore 服务访问方法 ====================

    /**
     * 获取 RPGCore 实例
     * @return RPGCore 实例，如果未启用则返回 null
     */
    public RPGCore getRPGCore() {
        return rpgCore;
    }

    /**
     * 检查是否已连接到 RPGCore
     * @return true 如果 RPGCore 可用
     */
    public boolean isRPGCoreEnabled() {
        return rpgCore != null;
    }

    /**
     * 获取 MiniMessage 服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级
     * @return MiniMessageService 实例（不会返回 null）
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    /**
     * 获取音效服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级
     * @return SoundService 实例（不会返回 null）
     */
    public SoundService getSoundService() {
        return soundService;
    }

    /**
     * 获取缓存提供者
     * 优先使用 RPGCore 统一服务
     * @return CacheProvider 实例，如果 RPGCore 不可用则返回 null
     */
    public CacheProvider getCacheProvider() {
        return cacheProvider;
    }

    /**
     * 获取 RPGCore 调度器
     * @return SyncScheduler 实例，如果 RPGCore 不可用则返回 null
     */
    public SyncScheduler getRPGCoreScheduler() {
        return rpgCoreScheduler;
    }

    /**
     * 获取外部服务集成
     * @return ExternalServiceIntegration 实例，如果 RPGCore 不可用则返回 null
     */
    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }

    /**
     * 获取异步执行器
     * 优先使用 RPGCore 统一服务，本地实现作为降级
     * @return AsyncExecutor 实例，如果都不可用则返回 null
     */
    public cn.guangdian.rpgcore.api.AsyncExecutor getAsyncExecutor() {
        return rpgCoreAsyncExecutor;
    }

    /**
     * 获取本地异步执行器（降级用）
     * @return AsyncExecutorService 实例，如果 RPGCore 可用则返回 null
     */
    public AsyncExecutorService getLocalAsyncExecutor() {
        return asyncExecutor;
    }

    public void reloadAllConfigs() {
        configManager.reloadAll();
        statsManager.reloadConfig();
        damageManager.reloadConfig();
        skillManager.loadSkills();
        combatLogManager.reload();
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
    
    /**
     * 获取增量属性管理器
     * @return IncrementalStatsManager 实例
     */
    public cn.guangdian.armorstats.manager.IncrementalStatsManager getIncrementalStatsManager() {
        return incrementalStatsManager;
    }
}