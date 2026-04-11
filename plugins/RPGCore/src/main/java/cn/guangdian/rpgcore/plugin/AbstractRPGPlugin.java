package cn.guangdian.rpgcore.plugin;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ExceptionHandler;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 抽象插件基类
 * 
 * 提供统一的：
 * - RPGCore 服务集成
 * - 生命周期管理
 * - 异常处理
 * - 调度器访问
 */
public abstract class AbstractRPGPlugin extends JavaPlugin {
    
    protected RPGCore rpgCore;
    protected ExternalServiceIntegration externalServices;
    protected SyncScheduler scheduler;
    protected ExceptionHandler exceptionHandler;
    
    private boolean initialized = false;
    
    @Override
    public final void onEnable() {
        if (!hookRPGCore()) {
            getLogger().severe("无法连接到 RPGCore，插件无法启动!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        try {
            onPluginEnable();
            initialized = true;
            getLogger().info(getPluginName() + " v" + getDescription().getVersion() + " 已启动");
        } catch (Exception e) {
            getLogger().severe("启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public final void onDisable() {
        if (!initialized) {
            return;
        }
        
        try {
            onPluginDisable();
        } catch (Exception e) {
            getLogger().warning("关闭时发生错误: " + e.getMessage());
        }
        
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    private boolean hookRPGCore() {
        var plugin = Bukkit.getPluginManager().getPlugin("RPGCore");
        if (plugin instanceof RPGCore core) {
            this.rpgCore = core;
            this.externalServices = core.getExternalServices();
            this.scheduler = core.getScheduler();
            this.exceptionHandler = new cn.guangdian.rpgcore.exception.ExceptionHandlerImpl(this);
            
            if (this.externalServices == null) {
                getLogger().warning("[RPGCore Hook] externalServices is null, attempting direct access...");
                this.externalServices = core.getExternalServices();
            }
            
            if (this.externalServices != null) {
                getLogger().info("[RPGCore Hook] Successfully hooked: " + getPluginName());
                getLogger().info("[RPGCore Hook] ExternalServices status: " + this.externalServices.getExternalServiceStatus());
            } else {
                getLogger().severe("[RPGCore Hook] Failed to get ExternalServices!");
            }
            
            return true;
        }
        return false;
    }
    
    protected abstract void onPluginEnable();
    
    protected abstract void onPluginDisable();
    
    protected abstract String getPluginName();
    
    protected <T> T safeCall(java.util.function.Supplier<T> operation, T defaultValue) {
        return exceptionHandler.safeCall(operation, defaultValue);
    }
    
    protected void safeRun(Runnable operation) {
        exceptionHandler.safeRun(operation);
    }
    
    protected boolean isRPGCoreAvailable() {
        return rpgCore != null;
    }
    
    protected boolean isExternalServicesAvailable() {
        return externalServices != null;
    }
    
    protected boolean isSchedulerAvailable() {
        return scheduler != null;
    }
    
    public SyncScheduler getScheduler() {
        return scheduler;
    }
    
    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }
    
    public RPGCore getRPGCore() {
        return rpgCore;
    }
}
