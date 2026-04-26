package cn.guangdian.rpgcore.cache;

import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.CacheStats;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class CaffeineCacheProvider implements CacheProvider {

    private final Cache<String, Object> cache;
    private final boolean recordStats;
    private final int maxSize;
    private volatile Duration defaultTTL;

    public CaffeineCacheProvider(int maxSize, Duration defaultTTL, boolean recordStats) {
        this(maxSize, defaultTTL, recordStats, false, false, false, Duration.ZERO);
    }

    public CaffeineCacheProvider(int maxSize, Duration defaultTTL, boolean recordStats,
                                  boolean weakKeys, boolean weakValues, boolean softValues,
                                  Duration refreshInterval) {
        this.maxSize = maxSize;
        this.defaultTTL = defaultTTL;
        this.recordStats = recordStats;

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize);

        // 设置过期策略
        if (defaultTTL != null && !defaultTTL.isZero() && !defaultTTL.isNegative()) {
            builder.expireAfterWrite(defaultTTL);
        }

        // 设置刷新间隔
        if (refreshInterval != null && !refreshInterval.isZero() && !refreshInterval.isNegative()) {
            builder.refreshAfterWrite(refreshInterval);
        }

        // 启用统计
        if (recordStats) {
            builder.recordStats();
        }

        // 启用弱引用键
        if (weakKeys) {
            builder.weakKeys();
        }

        // 启用弱引用值
        if (weakValues) {
            builder.weakValues();
        }

        // 启用软引用值（与弱引用值互斥）
        if (softValues && !weakValues) {
            builder.softValues();
        }

        this.cache = builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) cache.getIfPresent(key);
    }

    @Override
    public <T> Optional<T> getOptional(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type));
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            ttl = defaultTTL;
        }
        cache.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            ttl = defaultTTL;
        }

        return (T) cache.get(key, k -> loader.get());
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidatePattern(String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");

        for (String key : cache.asMap().keySet()) {
            if (key.matches(regex)) {
                cache.invalidate(key);
            }
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public CacheStats getStats() {
        if (!recordStats) {
            return CacheStats.empty();
        }

        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        long hitCount = stats.hitCount();
        long missCount = stats.missCount();
        long evictionCount = stats.evictionCount();
        long size = cache.estimatedSize();

        return new CacheStats(hitCount, missCount, evictionCount, size, maxSize);
    }

    @Override
    public int size() {
        return (int) cache.estimatedSize();
    }

    @Override
    public boolean containsKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public void setDefaultTTL(Duration ttl) {
        this.defaultTTL = ttl;
    }

    @Override
    public void setMaxSize(int maxSize) {
        cache.policy().eviction().ifPresent(policy -> {
            policy.setMaximum(maxSize);
        });
    }
}