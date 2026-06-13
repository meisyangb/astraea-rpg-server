package cn.guangdian.armorstats.storage;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AsyncExecutorService单元测试
 */
class AsyncExecutorServiceTest {
    
    @Mock
    private GuangDianArmorStats plugin;
    
    @Mock
    private Logger logger;
    
    private AsyncExecutorService service;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);
        service = new AsyncExecutorService(plugin, 2);
    }
    
    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }
    
    /**
     * 测试: 异步保存成功
     */
    @Test
    void testSavePlayerDataAsync_success() throws Exception {
        UUID uuid = UUID.randomUUID();
        AtomicBoolean saved = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = service.savePlayerDataAsync(uuid, () -> {
            saved.set(true);
        });
        
        future.get(5, TimeUnit.SECONDS);
        
        assertTrue(saved.get(), "保存任务应该被执行");
        assertEquals(0, service.getPendingSaveCount(), "保存完成后应该从队列移除");
        assertEquals(0, service.getFailureCount(uuid), "成功保存不应该有失败计数");
    }
    
    /**
     * 测试: 异步保存失败时保留数据
     */
    @Test
    void testSavePlayerDataAsync_failure_retainsData() throws Exception {
        UUID uuid = UUID.randomUUID();
        
        CompletableFuture<Void> future = service.savePlayerDataAsync(uuid, () -> {
            throw new RuntimeException("模拟保存失败");
        });
        
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("应该抛出异常");
        } catch (ExecutionException e) {
            // 预期异常
            assertTrue(e.getCause() instanceof RuntimeException);
        }
        
        // 失败后应该从队列移除
        assertEquals(0, service.getPendingSaveCount());
        
        // 应该记录失败次数
        assertEquals(1, service.getFailureCount(uuid));
    }
    
    /**
     * 测试: 重复保存请求合并
     */
    @Test
    void testSavePlayerDataAsync_mergeDuplicateRequests() throws Exception {
        UUID uuid = UUID.randomUUID();
        AtomicInteger saveCount = new AtomicInteger(0);
        
        // 快速提交3次保存请求
        CompletableFuture<Void> f1 = service.savePlayerDataAsync(uuid, () -> {
            try {
                Thread.sleep(100); // 模拟保存耗时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            saveCount.incrementAndGet();
        });
        
        CompletableFuture<Void> f2 = service.savePlayerDataAsync(uuid, () -> {
            saveCount.incrementAndGet();
        });
        
        CompletableFuture<Void> f3 = service.savePlayerDataAsync(uuid, () -> {
            saveCount.incrementAndGet();
        });
        
        CompletableFuture.allOf(f1, f2, f3).get(5, TimeUnit.SECONDS);
        
        // 应该执行所有保存（合并后串行执行）
        assertEquals(3, saveCount.get());
        assertEquals(0, service.getPendingSaveCount());
    }
    
    /**
     * 测试: 等待所有保存完成
     */
    @Test
    void testAwaitAllSaves_success() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        AtomicInteger completedCount = new AtomicInteger(0);
        
        service.savePlayerDataAsync(uuid1, () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completedCount.incrementAndGet();
        });
        
        service.savePlayerDataAsync(uuid2, () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completedCount.incrementAndGet();
        });
        
        boolean result = service.awaitAllSaves(5, TimeUnit.SECONDS);
        
        assertTrue(result, "所有保存应该完成");
        assertEquals(2, completedCount.get(), "两个保存任务都应该完成");
        assertEquals(0, service.getPendingSaveCount(), "队列应该为空");
    }
    
    /**
     * 测试: 等待保存超时
     */
    @Test
    void testAwaitAllSaves_timeout() throws Exception {
        UUID uuid = UUID.randomUUID();
        
        service.savePlayerDataAsync(uuid, () -> {
            try {
                Thread.sleep(5000); // 长时间任务
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        boolean result = service.awaitAllSaves(100, TimeUnit.MILLISECONDS);
        
        assertFalse(result, "应该超时");
    }
    
    /**
     * 测试: 获取待保存队列大小
     */
    @Test
    void testGetPendingSaveCount() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        assertEquals(0, service.getPendingSaveCount());
        
        CompletableFuture<Void> f1 = service.savePlayerDataAsync(uuid1, () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        CompletableFuture<Void> f2 = service.savePlayerDataAsync(uuid2, () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 等待任务提交
        Thread.sleep(50);
        
        assertTrue(service.getPendingSaveCount() > 0, "应该有待保存任务");
        
        CompletableFuture.allOf(f1, f2).get(5, TimeUnit.SECONDS);
        
        assertEquals(0, service.getPendingSaveCount(), "完成后队列应该为空");
    }
    
    /**
     * 测试: 清理玩家数据
     */
    @Test
    void testCleanup() throws Exception {
        UUID uuid = UUID.randomUUID();
        
        // 模拟保存失败
        CompletableFuture<Void> future = service.savePlayerDataAsync(uuid, () -> {
            throw new RuntimeException("失败");
        });
        
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // 忽略
        }
        
        assertEquals(1, service.getFailureCount(uuid));
        
        service.cleanup(uuid);
        
        assertEquals(0, service.getFailureCount(uuid), "清理后失败计数应该清零");
    }
    
    /**
     * 测试: 多次保存失败记录
     */
    @Test
    void testMultipleFailures() throws Exception {
        UUID uuid = UUID.randomUUID();
        
        for (int i = 0; i < 3; i++) {
            CompletableFuture<Void> future = service.savePlayerDataAsync(uuid, () -> {
                throw new RuntimeException("失败");
            });
            
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                // 忽略
            }
        }
        
        assertEquals(3, service.getFailureCount(uuid), "应该记录3次失败");
    }
    
    /**
     * 测试: 优雅关闭
     */
    @Test
    void testShutdown() throws Exception {
        UUID uuid = UUID.randomUUID();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        service.savePlayerDataAsync(uuid, () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completed.set(true);
        });
        
        service.shutdown();
        
        assertTrue(completed.get(), "关闭前应该等待任务完成");
    }
}
