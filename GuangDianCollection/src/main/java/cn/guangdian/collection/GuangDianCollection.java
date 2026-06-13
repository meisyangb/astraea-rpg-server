package cn.guangdian.collection;

import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.command.CollectionCommand;
import cn.guangdian.collection.config.ConfigManager;
import cn.guangdian.collection.data.CollectionDataHandler;
import cn.guangdian.collection.gui.CollectionGUIListener;
import cn.guangdian.collection.papi.CollectionPlaceholder;
import cn.guangdian.collection.service.CollectionServiceImpl;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.command.PluginCommand;

public class GuangDianCollection extends AbstractRPGPlugin {
    
    private ConfigManager configManager;
    private CollectionService collectionService;
    private CollectionDataHandler dataHandler;
    private CollectionPlaceholder placeholder;
    private CollectionGUIListener guiListener;
    private MiniMessageService miniMessage;
    
    @Override
    protected void onPluginEnable() {
        miniMessage = MiniMessageService.getInstance();
        
        saveDefaultConfig();
        saveResource("collections.yml", false);
        
        configManager = new ConfigManager(this);
        collectionService = new CollectionServiceImpl(this);
        
        dataHandler = new CollectionDataHandler(this);
        dataHandler.register();
        
        registerListeners();
        registerCommands();
        registerPlaceholder();
        
        getLogger().info("图鉴收集系统已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        // ✅ 保存所有缓存中的数据
        if (collectionService != null) {
            collectionService.saveAllPlayerData();
            getLogger().info("已保存所有玩家数据");
        }
        
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        if (placeholder != null) {
            me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(placeholder);
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
        PluginCommand cmd = getCommand("collection");
        if (cmd != null) {
            CollectionCommand command = new CollectionCommand(this, collectionService, guiListener);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }
    }
    
    private void registerPlaceholder() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholder = new CollectionPlaceholder(this, collectionService);
            placeholder.register();
            getLogger().info("已注册 PlaceholderAPI 占位符");
        }
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
    
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }
}
