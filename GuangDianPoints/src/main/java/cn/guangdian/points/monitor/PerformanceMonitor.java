package cn.guangdian.points.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 性能监控器
 * 记录和统计各模块性能指标
 */
public class PerformanceMonitor {

    private final String moduleName;
    private final Map<String, PerformanceMetrics> metricsMap;
    private final AtomicBoolean enabled;
    private long startTime;

    /**
     * 创建性能监控器
     *
     * @param moduleName 模块名称
     */
    public PerformanceMonitor(String moduleName) {
        this.moduleName = moduleName;
        this.metricsMap = new ConcurrentHashMap<>();
        this.enabled = new AtomicBoolean(true);
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 开始操作计时
     *
     * @param operationName 操作名称
     * @return 操作计时器
     */
    public OperationTimer startOperation(String operationName) {
        if (!enabled.get()) {
            return OperationTimer.NOOP;
        }
        return new OperationTimer(this, operationName);
    }

    /**
     * 记录操作完成
     *
     * @param operationName 操作名称
     * @param durationMs 耗时（毫秒）
     */
    public void recordOperation(String operationName, long durationMs) {
        if (!enabled.get()) return;

        PerformanceMetrics metrics = metricsMap.computeIfAbsent(operationName,
            k -> new PerformanceMetrics(operationName));
        metrics.recordOperation(durationMs);
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        recordCacheHit("default");
    }

    /**
     * 记录缓存命中
     *
     * @param cacheName 缓存名称
     */
    public void recordCacheHit(String cacheName) {
        if (!enabled.get()) return;

        String key = "cache_" + cacheName;
        PerformanceMetrics metrics = metricsMap.computeIfAbsent(key,
            k -> new PerformanceMetrics(key));
        metrics.recordCacheHit();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        recordCacheMiss("default");
    }

    /**
     * 记录缓存未命中
     *
     * @param cacheName 缓存名称
     */
    public void recordCacheMiss(String cacheName) {
        if (!enabled.get()) return;

        String key = "cache_" + cacheName;
        PerformanceMetrics metrics = metricsMap.computeIfAbsent(key,
            k -> new PerformanceMetrics(key));
        metrics.recordCacheMiss();
    }

    /**
     * 记录锁获取成功
     */
    public void recordLockAcquired() {
        if (!enabled.get()) return;

        PerformanceMetrics metrics = metricsMap.computeIfAbsent("lock",
            k -> new PerformanceMetrics("lock"));
        metrics.recordLockAcquired();
    }

    /**
     * 记录锁超时
     */
    public void recordLockTimeout() {
        if (!enabled.get()) return;

        PerformanceMetrics metrics = metricsMap.computeIfAbsent("lock",
            k -> new PerformanceMetrics("lock"));
        metrics.recordLockTimeout();
    }

    /**
     * 获取指标
     *
     * @param operationName 操作名称
     * @return 性能指标
     */
    public PerformanceMetrics getMetrics(String operationName) {
        return metricsMap.get(operationName);
    }

    /**
     * 获取所有指标
     *
     * @return 指标映射
     */
    public Map<String, PerformanceMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    /**
     * 生成性能报告
     *
     * @return 性能报告
     */
    public PerformanceReport generateReport() {
        return new PerformanceReport(moduleName, getAllMetrics(), startTime);
    }

    /**
     * 重置统计
     */
    public void reset() {
        metricsMap.clear();
        startTime = System.currentTimeMillis();
    }

    /**
     * 启用监控
     */
    public void enable() {
        enabled.set(true);
    }

    /**
     * 禁用监控
     */
    public void disable() {
        enabled.set(false);
    }

    /**
     * 检查是否启用
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 获取模块名称
     *
     * @return 模块名称
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * 获取运行时间（毫秒）
     *
     * @return 运行时间
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取简要统计
     *
     * @return 简要统计字符串
     */
    public String getSummary() {
        int totalOperations = 0;
        long totalDuration = 0;
        long totalCacheHits = 0;
        long totalCacheMisses = 0;

        for (PerformanceMetrics metrics : metricsMap.values()) {
            totalOperations += metrics.getOperationCount();
            totalDuration += metrics.getTotalDuration();
            totalCacheHits += metrics.getCacheHits();
            totalCacheMisses += metrics.getCacheMisses();
        }

        double avgDuration = totalOperations > 0 ? (double) totalDuration / totalOperations : 0;
        double hitRate = (totalCacheHits + totalCacheMisses) > 0
            ? (double) totalCacheHits / (totalCacheHits + totalCacheMisses) * 100 : 0;

        return String.format("[%s] ops=%d, avg=%.2fms, cache_hit=%.1f%%",
            moduleName, totalOperations, avgDuration, hitRate);
    }
}