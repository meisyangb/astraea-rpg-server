package cn.guangdian.rpgcore.concurrency;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 锁统计信息
 * 
 * <p>记录锁获取、超时和等待时间统计，支持多种命名风格的API。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LockStats {

    private final AtomicLong acquireCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicLong maxWaitTime = new AtomicLong(0);

    /**
     * 记录锁获取成功
     */
    public void recordAcquire() {
        acquireCount.incrementAndGet();
    }

    /**
     * 记录锁获取成功（兼容别名）
     */
    public void recordLockAcquired() {
        recordAcquire();
    }

    /**
     * 记录锁获取超时
     */
    public void recordTimeout() {
        timeoutCount.incrementAndGet();
    }

    /**
     * 记录锁获取超时（兼容别名）
     */
    public void recordLockTimeout() {
        recordTimeout();
    }

    /**
     * 记录等待时间
     * 
     * @param waitTimeMs 等待时间（毫秒）
     */
    public void recordWaitTime(long waitTimeMs) {
        totalWaitTime.addAndGet(waitTimeMs);
        updateMaxWaitTime(waitTimeMs);
    }

    private void updateMaxWaitTime(long waitTimeMs) {
        long currentMax;
        do {
            currentMax = maxWaitTime.get();
            if (waitTimeMs <= currentMax) {
                break;
            }
        } while (!maxWaitTime.compareAndSet(currentMax, waitTimeMs));
    }

    /**
     * 获取锁获取成功次数
     */
    public long getAcquireCount() {
        return acquireCount.get();
    }

    /**
     * 获取锁获取成功次数（兼容别名）
     */
    public long getLockAcquired() {
        return getAcquireCount();
    }

    /**
     * 获取锁获取超时次数
     */
    public long getTimeoutCount() {
        return timeoutCount.get();
    }

    /**
     * 获取锁获取超时次数（兼容别名）
     */
    public long getLockTimeout() {
        return getTimeoutCount();
    }

    /**
     * 获取总等待时间
     */
    public long getTotalWaitTime() {
        return totalWaitTime.get();
    }

    /**
     * 获取最大等待时间
     */
    public long getMaxWaitTime() {
        return maxWaitTime.get();
    }

    /**
     * 获取平均等待时间
     */
    public double getAverageWaitTime() {
        long acquires = acquireCount.get();
        return acquires == 0 ? 0 : (double) totalWaitTime.get() / acquires;
    }

    /**
     * 获取超时率
     */
    public double getTimeoutRate() {
        long total = acquireCount.get() + timeoutCount.get();
        return total == 0 ? 0 : (double) timeoutCount.get() / total;
    }

    /**
     * 获取锁成功率
     */
    public double getSuccessRate() {
        long acquired = acquireCount.get();
        long timeout = timeoutCount.get();
        long total = acquired + timeout;
        if (total == 0) {
            return 100.0;
        }
        return (double) acquired / total * 100.0;
    }

    /**
     * 重置统计
     */
    public void reset() {
        acquireCount.set(0);
        timeoutCount.set(0);
        totalWaitTime.set(0);
        maxWaitTime.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "LockStats{acquires=%d, timeouts=%d, timeoutRate=%.2f%%, avgWait=%.2fms}",
            acquireCount.get(), timeoutCount.get(), getTimeoutRate() * 100, getAverageWaitTime()
        );
    }

    /**
     * 获取格式化的统计信息
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 锁统计信息 ==========\n");
        sb.append(String.format("锁获取成功: %d 次\n", getLockAcquired()));
        sb.append(String.format("锁获取超时: %d 次\n", getLockTimeout()));
        sb.append(String.format("平均等待时间: %.2f ms\n", getAverageWaitTime()));
        sb.append(String.format("最大等待时间: %d ms\n", getMaxWaitTime()));
        sb.append(String.format("成功率: %.1f%%\n", getSuccessRate()));
        sb.append("================================");
        return sb.toString();
    }
}