package cn.guangdian.holo;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.holo.adapter.HoloServiceAdapter;
import cn.guangdian.holo.api.HologramAPI;
import cn.guangdian.holo.api.HologramAPIImpl;
import cn.guangdian.holo.command.GHoloCommand;
import cn.guangdian.holo.listener.HologramListener;
import cn.guangdian.holo.manager.HologramManager;
import cn.guangdian.holo.papi.HoloPlaceholders;
import cn.guangdian.holo.storage.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class GuangDianHolo extends AbstractRPGPlugin {

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
    protected void onPluginEnable() {
        instance = this;

        if (!hookRPGCore()) {
            getLogger().severe("无法连接到 RPGCore，插件禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        configManager.load();

        hologramManager = new HologramManager(this);
        
        // 先加载已存在世界的全息图
        hologramManager.loadHolograms();
        
        // 延迟加载其他世界的全息图（给世界加载时间）
        scheduleDelayedHologramLoading();

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
    
    /**
     * 延迟加载全息图，确保所有世界都加载完成
     */
    private void scheduleDelayedHologramLoading() {
        // 延迟5秒后检查并加载未加载的全息图
        scheduler.runSyncLater(() -> {
            int pendingCount = configManager.getHologramDataCache().size() - hologramManager.getHologramCount();
            if (pendingCount > 0) {
                getLogger().info("正在延迟加载 " + pendingCount + " 个全息图...");
                
                // 遍历所有已加载的世界，尝试加载对应的全息图
                for (World world : Bukkit.getWorlds()) {
                    hologramManager.loadHologramsForWorld(world);
                }
                
                getLogger().info("延迟加载完成，当前共 " + hologramManager.getHologramCount() + " 个全息图");
            }
        }, 100L); // 延迟5秒 (100 ticks)
    }

    @Override
    protected void onPluginDisable() {
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
        
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianHolo 全息显示插件已禁用！");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianHolo";
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
        
        // 重新延迟加载
        scheduler.runSyncLater(() -> {
            for (World world : Bukkit.getWorlds()) {
                hologramManager.loadHologramsForWorld(world);
            }
        }, 20L);
        
        getLogger().info("配置已重新加载！");
    }
}