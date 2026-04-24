package cn.guangdian.mobhealth;

import cn.guangdian.mobhealth.adapter.MobHealthServiceAdapter;
import cn.guangdian.mobhealth.command.MobHealthCommand;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class GuangDianMobHealth extends AbstractRPGPlugin implements Listener {

    private static GuangDianMobHealth instance;
    private MobHealthDisplayManager displayManager;
    private MythicMobsHook mythicMobsHook;
    private boolean pluginEnabled = true;
    private boolean debug = false;
    private MobHealthServiceAdapter mobHealthServiceAdapter;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        
        initRPGCoreServices();
        
        pluginEnabled = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        
        mythicMobsHook = new MythicMobsHook(this);
        displayManager = new MobHealthDisplayManager(this, mythicMobsHook);
        
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new MobListener(this, displayManager), this);
        
        // 注册命令 - 使用 RPGCore CommandFramework
        registerCommands();
        
        displayManager.startUpdateTask();
        
        registerRPGCoreService();
        
        getLogger().info("GuangDianMobHealth 已启动");
        getLogger().info("功能: 怪物血量显示 (TextDisplay实体)");
    }
    
    private void initRPGCoreServices() {
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore MiniMessageService 服务");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore MiniMessageService: " + e.getMessage());
            }
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    @Override
    protected void onPluginDisable() {
        unregisterRPGCoreService();
        
        if (displayManager != null) {
            displayManager.stopUpdateTask();
            displayManager.clear();
        }
        
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        getLogger().info("GuangDianMobHealth 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianMobHealth";
    }
    
    private void registerRPGCoreService() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
            if (serviceRegistry != null) {
                mobHealthServiceAdapter = new MobHealthServiceAdapter(this, serviceRegistry);
                mobHealthServiceAdapter.register();
            }
        }
    }
    
    private void unregisterRPGCoreService() {
        if (mobHealthServiceAdapter != null) {
            mobHealthServiceAdapter.unregister();
        }
    }

    public void reloadConfiguration() {
        reloadConfig();
        pluginEnabled = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        if (displayManager != null) {
            displayManager.loadConfig();
            displayManager.clear();
        }
        getLogger().info("配置已重载");
    }

    public static GuangDianMobHealth getInstance() {
        return instance;
    }

    public MobHealthDisplayManager getDisplayManager() {
        return displayManager;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void debug(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }

    /**
     * 注册命令 - 使用 RPGCore CommandFramework
     */
    private void registerCommands() {
        CommandFramework framework = CommandFramework.getInstance();
        if (framework != null) {
            framework.registerCommand(new MobHealthCommand(this));
            getLogger().info("已使用 CommandFramework 注册命令");
        } else {
            getLogger().severe("CommandFramework 不可用，命令注册失败");
        }
    }
}
