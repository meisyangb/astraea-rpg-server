package cn.guangdian.points.monitor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能指标类
 * 记录单个操作的统计数据
 */
public class PerformanceMetrics {

    private final String operationName;
    private final AtomicLong operationCount = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDuration = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong lockAcquired = new AtomicLong(0);
    private final AtomicLong lockTimeout = new AtomicLong(0);

    /**
     * 创建性能指标
     *
     * @param operationName 操作名称
     */
    public PerformanceMetrics(String operationName) {
        this.operationName = operationName;
    }

    /**
     * 记录操作
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordOperation(long durationMs) {
        operationCount.incrementAndGet();
        totalDuration.addAndGet(durationMs);

        // 更新最小值
        long currentMin;
        do {
            currentMin = minDuration.get();
            if (durationMs >= currentMin) break;
        } while (!minDuration.compareAndSet(currentMin, durationMs));

        // 更新最大值
        long currentMax;
        do {
            currentMax = maxDuration.get();
            if (durationMs <= currentMax) break;
        } while (!maxDuration.compareAndSet(currentMax, durationMs));
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * 记录锁获取成功
     */
    public void recordLockAcquired() {
        lockAcquired.incrementAndGet();
    }

    /**
     * 记录锁超时
     */
    public void recordLockTimeout() {
        lockTimeout.incrementAndGet();
    }

    /**
     * 获取操作名称
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * 获取操作次数
     */
    public long getOperationCount() {
        return operationCount.get();
    }

    /**
     * 获取总耗时
     */
    public long getTotalDuration() {
        return totalDuration.get();
    }

    /**
     * 获取最小耗时
     */
    public long getMinDuration() {
        long min = minDuration.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * 获取最大耗时
     */
    public long getMaxDuration() {
        return maxDuration.get();
    }

    /**
     * 获取平均耗时
     */
    public double getAverageDuration() {
        long count = operationCount.get();
        if (count == 0) return 0;
        return (double) totalDuration.get() / count;
    }

    /**
     * 获取缓存命中次数
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * 获取缓存未命中次数
     */
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        if (total == 0) return 0;
        return (double) hits / total * 100;
    }

    /**
     * 获取锁获取成功次数
     */
    public long getLockAcquired() {
        return lockAcquired.get();
    }

    /**
     * 获取锁超时次数
     */
    public long getLockTimeout() {
        return lockTimeout.get();
    }

    /**
     * 获取锁成功率
     */
    public double getLockSuccessRate() {
        long acquired = lockAcquired.get();
        long timeout = lockTimeout.get();
        long total = acquired + timeout;
        if (total == 0) return 100;
        return (double) acquired / total * 100;
    }

    /**
     * 重置统计
     */
    public void reset() {
        operationCount.set(0);
        totalDuration.set(0);
        minDuration.set(Long.MAX_VALUE);
        maxDuration.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        lockAcquired.set(0);
        lockTimeout.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{name='%s', count=%d, avg=%.2fms, min=%dms, max=%dms, cache_hit=%.1f%%}",
            operationName, getOperationCount(), getAverageDuration(),
            getMinDuration(), getMaxDuration(), getCacheHitRate()
        );
    }

    /**
     * 获取格式化的详细信息
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  Operation: ").append(operationName).append("\n");
        sb.append("    Count: ").append(getOperationCount()).append("\n");
        sb.append(String.format("    Duration: avg=%.2fms, min=%dms, max=%dms, total=%dms\n",
            getAverageDuration(), getMinDuration(), getMaxDuration(), getTotalDuration()));

        long cacheTotal = cacheHits.get() + cacheMisses.get();
        if (cacheTotal > 0) {
            sb.append(String.format("    Cache: hits=%d, misses=%d, hit_rate=%.1f%%\n",
                cacheHits.get(), cacheMisses.get(), getCacheHitRate()));
        }

        long lockTotal = lockAcquired.get() + lockTimeout.get();
        if (lockTotal > 0) {
            sb.append(String.format("    Lock: acquired=%d, timeout=%d, success_rate=%.1f%%\n",
                lockAcquired.get(), lockTimeout.get(), getLockSuccessRate()));
        }

        return sb.toString();
    }
}