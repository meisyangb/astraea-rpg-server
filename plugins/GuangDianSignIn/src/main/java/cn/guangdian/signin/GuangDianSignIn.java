package cn.guangdian.signin;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.command.CommandFramework;
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
    private CommandFramework commandFramework;
    
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
        
        initCommandFramework();
        registerPlaceholder();
        
        getLogger().info("签到系统已启动");
    }
    
    /**
     * 初始化 RPGCore CommandFramework
     */
    private void initCommandFramework() {
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry.hasService(CommandFramework.class)) {
                commandFramework = registry.getService(CommandFramework.class);
                commandFramework.registerCommand(new SignInCommand(this));
                getLogger().info("已注册 RPGCore CommandFramework 命令");
            } else {
                getLogger().warning("CommandFramework 不可用，使用备用命令注册");
                registerCommandsFallback();
            }
        } else {
            getLogger().warning("RPGCore 不可用，使用备用命令注册");
            registerCommandsFallback();
        }
    }
    
    /**
     * 备用命令注册（当 RPGCore 不可用时）
     */
    private void registerCommandsFallback() {
        org.bukkit.command.PluginCommand cmd = getCommand("signin");
        if (cmd != null) {
            SignInCommand command = new SignInCommand(this);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }
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
        
        if (commandFramework != null) {
            commandFramework.unregisterAll();
        }
        
        getLogger().info("签到系统已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianSignIn";
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
    
    public CommandFramework getCommandFramework() {
        return commandFramework;
    }
}
