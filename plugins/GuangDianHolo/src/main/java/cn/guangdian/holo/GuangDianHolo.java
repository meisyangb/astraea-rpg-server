package cn.guangdian.holo;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.holo.adapter.HoloServiceAdapter;
import cn.guangdian.holo.api.HologramAPI;
import cn.guangdian.holo.api.HologramAPIImpl;
import cn.guangdian.holo.command.GHoloCommand;
import cn.guangdian.holo.listener.HologramListener;
import cn.guangdian.holo.manager.HologramManager;
import cn.guangdian.holo.papi.HoloPlaceholders;
import cn.guangdian.holo.storage.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuangDianHolo extends JavaPlugin {

    private static GuangDianHolo instance;
    private ConfigManager configManager;
    private HologramManager hologramManager;
    private HologramAPIImpl hologramAPIImpl;
    private HoloServiceAdapter serviceAdapter;
    private HoloPlaceholders placeholders;

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

        hologramManager = new HologramManager(this);
        hologramManager.loadHolograms();

        hologramAPIImpl = new HologramAPIImpl(hologramManager);
        
        // 使用服务适配器包装 API（自动注册到 RPGCore）
        serviceAdapter = new HoloServiceAdapter(this, hologramAPIImpl);

        getCommand("gholo").setExecutor(new GHoloCommand(this));

        getServer().getPluginManager().registerEvents(new HologramListener(this), this);

        hookPlaceholderAPI();

        hologramManager.startUpdateTask();

        getLogger().info("GuangDianHolo 全息显示插件已启用！");
        getLogger().info("已加载全息显示数量: " + hologramManager.getHologramCount());
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

        if (hologramManager != null) {
            hologramManager.stopUpdateTask();
            hologramManager.saveHolograms();
            hologramManager.removeAllHolograms();
        }
        getLogger().info("GuangDianHolo 全息显示插件已禁用！");
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
            placeholders = new HoloPlaceholders(this);
            if (placeholders.register()) {
                getLogger().info("PlaceholderAPI 扩展已注册");
            }
        }
    }

    public static GuangDianHolo getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public HologramAPI getHologramAPI() {
        return serviceAdapter != null ? serviceAdapter : hologramAPIImpl;
    }

    public HoloServiceAdapter getServiceAdapter() {
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
        hologramManager.reloadHolograms();
        getLogger().info("配置已重新加载！");
    }
}
