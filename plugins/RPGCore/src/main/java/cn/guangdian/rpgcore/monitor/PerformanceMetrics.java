package cn.guangdian.rpgcore.monitor;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能指标
 * 
 * <p>记录单个操作的性能数据，包括调用次数、耗时统计、缓存命中/未命中等。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PerformanceMetrics {

    private final String name;
    private final LongAdder count = new LongAdder();
    private final DoubleAdder totalTime = new DoubleAdder();
    private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final DoubleAdder totalValue = new DoubleAdder();

    /**
     * 创建性能指标
     * 
     * @param name 指标名称
     */
    public PerformanceMetrics(String name) {
        this.name = name;
    }

    /**
     * 记录操作耗时
     * 
     * @param durationMs 耗时（毫秒）
     */
    public void record(long durationMs) {
        count.increment();
        totalTime.add(durationMs);
        updateMin(durationMs);
        updateMax(durationMs);
    }

    /**
     * 记录值（用于非时间指标）
     * 
     * @param value 值
     */
    public void recordValue(double value) {
        totalValue.add(value);
        count.increment();
    }

    /**
     * 记录缓存命中
     */
    public void recordHit() {
        cacheHits.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        cacheMisses.increment();
    }

    /**
     * 获取指标名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取调用次数
     */
    public long getCount() {
        return count.sum();
    }

    /**
     * 获取总耗时
     */
    public double getTotalTime() {
        return totalTime.sum();
    }

    /**
     * 获取平均耗时
     */
    public double getAverageTime() {
        long c = count.sum();
        return c == 0 ? 0 : totalTime.sum() / c;
    }

    /**
     * 获取最小耗时
     */
    public long getMinTime() {
        long min = minTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * 获取最大耗时
     */
    public long getMaxTime() {
        long max = maxTime.get();
        return max == Long.MIN_VALUE ? 0 : max;
    }

    /**
     * 获取缓存命中次数
     */
    public long getCacheHits() {
        return cacheHits.sum();
    }

    /**
     * 获取缓存未命中次数
     */
    public long getCacheMisses() {
        return cacheMisses.sum();
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }

    /**
     * 获取总值（用于非时间指标）
     */
    public double getTotalValue() {
        return totalValue.sum();
    }

    /**
     * 获取平均值（用于非时间指标）
     */
    public double getAverageValue() {
        long c = count.sum();
        return c == 0 ? 0 : totalValue.sum() / c;
    }

    /**
     * 重置统计
     */
    public void reset() {
        count.reset();
        totalTime.reset();
        minTime.set(Long.MAX_VALUE);
        maxTime.set(Long.MIN_VALUE);
        cacheHits.reset();
        cacheMisses.reset();
        totalValue.reset();
    }

    private void updateMin(long value) {
        long current;
        do {
            current = minTime.get();
            if (value >= current) {
                return;
            }
        } while (!minTime.compareAndSet(current, value));
    }

    private void updateMax(long value) {
        long current;
        do {
            current = maxTime.get();
            if (value <= current) {
                return;
            }
        } while (!maxTime.compareAndSet(current, value));
    }

    @Override
    public String toString() {
        if (name.startsWith("cache:")) {
            return String.format("%s{hits=%d, misses=%d, hitRate=%.2f%%}",
                name, getCacheHits(), getCacheMisses(), getCacheHitRate() * 100);
        }
        return String.format("%s{count=%d, avg=%.2fms, min=%dms, max=%dms}",
            name, getCount(), getAverageTime(), getMinTime(), getMaxTime());
    }
}