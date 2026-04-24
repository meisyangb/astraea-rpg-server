package cn.guangdian.collection;

import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.command.CollectionCommand;
import cn.guangdian.collection.config.ConfigManager;
import cn.guangdian.collection.data.CollectionDataHandler;
import cn.guangdian.collection.gui.CollectionGUIListener;
import cn.guangdian.collection.papi.CollectionPlaceholder;
import cn.guangdian.collection.service.CollectionServiceImpl;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;

public class GuangDianCollection extends AbstractRPGPlugin {
    
    private ConfigManager configManager;
    private CollectionService collectionService;
    private CollectionDataHandler dataHandler;
    private CollectionPlaceholder placeholder;
    private CollectionGUIListener guiListener;
    private long autoSaveTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        saveResource("collections.yml", false);
        
        configManager = new ConfigManager(this);
        collectionService = new CollectionServiceImpl(this);
        
        dataHandler = new CollectionDataHandler(this, collectionService);
        dataHandler.register();
        
        registerListeners();
        registerCommands();
        registerPlaceholder();
        startAutoSave();
        
        getLogger().info("图鉴收集系统已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        if (autoSaveTaskId != -1 && scheduler != null) {
            scheduler.cancelTask(autoSaveTaskId);
        }
        
        if (collectionService != null) {
            collectionService.saveAllPlayerData();
        }
        
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        if (placeholder != null) {
            placeholder.unregister();
        }
        
        getLogger().info("图鉴收集系统已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianCollection";
    }
    
    private void registerListeners() {
        guiListener = new CollectionGUIListener(this, collectionService);
        getServer().getPluginManager().registerEvents(guiListener, this);
    }
    
    private void registerCommands() {
        // 使用 RPGCore CommandFramework 注册命令
        CommandFramework framework = CommandFramework.getInstance();
        if (framework != null) {
            framework.registerCommand(new CollectionCommand(this, collectionService, guiListener));
            getLogger().info("已使用 CommandFramework 注册命令");
        } else {
            getLogger().severe("CommandFramework 不可用，命令注册失败");
        }
    }
    
    private void registerPlaceholder() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholder = new CollectionPlaceholder(this, collectionService);
            placeholder.register();
            getLogger().info("已注册 PlaceholderAPI 占位符");
        }
    }
    
    private void startAutoSave() {
        if (scheduler == null) return;
        
        long interval = (long) configManager.getSaveInterval() * 20L;
        autoSaveTaskId = scheduler.runSyncRepeating(() -> {
            collectionService.saveAllPlayerData();
        }, interval, interval);
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CollectionService getCollectionService() {
        return collectionService;
    }
    
    public CollectionGUIListener getGuiListener() {
        return guiListener;
    }
}
