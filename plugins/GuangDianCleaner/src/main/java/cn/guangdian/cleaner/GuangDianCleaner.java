package cn.guangdian.cleaner;

import cn.guangdian.cleaner.adapter.CleanerServiceAdapter;
import cn.guangdian.cleaner.command.CleanerCommand;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.listener.DropListener;
import cn.guangdian.cleaner.manager.CleanManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 光点扫地娘 - 高性能地面掉落物清理插件
 *
 * 功能特性：
 * - 定时自动清理地面掉落物
 * - 手动命令触发清理
 * - 物品黑白名单过滤
 * - 世界过滤
 * - 高性能异步处理（使用 RPGCore AsyncExecutor）
 * - 清理统计与报告
 *
 * @author Gumin
 */
public class GuangDianCleaner extends JavaPlugin {

    private static GuangDianCleaner instance;

    private ConfigManager configManager;
    private CleanManager cleanManager;
    private CleanerServiceAdapter serviceAdapter;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 初始化清理管理器
        cleanManager = new CleanManager(this, configManager);

        // 注册 RPGCore 服务适配器（替代 ThreadPoolManager）
        serviceAdapter = new CleanerServiceAdapter(this, cleanManager);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
            getLogger().info("使用统一 AsyncExecutor 替代本地线程池");
        }

        // 注册命令
        registerCommands();

        // 注册事件监听器
        registerListeners();

        // 启动定时清理任务
        cleanManager.startAutoCleanTask();

        getLogger().info("========================================");
        getLogger().info("  光点扫地娘插件已启用!");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("  作者: Gumin | QQ: 2271257344");
        getLogger().info("  自动清理间隔: " + configManager.getAutoCleanInterval() + "秒");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        // 停止所有任务
        if (cleanManager != null) {
            cleanManager.stopAutoCleanTask();
        }

        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        getLogger().info("光点扫地娘插件已禁用!");
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
        if (serviceAdapter != null && serviceAdapter.getAsyncExecutor() != null) {
            serviceAdapter.getAsyncExecutor().execute(task);
        } else {
            getServer().getScheduler().runTaskAsynchronously(this, task);
        }
    }

    /**
     * 在主线程执行任务
     */
    public void runSync(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    /**
     * 延迟执行任务
     */
    public void runTaskLater(Runnable task, long ticks) {
        getServer().getScheduler().runTaskLater(this, task, ticks);
    }
}