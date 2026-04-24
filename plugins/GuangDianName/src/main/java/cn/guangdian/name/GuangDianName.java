package cn.guangdian.name;

import cn.guangdian.name.adapter.DisplayServiceAdapter;
import cn.guangdian.name.command.GdNameCommand;
import cn.guangdian.name.command.GdNameToggleCommand;
import cn.guangdian.name.command.LegacyDebugCommand;
import cn.guangdian.name.command.LegacyToggleCommand;
import cn.guangdian.name.lifecycle.NameDataHandler;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * GuangDianName - 玩家头顶显示插件 (全TextDisplay版本)
 * 
 * 功能：
 * 1. 头顶血量显示 (TextDisplay) - 第1行
 * 2. 头顶称号+玩家名+婚姻显示 (TextDisplay) - 第2行
 * 3. 头顶工会显示 (TextDisplay) - 第3行
 * 
 * 显示层级（从下到上）：
 * - 第1行：血量
 * - 第2行：称号 + 玩家名 + 婚姻
 * - 第3行：工会
 * 
 * 设计原则：
 * 1. 全部使用 TextDisplay 实体显示，性能更好
 * 2. 支持 MiniMessage 颜色格式
 * 3. 每个玩家维护自己的显示实体
 * 
 * 命令系统：使用 RPGCore CommandFramework，支持降级到传统Bukkit命令
 */
public class GuangDianName extends AbstractRPGPlugin implements Listener {
    
    private NameDisplayManager nameDisplayManager;
    private HealthMonitor healthMonitor;
    private NameDataHandler dataHandler;
    private NamePlaceholder namePlaceholder;
    private DisplayServiceAdapter displayServiceAdapter;
    private MiniMessageService miniMessage;
    
    // RPGCore CommandFramework
    private GdNameCommand gdNameCommand;
    private GdNameToggleCommand gdNameToggleCommand;
    
    private long joinTaskId = -1;
    private long respawnTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        
        initRPGCoreServices();
        
        nameDisplayManager = new NameDisplayManager(this);
        healthMonitor = new HealthMonitor(this, nameDisplayManager);
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // 初始化命令系统 (使用 RPGCore CommandFramework)
        initCommandFramework();
        
        healthMonitor.start();
        nameDisplayManager.startUpdateTask();
        
        registerPlaceholderAPI();
        registerRPGCoreService();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            nameDisplayManager.initPlayer(player);
        }
        
        getLogger().info("GuangDianName 已启动 (全TextDisplay版本)");
        getLogger().info("功能: 血量显示、称号显示、工会显示、婚姻显示");
        getLogger().info("显示层级: 血量(第1行) -> 称号+玩家名+婚姻(第2行) -> 工会(第3行)");
    }
    
    /**
     * 初始化 RPGCore CommandFramework
     */
    private void initCommandFramework() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CommandFramework framework = CommandFramework.getInstance();

            // 注册 gdname 命令
            gdNameCommand = new GdNameCommand(this);
            framework.registerCommand(gdNameCommand);

            // 注册 gdnametoggle 命令
            gdNameToggleCommand = new GdNameToggleCommand(this, nameDisplayManager);
            framework.registerCommand(gdNameToggleCommand);

            getLogger().info("已注册 CommandFramework 命令");
        } else {
            // 降级处理：使用传统命令注册
            getLogger().warning("RPGCore 未加载，使用传统命令注册方式");
            initLegacyCommands();
        }
    }

    /**
     * 传统命令注册方式 (降级处理)
     */
    private void initLegacyCommands() {
        PluginCommand gdnameCmd = getCommand("gdname");
        if (gdnameCmd != null) {
            LegacyDebugCommand legacyDebug = new LegacyDebugCommand(this);
            gdnameCmd.setExecutor(legacyDebug);
            gdnameCmd.setTabCompleter(legacyDebug);
        }
        
        PluginCommand gdnametoggleCmd = getCommand("gdnametoggle");
        if (gdnametoggleCmd != null) {
            LegacyToggleCommand legacyToggle = new LegacyToggleCommand(this, nameDisplayManager);
            gdnametoggleCmd.setExecutor(legacyToggle);
            gdnametoggleCmd.setTabCompleter(legacyToggle);
        }
        
        getLogger().info("已注册传统命令处理器");
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
        nameDisplayManager.clear();
        
        unregisterPlaceholderAPI();
        unregisterRPGCoreService();
        
        // 注销 CommandFramework 命令
        if (gdNameCommand != null || gdNameToggleCommand != null) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                CommandFramework framework = CommandFramework.getInstance();
                framework.unregisterCommand("gdname");
                framework.unregisterCommand("gdnametoggle");
            }
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            nameDisplayManager.removeAllDisplays(player);
        }
        
        getLogger().info("GuangDianName 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianName";
    }
    
    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            namePlaceholder = new NamePlaceholder(this, nameDisplayManager);
            namePlaceholder.register();
            getLogger().info("已注册 PlaceholderAPI 扩展");
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
                    nameDisplayManager.initPlayer(player);
                }
            }, 50L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        nameDisplayManager.removeAllDisplays(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (scheduler != null) {
            respawnTaskId = scheduler.runSyncLater(() -> {
                if (player.isOnline()) {
                    nameDisplayManager.initPlayer(player);
                }
            }, 5L);
        }
    }
    
    public NameDisplayManager getNameDisplayManager() {
        return nameDisplayManager;
    }
    
    public HealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }
}
