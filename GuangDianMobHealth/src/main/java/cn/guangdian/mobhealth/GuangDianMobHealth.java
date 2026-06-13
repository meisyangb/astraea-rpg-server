package cn.guangdian.mobhealth;

import cn.guangdian.mobhealth.adapter.MobHealthServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
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
    private ActionBarHealthDisplay actionBarDisplay;
    private MythicMobsHook mythicMobsHook;
    private boolean pluginEnabled = true;
    private boolean debug = false;
    private MobHealthServiceAdapter mobHealthServiceAdapter;
    private MiniMessageService miniMessage;

    public enum DisplayMode {
        HOLOGRAM, ACTIONBAR, BOTH
    }
    private DisplayMode displayMode = DisplayMode.ACTIONBAR;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();

        initRPGCoreServices();

        pluginEnabled = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        loadDisplayMode();

        mythicMobsHook = new MythicMobsHook(this);

        // 根据显示模式初始化
        if (displayMode == DisplayMode.HOLOGRAM || displayMode == DisplayMode.BOTH) {
            displayManager = new MobHealthDisplayManager(this, mythicMobsHook);
            displayManager.startUpdateTask();
        }

        if (displayMode == DisplayMode.ACTIONBAR || displayMode == DisplayMode.BOTH) {
            actionBarDisplay = new ActionBarHealthDisplay(this, mythicMobsHook);
            actionBarDisplay.startCleanupTask();
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new MobListener(this, displayManager, actionBarDisplay), this);

        getCommand("gdmobhealth").setExecutor(new AdminCommand(this));

        registerRPGCoreService();

        getLogger().info("GuangDianMobHealth 已启动");
        getLogger().info("显示模式: " + displayMode.name());
    }

    private void loadDisplayMode() {
        String modeStr = getConfig().getString("display-mode", "ACTIONBAR").toUpperCase();
        try {
            displayMode = DisplayMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            displayMode = DisplayMode.ACTIONBAR;
            getLogger().warning("无效的显示模式: " + modeStr + "，使用默认模式 ACTIONBAR");
        }
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

        if (actionBarDisplay != null) {
            actionBarDisplay.stopCleanupTask();
            actionBarDisplay.clear();
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
        loadDisplayMode();

        if (displayManager != null) {
            displayManager.loadConfig();
            displayManager.clear();
        }

        if (actionBarDisplay != null) {
            actionBarDisplay.loadConfig();
            actionBarDisplay.clear();
        }

        getLogger().info("配置已重载");
        getLogger().info("显示模式: " + displayMode.name());
    }

    public static GuangDianMobHealth getInstance() {
        return instance;
    }

    public MobHealthDisplayManager getDisplayManager() {
        return displayManager;
    }

    public ActionBarHealthDisplay getActionBarDisplay() {
        return actionBarDisplay;
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
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
}
