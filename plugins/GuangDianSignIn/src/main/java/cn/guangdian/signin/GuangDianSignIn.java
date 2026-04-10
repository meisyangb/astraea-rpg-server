package cn.guangdian.signin;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.signin.adapter.SignInServiceAdapter;
import cn.guangdian.signin.api.SignInService;
import cn.guangdian.signin.command.SignInCommand;
import cn.guangdian.signin.config.ConfigManager;
import cn.guangdian.signin.lifecycle.SignInDataHandler;
import cn.guangdian.signin.placeholder.SignInPlaceholder;

public class GuangDianSignIn extends AbstractRPGPlugin {
    
    private SignInServiceAdapter serviceAdapter;
    private SignInDataHandler dataHandler;
    private ConfigManager configManager;
    private SignInPlaceholder placeholder;
    
    @Override
    protected void onPluginEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        dataHandler = new SignInDataHandler(this);
        if (rpgCore != null) {
            rpgCore.getPlayerLifecycle().registerHandler(dataHandler);
        }
        
        serviceAdapter = new SignInServiceAdapter(this);
        
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            placeholder = new SignInPlaceholder(this);
            placeholder.register();
        }
        
        SignInCommand command = new SignInCommand(this);
        getCommand("signin").setExecutor(command);
        getCommand("signin").setTabCompleter(command);
        
        getLogger().info("签到系统已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
                try {
                    me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(placeholder);
                } catch (Exception e) {
                    getLogger().warning("注销占位符失败: " + e.getMessage());
                }
            }
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        getLogger().info("签到系统已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianSignIn";
    }
    
    public SignInService getSignInService() {
        return serviceAdapter;
    }
    
    public SignInDataHandler getDataHandler() {
        return dataHandler;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
