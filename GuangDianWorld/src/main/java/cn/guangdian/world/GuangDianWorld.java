package cn.guangdian.world;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.world.adapter.WorldServiceAdapter;
import cn.guangdian.world.api.WorldAPI;
import cn.guangdian.world.api.WorldAPIImpl;
import cn.guangdian.world.command.GSpawnCommand;
import cn.guangdian.world.command.GWorldCommand;
import cn.guangdian.world.command.GWorldTPCommand;
import cn.guangdian.world.listener.WorldListener;
import cn.guangdian.world.manager.WorldManager;
import cn.guangdian.world.papi.WorldPlaceholders;
import cn.guangdian.world.storage.ConfigManager;
import cn.guangdian.world.storage.WorldStorage;

public final class GuangDianWorld extends AbstractRPGPlugin {

    private static GuangDianWorld instance;
    private ConfigManager configManager;
    private WorldManager worldManager;
    private WorldStorage worldStorage;
    private WorldAPIImpl worldAPIImpl;
    private WorldServiceAdapter serviceAdapter;
    private WorldPlaceholders placeholders;

    // RPGCore 服务 - 通过父类获取
    private AsyncExecutor asyncExecutor;
    private CacheProvider cacheProvider;
    private EventBus eventBus;
    private ServiceRegistry serviceRegistry;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 使用父类提供的方法检查 RPGCore 是否可用
        if (!isRPGCoreAvailable()) {
            getLogger().severe("无法连接到 RPGCore，插件禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 从父类获取 RPGCore 实例和服务
        RPGCore core = getRPGCore();
        this.asyncExecutor = core.getAsyncExecutor();
        this.cacheProvider = core.getCacheProvider();
        this.eventBus = core.getEventBus();
        this.serviceRegistry = core.getServiceRegistry();

        getLogger().info("已连接到 RPGCore");

        configManager = new ConfigManager(this);
        configManager.load();

        worldStorage = new WorldStorage(this);
        if (worldStorage.init()) worldStorage.load();
        
        worldManager = new WorldManager(this);
        worldManager.loadAllWorlds();

        worldAPIImpl = new WorldAPIImpl(worldManager);
        
        // 使用服务适配器包装 API（自动注册到 RPGCore）
        serviceAdapter = new WorldServiceAdapter(this, worldAPIImpl);

        getCommand("gworld").setExecutor(new GWorldCommand(this));
        getCommand("gworldtp").setExecutor(new GWorldTPCommand(this));
        getCommand("gspawn").setExecutor(new GSpawnCommand(this));

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        // 注册占位符到 RPGCore PlaceholderService
        registerPlaceholders();

        getLogger().info("GuangDianWorld 世界管理插件已启用！");
        getLogger().info("已加载世界数量: " + worldManager.getWorldCount());
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        
        if (worldManager != null) {
            worldManager.saveAllWorlds();
        }
        getLogger().info("GuangDianWorld 世界管理插件已禁用！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianWorld";
    }

    private void registerPlaceholders() {
        placeholders = new WorldPlaceholders(this);
        placeholders.register();
    }

    public static GuangDianWorld getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public WorldAPI getWorldAPI() {
        return serviceAdapter != null ? serviceAdapter : worldAPIImpl;
    }

    public WorldServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public CacheProvider getCacheProvider() {
        return cacheProvider;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void reloadAll() {
        configManager.reload();
        worldManager.loadAllWorlds();
        getLogger().info("配置已重新加载！");
    }
}
