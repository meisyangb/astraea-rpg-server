package cn.guangdian.dynamicview;

import cn.guangdian.dynamicview.command.DynamicViewCommand;
import cn.guangdian.dynamicview.data.PlayerViewData.ViewTier;
import cn.guangdian.dynamicview.listener.PlayerListener;
import cn.guangdian.dynamicview.manager.ViewDistanceManager;
import cn.guangdian.dynamicview.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * 动态视距插件 - 预设挡位模式
 *
 * <p>三个挡位：</p>
 * <ul>
 *   <li>跑图 (exploring) - 玩家正常移动时</li>
 *   <li>战斗 (combat) - 玩家进入战斗时</li>
 *   <li>挂机 (afk) - 玩家挂机时</li>
 * </ul>
 */
public class DynamicViewPlugin extends JavaPlugin {

    private static DynamicViewPlugin instance;

    private ConfigManager configManager;
    private ViewDistanceManager viewDistanceManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 加载配置
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        if (!configManager.isEnabled()) {
            getLogger().info("插件已禁用，跳过启动");
            return;
        }

        // 2. 初始化视距管理器
        viewDistanceManager = new ViewDistanceManager(this);

        // 3. 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 4. 注册命令
        DynamicViewCommand command = new DynamicViewCommand(this);
        getCommand("dynamicview").setExecutor(command);
        getCommand("dynamicview").setTabCompleter(command);

        // 5. 启动定时任务
        startTasks();

        getLogger().info("========================================");
        getLogger().info("GuangDianDynamicView 已启动");
        getLogger().info("全局默认 - 跑图: " + configManager.getDefaultTierViewDistance(ViewTier.EXPLORING)
            + " 战斗: " + configManager.getDefaultTierViewDistance(ViewTier.COMBAT)
            + " 挂机: " + configManager.getDefaultTierViewDistance(ViewTier.AFK));
        getLogger().info("========================================");

        if (configManager.isDebugEnabled()) {
            configManager.printDebugInfo();
        }
    }

    @Override
    public void onDisable() {
        stopTasks();
        getLogger().info("GuangDianDynamicView 已关闭");
        instance = null;
    }

    /**
     * 启动定时任务
     */
    private void startTasks() {
        long interval = configManager.getUpdateInterval();
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            if (viewDistanceManager != null) {
                viewDistanceManager.updateAllPlayers();
            }
        }, 0, interval * 50, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止所有任务
     */
    private void stopTasks() {
        getServer().getScheduler().cancelTasks(this);
    }

    /**
     * 重载配置
     */
    public void reload() {
        configManager.load();
        getLogger().info("配置已重载");

        if (configManager.isDebugEnabled()) {
            configManager.printDebugInfo();
        }
    }

    // Getters
    public static DynamicViewPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public ViewDistanceManager getViewDistanceManager() { return viewDistanceManager; }
}
