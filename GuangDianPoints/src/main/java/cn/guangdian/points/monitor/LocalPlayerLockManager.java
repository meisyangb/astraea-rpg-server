package cn.guangdian.points.monitor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 本地玩家锁管理器
 * 用于并发控制，确保玩家操作的原子性
 *
 * @author GuangDian
 * @since 1.2.0
 */
public class LocalPlayerLockManager {

    private final Logger logger;
    private final long timeoutMs;
    private final Map<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private final LockStats stats = new LockStats();

    public LocalPlayerLockManager(Logger logger, long timeoutMs) {
        this.logger = logger;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 在锁保护下执行操作
     *
     * @param playerId 玩家UUID
     * @param operation 要执行的操作
     * @return 操作结果
     * @throws LockTimeoutException 如果获取锁超时
     */
    public <T> T executeWithLock(UUID playerId, Supplier<T> operation) throws LockTimeoutException {
        ReentrantLock lock = playerLocks.computeIfAbsent(playerId, k -> new ReentrantLock());

        boolean acquired = false;
        try {
            acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                stats.recordTimeout();
                throw new LockTimeoutException("获取玩家锁超时: " + playerId);
            }

            stats.recordAcquired();
            return operation.get();

        } catch (InterruptedException e) {
            stats.recordTimeout();
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("获取锁被中断: " + playerId);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    /**
     * 在锁保护下执行无返回值的操作
     *
     * @param playerId 玩家UUID
     * @param operation 要执行的操作
     * @throws LockTimeoutException 如果获取锁超时
     */
    public void executeWithLock(UUID playerId, Runnable operation) throws LockTimeoutException {
        executeWithLock(playerId, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 使用双锁保护执行转账操作
     *
     * @param from 转出玩家
     * @param to 转入玩家
     * @param operation 要执行的操作
     * @return 操作结果
     * @throws LockTimeoutException 如果获取锁超时
     */
    public <T> T executeWithDualLock(UUID from, UUID to, Supplier<T> operation) throws LockTimeoutException {
        // 为防止死锁，按UUID顺序获取锁
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = from.compareTo(to) < 0 ? to : from;

        ReentrantLock lock1 = playerLocks.computeIfAbsent(first, k -> new ReentrantLock());
        ReentrantLock lock2 = playerLocks.computeIfAbsent(second, k -> new ReentrantLock());

        boolean acquired1 = false;
        boolean acquired2 = false;

        try {
            acquired1 = lock1.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired1) {
                stats.recordTimeout();
                throw new LockTimeoutException("获取第一个玩家锁超时: " + first);
            }

            acquired2 = lock2.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired2) {
                stats.recordTimeout();
                throw new LockTimeoutException("获取第二个玩家锁超时: " + second);
            }

            stats.recordAcquired();
            stats.recordAcquired();
            return operation.get();

        } catch (InterruptedException e) {
            stats.recordTimeout();
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("获取锁被中断");
        } finally {
            if (acquired2) {
                lock2.unlock();
            }
            if (acquired1) {
                lock1.unlock();
            }
        }
    }

    /**
     * 使用双锁保护执行无返回值的操作
     */
    public void executeWithDualLock(UUID from, UUID to, Runnable operation) throws LockTimeoutException {
        executeWithDualLock(from, to, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 清理玩家锁资源
     *
     * @param playerId 玩家UUID
     */
    public void cleanup(UUID playerId) {
        ReentrantLock lock = playerLocks.get(playerId);
        if (lock != null && !lock.isLocked()) {
            playerLocks.remove(playerId);
        }
    }

    /**
     * 获取统计信息
     */
    public LockStats getStats() {
        return stats;
    }

    /**
     * 锁统计信息类
     */
    public static class LockStats {
        private long acquiredCount = 0;
        private long timeoutCount = 0;

        public void recordAcquired() {
            acquiredCount++;
        }

        public void recordTimeout() {
            timeoutCount++;
        }

        public long getAcquiredCount() {
            return acquiredCount;
        }

        public long getTimeoutCount() {
            return timeoutCount;
        }

        public void reset() {
            acquiredCount = 0;
            timeoutCount = 0;
        }

        public String toFormattedString() {
            return String.format("锁统计: 成功=%d, 超时=%d", acquiredCount, timeoutCount);
        }
    }

    /**
     * 锁超时异常
     */
    public static class LockTimeoutException extends Exception {
        public LockTimeoutException(String message) {
            super(message);
        }
    }
}