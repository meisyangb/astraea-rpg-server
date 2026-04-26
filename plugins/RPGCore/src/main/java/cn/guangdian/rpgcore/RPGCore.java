package cn.guangdian.rpgcore;

import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.AuditLog;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.ConfigManager;
import cn.guangdian.rpgcore.api.ConfigMigrator;
import cn.guangdian.rpgcore.api.CronScheduler;
import cn.guangdian.rpgcore.api.DataExporter;
import cn.guangdian.rpgcore.api.ExceptionHandler;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.api.HttpClient;
import cn.guangdian.rpgcore.api.PluginLifecycleManager;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.audit.AuditLogImpl;
import cn.guangdian.rpgcore.async.AsyncExecutorImpl;
import cn.guangdian.rpgcore.cache.TTLCacheManager;
import cn.guangdian.rpgcore.cache.TTLCacheManager.Mode;
import cn.guangdian.rpgcore.config.ConfigMigratorImpl;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.config.ConfigManagerImpl;
import cn.guangdian.rpgcore.cron.CronSchedulerImpl;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.data.YamlDataStore;
import cn.guangdian.rpgcore.display.AdventureBossBarService;
import cn.guangdian.rpgcore.display.DisplayService;
import cn.guangdian.rpgcore.display.DisplayServiceImpl;
import cn.guangdian.rpgcore.display.TextDisplayServiceImpl;
import cn.guangdian.rpgcore.gui.GUIManager;
import cn.guangdian.rpgcore.config.ConfigurateManager;
import cn.guangdian.rpgcore.exception.ExceptionHandlerImpl;
import cn.guangdian.rpgcore.util.JacksonUtils;
import cn.guangdian.rpgcore.export.DataExporterImpl;
import cn.guangdian.rpgcore.http.HttpClientImpl;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegrationImpl;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import cn.guangdian.rpgcore.lifecycle.PlayerLifecycleManager;
import cn.guangdian.rpgcore.logging.AsyncLogger;
import cn.guangdian.rpgcore.manager.ItemAttributeManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.message.MessageServiceImpl;
import cn.guangdian.rpgcore.module.RPGModule;
import cn.guangdian.rpgcore.monitor.PerformanceMonitor;
import cn.guangdian.rpgcore.monitor.PerformanceReport;
import cn.guangdian.rpgcore.ratelimit.RateLimiterImpl;
import cn.guangdian.rpgcore.scheduler.UnifiedSchedulerImpl;
import cn.guangdian.rpgcore.service.ServiceScanner;
import cn.guangdian.rpgcore.service.SimpleServiceRegistry;
import cn.guangdian.rpgcore.entity.EntityService;
import cn.guangdian.rpgcore.server.ServerService;
import cn.guangdian.rpgcore.service.api.MessageService;
import cn.guangdian.rpgcore.service.api.TextDisplayService;
import cn.guangdian.rpgcore.service.api.MenuService;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgcore.storage.UnifiedDataManager;
import cn.guangdian.rpgcore.util.CooldownManager;
import cn.guangdian.rpgcore.inject.GuiceSupport;
import cn.guangdian.rpgcore.inject.RPGCoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RPGCore 核心插件入口类
 * 
 * <p>RPGCore 是光点RPG服务器的核心框架，提供统一的服务管理、事件总线、
 * 缓存管理、异步执行、数据库连接池等功能。</p>
 * 
 * <p>占位符功能由各业务插件通过 PlaceholderAPI 实现，RPGCore 不提供占位符服务。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RPGCore extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static volatile RPGCore instance;

    private volatile boolean isInitializing = false;
    private volatile boolean fullyInitialized = false;

    private ServiceRegistry serviceRegistry;
    private CacheProvider cacheProvider;
    private AsyncExecutor asyncExecutor;
    private PlayerLockManager lockManager;
    private PerformanceMonitor performanceMonitor;
    private ServiceScanner serviceScanner;
    private ExternalServiceIntegration externalServices;
    private SyncScheduler scheduler;
    private PlayerLifecycleManager lifecycleManager;
    private DisplayService displayService;
    private UnifiedDataManager dataManager;
    private ConfigManager configManager;
    private ExceptionHandler exceptionHandler;
    private HttpClient httpClient;
    private HttpClient.RateLimiter rateLimiter;
    private CronScheduler cronScheduler;
    private ConfigMigrator configMigrator;
    private AuditLog auditLog;
    private DataExporter dataExporter;
    private MessageService messageService;
    private TextDisplayService textDisplayService;
    private GameLogger gameLogger;
    private MiniMessageService miniMessageService;
    private ItemAttributeManager itemAttributeManager;

    // 新增：新引入的库管理器
    private ConfigurateManager configurateManager;

    // Guice 依赖注入
    private Injector guiceInjector;

    private int cacheMaxSize;
    private Duration cacheDefaultTTL;
    private Mode cacheMode;
    private boolean cacheRecordStats;
    private boolean cacheWeakKeys;
    private boolean cacheWeakValues;
    private boolean cacheSoftValues;
    private Duration cacheRefreshInterval;

    private long lockTimeoutMs;
    private boolean databaseEnabled;

    private int asyncThreadPoolSize;
    private int asyncQueueCapacity;
    private long asyncKeepAliveSeconds;
    private boolean asyncAllowCoreThreadTimeout;
    private String asyncThreadNamePrefix;

    // 监控配置
    private boolean monitorEnabled;
    private boolean monitorMemory;
    private boolean monitorTps;
    private boolean monitorLogSlowOps;
    private long monitorMemoryThresholdMb;
    private long monitorSlowOpThresholdMs;
    private Duration monitorReportInterval;

    private final Map<String, RPGModule> modules = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        isInitializing = true;

        try {
            loadConfiguration();

            initCoreComponents();

            initDatabase();

            registerCommands();

            registerListeners();

            // 核心组件全部初始化完成后，再暴露实例引用
            // 避免其他线程获取到未完全初始化的实例
            instance = this;
            isInitializing = false;
            fullyInitialized = true;

            // 延迟刷新外部服务状态（等待所有插件加载完成）
            scheduleExternalServicesRefresh();

            logStartupInfo();

            getLogger().info("RPGCore v" + getDescription().getVersion() + " enabled!");
        } catch (Exception e) {
            getLogger().severe("RPGCore 初始化失败: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "初始化异常详情", e);
            instance = null;
            isInitializing = false;
            throw e;
        }
    }

    /**
     * 检查 RPGCore 是否正在初始化中
     * 
     * @return 如果正在初始化返回 true
     */
    public boolean isInitializing() {
        return isInitializing;
    }

    /**
     * 延迟刷新外部服务状态
     * 解决插件加载顺序导致的检测不准确问题
     */
    private void scheduleExternalServicesRefresh() {
        // 延迟 5 秒后刷新，确保所有插件已加载
        scheduler.runSyncLater(() -> {
            if (externalServices instanceof ExternalServiceIntegrationImpl esi) {
                esi.refreshPlaceholderAPI();
                getLogger().info("[ExternalService] 已刷新外部服务状态: " + esi.getExternalServiceStatus());
            }
        }, 5 * 20L); // 5 秒 = 100 ticks
    }

    @Override
    public void onDisable() {
        // 1. 停止接受新任务
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            asyncExecutor.awaitTermination(30, TimeUnit.SECONDS);
        }

        // 2. 保存所有数据（统一数据保存管理）
        saveAllPlayerData();

        // 3. 清理服务注册表
        if (serviceRegistry instanceof SimpleServiceRegistry ssr) {
            ssr.clear();
        }
        
        // 4. 清理缓存
        if (cacheProvider != null) {
            cacheProvider.clear();
        }
        
        // 5. 清理离线玩家缓存
        cn.guangdian.rpgcore.util.OfflinePlayerCache.clearCache();
        
        // 6. 释放锁
        if (lockManager != null) {
            lockManager.releaseAllLocks();
        }
        
        // 7. 关闭外部服务集成
        if (externalServices instanceof ExternalServiceIntegrationImpl esi) {
            esi.shutdown();
        }
        
        // 8. 关闭调度器
        if (scheduler instanceof UnifiedSchedulerImpl usi) {
            usi.shutdown();
        }
        
        // 9. 关闭 HTTP 客户端
        if (httpClient instanceof HttpClientImpl hci) {
            hci.shutdown();
        }
        
        // 10. 关闭文本显示服务
        if (textDisplayService instanceof TextDisplayServiceImpl tdsi) {
            tdsi.shutdown();
        }
        
        // 11. 关闭 GUI 管理器
        cn.guangdian.rpgcore.gui.GUIManager guiManager = cn.guangdian.rpgcore.gui.GUIManager.getInstance();
        if (guiManager.isInitialized()) {
            guiManager.shutdown();
        }

        // 12. 关闭 BossBar 服务
        AdventureBossBarService.getInstance().shutdown();

        // 13. 停止性能监控
        if (performanceMonitor != null) {
            performanceMonitor.stopReportScheduler();
        }

        // 14. 注销生命周期管理器
        if (lifecycleManager != null) {
            lifecycleManager.unregister();
            lifecycleManager.shutdown();
        }
        
        // 15. 保存配置
        if (configManager instanceof ConfigManagerImpl cmi) {
            cmi.saveAll();
        }

        // 16. 关闭数据库连接池
        CoreDatabase.shutdown();

        // 17. 清理 Guice 注入器（防止内存泄漏）
        GuiceSupport.cleanup();

        // 记录关闭信息
        logShutdownInfo();

        // 最后清理实例引用
        fullyInitialized = false;
        instance = null;
        getLogger().info("RPGCore disabled!");
    }

    /**
     * 统一保存所有玩家数据（避免重复保存）
     */
    private void saveAllPlayerData() {
        getLogger().info("正在保存玩家数据...");
        
        // 1. 触发 PlayerLifecycleManager 保存所有在线玩家数据
        if (lifecycleManager != null) {
            try {
                lifecycleManager.saveAllPlayers(true);
            } catch (Exception e) {
                getLogger().log(java.util.logging.Level.SEVERE, "PlayerLifecycleManager 保存数据失败", e);
            }
        }
        
        // 2. 保存统一数据管理器中的数据
        if (dataManager != null) {
            try {
                dataManager.saveAll();
            } catch (Exception e) {
                getLogger().log(java.util.logging.Level.SEVERE, "UnifiedDataManager 保存数据失败", e);
            }
        }
        
        getLogger().info("玩家数据保存完成");
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        databaseEnabled = config.getBoolean("database.enabled", false);

        asyncThreadPoolSize = Math.max(1, Math.min(32, config.getInt("async.thread-pool-size", 4)));
        asyncQueueCapacity = Math.max(0, config.getInt("async.queue-capacity", 1000));
        asyncKeepAliveSeconds = Math.max(10, config.getLong("async.keep-alive-seconds", 60));
        asyncAllowCoreThreadTimeout = config.getBoolean("async.allow-core-thread-timeout", false);
        asyncThreadNamePrefix = config.getString("async.thread-name-prefix", "RPGCore-Async-");
        
        if (asyncThreadNamePrefix == null || asyncThreadNamePrefix.isEmpty()) {
            asyncThreadNamePrefix = "RPGCore-Async-";
        }

        cacheMaxSize = Math.max(100, Math.min(100000, config.getInt("cache.max-size", 2000)));
        cacheDefaultTTL = Duration.ofMinutes(Math.max(1, config.getLong("cache.default-ttl-minutes", 30)));

        String modeStr = config.getString("cache.mode", "caffeine").toLowerCase();
        if (!Arrays.asList("caffeine", "lightweight", "high_performance", "highperformance").contains(modeStr)) {
            getLogger().warning("无效的缓存模式: " + modeStr + ", 使用默认值 caffeine");
            modeStr = "caffeine";
        }
        
        cacheMode = switch (modeStr) {
            case "high_performance", "highperformance" -> Mode.HIGH_PERFORMANCE;
            case "lightweight" -> Mode.LIGHTWEIGHT;
            default -> Mode.CAFFEINE;
        };

        cacheRecordStats = config.getBoolean("cache.record-stats", true);
        cacheWeakKeys = config.getBoolean("cache.weak-keys", false);
        cacheWeakValues = config.getBoolean("cache.weak-values", false);
        cacheSoftValues = config.getBoolean("cache.soft-values", false);
        long refreshMinutes = Math.max(0, config.getLong("cache.refresh-interval-minutes", 0));
        cacheRefreshInterval = refreshMinutes > 0 ? Duration.ofMinutes(refreshMinutes) : Duration.ZERO;

        lockTimeoutMs = Math.max(100, Math.min(60000, config.getLong("lock.timeout-ms", 3000)));

        monitorEnabled = config.getBoolean("monitor.enabled", true);
        monitorMemory = config.getBoolean("monitor.memory-monitoring", true);
        monitorTps = config.getBoolean("monitor.tps-monitoring", true);
        monitorLogSlowOps = config.getBoolean("monitor.log-slow-operations", true);
        monitorMemoryThresholdMb = Math.max(256, config.getLong("monitor.memory-warning-threshold-mb", 1024));
        monitorSlowOpThresholdMs = Math.max(10, config.getLong("monitor.slow-operation-threshold-ms", 100));
        long reportIntervalMinutes = Math.max(0, config.getLong("monitor.report-interval-minutes", 30));
        monitorReportInterval = reportIntervalMinutes > 0 ? Duration.ofMinutes(reportIntervalMinutes) : Duration.ZERO;
        
        if (asyncThreadPoolSize != config.getInt("async.thread-pool-size", 4)) {
            getLogger().warning("异步线程池大小已调整为有效范围: " + asyncThreadPoolSize);
        }
        if (cacheMaxSize != config.getInt("cache.max-size", 2000)) {
            getLogger().warning("缓存最大容量已调整为有效范围: " + cacheMaxSize);
        }
        if (lockTimeoutMs != config.getLong("lock.timeout-ms", 3000)) {
            getLogger().warning("锁超时时间已调整为有效范围: " + lockTimeoutMs + "ms");
        }
    }

    private void initCoreComponents() {
        // 1. 初始化基础设施层
        initInfrastructure();

        // 2. 初始化数据层
        initDataLayer();

        // 3. 初始化服务层
        initServiceLayer();

        // 4. 初始化展示层
        initPresentationLayer();

        // 5. 初始化工具层
        initUtilityLayer();

        // 6. 初始化 Guice 依赖注入
        initGuice();
    }

    /**
     * 初始化基础设施层
     */
    private void initInfrastructure() {
        configurateManager = new ConfigurateManager(getLogger(), getDataFolder().toPath());
        serviceRegistry = new SimpleServiceRegistry(this);
        asyncExecutor = new AsyncExecutorImpl(this, asyncThreadPoolSize, asyncQueueCapacity,
            asyncKeepAliveSeconds, asyncAllowCoreThreadTimeout, asyncThreadNamePrefix);
        scheduler = new UnifiedSchedulerImpl(this);
        getLogger().info("基础设施层初始化完成 (AsyncPool: " + asyncThreadPoolSize + "线程, 队列: " + asyncQueueCapacity + ")");
    }

    /**
     * 初始化数据层
     */
    private void initDataLayer() {
        cacheProvider = new TTLCacheManager(cacheMaxSize, cacheDefaultTTL, cacheRecordStats,
            cacheMode, cacheWeakKeys, cacheWeakValues, cacheSoftValues, cacheRefreshInterval);
        dataManager = new UnifiedDataManager(this);
        configManager = new ConfigManagerImpl(this);
        getLogger().info("数据层初始化完成 (缓存: " + cacheMode + "/" + cacheMaxSize + ", TTL: " + cacheDefaultTTL.toMinutes() + "分钟)");
    }

    /**
     * 初始化服务层
     */
    private void initServiceLayer() {
        lockManager = new PlayerLockManager(getLogger(), lockTimeoutMs);
        performanceMonitor = new PerformanceMonitor("RPGCore", getLogger());
        performanceMonitor.setEnabled(monitorEnabled);
        performanceMonitor.setMemoryMonitoring(monitorMemory);
        performanceMonitor.setTpsMonitoring(monitorTps);
        performanceMonitor.setLogSlowOperations(monitorLogSlowOps);
        performanceMonitor.setMemoryWarningThreshold(monitorMemoryThresholdMb);
        performanceMonitor.setSlowOperationThreshold(monitorSlowOpThresholdMs);
        if (!monitorReportInterval.isZero()) {
            performanceMonitor.setReportInterval(monitorReportInterval);
            performanceMonitor.startReportScheduler();
        }
        serviceScanner = new ServiceScanner(this);
        externalServices = new ExternalServiceIntegrationImpl(this);
        lifecycleManager = new PlayerLifecycleManager(this);
        lifecycleManager.register();
        exceptionHandler = new ExceptionHandlerImpl(this);
        httpClient = new HttpClientImpl(this);
        rateLimiter = new RateLimiterImpl(this);
        cronScheduler = new CronSchedulerImpl(this);
        configMigrator = new ConfigMigratorImpl(this);
        auditLog = new AuditLogImpl(this);
        dataExporter = new DataExporterImpl(this);
        getLogger().info("服务层初始化完成 (" + getActiveServiceCount() + "个组件, 监控: " + (monitorEnabled ? "启用" : "禁用") + ")");
    }

    /**
     * 初始化展示层
     */
    private void initPresentationLayer() {
        displayService = new DisplayServiceImpl(this);
        miniMessageService = MiniMessageService.getInstance();
        messageService = new MessageServiceImpl();
        serviceRegistry.registerService(MessageService.class, messageService);
        textDisplayService = new TextDisplayServiceImpl(this);
        serviceRegistry.registerService(TextDisplayService.class, textDisplayService);
        GUIManager guiManager = GUIManager.getInstance();
        guiManager.initialize(this);
        getLogger().info("展示层初始化完成");
    }

    /**
     * 初始化工具层
     */
    private void initUtilityLayer() {
        gameLogger = new AsyncLogger();
        itemAttributeManager = new ItemAttributeManager(this);
        serviceRegistry.registerService(MiniMessageService.class, miniMessageService);
        serviceRegistry.registerService(SoundService.class, SoundService.getInstance());
        serviceRegistry.registerService(CooldownManager.class, CooldownManager.getInstance());
        serviceRegistry.registerService(YamlDataStore.class, YamlDataStore.getInstance());
        serviceRegistry.registerService(CommandFramework.class, CommandFramework.getInstance());
        serviceRegistry.registerService(PlaceholderService.class, PlaceholderService.getInstance());
        getLogger().info("工具层初始化完成");
    }

    /**
     * 计算活跃服务组件数量
     */
    private int getActiveServiceCount() {
        int count = 0;
        if (lockManager != null) count++;
        if (performanceMonitor != null) count++;
        if (serviceScanner != null) count++;
        if (externalServices != null) count++;
        if (lifecycleManager != null) count++;
        if (exceptionHandler != null) count++;
        if (httpClient != null) count++;
        if (rateLimiter != null) count++;
        if (cronScheduler != null) count++;
        if (configMigrator != null) count++;
        if (auditLog != null) count++;
        if (dataExporter != null) count++;
        return count;
    }

    /**
     * 初始化 Guice 依赖注入框架
     */
    private void initGuice() {
        try {
            guiceInjector = Guice.createInjector(new RPGCoreModule(this));
            GuiceSupport.initialize(this);
            getLogger().info("Guice 依赖注入框架已初始化");
        } catch (Exception e) {
            getLogger().warning("Guice 初始化失败: " + e.getMessage());
            getLogger().info("详细异常信息: ");
            for (StackTraceElement element : e.getStackTrace()) {
                getLogger().info("  at " + element.toString());
            }
        }
    }

    /**
     * 获取 Guice Injector
     *
     * @return Guice Injector 实例
     */
    public Injector getGuiceInjector() {
        return guiceInjector;
    }

    private void initDatabase() {
        if (databaseEnabled) {
            boolean success = CoreDatabase.initialize(this);
            if (success) {
                getLogger().info("CoreDatabase initialized successfully");
            } else {
                getLogger().warning("CoreDatabase initialization failed, running in YAML-only mode");
            }
        } else {
            getLogger().info("Database disabled, running in YAML-only mode");
        }
    }

    private void registerCommands() {
        if (getCommand("rpgcore") != null) {
            getCommand("rpgcore").setExecutor(this);
            getCommand("rpgcore").setTabCompleter(this);
        }
    }

    private void registerListeners() {
    }

    private void logStartupInfo() {
        getLogger().info("========== RPGCore 启动信息 ==========");
        getLogger().info("版本: " + getDescription().getVersion());
        getLogger().info("数据库: " + (databaseEnabled ? "启用" : "禁用"));
        getLogger().info("异步线程池大小: " + asyncThreadPoolSize);
        getLogger().info("缓存模式: " + cacheMode);
        getLogger().info("缓存最大容量: " + cacheMaxSize);
        getLogger().info("缓存默认TTL: " + cacheDefaultTTL.toMinutes() + "分钟");
        getLogger().info("锁超时时间: " + lockTimeoutMs + "毫秒");
        getLogger().info("======================================");
    }

    private void logShutdownInfo() {
        if (performanceMonitor != null) {
            PerformanceReport report = performanceMonitor.generateReport();
            getLogger().info(report.toString());
        }

        if (cacheProvider != null) {
            getLogger().info("Cache stats: " + cacheProvider.getStats());
        }

        if (lockManager != null) {
            getLogger().info("Lock stats: " + lockManager.getStats());
        }

        if (CoreDatabase.isEnabled()) {
            getLogger().info("Database pool status: " + CoreDatabase.getPoolStatus());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rpgcore")) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> sendInfo(sender);
            case "stats" -> sendStats(sender);
            case "reload" -> {
                if (!sender.hasPermission("rpgcore.reload")) {
                    sender.sendMessage(miniMessageService.red("没有权限!"));
                    return true;
                }
                if (!fullyInitialized) {
                    sender.sendMessage(miniMessageService.yellow("RPGCore 尚未完全初始化，请稍后再试"));
                    return true;
                }
                reloadConfig();
                loadConfiguration();
                
                // 刷新依赖配置的服务
                if (performanceMonitor != null) {
                    performanceMonitor.setLogSlowOperations(monitorLogSlowOps);
                    performanceMonitor.setMemoryWarningThreshold(monitorMemoryThresholdMb);
                    performanceMonitor.setSlowOperationThreshold(monitorSlowOpThresholdMs);
                }
                if (cacheProvider != null) {
                    cacheProvider.setDefaultTTL(cacheDefaultTTL);
                    cacheProvider.setMaxSize(cacheMaxSize);
                }
                if (lockManager != null) {
                    lockManager.setDefaultTimeout(lockTimeoutMs);
                }
                
                sender.sendMessage(miniMessageService.green("配置已重新加载!"));
                sender.sendMessage(miniMessageService.yellow("注意: 部分配置需要重启服务器才能生效"));
            }
            case "services" -> sendServicesList(sender);
            case "modules" -> sendModulesList(sender);
            case "caches" -> sendCachesStats(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("stats");
            completions.add("services");
            completions.add("modules");
            completions.add("caches");
            if (sender.hasPermission("rpgcore.reload")) {
                completions.add("reload");
            }
            completions.add("help");
        }

        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== RPGCore 帮助 =========="));
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore info <gray>- 查看信息"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore stats <gray>- 查看统计"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore services <gray>- 列出已注册服务"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore modules <gray>- 列出已加载模块"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore caches <gray>- 查看缓存状态"));
        if (sender.hasPermission("rpgcore.reload")) {
            sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore reload <gray>- 重载配置"));
        }
        sender.sendMessage(miniMessageService.colorize("<yellow>/rpgcore help <gray>- 显示帮助"));
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== RPGCore 信息 =========="));
        sender.sendMessage(miniMessageService.colorize("<yellow>版本: <white>" + getDescription().getVersion()));
        sender.sendMessage(miniMessageService.colorize("<yellow>数据库: <white>" + (databaseEnabled ? "启用" : "禁用")));
        sender.sendMessage(miniMessageService.colorize("<yellow>异步线程: <white>" + asyncThreadPoolSize));
        sender.sendMessage(miniMessageService.colorize("<yellow>缓存模式: <white>" + cacheMode));
        sender.sendMessage(miniMessageService.colorize("<yellow>缓存容量: <white>" + cacheMaxSize));
        sender.sendMessage(miniMessageService.colorize("<yellow>缓存TTL: <white>" + cacheDefaultTTL.toMinutes() + "分钟"));
        sender.sendMessage(miniMessageService.colorize("<yellow>锁超时: <white>" + lockTimeoutMs + "毫秒"));
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    private void sendStats(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== RPGCore 统计 =========="));
        sender.sendMessage(miniMessageService.colorize("<yellow>缓存统计: <white>" + cacheProvider.getStats()));
        sender.sendMessage(miniMessageService.colorize("<yellow>锁统计: <white>" + lockManager.getStats()));
        sender.sendMessage(miniMessageService.colorize("<yellow>服务数量: <white>" + serviceRegistry.getServiceCount()));
        sender.sendMessage(miniMessageService.colorize("<yellow>待处理任务: <white>" + asyncExecutor.getPendingTaskCount()));
        if (CoreDatabase.isEnabled()) {
            sender.sendMessage(miniMessageService.colorize("<yellow>数据库连接池: <white>" + CoreDatabase.getPoolStatus()));
        }
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    private void sendServicesList(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== 已注册服务 =========="));
        
        if (serviceRegistry instanceof SimpleServiceRegistry ssr) {
            var serviceTypes = ssr.getRegisteredServiceTypes();
            if (serviceTypes.isEmpty()) {
                sender.sendMessage(miniMessageService.colorize("<gray>暂无已注册的服务"));
            } else {
                int index = 1;
                for (Class<?> serviceType : serviceTypes) {
                    String serviceName = serviceType.getSimpleName();
                    sender.sendMessage(miniMessageService.colorize(
                        String.format("<gray>%d. <gold>%s", index++, serviceName)));
                }
                sender.sendMessage(miniMessageService.colorize("<gray>共 <gold>" + serviceTypes.size() + " <gray>个服务"));
            }
        } else {
            sender.sendMessage(miniMessageService.colorize("<gray>服务数量: <gold>" + serviceRegistry.getServiceCount()));
        }
        
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    private void sendModulesList(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== 已加载模块 =========="));
        
        if (modules.isEmpty()) {
            sender.sendMessage(miniMessageService.colorize("<gray>暂无已加载的模块"));
        } else {
            int index = 1;
            for (Map.Entry<String, RPGModule> entry : modules.entrySet()) {
                String moduleId = entry.getKey();
                RPGModule module = entry.getValue();
                String state = module.getState().name();
                String stateColor = state.equals("ENABLED") ? "<green>" : 
                                   state.equals("LOADED") ? "<yellow>" : "<red>";
                sender.sendMessage(miniMessageService.colorize(
                    String.format("<gray>%d. <gold>%s <gray>[%s%s<gray>]", index++, moduleId, stateColor, state)));
            }
            sender.sendMessage(miniMessageService.colorize("<gray>共 <gold>" + modules.size() + " <gray>个模块"));
        }
        
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    private void sendCachesStats(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== 缓存状态 =========="));
        
        sender.sendMessage(miniMessageService.colorize("<yellow>缓存模式: <white>" + cacheMode));
        sender.sendMessage(miniMessageService.colorize("<yellow>最大容量: <white>" + cacheMaxSize));
        sender.sendMessage(miniMessageService.colorize("<yellow>默认TTL: <white>" + cacheDefaultTTL.toMinutes() + "分钟"));
        sender.sendMessage(miniMessageService.colorize(""));
        sender.sendMessage(miniMessageService.colorize("<yellow>统计信息:"));
        sender.sendMessage(miniMessageService.colorize("<gray>" + cacheProvider.getStats()));
        
        sender.sendMessage(miniMessageService.gold("=================================="));
    }

    public static RPGCore getInstance() {
        return instance;
    }

    /**
     * 检查 RPGCore 是否完全初始化
     * 
     * @return 如果完全初始化返回 true
     */
    public boolean isFullyInitialized() {
        return fullyInitialized && instance != null;
    }

    /**
     * 获取 EventBus 实例（过渡期兼容方案）
     *
     * <p><b>⚠️ 此方法已废弃，将在未来版本移除。</b></p>
     * <p>请迁移到以下方案：</p>
     * <ul>
     *   <li>{@link cn.guangdian.rpgcore.event.EventPublisher#publish(org.bukkit.event.Event)}</li>
     *   <li>{@code Bukkit.getPluginManager().callEvent(Event)}</li>
     * </ul>
     *
     * @return EventBus 代理实例
     * @deprecated 使用 {@link cn.guangdian.rpgcore.event.EventPublisher} 或 Bukkit 原生事件系统替代
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public cn.guangdian.rpgcore.api.EventBus getEventBus() {
        return new cn.guangdian.rpgcore.event.EventBusProxy();
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public CacheProvider getCacheProvider() {
        return cacheProvider;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public PlayerLockManager getLockManager() {
        return lockManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public ServiceScanner getServiceScanner() {
        return serviceScanner;
    }
    
    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }
    
    public SyncScheduler getScheduler() {
        return scheduler;
    }
    
    public PlayerLifecycleManager getPlayerLifecycle() {
        return lifecycleManager;
    }
    
    public DisplayService getDisplayService() {
        return displayService;
    }

    public UnifiedDataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public HttpClient.RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public CronScheduler getCronScheduler() {
        return cronScheduler;
    }

    public ConfigMigrator getConfigMigrator() {
        return configMigrator;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public DataExporter getDataExporter() {
        return dataExporter;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public TextDisplayService getTextDisplayService() {
        return textDisplayService;
    }

    public MenuService getMenuService() {
        return serviceRegistry.getService(MenuService.class);
    }

    public GameLogger getGameLogger() {
        return gameLogger;
    }

    public MiniMessageService getMiniMessageService() {
        return miniMessageService;
    }

    /**
     * 获取音效服务
     * @return SoundService 实例
     */
    public SoundService getSoundService() {
        return SoundService.getInstance();
    }

    /**
     * 获取服务器服务
     * @return ServerService 实例
     */
    public ServerService getServerService() {
        return ServerService.getInstance(this);
    }

    /**
     * 获取实体服务
     * @return EntityService 实例
     */
    public EntityService getEntityService() {
        return EntityService.getInstance();
    }

    /**
     * 获取物品属性管理器
     * @return ItemAttributeManager 实例
     */
    public ItemAttributeManager getItemAttributeManager() {
        return itemAttributeManager;
    }

    public boolean isDatabaseEnabled() {
        return databaseEnabled && CoreDatabase.isEnabled();
    }

    /**
     * 获取 Configurate 配置管理器
     *
     * @return ConfigurateManager 实例
     */
    public ConfigurateManager getConfigurateManager() {
        return configurateManager;
    }

    /**
     * 获取 Jackson JSON ObjectMapper
     *
     * @return Jackson ObjectMapper
     */
    public com.fasterxml.jackson.databind.ObjectMapper getJsonMapper() {
        return cn.guangdian.rpgcore.util.JacksonUtils.getJsonMapper();
    }

    /**
     * 获取 Jackson YAML ObjectMapper
     *
     * @return YAML ObjectMapper
     */
    public com.fasterxml.jackson.databind.ObjectMapper getYamlMapper() {
        return cn.guangdian.rpgcore.util.JacksonUtils.getYamlMapper();
    }

    public void registerModule(RPGModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        
        String moduleId = module.getModuleId();
        RPGModule existing = modules.putIfAbsent(moduleId, module);
        if (existing != null) {
            getLogger().warning("Module " + moduleId + " already registered, skipping");
            return;
        }
        
        module.load();
        module.enable();
        
        getLogger().info("Module registered and enabled: " + moduleId);
    }

    public void unregisterModule(String moduleId) {
        RPGModule module = modules.remove(moduleId);
        if (module != null) {
            module.disable();
            getLogger().info("Module unregistered: " + moduleId);
        }
    }

    public RPGModule getModule(String moduleId) {
        return modules.get(moduleId);
    }

    public Map<String, RPGModule> getModules() {
        return new ConcurrentHashMap<>(modules);
    }

    public int getModuleCount() {
        return modules.size();
    }
}