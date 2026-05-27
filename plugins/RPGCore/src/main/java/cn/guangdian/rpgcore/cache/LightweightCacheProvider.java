package cn.guangdian.rpgcore.cache;

import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.CacheStats;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 轻量级缓存实现（适用于中小型服务器）
 *
 * <p>专为低并发、中小规模场景设计，追求简单高效。</p>
 *
 * <h3>性能特性：</h3>
 * <ul>
 *   <li>无锁读取：ConcurrentHashMap 原子操作</li>
 *   <li>简单淘汰：随机移除，避免遍历开销</li>
 *   <li>延迟清理：定期异步清理过期条目</li>
 *   <li>单数据结构：减少内存占用</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>在线玩家 < 50</li>
 *   <li>缓存条目 < 1000</li>
 *   <li>中等频率读写操作</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class LightweightCacheProvider implements CacheProvider {

    // 单一数据结构 - 无锁读取
    private final ConcurrentHashMap<String, CacheEntry<?>> cache;

    // Pattern 缓存
    private final ConcurrentHashMap<String, Pattern> patternCache;
    private static final int MAX_PATTERN_CACHE_SIZE = 50;

    // 统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    // 配置
    private volatile Duration defaultTTL = Duration.ofMinutes(30);
    private volatile int maxSize = 1000;
    private final boolean recordStats;

    // 清理控制
    private final AtomicLong lastCleanupTime = new AtomicLong(0);
    private static final long CLEANUP_INTERVAL_MS = 120_000; // 2分钟

    public LightweightCacheProvider(int maxSize, Duration defaultTTL, boolean recordStats) {
        this.maxSize = maxSize;
        this.defaultTTL = defaultTTL;
        this.recordStats = recordStats;
        this.cache = new ConcurrentHashMap<>(Math.max(16, maxSize / 4));
        this.patternCache = new ConcurrentHashMap<>(8);
    }

    public LightweightCacheProvider() {
        this(1000, Duration.ofMinutes(30), true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }

        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            recordMiss();
            return null;
        }

        // 检查过期
        if (entry.isExpired()) {
            cache.remove(key, entry); // 原子移除
            evictionCount.incrementAndGet();
            recordMiss();
            return null;
        }

        recordHit();

        Object value = entry.getValue();
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }

        return null;
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (key == null || value == null) {
            return;
        }

        // 简单容量控制
        if (cache.size() >= maxSize) {
            simpleEvict();
        }

        CacheEntry<?> entry = new CacheEntry<>(value, ttl != null ? ttl : defaultTTL);
        cache.put(key, entry);

        // 延迟清理
        maybeCleanup();
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        T value = get(key, type);
        if (value != null) {
            return value;
        }

        if (loader == null) {
            return null;
        }

        value = loader.get();
        if (value != null) {
            put(key, value, ttl != null ? ttl : defaultTTL);
        }

        return value;
    }

    @Override
    public void invalidate(String key) {
        if (key == null) {
            return;
        }

        CacheEntry<?> removed = cache.remove(key);
        if (removed != null) {
            evictionCount.incrementAndGet();
        }
    }

    @Override
    public void invalidatePattern(String pattern) {
        if (pattern == null) {
            return;
        }

        Pattern compiledPattern = patternCache.computeIfAbsent(pattern, p -> {
            String regex = p.replace("*", ".*").replace("?", ".");
            return Pattern.compile("^" + regex + "$");
        });

        if (patternCache.size() > MAX_PATTERN_CACHE_SIZE) {
            patternCache.clear();
        }

        int removed = 0;
        for (String key : new ArrayList<>(cache.keySet())) {
            if (compiledPattern.matcher(key).matches()) {
                if (cache.remove(key) != null) {
                    removed++;
                }
            }
        }
        evictionCount.addAndGet(removed);
    }

    @Override
    public void clear() {
        int size = cache.size();
        cache.clear();
        patternCache.clear();
        evictionCount.addAndGet(size);
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(hitCount.get(), missCount.get(), evictionCount.get(), cache.size(), maxSize);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            cache.remove(key, entry);
            evictionCount.incrementAndGet();
            return false;
        }

        return true;
    }

    @Override
    public void setDefaultTTL(Duration ttl) {
        if (ttl != null && !ttl.isNegative()) {
            this.defaultTTL = ttl;
        }
    }

    @Override
    public void setMaxSize(int maxSize) {
        if (maxSize > 0) {
            this.maxSize = maxSize;
        }
    }

    /**
     * 简单淘汰 - 随机移除部分条目
     */
    private void simpleEvict() {
        int toRemove = Math.max(10, cache.size() / 10);
        Iterator<String> iterator = cache.keySet().iterator();
        int removed = 0;
        while (iterator.hasNext() && removed < toRemove) {
            iterator.next();
            iterator.remove();
            removed++;
        }
        evictionCount.addAndGet(removed);
    }

    /**
     * 延迟清理 - 定期清理过期条目
     */
    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        long lastCleanup = lastCleanupTime.get();

        if (now - lastCleanup < CLEANUP_INTERVAL_MS) {
            return;
        }

        if (lastCleanupTime.compareAndSet(lastCleanup, now)) {
            cleanupExpired();
        }
    }

    /**
     * 清理所有过期条目
     */
    public int cleanupExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<String, CacheEntry<?>>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry<?>> entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
                iterator.remove();
                removed++;
            }
        }

        evictionCount.addAndGet(removed);
        return removed;
    }

    private void recordHit() {
        if (recordStats) {
            hitCount.incrementAndGet();
        }
    }

    private void recordMiss() {
        if (recordStats) {
            missCount.incrementAndGet();
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;

        CacheEntry(T value, Duration ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl.toMillis();
        }

        T getValue() { return value; }
        boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
        boolean isExpired(long now) { return now > expiryTime; }
    }
}