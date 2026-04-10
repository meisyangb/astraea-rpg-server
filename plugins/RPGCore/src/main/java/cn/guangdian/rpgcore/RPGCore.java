package cn.guangdian.rpgcore;

import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.ConfigManager;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ExceptionHandler;
import cn.guangdian.rpgcore.api.PluginLifecycleManager;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.async.AsyncExecutorImpl;
import cn.guangdian.rpgcore.cache.TTLCacheManager;
import cn.guangdian.rpgcore.cache.TTLCacheManager.Mode;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.config.ConfigManagerImpl;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.display.DisplayService;
import cn.guangdian.rpgcore.display.DisplayServiceImpl;
import cn.guangdian.rpgcore.event.SimpleEventBus;
import cn.guangdian.rpgcore.exception.ExceptionHandlerImpl;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegrationImpl;
import cn.guangdian.rpgcore.lifecycle.PlayerLifecycleManager;
import cn.guangdian.rpgcore.module.RPGModule;
import cn.guangdian.rpgcore.monitor.PerformanceMonitor;
import cn.guangdian.rpgcore.monitor.PerformanceReport;
import cn.guangdian.rpgcore.scheduler.UnifiedSchedulerImpl;
import cn.guangdian.rpgcore.service.ServiceScanner;
import cn.guangdian.rpgcore.service.SimpleServiceRegistry;
import cn.guangdian.rpgcore.storage.UnifiedDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
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

    private static RPGCore instance;

    private EventBus eventBus;
    private ServiceRegistry serviceRegistry;
    private CacheProvider cacheProvider;
    private AsyncExecutor asyncExecutor;
    private PlayerLockManager lockManager;
    private PerformanceMonitor performanceMonitor;
    private ServiceScanner serviceScanner;
    private ExternalServiceIntegration externalServices;
    private SyncScheduler scheduler;
    private cn.guangdian.rpgcore.lifecycle.PlayerLifecycleManager lifecycleManager;
    private cn.guangdian.rpgcore.display.DisplayService displayService;
    private cn.guangdian.rpgcore.storage.UnifiedDataManager dataManager;
    private ConfigManager configManager;
    private ExceptionHandler exceptionHandler;

    private int asyncThreadPoolSize;
    private int cacheMaxSize;
    private Duration cacheDefaultTTL;
    private Mode cacheMode;
    private long lockTimeoutMs;
    private boolean databaseEnabled;

    private final Map<String, RPGModule> modules = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        loadConfiguration();

        initCoreComponents();

        initDatabase();

        registerCommands();

        registerListeners();

        logStartupInfo();

        getLogger().info("RPGCore v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (asyncExecutor != null) {
            getLogger().info("Waiting for async tasks to complete...");
            asyncExecutor.awaitTermination(30, TimeUnit.SECONDS);
            asyncExecutor.shutdown();
        }

        if (eventBus instanceof SimpleEventBus seb) {
            seb.clear();
        }
        if (serviceRegistry instanceof SimpleServiceRegistry ssr) {
            ssr.clear();
        }
        if (cacheProvider != null) {
            cacheProvider.clear();
        }
        if (lockManager != null) {
            lockManager.releaseAllLocks();
        }
        
        if (externalServices instanceof ExternalServiceIntegrationImpl esi) {
            esi.shutdown();
        }
        
        if (scheduler instanceof UnifiedSchedulerImpl usi) {
            usi.shutdown();
        }
        
        if (lifecycleManager != null) {
            lifecycleManager.unregister();
        }
        
        if (configManager instanceof ConfigManagerImpl cmi) {
            cmi.saveAll();
        }

        CoreDatabase.shutdown();

        logShutdownInfo();

        instance = null;
        getLogger().info("RPGCore disabled!");
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        databaseEnabled = config.getBoolean("database.enabled", false);
        asyncThreadPoolSize = config.getInt("async.thread-pool-size", 4);
        cacheMaxSize = config.getInt("cache.max-size", 2000);
        cacheDefaultTTL = Duration.ofMinutes(config.getLong("cache.default-ttl-minutes", 30));
        
        String modeStr = config.getString("cache.mode", "lightweight").toLowerCase();
        cacheMode = switch (modeStr) {
            case "high_performance", "highperformance" -> Mode.HIGH_PERFORMANCE;
            default -> Mode.LIGHTWEIGHT;
        };
        
        lockTimeoutMs = config.getLong("lock.timeout-ms", 3000);
    }

    private void initCoreComponents() {
        eventBus = new SimpleEventBus(this);
        getLogger().info("EventBus initialized");

        serviceRegistry = new SimpleServiceRegistry(this);
        getLogger().info("ServiceRegistry initialized");

        cacheProvider = new TTLCacheManager(cacheMaxSize, cacheDefaultTTL, true, cacheMode);
        getLogger().info("CacheProvider initialized (mode: " + cacheMode + ", maxSize: " + cacheMaxSize + ", TTL: " + cacheDefaultTTL + ")");

        asyncExecutor = new AsyncExecutorImpl(this, asyncThreadPoolSize);
        getLogger().info("AsyncExecutor initialized (threads: " + asyncThreadPoolSize + ")");

        lockManager = new PlayerLockManager(getLogger(), lockTimeoutMs);
        getLogger().info("PlayerLockManager initialized (timeout: " + lockTimeoutMs + "ms)");

        performanceMonitor = new PerformanceMonitor("RPGCore");
        getLogger().info("PerformanceMonitor initialized");

        serviceScanner = new ServiceScanner(this);
        getLogger().info("ServiceScanner initialized");
        
        externalServices = new ExternalServiceIntegrationImpl(this);
        getLogger().info("ExternalServices: " + externalServices.getExternalServiceStatus());
        
        scheduler = new UnifiedSchedulerImpl(this);
        getLogger().info("UnifiedScheduler initialized");
        
        lifecycleManager = new PlayerLifecycleManager(this);
        getLogger().info("PlayerLifecycleManager initialized");
        
        displayService = new DisplayServiceImpl(this);
        getLogger().info("DisplayService initialized");
        
        dataManager = new UnifiedDataManager(this);
        getLogger().info("UnifiedDataManager initialized");
        
        configManager = new ConfigManagerImpl(this);
        getLogger().info("ConfigManager initialized");
        
        exceptionHandler = new ExceptionHandlerImpl(this);
        getLogger().info("ExceptionHandler initialized");
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
                    sender.sendMessage(ChatColor.RED + "没有权限!");
                    return true;
                }
                reloadConfig();
                loadConfiguration();
                sender.sendMessage(ChatColor.GREEN + "配置已重新加载!");
            }
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
            if (sender.hasPermission("rpgcore.reload")) {
                completions.add("reload");
            }
            completions.add("help");
        }

        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== RPGCore 帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/rpgcore info " + ChatColor.GRAY + "- 查看信息");
        sender.sendMessage(ChatColor.YELLOW + "/rpgcore stats " + ChatColor.GRAY + "- 查看统计");
        if (sender.hasPermission("rpgcore.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/rpgcore reload " + ChatColor.GRAY + "- 重载配置");
        }
        sender.sendMessage(ChatColor.YELLOW + "/rpgcore help " + ChatColor.GRAY + "- 显示帮助");
        sender.sendMessage(ChatColor.GOLD + "==================================");
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== RPGCore 信息 ==========");
        sender.sendMessage(ChatColor.YELLOW + "版本: " + ChatColor.WHITE + getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "数据库: " + ChatColor.WHITE + (databaseEnabled ? "启用" : "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "异步线程: " + ChatColor.WHITE + asyncThreadPoolSize);
        sender.sendMessage(ChatColor.YELLOW + "缓存模式: " + ChatColor.WHITE + cacheMode);
        sender.sendMessage(ChatColor.YELLOW + "缓存容量: " + ChatColor.WHITE + cacheMaxSize);
        sender.sendMessage(ChatColor.YELLOW + "缓存TTL: " + ChatColor.WHITE + cacheDefaultTTL.toMinutes() + "分钟");
        sender.sendMessage(ChatColor.YELLOW + "锁超时: " + ChatColor.WHITE + lockTimeoutMs + "毫秒");
        sender.sendMessage(ChatColor.GOLD + "==================================");
    }

    private void sendStats(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== RPGCore 统计 ==========");
        sender.sendMessage(ChatColor.YELLOW + "缓存统计: " + ChatColor.WHITE + cacheProvider.getStats());
        sender.sendMessage(ChatColor.YELLOW + "锁统计: " + ChatColor.WHITE + lockManager.getStats());
        sender.sendMessage(ChatColor.YELLOW + "服务数量: " + ChatColor.WHITE + serviceRegistry.getServiceCount());
        sender.sendMessage(ChatColor.YELLOW + "待处理任务: " + ChatColor.WHITE + asyncExecutor.getPendingTaskCount());
        if (CoreDatabase.isEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "数据库连接池: " + ChatColor.WHITE + CoreDatabase.getPoolStatus());
        }
        sender.sendMessage(ChatColor.GOLD + "==================================");
    }

    public static RPGCore getInstance() {
        return instance;
    }

    public EventBus getEventBus() {
        return eventBus;
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
    
    public cn.guangdian.rpgcore.display.DisplayService getDisplayService() {
        return displayService;
    }
    
    public cn.guangdian.rpgcore.storage.UnifiedDataManager getDataManager() {
        return dataManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public boolean isDatabaseEnabled() {
        return databaseEnabled && CoreDatabase.isEnabled();
    }

    public void registerModule(RPGModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        
        String moduleId = module.getModuleId();
        if (modules.containsKey(moduleId)) {
            getLogger().warning("Module " + moduleId + " already registered, skipping");
            return;
        }
        
        modules.put(moduleId, module);
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