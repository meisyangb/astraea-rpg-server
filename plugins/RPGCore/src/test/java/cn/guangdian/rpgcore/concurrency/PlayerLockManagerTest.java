package cn.guangdian.rpgcore.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerLockManager 单元测试
 * 
 * @author GuangDian
 * @since 1.0.0
 */
@DisplayName("玩家锁管理器测试")
class PlayerLockManagerTest {

    private PlayerLockManager lockManager;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        lockManager = new PlayerLockManager(Logger.getLogger("TestLogger"), 1000);
        testPlayerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("基本锁操作 - 有返回值")
    void testBasicLockOperationWithReturn() throws LockTimeoutException {
        AtomicInteger counter = new AtomicInteger(0);
        
        Integer result = lockManager.executeWithLock(testPlayerId, () -> {
            counter.incrementAndGet();
            return 42;
        });
        
        assertEquals(42, result);
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("基本锁操作 - 无返回值")
    void testBasicLockOperationNoReturn() throws LockTimeoutException {
        AtomicInteger counter = new AtomicInteger(0);
        
        lockManager.executeWithLock(testPlayerId, () -> {
            counter.incrementAndGet();
        });
        
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("双锁操作 - 不同玩家")
    void testDualLockDifferentPlayers() throws LockTimeoutException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger(0);
        
        lockManager.executeWithDualLock(player1, player2, () -> {
            counter.incrementAndGet();
        });
        
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("双锁操作 - 相同玩家降级为单锁")
    void testDualLockSamePlayer() throws LockTimeoutException {
        AtomicInteger counter = new AtomicInteger(0);
        
        lockManager.executeWithDualLock(testPlayerId, testPlayerId, () -> {
            counter.incrementAndGet();
        });
        
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("锁超时抛出异常")
    void testLockTimeout() throws Exception {
        PlayerLockManager shortTimeoutManager = new PlayerLockManager(
            Logger.getLogger("TestLogger"), 100);
        
        CountDownLatch heldLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.submit(() -> {
            try {
                shortTimeoutManager.executeWithLock(testPlayerId, () -> {
                    heldLatch.countDown();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (LockTimeoutException e) {
                // ignore
            }
            finishLatch.countDown();
        });
        
        heldLatch.await();
        
        assertThrows(LockTimeoutException.class, () -> {
            shortTimeoutManager.executeWithLock(testPlayerId, () -> "should timeout");
        });
        
        finishLatch.await();
        executor.shutdown();
    }

    @Test
    @DisplayName("并发锁操作")
    void testConcurrentLockOperations() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        lockManager.executeWithLock(testPlayerId, () -> {
                            counter.incrementAndGet();
                            return null;
                        });
                    }
                } catch (LockTimeoutException e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(threadCount * operationsPerThread, counter.get());
        assertEquals(0, errorCount.get());
    }

    @Test
    @DisplayName("死锁预防测试")
    void testDeadlockPrevention() throws Exception {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        int iterations = 10;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(iterations * 2);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    lockManager.executeWithDualLock(player1, player2, () -> {
                        successCount.incrementAndGet();
                        return null;
                    });
                } catch (LockTimeoutException e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
            
            executor.submit(() -> {
                try {
                    lockManager.executeWithDualLock(player2, player1, () -> {
                        successCount.incrementAndGet();
                        return null;
                    });
                } catch (LockTimeoutException e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertTrue(successCount.get() > 0);
    }

    @Test
    @DisplayName("锁统计")
    void testLockStats() throws LockTimeoutException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        lockManager.executeWithLock(player1, () -> "test1");
        lockManager.executeWithLock(player2, () -> "test2");
        
        LockStats stats = lockManager.getStats();
        
        assertEquals(2, stats.getAcquireCount());
        assertEquals(0, stats.getTimeoutCount());
    }

    @Test
    @DisplayName("释放玩家锁")
    void testReleaseLock() throws LockTimeoutException {
        lockManager.executeWithLock(testPlayerId, () -> "test");
        
        lockManager.releaseLock(testPlayerId);
        
        assertEquals(0, lockManager.getLockCount());
    }

    @Test
    @DisplayName("清理玩家资源")
    void testCleanup() throws LockTimeoutException {
        lockManager.executeWithLock(testPlayerId, () -> "test");
        
        lockManager.cleanup(testPlayerId);
        
        assertEquals(0, lockManager.getLockCount());
    }
}
