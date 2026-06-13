package cn.guangdian.rpgcore.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 玩家锁管理器
 * 
 * <p>提供玩家级别的细粒度锁，避免全局锁竞争。
 * 支持超时机制和死锁预防。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 单锁操作
 * lockManager.executeWithLock(playerId, () -> {
 *     // 临界区操作
 *     return result;
 * });
 * 
 * // 双锁操作（如转账）
 * lockManager.executeWithDualLock(fromPlayerId, toPlayerId, () -> {
 *     // 临界区操作
 *     return result;
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerLockManager {

    private final Logger logger;
    private final ConcurrentHashMap<UUID, ReentrantLock> locks;
    private final long defaultTimeoutMs;
    private final LockStats stats;

    /**
     * 创建玩家锁管理器
     * 
     * @param logger 日志记录器
     * @param defaultTimeoutMs 默认锁超时时间（毫秒）
     */
    public PlayerLockManager(Logger logger, long defaultTimeoutMs) {
        this.logger = logger;
        this.locks = new ConcurrentHashMap<>();
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.stats = new LockStats();
    }

    /**
     * 创建玩家锁管理器（默认超时3秒）
     */
    public PlayerLockManager(Logger logger) {
        this(logger, 3000);
    }

    /**
     * 使用锁执行操作
     * 
     * @param playerId 玩家UUID
     * @param operation 操作
     * @return 操作结果
     * @param <T> 返回类型
     * @throws LockTimeoutException 如果获取锁超时
     */
    public <T> T executeWithLock(UUID playerId, Supplier<T> operation) throws LockTimeoutException {
        ReentrantLock lock = getOrCreateLock(playerId);
        
        boolean acquired = false;
        try {
            acquired = tryLock(lock, defaultTimeoutMs);
            if (!acquired) {
                stats.recordTimeout();
                throw new LockTimeoutException("Failed to acquire lock for player " + playerId + 
                    " within " + defaultTimeoutMs + "ms");
            }
            stats.recordAcquire();
            
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("Lock acquisition interrupted for player " + playerId);
        } finally {
            if (acquired) {
                unlock(lock);
            }
        }
    }

    /**
     * 使用锁执行操作（无返回值）
     * 
     * @param playerId 玩家UUID
     * @param operation 操作
     * @throws LockTimeoutException 如果获取锁超时
     */
    public void executeWithLock(UUID playerId, Runnable operation) throws LockTimeoutException {
        executeWithLock(playerId, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 使用双锁执行操作（防止死锁）
     * 
     * <p>按UUID顺序获取锁，避免死锁。</p>
     * 
     * @param playerId1 第一个玩家UUID
     * @param playerId2 第二个玩家UUID
     * @param operation 操作
     * @return 操作结果
     * @param <T> 返回类型
     * @throws LockTimeoutException 如果获取锁超时
     */
    public <T> T executeWithDualLock(UUID playerId1, UUID playerId2, Supplier<T> operation) 
            throws LockTimeoutException {
        // 按UUID排序获取锁，防止死锁
        UUID first = playerId1.compareTo(playerId2) < 0 ? playerId1 : playerId2;
        UUID second = playerId1.compareTo(playerId2) < 0 ? playerId2 : playerId1;

        // 如果是同一个玩家，使用单锁
        if (first.equals(second)) {
            return executeWithLock(first, operation);
        }

        ReentrantLock lock1 = getOrCreateLock(first);
        ReentrantLock lock2 = getOrCreateLock(second);

        boolean acquired1 = false;
        boolean acquired2 = false;

        try {
            acquired1 = tryLock(lock1, defaultTimeoutMs);
            if (!acquired1) {
                stats.recordTimeout();
                throw new LockTimeoutException("Failed to acquire first lock for dual operation: " + first);
            }

            acquired2 = tryLock(lock2, defaultTimeoutMs);
            if (!acquired2) {
                stats.recordTimeout();
                throw new LockTimeoutException("Failed to acquire second lock for dual operation: " + second);
            }

            stats.recordAcquire();
            stats.recordAcquire();

            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("Lock acquisition interrupted during dual lock operation");
        } finally {
            if (acquired2) {
                unlock(lock2);
            }
            if (acquired1) {
                unlock(lock1);
            }
        }
    }

    /**
     * 使用双锁执行操作（无返回值）
     */
    public void executeWithDualLock(UUID playerId1, UUID playerId2, Runnable operation) 
            throws LockTimeoutException {
        executeWithDualLock(playerId1, playerId2, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 释放玩家锁（仅在玩家退出时调用）
     * 
     * @param playerId 玩家UUID
     */
    public void releaseLock(UUID playerId) {
        ReentrantLock lock = locks.remove(playerId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 释放所有锁
     */
    public void releaseAllLocks() {
        for (ReentrantLock lock : locks.values()) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        locks.clear();
    }

    /**
     * 清理玩家资源
     * 
     * @param playerId 玩家UUID
     */
    public void cleanup(UUID playerId) {
        locks.remove(playerId);
    }

    /**
     * 获取锁统计信息
     */
    public LockStats getStats() {
        return stats;
    }

    /**
     * 获取当前锁数量
     */
    public int getLockCount() {
        return locks.size();
    }

    private ReentrantLock getOrCreateLock(UUID playerId) {
        return locks.computeIfAbsent(playerId, k -> new ReentrantLock(true));
    }

    private boolean tryLock(ReentrantLock lock, long timeoutMs) throws InterruptedException {
        return lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void unlock(ReentrantLock lock) {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            logger.log(Level.WARNING, "Attempted to unlock a lock not held by current thread", e);
        }
    }
}