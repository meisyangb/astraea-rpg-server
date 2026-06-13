package cn.guangdian.rpgcore.plugin;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ExceptionHandler;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 抽象插件基类
 * 
 * <p>提供统一的：</p>
 * <ul>
 *   <li>RPGCore 服务集成</li>
 *   <li>生命周期管理</li>
 *   <li>异常处理</li>
 *   <li>调度器访问</li>
 *   <li>通用服务初始化 (MiniMessage, SoundService等)</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * public class MyPlugin extends AbstractRPGPlugin {
 *     private MiniMessageService miniMessage;
 *     private SoundService soundService;
 *     
 *     @Override
 *     protected void onPluginEnable() {
 *         // 自动初始化通用服务
 *         initCommonServices();
 *         
 *         // 插件特定逻辑
 *         getLogger().info("MyPlugin enabled!");
 *     }
 *     
 *     @Override
 *     protected void onPluginDisable() {
 *         // 清理资源
 *     }
 *     
 *     @Override
 *     protected String getPluginName() {
 *         return "MyPlugin";
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractRPGPlugin extends JavaPlugin {
    
    protected RPGCore rpgCore;
    protected ExternalServiceIntegration externalServices;
    protected SyncScheduler scheduler;
    protected ExceptionHandler exceptionHandler;
    
    // 常用服务 - 由 initCommonServices() 自动初始化
    protected MiniMessageService miniMessageService;
    protected MiniMessage miniMessageParser;
    protected SoundService soundService;
    
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
    
    /**
     * 初始化通用服务 (MiniMessage, SoundService等)
     * 
     * <p>此方法会自动从 RPGCore 获取常用服务，如果 RPGCore 不可用则使用降级方案。</p>
     * <p>建议在 {@link #onPluginEnable()} 的开头调用此方法。</p>
     */
    protected void initCommonServices() {
        // 初始化 MiniMessage 服务
        if (rpgCore != null) {
            miniMessageService = rpgCore.getMiniMessageService();
            soundService = rpgCore.getSoundService();
        }
        
        // 降级方案
        if (miniMessageService == null) {
            miniMessageService = MiniMessageService.getInstance();
        }
        if (miniMessageParser == null && miniMessageService != null) {
            miniMessageParser = miniMessageService.getMiniMessage();
        }
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        
        getLogger().info("通用服务已初始化: MiniMessage=" + (miniMessageService != null) + 
                        ", SoundService=" + (soundService != null));
    }
    
    /**
     * 安全地取消所有调度任务
     * 
     * <p>在插件禁用时调用，确保所有任务被正确取消。</p>
     */
    protected void cancelAllTasks() {
        if (scheduler != null) {
            try {
                scheduler.cancelAllTasks();
                getLogger().fine("所有调度任务已取消");
            } catch (Exception e) {
                getLogger().warning("取消任务时发生错误: " + e.getMessage());
            }
        }
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
    
    /**
     * 获取 MiniMessage 服务
     * @return MiniMessageService 实例
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessageService;
    }
    
    /**
     * 获取 MiniMessage 解析器
     * @return MiniMessage 实例
     */
    public MiniMessage getMiniMessageParser() {
        return miniMessageParser;
    }
    
    /**
     * 获取音效服务
     * @return SoundService 实例
     */
    public SoundService getSoundService() {
        return soundService;
    }
}
