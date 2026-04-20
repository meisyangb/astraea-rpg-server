package cn.guangdian.rpgskill.cooldown;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.CacheProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冷却管理器
 * 管理所有玩家的技能冷却
 * 
 * <p>使用 Caffeine 缓存实现自动过期清理，避免内存泄漏</p>
 */
public class CooldownManager {

    private static final String CACHE_NAME = "skill_cooldowns";
    private static final long DEFAULT_COOLDOWN_MAX_SECONDS = 3600; // 最大冷却1小时

    // 使用 Caffeine 缓存存储冷却数据
    // key: playerId + ":" + skillId, value: lastUseTime
    private final Cache<String, Long> cooldownCache;

    public CooldownManager() {
        this.cooldownCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(DEFAULT_COOLDOWN_MAX_SECONDS))
                .build();
    }

    /**
     * 构建缓存键
     */
    private String buildKey(String playerId, String skillId) {
        return playerId + ":" + skillId;
    }

    /**
     * 检查技能是否在冷却中
     */
    public boolean isOnCooldown(String playerId, String skillId, long cooldownSeconds) {
        Long lastUse = cooldownCache.getIfPresent(buildKey(playerId, skillId));
        if (lastUse == null) return false;

        long cooldownMillis = cooldownSeconds * 1000;
        return System.currentTimeMillis() - lastUse < cooldownMillis;
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    public long getCooldownRemaining(String playerId, String skillId, long cooldownSeconds) {
        Long lastUse = cooldownCache.getIfPresent(buildKey(playerId, skillId));
        if (lastUse == null) return 0;

        long cooldownMillis = cooldownSeconds * 1000;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining / 1000);
    }

    /**
     * 设置技能冷却
     */
    public void setCooldown(String playerId, String skillId) {
        cooldownCache.put(buildKey(playerId, skillId), System.currentTimeMillis());
    }

    /**
     * 清除玩家的所有冷却
     */
    public void clearPlayerCooldowns(String playerId) {
        // 使用 Caffeine 的 asMap() 遍历并移除匹配的键
        cooldownCache.asMap().keySet().removeIf(key -> key.startsWith(playerId + ":"));
    }

    /**
     * 清除特定技能的冷却
     */
    public void clearCooldown(String playerId, String skillId) {
        cooldownCache.invalidate(buildKey(playerId, skillId));
    }

    /**
     * 清除所有冷却
     */
    public void clearAll() {
        cooldownCache.invalidateAll();
    }

    /**
     * 获取缓存统计信息（用于调试）
     */
    public String getStats() {
        return cooldownCache.stats().toString();
    }
}
