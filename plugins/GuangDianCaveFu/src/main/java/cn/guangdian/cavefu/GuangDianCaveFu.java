package cn.guangdian.cavefu;

import cn.guangdian.cavefu.adapter.CaveServiceAdapter;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.command.CaveAdminCommand;
import cn.guangdian.cavefu.command.CaveCommand;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.placeholder.CavePlaceholder;
import cn.guangdian.cavefu.protection.ProtectionListener;
import cn.guangdian.cavefu.storage.DataManager;
import cn.guangdian.cavefu.upgrade.UpgradeManager;
import cn.guangdian.cavefu.world.CaveWorldManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * 光点洞府插件 - GuangDianCaveFu
 *
 * <p>RPGCore 服务集成:
 * <ul>
 *   <li>GameLogger: 使用 RPGCore 统一日志服务</li>
 *   <li>ExternalServiceIntegration: 使用 RPGCore 外部服务集成</li>
 * </ul>
 *
 * <p>优先级模式: 优先使用 RPGCore 服务，不可用则降级到本地实现
 *
 * @author Gumin
 * @QQ 2271257344
 * @version 1.0.0
 */
public final class GuangDianCaveFu extends AbstractRPGPlugin {

    private static GuangDianCaveFu instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CaveWorldManager worldManager;
    private CaveManager caveManager;
    private UpgradeManager upgradeManager;
    private CavePlaceholder placeholderExpansion;
    private CaveServiceAdapter serviceAdapter;
    private ExternalServiceIntegration externalServices;

    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        // 初始化配置
        configManager = new ConfigManager(this);
        configManager.load();

        // 初始化数据存储
        dataManager = new DataManager(this);
        dataManager.load();

        // 初始化世界管理（包含LuckPerms权限继承配置）
        worldManager = new CaveWorldManager(this);
        worldManager.init();

        // 初始化洞府管理
        caveManager = new CaveManager(this);

        // 初始化升级管理
        upgradeManager = new UpgradeManager(this);

        // 注册命令
        getCommand("cave").setExecutor(new CaveCommand(this));
        getCommand("caveadmin").setExecutor(new CaveAdminCommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // 注册PlaceholderAPI
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new CavePlaceholder(this);
            placeholderExpansion.register();
            logInfo("已注册PlaceholderAPI扩展: gdcave");
        }

        // 注册RPGCore服务适配器
        serviceAdapter = new CaveServiceAdapter(this);
        
        // 连接RPGCore外部服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                externalServices = rpgCore.getExternalServices();
                logInfo("已连接到 RPGCore 外部服务");
            }
        }

        logInfo("GuangDianCaveFu 洞府插件已启用！");
        logInfo("当前洞府数量: " + dataManager.getCaveCount());
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                gameLogger = rpgCore.getGameLogger();
                miniMessage = rpgCore.getMiniMessageService();
                logInfo("使用 RPGCore GameLogger 和 MiniMessageService 服务");
            } catch (Exception e) {
                logWarning("无法获取 RPGCore 服务: " + e.getMessage());
            }
        }

        if (gameLogger == null) {
            gameLogger = new GameLogger() {
                @Override
                public void info(String message) { getLogger().info(message); }
                @Override
                public void warning(String message) { getLogger().warning(message); }
                @Override
                public void severe(String message) { getLogger().severe(message); }
                @Override
                public void debug(String message) { getLogger().info("[DEBUG] " + message); }
                @Override
                public int getQueueSize() { return 0; }
                @Override
                public long getTotalLogged() { return 0; }
                @Override
                public long getTotalDropped() { return 0; }
                @Override
                public void shutdown() { }
            };
        }

        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
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
        // 取消所有调度任务
        cancelAllTasks();
        
        if (dataManager != null) {
            dataManager.saveSyncAndAwait();
        }

        // 注销 PlaceholderAPI 扩展
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        logInfo("GuangDianCaveFu 洞府插件已禁用！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianCaveFu";
    }

    public static GuangDianCaveFu getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CaveWorldManager getWorldManager() {
        return worldManager;
    }

    public CaveManager getCaveManager() {
        return caveManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例（可能为本地降级实现）
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }

    public void reloadAll() {
        configManager.reload();
        dataManager.load();
        logInfo("配置已重新加载！");
    }
}