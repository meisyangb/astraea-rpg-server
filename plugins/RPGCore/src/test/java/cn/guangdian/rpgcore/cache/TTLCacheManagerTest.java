package cn.guangdian.rpgcore.cache;

import cn.guangdian.rpgcore.api.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TTLCacheManager 单元测试
 * 
 * @author GuangDian
 * @since 1.0.0
 */
@DisplayName("TTL缓存管理器测试")
class TTLCacheManagerTest {

    private TTLCacheManager cache;

    @BeforeEach
    void setUp() {
        cache = new TTLCacheManager(100, Duration.ofMinutes(5), true);
    }

    @Test
    @DisplayName("基本存取操作")
    void testBasicPutAndGet() {
        cache.put("key1", "value1");
        
        String result = cache.get("key1", String.class);
        
        assertNotNull(result);
        assertEquals("value1", result);
    }

    @Test
    @DisplayName("获取不存在的键返回null")
    void testGetNonExistentKey() {
        String result = cache.get("nonexistent", String.class);
        
        assertNull(result);
    }

    @Test
    @DisplayName("缓存命中和未命中统计")
    void testHitAndMissStats() {
        cache.put("key1", "value1");
        
        cache.get("key1", String.class);
        cache.get("key1", String.class);
        cache.get("nonexistent", String.class);
        
        CacheStats stats = cache.getStats();
        
        assertEquals(2, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(0.666, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("TTL过期测试")
    void testTTLExpiry() throws InterruptedException {
        TTLCacheManager shortTTLCache = new TTLCacheManager(100, Duration.ofMillis(50), true);
        
        shortTTLCache.put("key1", "value1");
        
        assertNotNull(shortTTLCache.get("key1", String.class));
        
        Thread.sleep(100);
        
        assertNull(shortTTLCache.get("key1", String.class));
    }

    @Test
    @DisplayName("自定义TTL")
    void testCustomTTL() throws InterruptedException {
        cache.put("key1", "value1", Duration.ofMillis(50));
        
        assertNotNull(cache.get("key1", String.class));
        
        Thread.sleep(100);
        
        assertNull(cache.get("key1", String.class));
    }

    @Test
    @DisplayName("LRU淘汰策略")
    void testLRUEviction() {
        TTLCacheManager smallCache = new TTLCacheManager(3, Duration.ofMinutes(5), true);
        
        smallCache.put("key1", "value1");
        smallCache.put("key2", "value2");
        smallCache.put("key3", "value3");
        
        smallCache.get("key1", String.class);
        smallCache.get("key2", String.class);
        
        smallCache.put("key4", "value4");
        
        assertNull(smallCache.get("key3", String.class));
        assertNotNull(smallCache.get("key1", String.class));
        assertNotNull(smallCache.get("key2", String.class));
        assertNotNull(smallCache.get("key4", String.class));
    }

    @Test
    @DisplayName("getOrLoad延迟加载")
    void testGetOrLoad() {
        AtomicInteger loadCount = new AtomicInteger(0);
        
        String result1 = cache.getOrLoad("key1", String.class, () -> {
            loadCount.incrementAndGet();
            return "loaded";
        }, Duration.ofMinutes(5));
        
        String result2 = cache.getOrLoad("key1", String.class, () -> {
            loadCount.incrementAndGet();
            return "loaded";
        }, Duration.ofMinutes(5));
        
        assertEquals("loaded", result1);
        assertEquals("loaded", result2);
        assertEquals(1, loadCount.get());
    }

    @Test
    @DisplayName("invalidate删除缓存")
    void testInvalidate() {
        cache.put("key1", "value1");
        
        cache.invalidate("key1");
        
        assertNull(cache.get("key1", String.class));
    }

    @Test
    @DisplayName("模式匹配删除")
    void testInvalidatePattern() {
        cache.put("player:1:name", "Alice");
        cache.put("player:1:level", "10");
        cache.put("player:2:name", "Bob");
        cache.put("guild:1:name", "TestGuild");
        
        cache.invalidatePattern("player:1:*");
        
        assertNull(cache.get("player:1:name", String.class));
        assertNull(cache.get("player:1:level", String.class));
        assertNotNull(cache.get("player:2:name", String.class));
        assertNotNull(cache.get("guild:1:name", String.class));
    }

    @Test
    @DisplayName("清空缓存")
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        cache.clear();
        
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("containsKey检查")
    void testContainsKey() {
        cache.put("key1", "value1");
        
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }

    @Test
    @DisplayName("并发访问测试")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        cache.put(key, "value-" + j);
                        cache.get(key, String.class);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(0, errorCount.get());
    }

    @Test
    @DisplayName("null键值处理")
    void testNullKeyValue() {
        cache.put(null, "value");
        cache.put("key", null);
        
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("类型不匹配返回null")
    void testTypeMismatch() {
        cache.put("key1", "stringValue");
        
        Integer result = cache.get("key1", Integer.class);
        
        assertNull(result);
    }
}
