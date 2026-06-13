package cn.guangdian.worldrules;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.worldrules.adapter.WorldRulesServiceAdapter;
import cn.guangdian.worldrules.api.WorldRulesAPI;
import cn.guangdian.worldrules.api.WorldRulesAPIImpl;
import cn.guangdian.worldrules.command.WorldRulesCommand;
import cn.guangdian.worldrules.listener.ChunkLoadListener;
import cn.guangdian.worldrules.listener.RegionEnterListener;
import cn.guangdian.worldrules.listener.RegionSelectionListener;
import cn.guangdian.worldrules.listener.WorldRulesListener;
import cn.guangdian.worldrules.manager.RegionManager;
import cn.guangdian.worldrules.manager.WorldRulesManager;
import cn.guangdian.worldrules.storage.ConfigManager;

public final class GuangDianWorldRules extends AbstractRPGPlugin {

    private static GuangDianWorldRules instance;
    private ConfigManager configManager;
    private WorldRulesManager worldRulesManager;
    private RegionManager regionManager;
    private WorldRulesAPIImpl worldRulesAPIImpl;
    private WorldRulesServiceAdapter serviceAdapter;
    private RegionSelectionListener regionSelectionListener;
    private RegionEnterListener regionEnterListener;
    private ChunkLoadListener chunkLoadListener;

    @Override
    protected void onPluginEnable() {
        instance = this;

        if (!isRPGCoreAvailable()) {
            getLogger().severe("无法连接到 RPGCore，插件禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RPGCore core = getRPGCore();
        getLogger().info("已连接到 RPGCore");

        configManager = new ConfigManager(this);
        configManager.load();

        worldRulesManager = new WorldRulesManager(this);
        worldRulesManager.loadRules();

        regionManager = new RegionManager(this);
        regionManager.loadRegions();

        worldRulesAPIImpl = new WorldRulesAPIImpl(worldRulesManager);
        serviceAdapter = new WorldRulesServiceAdapter(this, worldRulesAPIImpl);

        getCommand("gworldrules").setExecutor(new WorldRulesCommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new WorldRulesListener(this), this);
        regionSelectionListener = new RegionSelectionListener(this);
        getServer().getPluginManager().registerEvents(regionSelectionListener, this);
        regionEnterListener = new RegionEnterListener(this);
        getServer().getPluginManager().registerEvents(regionEnterListener, this);
        chunkLoadListener = new ChunkLoadListener(this);
        getServer().getPluginManager().registerEvents(chunkLoadListener, this);
        chunkLoadListener.loadFromConfig();

        getLogger().info("GuangDianWorldRules 世界规则插件已启用！");
        getLogger().info("已加载 " + worldRulesManager.getWorldRulesCount() + " 个世界的规则配置");
        getLogger().info("已加载 " + regionManager.getRegionCount() + " 个保护区域");
    }

    @Override
    protected void onPluginDisable() {
        cancelAllTasks();

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        if (worldRulesManager != null) {
            worldRulesManager.saveRules();
        }

        if (regionManager != null) {
            regionManager.saveRegions();
        }

        getLogger().info("GuangDianWorldRules 世界规则插件已禁用！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianWorldRules";
    }

    public static GuangDianWorldRules getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldRulesManager getWorldRulesManager() {
        return worldRulesManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public WorldRulesAPI getWorldRulesAPI() {
        return serviceAdapter != null ? serviceAdapter : worldRulesAPIImpl;
    }

    public WorldRulesServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    public RegionSelectionListener getRegionSelectionListener() {
        return regionSelectionListener;
    }

    public RegionEnterListener getRegionEnterListener() {
        return regionEnterListener;
    }

    public ChunkLoadListener getChunkLoadListener() {
        return chunkLoadListener;
    }

    public void reloadAll() {
        configManager.reload();
        worldRulesManager.loadRules();
        regionManager.loadRegions();
        chunkLoadListener.loadFromConfig();
        getLogger().info("配置已重新加载！");
    }
}
