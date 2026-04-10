package cn.guangdian.name;

import cn.guangdian.name.adapter.DisplayServiceAdapter;
import cn.guangdian.name.lifecycle.NameDataHandler;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * GuangDianName - 玩家头顶显示插件
 * 
 * 功能：
 * 1. 头顶血量显示 (BELOW_NAME) - 第3行
 * 2. 头顶工会显示 (TextDisplay) - 第1行
 * 3. 头顶称号显示 (Team Prefix) - 第2行
 * 4. 头顶婚姻显示 (Team Suffix) - 第2行
 * 
 * 显示层级：
 * - 第1行：工会（TextDisplay实体骑在玩家头上）
 * - 第2行：称号 + 玩家名 + 婚姻（Team Prefix/Suffix）
 * - 第3行：血量（Below Name）
 * 
 * 设计原则：
 * 1. 订阅 RPGCore 血量事件，实时更新显示
 * 2. 使用 Scoreboard BELOW_NAME 方式显示血量
 * 3. 使用 Scoreboard Team 方式显示称号/婚姻
 * 4. 使用 TextDisplay 实体方式显示工会（1.19.4+最优方案）
 */
public class GuangDianName extends AbstractRPGPlugin implements Listener {
    
    private HealthDisplay healthDisplay;
    private TitleDisplay titleDisplay;
    private TextDisplayManager textDisplayManager;
    private RPGCoreListener rpgCoreListener;
    private HealthMonitor healthMonitor;
    private NameDataHandler dataHandler;
    private NamePlaceholder namePlaceholder;
    private DisplayServiceAdapter displayServiceAdapter;
    private long joinTaskId = -1;
    private long respawnTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        
        healthDisplay = new HealthDisplay(this);
        titleDisplay = new TitleDisplay(this);
        textDisplayManager = new TextDisplayManager(this);
        rpgCoreListener = new RPGCoreListener(this, healthDisplay);
        healthMonitor = new HealthMonitor(this);
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getCommand("gdname").setExecutor(new DebugCommand(this));
        getCommand("gdnametoggle").setExecutor(new ToggleCommand(this, titleDisplay));
        
        rpgCoreListener.subscribe();
        
        healthMonitor.start();
        
        textDisplayManager.startUpdateTask();
        
        registerPlaceholderAPI();
        
        registerRPGCoreService();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            healthDisplay.initPlayer(player);
            titleDisplay.initPlayer(player);
            textDisplayManager.createTextDisplay(player);
        }
        
        getLogger().info("GuangDianName 已启动");
        getLogger().info("功能: 血量显示、工会显示(TextDisplay)、称号显示、婚姻显示");
        getLogger().info("显示层级: 工会(第1行) -> 称号+玩家名+婚姻(第2行) -> 血量(第3行)");
    }
    
    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        if (scheduler != null) {
            if (joinTaskId >= 0) {
                scheduler.cancelTask(joinTaskId);
                joinTaskId = -1;
            }
            if (respawnTaskId >= 0) {
                scheduler.cancelTask(respawnTaskId);
                respawnTaskId = -1;
            }
        }
        
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        healthMonitor.stop();
        rpgCoreListener.unsubscribe();
        
        textDisplayManager.clear();
        
        unregisterPlaceholderAPI();
        
        unregisterRPGCoreService();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            healthDisplay.cleanupPlayer(player);
            titleDisplay.cleanupPlayer(player);
            textDisplayManager.removeTextDisplay(player);
        }
        
        healthDisplay.clear();
        titleDisplay.clear();
        
        getLogger().info("GuangDianName 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianName";
    }
    
    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            namePlaceholder = new NamePlaceholder(this, titleDisplay);
            if (namePlaceholder.register()) {
                getLogger().info("已注册 PlaceholderAPI 扩展");
            }
        }
    }
    
    private void unregisterPlaceholderAPI() {
        if (namePlaceholder != null) {
            namePlaceholder = null;
        }
    }
    
    private void registerRPGCoreService() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
            if (serviceRegistry != null) {
                displayServiceAdapter = new DisplayServiceAdapter(this, serviceRegistry, rpgCore.getEventBus());
                displayServiceAdapter.register();
            }
        }
    }
    
    private void unregisterRPGCoreService() {
        if (displayServiceAdapter != null) {
            displayServiceAdapter.unregister();
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (scheduler != null) {
            joinTaskId = scheduler.runSyncLater(() -> {
                if (player.isOnline()) {
                    healthDisplay.initPlayer(player);
                    titleDisplay.initPlayer(player);
                    textDisplayManager.createTextDisplay(player);
                }
            }, 50L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        healthDisplay.cleanupPlayer(player);
        titleDisplay.cleanupPlayer(player);
        textDisplayManager.removeTextDisplay(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (scheduler != null) {
            respawnTaskId = scheduler.runSyncLater(() -> {
                if (player.isOnline()) {
                    textDisplayManager.createTextDisplay(player);
                }
            }, 5L);
        }
    }
    
    public HealthDisplay getHealthDisplay() {
        return healthDisplay;
    }
    
    public TitleDisplay getTitleDisplay() {
        return titleDisplay;
    }
    
    public TextDisplayManager getTextDisplayManager() {
        return textDisplayManager;
    }
    
    public HealthMonitor getHealthMonitor() {
        return healthMonitor;
    }
}
