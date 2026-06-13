package cn.guangdian.rpgcore.ratelimit;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.HttpClient;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterImpl implements HttpClient.RateLimiter {

    private final RPGCore plugin;
    private final Map<String, LimitConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RateLimitEntry>> playerData = new ConcurrentHashMap<>();

    private static final int DEFAULT_WINDOW_SECONDS = 60;

    public RateLimiterImpl(RPGCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean tryAcquire(Player player, String action) {
        return tryAcquire(player.getUniqueId().toString(), action);
    }

    @Override
    public boolean tryAcquire(String playerId, String action) {
        LimitConfig config = configs.get(action);
        if (config == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowMs = config.windowSeconds() * 1000L;

        Map<String, RateLimitEntry> playerActions = playerData.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        RateLimitEntry entry = playerActions.get(action);

        if (entry == null || (now - entry.windowStart) > windowMs) {
            playerActions.put(action, new RateLimitEntry(1, now));
            return true;
        }

        if (entry.count >= config.maxRequests()) {
            return false;
        }

        playerActions.put(action, new RateLimitEntry(entry.count + 1, entry.windowStart));
        return true;
    }

    @Override
    public void setLimit(String action, int maxPerMinute) {
        setLimit(action, maxPerMinute, DEFAULT_WINDOW_SECONDS);
    }

    @Override
    public void setLimit(String action, int maxPerMinute, int windowSeconds) {
        configs.put(action, new LimitConfig(maxPerMinute, windowSeconds));
    }

    @Override
    public int getRemaining(String playerId, String action) {
        LimitConfig config = configs.get(action);
        if (config == null) {
            return Integer.MAX_VALUE;
        }

        Map<String, RateLimitEntry> playerActions = playerData.get(playerId);
        if (playerActions == null) {
            return config.maxRequests();
        }

        RateLimitEntry entry = playerActions.get(action);
        if (entry == null) {
            return config.maxRequests();
        }

        long now = System.currentTimeMillis();
        long windowMs = config.windowSeconds() * 1000L;

        if ((now - entry.windowStart) > windowMs) {
            return config.maxRequests();
        }

        return Math.max(0, config.maxRequests() - entry.count);
    }

    @Override
    public void clearPlayer(String playerId) {
        playerData.remove(playerId);
    }

    @Override
    public void clearAll() {
        playerData.clear();
    }

    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();

        for (Map<String, RateLimitEntry> playerActions : playerData.values()) {
            playerActions.entrySet().removeIf(entry -> {
                LimitConfig config = configs.get(entry.getKey());
                if (config == null) {
                    return true;
                }
                long windowMs = config.windowSeconds() * 1000L;
                return (now - entry.getValue().windowStart) > windowMs;
            });
        }
    }

    public int getPlayerActionCount(String playerId, String action) {
        Map<String, RateLimitEntry> playerActions = playerData.get(playerId);
        if (playerActions == null) {
            return 0;
        }

        RateLimitEntry entry = playerActions.get(action);
        if (entry == null) {
            return 0;
        }

        LimitConfig config = configs.get(action);
        if (config == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long windowMs = config.windowSeconds() * 1000L;

        if ((now - entry.windowStart) > windowMs) {
            return 0;
        }

        return entry.count;
    }

    private record RateLimitEntry(int count, long windowStart) {}
}
