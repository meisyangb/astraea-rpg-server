package cn.guangdian.rpgcore.storage;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UnifiedDataManager {
    
    private final RPGCore plugin;
    private final Logger logger;
    private final Map<String, PlayerStorageHandler> handlers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> playerDataCache = new ConcurrentHashMap<>();
    
    public UnifiedDataManager(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void registerHandler(PlayerStorageHandler handler) {
        handlers.put(handler.getHandlerName(), handler);
        logger.info("注册存储处理器: " + handler.getHandlerName());
    }
    
    public void unregisterHandler(String handlerName) {
        handlers.remove(handlerName);
        logger.info("注销存储处理器: " + handlerName);
    }
    
    public void loadPlayerData(UUID playerId) {
        Map<String, Object> data = playerDataCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        Player player = Bukkit.getPlayer(playerId);
        
        for (PlayerStorageHandler handler : getSortedHandlers()) {
            try {
                Object loadedData = handler.load(playerId);
                if (loadedData != null) {
                    data.put(handler.getHandlerName(), loadedData);
                }
            } catch (Exception e) {
                logger.severe("加载数据失败 [" + handler.getHandlerName() + "]: " + e.getMessage());
            }
        }
    }
    
    public void savePlayerData(UUID playerId, boolean async) {
        Map<String, Object> data = playerDataCache.get(playerId);
        if (data == null) return;
        
        Runnable saveTask = () -> {
            for (PlayerStorageHandler handler : getSortedHandlers()) {
                try {
                    Object handlerData = data.get(handler.getHandlerName());
                    if (handlerData != null) {
                        handler.save(playerId, handlerData);
                    }
                } catch (Exception e) {
                    logger.severe("保存数据失败 [" + handler.getHandlerName() + "]: " + e.getMessage());
                }
            }
        };
        
        if (async) {
            AsyncExecutor asyncExecutor = plugin.getAsyncExecutor();
            if (asyncExecutor != null) {
                asyncExecutor.execute(saveTask);
            } else {
                saveTask.run();
            }
        } else {
            saveTask.run();
        }
    }
    
    public void saveAll() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId, false);
        }
    }
    
    public void saveAllPlayers(boolean async) {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId, async);
        }
    }
    
    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId, false);
        playerDataCache.remove(playerId);
    }
    
    public Object getPlayerData(UUID playerId, String handlerName) {
        Map<String, Object> data = playerDataCache.get(playerId);
        return data != null ? data.get(handlerName) : null;
    }
    
    public void setPlayerData(UUID playerId, String handlerName, Object value) {
        Map<String, Object> data = playerDataCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        data.put(handlerName, value);
    }
    
    public void clearPlayerCache(UUID playerId) {
        playerDataCache.remove(playerId);
    }
    
    private List<PlayerStorageHandler> getSortedHandlers() {
        List<PlayerStorageHandler> sorted = new ArrayList<>(handlers.values());
        sorted.sort(Comparator.comparingInt(PlayerStorageHandler::getPriority));
        return sorted;
    }
    
    public Collection<String> getHandlerNames() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
    
    public int getHandlerCount() {
        return handlers.size();
    }
}
