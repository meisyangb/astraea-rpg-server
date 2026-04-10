package cn.guangdian.mobhealth;

import cn.guangdian.mobhealth.adapter.MobHealthServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
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
import org.bukkit.plugin.java.JavaPlugin;

public class GuangDianMobHealth extends JavaPlugin implements Listener {

    private static GuangDianMobHealth instance;
    private MobHealthDisplayManager displayManager;
    private MythicMobsHook mythicMobsHook;
    private boolean pluginEnabled = true;
    private boolean debug = false;
    private MobHealthServiceAdapter mobHealthServiceAdapter;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        pluginEnabled = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        
        mythicMobsHook = new MythicMobsHook(this);
        displayManager = new MobHealthDisplayManager(this, mythicMobsHook);
        
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new MobListener(this, displayManager), this);
        
        getCommand("gdmobhealth").setExecutor(new AdminCommand(this));
        
        displayManager.startUpdateTask();
        
        registerRPGCoreService();
        
        getLogger().info("GuangDianMobHealth 已启动");
        getLogger().info("功能: 怪物血量显示 (TextDisplay实体)");
    }

    @Override
    public void onDisable() {
        unregisterRPGCoreService();
        
        if (displayManager != null) {
            displayManager.stopUpdateTask();
            displayManager.clear();
        }
        
        getLogger().info("GuangDianMobHealth 已关闭");
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
}
