package cn.guangdian.cleaner;

import cn.guangdian.cleaner.adapter.CleanerServiceAdapter;
import cn.guangdian.cleaner.command.CleanerCommand;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.listener.DropListener;
import cn.guangdian.cleaner.manager.CleanManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;

/**
 * 光点扫地娘插件 - GuangDianCleaner
 *
 * <p>RPGCore 服务集成:
 * <ul>
 *   <li>GameLogger: 使用 RPGCore 统一日志服务</li>
 *   <li>SyncScheduler: 使用 RPGCore 同步任务调度器</li>
 * </ul>
 *
 * <p>优先级模式: 优先使用 RPGCore 服务，不可用则降级到本地实现
 *
 * @author Gumin
 * @QQ 2271257344
 * @version 1.0.0
 */
public class GuangDianCleaner extends AbstractRPGPlugin {

    private static GuangDianCleaner instance;

    private ConfigManager configManager;
    private CleanManager cleanManager;
    private CleanerServiceAdapter serviceAdapter;

    // RPGCore 日志服务
    private GameLogger gameLogger;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 初始化清理管理器
        cleanManager = new CleanManager(this, configManager);

        // 注册 RPGCore 服务适配器（替代 ThreadPoolManager）
        serviceAdapter = new CleanerServiceAdapter(this, cleanManager);
        if (serviceAdapter.isUsingRPGCore()) {
            logInfo("已集成 RPGCore 服务系统!");
            logInfo("使用统一 AsyncExecutor 替代本地线程池");
        }

        // 注册命令
        registerCommands();

        // 注册事件监听器
        registerListeners();

        // 启动定时清理任务
        cleanManager.startAutoCleanTask();

        logInfo("========================================");
        logInfo("  光点扫地娘插件已启用!");
        logInfo("  版本: " + getDescription().getVersion());
        logInfo("  作者: Gumin | QQ: 2271257344");
        logInfo("  自动清理间隔: " + configManager.getAutoCleanInterval() + "秒");
        logInfo("========================================");
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            gameLogger = rpgCore.getGameLogger();
            if (gameLogger != null) {
                logInfo("已连接到 RPGCore GameLogger");
            }
        }
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
    }

    /**
     * 日志辅助方法 - 优先使用 RPGCore GameLogger
     */
    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        } else {
            getLogger().info("[DEBUG] " + message);
        }
    }

    @Override
    protected void onPluginDisable() {
        if (cleanManager != null) {
            cleanManager.stopAutoCleanTask();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        logInfo("光点扫地娘插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianCleaner";
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        CleanerCommand cleanerCommand = new CleanerCommand(this, configManager, cleanManager);
        getCommand("gdclean").setExecutor(cleanerCommand);
        getCommand("gdclean").setTabCompleter(cleanerCommand);
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DropListener(this, cleanManager), this);
    }

    /**
     * 获取插件实例
     */
    public static GuangDianCleaner getInstance() {
        return instance;
    }

    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取清理管理器
     */
    public CleanManager getCleanManager() {
        return cleanManager;
    }

    /**
     * 获取服务适配器
     */
    public CleanerServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    /**
     * 异步执行任务（通过服务适配器使用 RPGCore AsyncExecutor）
     */
    public void runAsync(Runnable task) {
        if (scheduler != null) {
            scheduler.runAsync(task);
        }
    }

    /**
     * 在主线程执行任务
     */
    public void runSync(Runnable task) {
        if (scheduler != null) {
            scheduler.runSync(task);
        }
    }

    /**
     * 延迟执行任务
     */
    public void runTaskLater(Runnable task, long ticks) {
        if (scheduler != null) {
            scheduler.runSyncLater(task, ticks);
        }
    }
}