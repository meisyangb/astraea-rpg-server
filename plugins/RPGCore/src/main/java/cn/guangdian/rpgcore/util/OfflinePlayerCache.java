package cn.guangdian.rpgcore.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 离线玩家缓存工具类
 * 
 * <p>用于缓存玩家UUID与名称的映射关系，避免频繁查询离线玩家数据。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取玩家名称
 * String name = OfflinePlayerCache.getPlayerName(uuid);
 * 
 * // 获取玩家UUID
 * UUID uuid = OfflinePlayerCache.getPlayerUUID("playerName");
 * 
 * // 手动更新缓存
 * OfflinePlayerCache.updateCache(uuid, "playerName");
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public final class OfflinePlayerCache {

    private OfflinePlayerCache() {
        // 工具类，不允许实例化
    }

    // UUID -> 玩家名称缓存
    private static final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    
    // 玩家名称(小写) -> UUID缓存
    private static final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    
    // 缓存时间戳（用于清理过期缓存）
    private static final Map<UUID, Long> cacheTime = new ConcurrentHashMap<>();
    
    // 缓存过期时间（默认30分钟）
    private static final long CACHE_EXPIRE_MS = TimeUnit.MINUTES.toMillis(30);
    
    // 最大缓存数量
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * 获取玩家名称
     * 
     * <p>查找顺序：</p>
     * <ol>
     *   <li>在线玩家</li>
     *   <li>缓存</li>
     *   <li>离线玩家查询</li>
     * </ol>
     * 
     * @param uuid 玩家UUID
     * @return 玩家名称，如果不存在返回 null
     */
    public static String getPlayerName(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        // 1. 尝试在线玩家
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            String name = online.getName();
            updateCache(uuid, name);
            return name;
        }
        
        // 2. 检查缓存
        String cached = uuidToName.get(uuid);
        if (cached != null) {
            return cached;
        }
        
        // 3. 离线玩家查询
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null) {
            updateCache(uuid, offline.getName());
            return offline.getName();
        }
        
        return null;
    }

    /**
     * 获取玩家UUID
     * 
     * <p>查找顺序：</p>
     * <ol>
     *   <li>在线玩家</li>
     *   <li>缓存</li>
     *   <li>离线玩家查询</li>
     * </ol>
     * 
     * @param name 玩家名称（不区分大小写）
     * @return 玩家UUID，如果不存在返回 null
     */
    public static UUID getPlayerUUID(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        
        String lowerName = name.toLowerCase();
        
        // 1. 尝试在线玩家
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            UUID uuid = online.getUniqueId();
            updateCache(uuid, name);
            return uuid;
        }
        
        // 2. 检查缓存
        UUID cached = nameToUuid.get(lowerName);
        if (cached != null) {
            return cached;
        }
        
        // 3. 离线玩家查询（需要注意：这可能返回一个从未登录过的玩家UUID）
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            UUID uuid = offline.getUniqueId();
            updateCache(uuid, name);
            return uuid;
        }
        
        return null;
    }

    /**
     * 更新缓存
     * 
     * @param uuid 玩家UUID
     * @param name 玩家名称
     */
    public static void updateCache(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) {
            return;
        }
        
        // 清理过期缓存
        if (uuidToName.size() >= MAX_CACHE_SIZE) {
            cleanExpiredCache();
        }
        
        uuidToName.put(uuid, name);
        nameToUuid.put(name.toLowerCase(), uuid);
        cacheTime.put(uuid, System.currentTimeMillis());
    }

    /**
     * 从缓存移除
     * 
     * @param uuid 玩家UUID
     */
    public static void removeFromCache(UUID uuid) {
        if (uuid == null) {
            return;
        }
        
        String name = uuidToName.remove(uuid);
        if (name != null) {
            nameToUuid.remove(name.toLowerCase());
        }
        cacheTime.remove(uuid);
    }

    /**
     * 清理过期缓存
     */
    private static void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        cacheTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > CACHE_EXPIRE_MS) {
                String name = uuidToName.remove(entry.getKey());
                if (name != null) {
                    nameToUuid.remove(name.toLowerCase());
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 清空所有缓存
     */
    public static void clearCache() {
        uuidToName.clear();
        nameToUuid.clear();
        cacheTime.clear();
    }

    /**
     * 获取缓存统计
     * 
     * @return 缓存大小
     */
    public static int getCacheSize() {
        return uuidToName.size();
    }

    /**
     * 检查玩家是否在缓存中
     * 
     * @param uuid 玩家UUID
     * @return 如果缓存中存在返回 true
     */
    public static boolean isCached(UUID uuid) {
        return uuidToName.containsKey(uuid);
    }

    /**
     * 检查玩家名称是否在缓存中
     * 
     * @param name 玩家名称
     * @return 如果缓存中存在返回 true
     */
    public static boolean isCached(String name) {
        return name != null && nameToUuid.containsKey(name.toLowerCase());
    }
}