package cn.guangdian.rpgcore.lifecycle;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerLifecycleManager implements Listener {
    
    private final RPGCore plugin;
    private final Logger logger;
    private final List<PlayerDataHandler> handlers = new ArrayList<>();
    private final Map<UUID, Long> loadTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> saveTimes = new ConcurrentHashMap<>();
    private long autoSaveTask = -1;
    private long autoSaveInterval = 5 * 60 * 20L;
    
    public PlayerLifecycleManager(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startAutoSave();
        logger.info("PlayerLifecycleManager 已启用");
    }
    
    public void unregister() {
        HandlerList.unregisterAll(this);
        stopAutoSave();
        handlers.clear();
        logger.info("PlayerLifecycleManager 已禁用");
    }
    
    public void registerHandler(PlayerDataHandler handler) {
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(PlayerDataHandler::getPriority));
        logger.info("注册数据处理器: " + handler.getHandlerName() + " (优先级: " + handler.getPriority() + ")");
    }
    
    public void unregisterHandler(PlayerDataHandler handler) {
        handlers.remove(handler);
        logger.info("注销数据处理器: " + handler.getHandlerName());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long startTime = System.currentTimeMillis();
        loadTimes.put(playerId, startTime);
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            logger.warning("RPGCore instance is null, skipping data load for " + player.getName());
            return;
        }
        
        SyncScheduler scheduler = rpgCore.getScheduler();
        if (scheduler == null) {
            logger.warning("Scheduler is null, skipping data load for " + player.getName());
            return;
        }
        
        PlayerDataLoadEvent loadEvent = new PlayerDataLoadEvent(player);
        
        scheduler.runAsync(() -> {
            int handlerCount = handlers.size();
            for (PlayerDataHandler handler : handlers) {
                try {
                    if (handler.shouldLoad(player)) {
                        long handlerStart = System.currentTimeMillis();
                        handler.onLoad(loadEvent);
                        long handlerTime = System.currentTimeMillis() - handlerStart;
                        if (handlerTime > 100) {
                            logger.warning("[性能] " + handler.getHandlerName() + " 加载耗时: " + handlerTime + "ms");
                        }
                    }
                } catch (Exception e) {
                    logger.severe("数据加载失败 [" + handler.getHandlerName() + "]: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            scheduler.runSync(() -> {
                logger.info("[登录] " + player.getName() + " 数据加载完成 (" + handlerCount + "个处理器, 耗时" + totalTime + "ms)");
            });
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        savePlayerData(player, false);
        
        loadTimes.remove(playerId);
        saveTimes.remove(playerId);
    }
    
    public void savePlayerData(Player player, boolean async) {
        UUID playerId = player.getUniqueId();
        long startTime = System.currentTimeMillis();
        saveTimes.put(playerId, startTime);
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            logger.warning("RPGCore instance is null, skipping data save for " + player.getName());
            return;
        }
        
        PlayerDataSaveEvent saveEvent = new PlayerDataSaveEvent(player, async);
        
        Runnable saveTask = () -> {
            int handlerCount = handlers.size();
            for (PlayerDataHandler handler : handlers) {
                try {
                    if (handler.shouldSave(player)) {
                        long handlerStart = System.currentTimeMillis();
                        handler.onSave(saveEvent);
                        long handlerTime = System.currentTimeMillis() - handlerStart;
                        if (handlerTime > 100) {
                            logger.warning("[性能] " + handler.getHandlerName() + " 保存耗时: " + handlerTime + "ms");
                        }
                    }
                } catch (Exception e) {
                    logger.severe("数据保存失败 [" + handler.getHandlerName() + "]: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            SyncScheduler scheduler = rpgCore.getScheduler();
            if (scheduler != null) {
                scheduler.runSync(() -> {
                    logger.info("[退出] " + player.getName() + " 数据保存完成 (" + handlerCount + "个处理器, 耗时" + totalTime + "ms)");
                });
            }
        };
        
        if (async) {
            SyncScheduler scheduler = rpgCore.getScheduler();
            if (scheduler != null) {
                scheduler.runAsync(saveTask);
            }
        } else {
            saveTask.run();
        }
    }
    
    public void saveAllPlayers(boolean async) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player, async);
        }
        logger.info("已保存所有在线玩家数据");
    }
    
    private void startAutoSave() {
        if (autoSaveTask >= 0) {
            SyncScheduler scheduler = RPGCore.getInstance().getScheduler();
            if (scheduler != null) {
                scheduler.cancelTask(autoSaveTask);
            }
        }
        
        SyncScheduler scheduler = RPGCore.getInstance().getScheduler();
        if (scheduler != null) {
            autoSaveTask = scheduler.runSyncRepeating(() -> {
                saveAllPlayers(true);
            }, autoSaveInterval, autoSaveInterval);
        }
        
        logger.info("自动保存任务已启动 (间隔: " + (autoSaveInterval / 20 / 60) + "分钟)");
    }
    
    private void stopAutoSave() {
        if (autoSaveTask >= 0) {
            SyncScheduler scheduler = RPGCore.getInstance().getScheduler();
            if (scheduler != null) {
                scheduler.cancelTask(autoSaveTask);
            }
            autoSaveTask = -1;
        }
    }
    
    public void setAutoSaveInterval(long ticks) {
        this.autoSaveInterval = ticks;
        startAutoSave();
    }
    
    public List<PlayerDataHandler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }
    
    public int getHandlerCount() {
        return handlers.size();
    }
    
    public Long getLoadTime(UUID playerId) {
        return loadTimes.get(playerId);
    }
    
    public Long getSaveTime(UUID playerId) {
        return saveTimes.get(playerId);
    }
}
