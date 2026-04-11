package cn.guangdian.cleaner;

import cn.guangdian.cleaner.adapter.CleanerServiceAdapter;
import cn.guangdian.cleaner.command.CleanerCommand;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.cleaner.listener.DropListener;
import cn.guangdian.cleaner.manager.CleanManager;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;

public class GuangDianCleaner extends AbstractRPGPlugin {

    private static GuangDianCleaner instance;

    private ConfigManager configManager;
    private CleanManager cleanManager;
    private CleanerServiceAdapter serviceAdapter;

    @Override
    protected void onPluginEnable() {
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
    protected void onPluginDisable() {
        if (cleanManager != null) {
            cleanManager.stopAutoCleanTask();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        getLogger().info("光点扫地娘插件已禁用!");
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