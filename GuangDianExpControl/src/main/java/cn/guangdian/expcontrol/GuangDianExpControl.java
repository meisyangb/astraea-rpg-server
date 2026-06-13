package cn.guangdian.expcontrol;

import cn.guangdian.expcontrol.api.ExpControlService;
import cn.guangdian.expcontrol.command.ExpControlCommand;
import cn.guangdian.expcontrol.listener.ExpBlockListener;
import cn.guangdian.expcontrol.service.ExpControlServiceImpl;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

/**
 * 经验控制插件主类
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>拦截所有经验获取事件</li>
 *   <li>提供API供其他插件发放经验</li>
 *   <li>提供管理员命令手动管理经验</li>
 * </ul>
 */
public class GuangDianExpControl extends AbstractRPGPlugin {
    
    private static GuangDianExpControl instance;
    
    private ExpControlServiceImpl expService;
    private ExpBlockListener expBlockListener;
    
    private boolean enabled;
    private boolean blockAllExp;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        // 加载配置
        saveDefaultConfig();
        reloadConfig();
        loadConfig();
        
        // 初始化服务
        expService = new ExpControlServiceImpl(this);
        
        // 注册服务到Bukkit ServiceManager
        getServer().getServicesManager().register(ExpControlService.class, expService, this, ServicePriority.Normal);
        
        // 注册监听器
        expBlockListener = new ExpBlockListener(this, expService);
        getServer().getPluginManager().registerEvents(expBlockListener, this);
        
        // 注册命令
        registerCommands();
        
        getLogger().info("GuangDianExpControl 经验控制插件已启动!");
        getLogger().info("经验拦截状态: " + (blockAllExp ? "已启用" : "已禁用"));
    }
    
    @Override
    protected void onPluginDisable() {
        // 注销服务
        if (expService != null) {
            getServer().getServicesManager().unregister(expService);
        }
        
        // 取消所有任务
        cancelAllTasks();
        
        getLogger().info("GuangDianExpControl 经验控制插件已关闭!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianExpControl";
    }
    
    private void loadConfig() {
        enabled = getConfig().getBoolean("enabled", true);
        blockAllExp = getConfig().getBoolean("block-all-exp", true);
    }
    
    private void registerCommands() {
        var cmd = getCommand("expcontrol");
        if (cmd != null) {
            ExpControlCommand commandHandler = new ExpControlCommand(this, expService);
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        reloadConfig();
        loadConfig();
    }
    
    /**
     * 获取插件实例
     */
    public static GuangDianExpControl getInstance() {
        return instance;
    }
    
    /**
     * 获取经验控制服务
     */
    public ExpControlService getExpService() {
        return expService;
    }
    
    /**
     * 是否启用经验拦截
     */
    public boolean isBlockAllExp() {
        return enabled && blockAllExp;
    }
    
    /**
     * 设置是否拦截所有经验
     */
    public void setBlockAllExp(boolean block) {
        this.blockAllExp = block;
        getConfig().set("block-all-exp", block);
        saveConfig();
    }
}
