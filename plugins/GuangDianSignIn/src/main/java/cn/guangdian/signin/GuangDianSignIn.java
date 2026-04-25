package cn.guangdian.signin;

import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.signin.adapter.SignInServiceAdapter;
import cn.guangdian.signin.api.SignInService;
import cn.guangdian.signin.command.SignInCommand;
import cn.guangdian.signin.config.ConfigManager;
import cn.guangdian.signin.lifecycle.SignInDataHandler;
import cn.guangdian.signin.placeholder.SignInPlaceholder;
import org.bukkit.command.PluginCommand;

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
        dataHandler.initialize();
        if (rpgCore != null) {
            rpgCore.getPlayerLifecycle().registerHandler(dataHandler);
        }
        
        serviceAdapter = new SignInServiceAdapter(this);
        
        registerCommands();
        registerPlaceholder();
        
        getLogger().info("签到系统已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            placeholder.unregister();
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
    
    private void registerCommands() {
        PluginCommand cmd = getCommand("signin");
        if (cmd != null) {
            SignInCommand command = new SignInCommand(this);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }
    }
    
    private void registerPlaceholder() {
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            placeholder = new SignInPlaceholder(this);
            placeholder.register();
            getLogger().info("已注册 PlaceholderAPI 占位符");
        }
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
