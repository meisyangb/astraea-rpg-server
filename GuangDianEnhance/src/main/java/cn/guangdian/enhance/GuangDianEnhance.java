package cn.guangdian.enhance;

import cn.guangdian.enhance.adapter.EnhanceServiceAdapter;
import cn.guangdian.enhance.command.EnhanceCommand;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.gui.EnhanceGUI;
import cn.guangdian.enhance.listener.EnhanceListener;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.enhance.stone.EnhanceStoneManager;
import cn.guangdian.enhance.storage.EnhanceStorage;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;

public final class GuangDianEnhance extends AbstractRPGPlugin {

    private static GuangDianEnhance instance;
    
    private EnhanceConfig enhanceConfig;
    private EnhanceStorage enhanceStorage;
    private EnhanceManager enhanceManager;
    private EnhanceServiceAdapter serviceAdapter;
    private EnhanceGUI enhanceGUI;
    private EnhanceStoneManager stoneManager;
    
    private RPGCore rpgCore;
    private SyncScheduler rpgCoreScheduler;
    private ExternalServiceIntegration externalServices;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        initRPGCoreServices();
        
        enhanceConfig = new EnhanceConfig(this);
        enhanceConfig.load();
        
        enhanceStorage = new EnhanceStorage(this);
        enhanceManager = new EnhanceManager(this, enhanceStorage, enhanceConfig);
        
        stoneManager = new EnhanceStoneManager(this);
        
        enhanceGUI = new EnhanceGUI(this, enhanceManager);
        
        getServer().getPluginManager().registerEvents(
            new EnhanceListener(this, enhanceManager), this);
        
        getCommand("enhance").setExecutor(new EnhanceCommand(this, enhanceManager, enhanceGUI, stoneManager));
        
        serviceAdapter = new EnhanceServiceAdapter(this, enhanceManager);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已注册到 RPGCore 服务系统!");
        }
        
        getLogger().info("GuangDianEnhance Plugin Enabled!");
        getLogger().info("强化系统已加载 - 最高等级: " + enhanceConfig.getMaxLevel());
    }

    @Override
    protected void onPluginDisable() {
        if (enhanceGUI != null) {
            enhanceGUI.closeAll();
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (rpgCoreScheduler != null) {
            rpgCoreScheduler.cancelAllTasks();
        }
        
        getLogger().info("GuangDianEnhance Plugin Disabled!");
    }
    
    private void initRPGCoreServices() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    rpgCoreScheduler = rpgCore.getScheduler();
                    externalServices = rpgCore.getExternalServices();
                    miniMessage = rpgCore.getMiniMessageService();
                    getLogger().info("已连接到 RPGCore 核心服务");
                }
            } catch (Exception e) {
                getLogger().warning("连接 RPGCore 服务失败: " + e.getMessage());
            }
        }
        
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            getLogger().info("使用本地 MiniMessageService（降级）");
        }
    }

    @Override
    protected String getPluginName() {
        return "GuangDianEnhance";
    }

    public static GuangDianEnhance getInstance() {
        return instance;
    }

    public EnhanceConfig getEnhanceConfig() {
        return enhanceConfig;
    }

    public EnhanceStorage getEnhanceStorage() {
        return enhanceStorage;
    }

    public EnhanceManager getEnhanceManager() {
        return enhanceManager;
    }

    public EnhanceGUI getEnhanceGUI() {
        return enhanceGUI;
    }

    public EnhanceStoneManager getStoneManager() {
        return stoneManager;
    }

    public RPGCore getRPGCore() {
        return rpgCore;
    }

    public boolean isRPGCoreEnabled() {
        return rpgCore != null;
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }

    public SyncScheduler getScheduler() {
        return rpgCoreScheduler;
    }
}
