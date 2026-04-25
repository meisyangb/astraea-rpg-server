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
 * 高性能缓存实现（适用于大型服务器）
 *
 * <p><b>⚠️ 已废弃：</b>推荐使用 {@link CaffeineCacheProvider}，Caffeine 是业界成熟的缓存库，
 * 性能更优、功能更完善（自动过期、LRU、统计、并发安全）。</p>
 *
 * <p>专为高并发、大数据量场景设计。</p>
 *
 * <h3>性能特性：</h3>
 * <ul>
 *   <li>LRU淘汰：O(1) LinkedHashMap实现</li>
 *   <li>Pattern缓存：避免正则重复编译</li>
 *   <li>增量清理：随机抽样替代全量扫描</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>在线玩家 > 50</li>
 *   <li>缓存条目 > 1000</li>
 *   <li>高频率读写操作</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 * @deprecated 使用 {@link CaffeineCacheProvider} 替代。Caffeine 提供更好的性能和更完善的功能。
 * @see CaffeineCacheProvider
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public class HighPerformanceCacheProvider implements CacheProvider {

    // LRU 缓存 - LinkedHashMap (access-order)
    private final LinkedHashMap<String, CacheEntry<?>> lruCache;
    // 快速查找映射
    private final ConcurrentHashMap<String, CacheEntry<?>> fastLookup;
    // Pattern 缓存
    private final ConcurrentHashMap<String, Pattern> patternCache;

    private static final int MAX_PATTERN_CACHE_SIZE = 100;

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    private Duration defaultTTL = Duration.ofMinutes(30);
    private int maxSize = 1000;
    private final boolean recordStats;

    private final AtomicLong lastCleanupTime = new AtomicLong(0);
    private static final long CLEANUP_INTERVAL_MS = 60_000;
    private final Random cleanupRandom = new Random();

    public HighPerformanceCacheProvider(int maxSize, Duration defaultTTL, boolean recordStats) {
        this.maxSize = maxSize;
        this.defaultTTL = defaultTTL;
        this.recordStats = recordStats;

        this.lruCache = new LinkedHashMap<>(16, 0.75f, true);
        this.fastLookup = new ConcurrentHashMap<>(maxSize);
        this.patternCache = new ConcurrentHashMap<>(MAX_PATTERN_CACHE_SIZE);
    }

    public HighPerformanceCacheProvider() {
        this(1000, Duration.ofMinutes(30), true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }

        CacheEntry<?> entry = fastLookup.get(key);
        if (entry == null) {
            recordMiss();
            return null;
        }

        if (entry.isExpired()) {
            removeEntry(key);
            evictionCount.incrementAndGet();
            recordMiss();
            return null;
        }

        entry.touch();
        synchronized (lruCache) {
            lruCache.get(key); // 触发 access-order 重排序
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

        synchronized (lruCache) {
            if (lruCache.size() >= maxSize && !lruCache.containsKey(key)) {
                evictLRU();
            }

            CacheEntry<?> entry = new CacheEntry<>(value, ttl != null ? ttl : defaultTTL);
            lruCache.put(key, entry);
            fastLookup.put(key, entry);

            incrementalCleanup();
        }
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

        if (removeEntry(key)) {
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

        List<String> keysToRemove = new ArrayList<>();
        for (String key : fastLookup.keySet()) {
            if (compiledPattern.matcher(key).matches()) {
                keysToRemove.add(key);
            }
        }

        int removed = 0;
        for (String key : keysToRemove) {
            if (removeEntry(key)) {
                removed++;
            }
        }
        evictionCount.addAndGet(removed);
    }

    @Override
    public void clear() {
        int size;
        synchronized (lruCache) {
            size = lruCache.size();
            lruCache.clear();
        }
        fastLookup.clear();
        patternCache.clear();
        evictionCount.addAndGet(size);
    }

    @Override
    public CacheStats getStats() {
        int currentSize;
        synchronized (lruCache) {
            currentSize = lruCache.size();
        }
        return new CacheStats(hitCount.get(), missCount.get(), evictionCount.get(), currentSize, maxSize);
    }

    @Override
    public int size() {
        synchronized (lruCache) {
            return lruCache.size();
        }
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }

        CacheEntry<?> entry = fastLookup.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            removeEntry(key);
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
            synchronized (lruCache) {
                this.maxSize = maxSize;
                while (lruCache.size() > maxSize) {
                    evictLRU();
                }
            }
        }
    }

    private void incrementalCleanup() {
        long now = System.currentTimeMillis();
        long lastCleanup = lastCleanupTime.get();

        if (now - lastCleanup < CLEANUP_INTERVAL_MS) {
            return;
        }

        if (!lastCleanupTime.compareAndSet(lastCleanup, now)) {
            return;
        }

        int sampleSize = Math.min(50, lruCache.size());
        if (sampleSize <= 0) {
            return;
        }

        List<String> keysToCheck = new ArrayList<>(fastLookup.keySet());
        List<String> expiredKeys = new ArrayList<>();

        for (int i = 0; i < sampleSize && i < keysToCheck.size(); i++) {
            int idx = cleanupRandom.nextInt(keysToCheck.size());
            String key = keysToCheck.get(idx);
            CacheEntry<?> entry = fastLookup.get(key);
            if (entry != null && entry.isExpired(now)) {
                expiredKeys.add(key);
            }
        }

        for (String key : expiredKeys) {
            if (removeEntry(key)) {
                evictionCount.incrementAndGet();
            }
        }
    }

    private void evictLRU() {
        if (lruCache.isEmpty()) {
            return;
        }

        Iterator<String> iterator = lruCache.keySet().iterator();
        if (iterator.hasNext()) {
            String lruKey = iterator.next();
            iterator.remove();
            fastLookup.remove(lruKey);
            evictionCount.incrementAndGet();
        }
    }

    private boolean removeEntry(String key) {
        synchronized (lruCache) {
            CacheEntry<?> removed = lruCache.remove(key);
            fastLookup.remove(key);
            return removed != null;
        }
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

    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;
        private volatile long lastAccessTime;

        CacheEntry(T value, Duration ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl.toMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }

        T getValue() { return value; }
        boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
        boolean isExpired(long now) { return now > expiryTime; }
        void touch() { this.lastAccessTime = System.currentTimeMillis(); }
    }
}