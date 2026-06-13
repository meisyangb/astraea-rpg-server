package cn.guangdian.raid;

import cn.guangdian.raid.adapter.RaidServiceAdapter;
import cn.guangdian.raid.api.RaidService;
import cn.guangdian.raid.command.RaidAdminCommand;
import cn.guangdian.raid.command.RaidCommand;
import cn.guangdian.raid.config.RaidConfigManager;
import cn.guangdian.raid.instance.RaidInstanceManager;
import cn.guangdian.raid.listener.RaidListener;
import cn.guangdian.raid.manager.ExtractionManager;
import cn.guangdian.raid.manager.IntelManager;
import cn.guangdian.raid.manager.LootManager;
import cn.guangdian.raid.manager.SpawnManager;
import cn.guangdian.raid.placeholder.RaidPlaceholder;
import cn.guangdian.raid.ui.RaidBoard;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class GuangDianRaid extends AbstractRPGPlugin {

    private static GuangDianRaid instance;

    private RaidConfigManager configManager;
    private RaidInstanceManager instanceManager;
    private RaidServiceAdapter serviceAdapter;
    private RaidPlaceholder placeholder;

    private IntelManager intelManager;
    private ExtractionManager extractionManager;
    private SpawnManager spawnManager;
    private LootManager lootManager;
    private RaidBoard raidBoard;

    @Override
    protected void onPluginEnable() {
        instance = this;

        configManager = new RaidConfigManager(this);
        configManager.loadConfigs();

        instanceManager = new RaidInstanceManager(this);

        intelManager = new IntelManager(this);
        extractionManager = new ExtractionManager(this);
        spawnManager = new SpawnManager(this);
        lootManager = new LootManager(this);
        raidBoard = new RaidBoard(this);

        serviceAdapter = new RaidServiceAdapter(this);

        registerCommands();
        registerListeners();

        if (isExternalServicesAvailable()) {
            placeholder = new RaidPlaceholder(this);
            placeholder.registerExpansion();
        }

        getLogger().info("搜打撤副本系统已启动");
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();
        
        if (instanceManager != null) {
            instanceManager.shutdownAll();
        }

        if (placeholder != null) {
            placeholder.unregisterExpansion();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        getLogger().info("搜打撤副本系统已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianRaid";
    }

    private void registerCommands() {
        RaidCommand raidCommand = new RaidCommand(this);
        RaidAdminCommand adminCommand = new RaidAdminCommand(this);

        getCommand("raid").setExecutor(raidCommand);
        getCommand("raid").setTabCompleter(raidCommand);

        getCommand("raidadmin").setExecutor(adminCommand);
        getCommand("raidadmin").setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);
    }

    public static GuangDianRaid getInstance() {
        return instance;
    }

    public RaidConfigManager getConfigManager() {
        return configManager;
    }

    public RaidInstanceManager getInstanceManager() {
        return instanceManager;
    }

    public IntelManager getIntelManager() {
        return intelManager;
    }

    public ExtractionManager getExtractionManager() {
        return extractionManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public RaidBoard getRaidBoard() {
        return raidBoard;
    }

    public RaidService getService() {
        return serviceAdapter;
    }
}
