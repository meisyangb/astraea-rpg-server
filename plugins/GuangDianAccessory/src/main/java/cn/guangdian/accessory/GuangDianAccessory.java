package cn.guangdian.accessory;

import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.command.AccessoryCommand;
import cn.guangdian.accessory.gui.AccessoryGUI;
import cn.guangdian.accessory.lifecycle.AccessoryDataHandler;
import cn.guangdian.accessory.manager.AccessoryManager;
import cn.guangdian.accessory.placeholder.AccessoryPlaceholder;
import cn.guangdian.accessory.service.AccessoryServiceAdapter;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GuangDianAccessory extends AbstractRPGPlugin {
    
    private AccessoryServiceAdapter serviceAdapter;
    private AccessoryManager accessoryManager;
    private AccessoryGUI accessoryGUI;
    private AccessoryPlaceholder placeholder;
    private AccessoryCommand command;
    
    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        
        accessoryManager = new AccessoryManager(this);
        accessoryManager.loadAccessories();
        
        serviceAdapter = new AccessoryServiceAdapter(this);
        
        AccessoryDataHandler dataHandler = new AccessoryDataHandler(this);
        dataHandler.register();
        
        accessoryGUI = new AccessoryGUI(this);
        getServer().getPluginManager().registerEvents(accessoryGUI, this);
        
        placeholder = new AccessoryPlaceholder(this);
        placeholder.register();
        
        command = new AccessoryCommand(this);
        getCommand("accessory").setExecutor(command);
        
        getLogger().info("饰品系统已启动 - 支持: 徽章、勋章、圣物");
    }
    
    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            placeholder.unregister();
        }
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        getLogger().info("饰品系统已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianAccessory";
    }
    
    public AccessoryService getAccessoryService() {
        return serviceAdapter;
    }
    
    public AccessoryManager getAccessoryManager() {
        return accessoryManager;
    }
    
    public AccessoryGUI getAccessoryGUI() {
        return accessoryGUI;
    }
}
