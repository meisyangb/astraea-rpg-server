package cn.guangdian.world;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
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
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuangDianWorld extends JavaPlugin {

    private static GuangDianWorld instance;
    private ConfigManager configManager;
    private WorldManager worldManager;
    private WorldAPIImpl worldAPIImpl;
    private WorldServiceAdapter serviceAdapter;
    private WorldPlaceholders placeholders;

    private RPGCore rpgCore;
    private AsyncExecutor asyncExecutor;
    private CacheProvider cacheProvider;
    private EventBus eventBus;
    private ServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        instance = this;

        if (!hookRPGCore()) {
            getLogger().severe("无法连接到 RPGCore，插件禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        configManager.load();

        worldManager = new WorldManager(this);
        worldManager.loadAllWorlds();

        worldAPIImpl = new WorldAPIImpl(worldManager);
        
        // 使用服务适配器包装 API（自动注册到 RPGCore）
        serviceAdapter = new WorldServiceAdapter(this, worldAPIImpl);

        getCommand("gworld").setExecutor(new GWorldCommand(this));
        getCommand("gworldtp").setExecutor(new GWorldTPCommand(this));
        getCommand("gspawn").setExecutor(new GSpawnCommand(this));

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        hookPlaceholderAPI();

        getLogger().info("GuangDianWorld 世界管理插件已启用！");
        getLogger().info("已加载世界数量: " + worldManager.getWorldCount());
    }

    @Override
    public void onDisable() {
        // 注销服务适配器
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (placeholders != null) {
            PlaceholderAPI.unregisterExpansion(placeholders);
            placeholders = null;
        }
        
        if (worldManager != null) {
            worldManager.saveAllWorlds();
        }
        getLogger().info("GuangDianWorld 世界管理插件已禁用！");
    }

    private boolean hookRPGCore() {
        var plugin = getServer().getPluginManager().getPlugin("RPGCore");
        if (!(plugin instanceof RPGCore core)) {
            return false;
        }
        
        this.rpgCore = core;
        this.asyncExecutor = core.getAsyncExecutor();
        this.cacheProvider = core.getCacheProvider();
        this.eventBus = core.getEventBus();
        this.serviceRegistry = core.getServiceRegistry();
        
        getLogger().info("已连接到 RPGCore");
        return true;
    }

    private void hookPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new WorldPlaceholders(this);
            if (placeholders.register()) {
                getLogger().info("PlaceholderAPI 扩展已注册");
            }
        }
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

    public RPGCore getRPGCore() {
        return rpgCore;
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
