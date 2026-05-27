package cn.guangdian.rpgcore.module;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.monitor.PerformanceMonitor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * RPG模块基类
 * 
 * <p>所有RPG业务模块都应继承此类，获得标准化的生命周期管理。</p>
 * 
 * <p>占位符功能由子类通过 PlaceholderAPI 实现，在 {@link #registerPlaceholders()} 中注册。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class PointsModule extends RPGModule {
 *     
 *     public PointsModule(JavaPlugin plugin) {
 *         super(plugin, "Points");
 *     }
 *     
 *     @Override
 *     protected void registerServices() {
 *         getServices().registerService(PointsService.class, new PointsServiceImpl());
 *     }
 *     
 *     @Override
 *     protected void registerCommands() {
 *         getCommand("points").setExecutor(new PointsCommand());
 *     }
 *     
 *     @Override
 *     protected void registerListeners() {
 *         registerListener(new PointsListener());
 *     }
 *     
 *     @Override
 *     protected void registerPlaceholders() {
 *         // 注册到 PlaceholderAPI
 *         new PointsPlaceholder(plugin).register();
 *     }
 *     
 *     @Override
 *     protected void saveAllData() {
 *         // 保存所有数据
 *     }
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class RPGModule implements Listener {

    protected final JavaPlugin plugin;
    protected final String moduleId;
    protected final Logger logger;
    private ModuleState state = ModuleState.CREATED;

    /**
     * 创建RPG模块
     *
     * @param plugin 插件实例
     * @param moduleId 模块ID
     */
    protected RPGModule(JavaPlugin plugin, String moduleId) {
        this.plugin = plugin;
        this.moduleId = moduleId;
        this.logger = Logger.getLogger(moduleId);

        // 触发创建回调
        onCreate();
    }

    /**
     * 模块创建时调用（构造阶段）
     *
     * <p>子类可覆盖此方法执行初始化逻辑，如依赖检查等。</p>
     */
    protected void onCreate() {}

    // ==================== 生命周期方法 ====================

    /**
     * 加载模块
     */
    public final void load() {
        // 接受 CREATED 或 UNLOADED 状态
        if (state != ModuleState.UNLOADED && state != ModuleState.CREATED) {
            return;
        }

        setState(ModuleState.LOADING);

        try {
            onLoad();
            loadConfig();
            setState(ModuleState.LOADED);
            log("Module loaded: " + moduleId);
        } catch (Exception e) {
            setState(ModuleState.ERROR);
            logger.severe("Failed to load module " + moduleId + ": " + e.getMessage());
            throw new RuntimeException("Module load failed", e);
        }
    }

    /**
     * 启用模块
     */
    public final void enable() {
        if (state != ModuleState.LOADED && state != ModuleState.DISABLED) {
            return;
        }

        setState(ModuleState.ENABLING);

        try {
            initOptimizationComponents();
            registerServices();
            registerCommands();
            registerListeners();
            registerPlaceholders();
            registerEventHandlers();
            startTasks();
            setState(ModuleState.ENABLED);
            log("Module enabled: " + moduleId);
        } catch (Exception e) {
            setState(ModuleState.ERROR);
            logger.severe("Failed to enable module " + moduleId + ": " + e.getMessage());
            throw new RuntimeException("Module enable failed", e);
        }
    }

    /**
     * 禁用模块
     */
    public final void disable() {
        if (state != ModuleState.ENABLED) {
            return;
        }

        setState(ModuleState.DISABLING);

        try {
            saveAllData();
            stopTasks();
            unregisterListeners();
            unregisterServices();
            cleanupResources();
            setState(ModuleState.DISABLED);
            log("Module disabled: " + moduleId);
        } catch (Exception e) {
            setState(ModuleState.ERROR);
            logger.severe("Failed to disable module " + moduleId + ": " + e.getMessage());
        }
    }

    /**
     * 销毁模块
     *
     * <p>完全销毁模块，释放所有资源。销毁后模块不可重用。</p>
     */
    public final void destroy() {
        if (state == ModuleState.DESTROYED) {
            return;
        }

        // 如果还在启用状态，先禁用
        if (state == ModuleState.ENABLED) {
            disable();
        }

        setState(ModuleState.DESTROYING);

        try {
            onDestroy();
            setState(ModuleState.DESTROYED);
            log("Module destroyed: " + moduleId);
        } catch (Exception e) {
            setState(ModuleState.ERROR);
            logger.severe("Failed to destroy module " + moduleId + ": " + e.getMessage());
        }
    }

    /**
     * 模块销毁时调用
     *
     * <p>子类可覆盖此方法执行最终的资源清理。</p>
     */
    protected void onDestroy() {}

    /**
     * 重载模块
     */
    public final void reload() {
        try {
            reloadConfig();
            onReload();
            log("Module reloaded: " + moduleId);
        } catch (Exception e) {
            logger.severe("Failed to reload module " + moduleId + ": " + e.getMessage());
        }
    }

    // ==================== 子类实现方法 ====================

    /**
     * 模块加载时调用（配置加载前）
     */
    protected void onLoad() {}

    /**
     * 加载配置
     */
    protected void loadConfig() {}

    /**
     * 重载配置
     */
    protected void reloadConfig() {
        plugin.reloadConfig();
    }

    /**
     * 注册服务
     */
    protected abstract void registerServices();

    /**
     * 注册命令
     */
    protected abstract void registerCommands();

    /**
     * 注册监听器
     */
    protected abstract void registerListeners();

    /**
     * 保存所有数据
     */
    protected abstract void saveAllData();

    // ==================== 可选覆盖方法 ====================

    /**
     * 初始化优化组件
     */
    protected void initOptimizationComponents() {}

    /**
     * 注册PlaceholderAPI扩展
     * 
     * <p>子类应在此方法中注册自己的 PlaceholderExpansion。</p>
     * 
     * <pre>{@code
     * @Override
     * protected void registerPlaceholders() {
     *     new MyPluginPlaceholder(plugin).register();
     * }
     * }</pre>
     */
    protected void registerPlaceholders() {}

    /**
     * 注册事件处理器
     */
    protected void registerEventHandlers() {}

    /**
     * 启动定时任务
     */
    protected void startTasks() {}

    /**
     * 停止定时任务
     */
    protected void stopTasks() {}

    /**
     * 注销监听器
     */
    protected void unregisterListeners() {}

    /**
     * 注销服务
     */
    protected void unregisterServices() {}

    /**
     * 清理资源
     */
    protected void cleanupResources() {}

    /**
     * 重载时调用
     */
    protected void onReload() {}

    // ==================== 便捷方法 ====================

    /**
     * 获取RPGCore实例
     */
    protected RPGCore getCore() {
        return RPGCore.getInstance();
    }

    /**
     * 获取事件总线
     */
    protected EventBus getEventBus() {
        return getCore().getEventBus();
    }

    /**
     * 获取服务注册表
     */
    protected ServiceRegistry getServices() {
        return getCore().getServiceRegistry();
    }

    /**
     * 获取缓存提供者
     */
    protected CacheProvider getCache() {
        return getCore().getCacheProvider();
    }

    /**
     * 获取异步执行器
     */
    protected AsyncExecutor getAsyncExecutor() {
        return getCore().getAsyncExecutor();
    }

    /**
     * 获取玩家锁管理器
     */
    protected PlayerLockManager getLockManager() {
        return getCore().getLockManager();
    }

    /**
     * 获取性能监控器
     */
    protected PerformanceMonitor getPerformanceMonitor() {
        return getCore().getPerformanceMonitor();
    }

    /**
     * 注册监听器（便捷方法）
     */
    protected void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    /**
     * 记录日志
     */
    protected void log(String message) {
        logger.info("[" + moduleId + "] " + message);
    }

    /**
     * 记录警告日志
     */
    protected void logWarning(String message) {
        logger.warning("[" + moduleId + "] " + message);
    }

    /**
     * 记录错误日志
     */
    protected void logError(String message) {
        logger.severe("[" + moduleId + "] " + message);
    }

    // ==================== 状态管理 ====================

    /**
     * 获取模块状态
     */
    public ModuleState getState() {
        return state;
    }

    /**
     * 设置模块状态
     */
    protected void setState(ModuleState state) {
        this.state = state;
    }

    /**
     * 获取模块ID
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * 获取插件实例
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * 检查模块是否已启用
     */
    public boolean isEnabled() {
        return state == ModuleState.ENABLED;
    }

    /**
     * 模块状态枚举
     */
    public enum ModuleState {
        /**
         * 已创建（构造完成）
         */
        CREATED,
        /**
         * 未加载
         */
        UNLOADED,
        /**
         * 加载中
         */
        LOADING,
        /**
         * 已加载
         */
        LOADED,
        /**
         * 启用中
         */
        ENABLING,
        /**
         * 已启用
         */
        ENABLED,
        /**
         * 禁用中
         */
        DISABLING,
        /**
         * 已禁用
         */
        DISABLED,
        /**
         * 销毁中
         */
        DESTROYING,
        /**
         * 已销毁
         */
        DESTROYED,
        /**
         * 错误状态
         */
        ERROR
    }
}