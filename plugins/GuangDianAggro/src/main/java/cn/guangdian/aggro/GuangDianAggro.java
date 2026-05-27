package cn.guangdian.aggro;

import cn.guangdian.aggro.adapter.AggroServiceAdapter;
import cn.guangdian.aggro.hook.MythicMobsHook;
import cn.guangdian.aggro.listener.AggroListener;
import cn.guangdian.aggro.manager.AggroManager;
import cn.guangdian.aggro.placeholder.AggroPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

/**
 * GuangDianAggro - 光点仇恨系统插件
 *
 * <p>本插件已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志系统 - 使用 RPGCore GameLogger（带降级兼容）</li>
 *   <li>任务调度 - 使用 RPGCore SyncScheduler</li>
 * </ul>
 *
 * <p>当 RPGCore 不可用时，自动降级到 Bukkit 原生实现。</p>
 *
 * @author GuangDian
 * @version 1.1.0
 * @since 1.0.0
 * @see OPTIMIZATION.md 优化详情
 */
public class GuangDianAggro extends AbstractRPGPlugin {

    private static GuangDianAggro instance;

    private AggroManager aggroManager;
    private MythicMobsHook mythicMobsHook;
    private AggroServiceAdapter serviceAdapter;
    private AggroPlaceholder placeholder;

    // RPGCore 服务
    private GameLogger gameLogger;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();

        mythicMobsHook = new MythicMobsHook();
        mythicMobsHook.init();

        aggroManager = new AggroManager(this, mythicMobsHook);
        aggroManager.loadConfig();

        getServer().getPluginManager().registerEvents(new AggroListener(this, aggroManager), this);

        serviceAdapter = new AggroServiceAdapter(this, aggroManager);

        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            placeholder = new AggroPlaceholder(this, aggroManager);
            placeholder.register();
        }

        logInfo("GuangDianAggro 仇恨系统插件已启用!");
        logInfo("MythicMobs 集成: " + (mythicMobsHook.isEnabled() ? "已启用" : "未启用"));
    }

    /**
     * 初始化 RPGCore 服务
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.gameLogger = rpgCore.getGameLogger();
        } else {
            // 降级：使用 Bukkit 原生
            getLogger().warning("RPGCore 不可用，使用备用日志");
        }
    }

    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            PlaceholderAPI.unregisterExpansion(placeholder);
            placeholder = null;
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        if (aggroManager != null) {
            aggroManager.stopDecayTask();
            aggroManager.clearAll();
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        logInfo("GuangDianAggro 仇恨系统插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianAggro";
    }

    // ==================== RPGCore 服务访问 ====================

    public GameLogger getGameLogger() {
        return gameLogger;
    }

    /**
     * 检查是否使用 RPGCore 服务
     */
    public boolean isUsingRPGCore() {
        return gameLogger != null;
    }

    // ==================== 日志快捷方法 ====================

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

    public void logSevere(String message, Throwable throwable) {
        if (gameLogger != null) {
            gameLogger.severe(message, throwable);
        } else {
            getLogger().severe(message + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        }
        // 降级：DEBUG 级别不输出到 Bukkit 原生日志
    }

    // ==================== 业务方法 ====================

    public static GuangDianAggro getInstance() {
        return instance;
    }

    public AggroManager getAggroManager() {
        return aggroManager;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public RPGCore getRPGCore() {
        return rpgCore;
    }
}
