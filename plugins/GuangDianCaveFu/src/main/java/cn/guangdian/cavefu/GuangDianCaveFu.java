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
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 光点洞府插件主类
 */
public final class GuangDianCaveFu extends JavaPlugin {

    private static GuangDianCaveFu instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CaveWorldManager worldManager;
    private CaveManager caveManager;
    private UpgradeManager upgradeManager;
    private CavePlaceholder placeholderExpansion;
    private CaveServiceAdapter serviceAdapter;
    private ExternalServiceIntegration externalServices;

    @Override
    public void onEnable() {
        instance = this;

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
            getLogger().info("已注册PlaceholderAPI扩展: gdcave");
        }

        // 注册RPGCore服务适配器
        serviceAdapter = new CaveServiceAdapter(this);
        
        // 连接RPGCore外部服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                externalServices = rpgCore.getExternalServices();
                getLogger().info("已连接到 RPGCore 外部服务");
            }
        }

        getLogger().info("GuangDianCaveFu 洞府插件已启用！");
        getLogger().info("当前洞府数量: " + dataManager.getCaveCount());
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveSyncAndAwait();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion = null;
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        getLogger().info("GuangDianCaveFu 洞府插件已禁用！");
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

    public void reloadAll() {
        configManager.reload();
        dataManager.load();
        getLogger().info("配置已重新加载！");
    }
}