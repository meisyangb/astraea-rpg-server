package cn.guangdian.devour;

import cn.guangdian.devour.command.DevourCommand;
import cn.guangdian.devour.config.ConfigManager;
import cn.guangdian.devour.listener.DevourListener;
import cn.guangdian.devour.manager.DevourManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.Bukkit;

/**
 * 吞噬插件主类
 * 
 * 功能: 武器吞噬系统，支持通过GUI吞噬其他武器并累加属性
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class GuangDianDevour extends AbstractRPGPlugin {

    private static GuangDianDevour instance;
    
    // RPGCore 服务
    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected MiniMessageService miniMessage;
    
    // 插件组件
    private ConfigManager configManager;
    private DevourManager devourManager;
    private DevourCommand devourCommand;
    private DevourListener devourListener;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        // 获取 RPGCore 服务
        rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            getLogger().severe("未找到 RPGCore！插件无法启动。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        scheduler = rpgCore.getScheduler();
        miniMessage = MiniMessageService.getInstance();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.load();
        
        // 初始化吞噬管理器
        devourManager = new DevourManager(this);
        
        // 注册命令
        devourCommand = new DevourCommand(this);
        devourCommand.register();
        
        // 注册事件监听器
        devourListener = new DevourListener(this);
        Bukkit.getPluginManager().registerEvents(devourListener, this);
        
        getLogger().info("========================================");
        getLogger().info("  GuangDianDevour 吞噬系统已启动");
        getLogger().info("  版本: " + getPluginMeta().getVersion());
        getLogger().info("  吞噬剑数量: " + configManager.getDevourWeapons().size());
        getLogger().info("========================================");
    }
    
    @Override
    protected void onPluginDisable() {
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        getLogger().info("GuangDianDevour 吞噬系统已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianDevour";
    }
    
    /**
     * 获取插件实例
     */
    public static GuangDianDevour getInstance() {
        return instance;
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取吞噬管理器
     */
    public DevourManager getDevourManager() {
        return devourManager;
    }
    
    /**
     * 获取 MiniMessage 服务
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }
    
    /**
     * 获取调度器
     */
    public SyncScheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * 重载配置
     */
    public void reload() {
        configManager.load();
        getLogger().info("配置已重载");
    }
}
