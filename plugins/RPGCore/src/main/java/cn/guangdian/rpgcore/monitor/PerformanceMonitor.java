package cn.guangdian.rpgcore.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 * 
 * <p>提供操作计时和性能指标收集功能。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用 try-with-resources 自动计时
 * try (OperationTimer timer = monitor.startOperation("loadPlayerData")) {
 *     // 业务逻辑
 * } // 自动记录耗时
 * 
 * // 手动记录指标
 * monitor.recordMetric("cacheHitRate", 0.85);
 * 
 * // 生成报告
 * PerformanceReport report = monitor.generateReport();
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PerformanceMonitor {

    private final String name;
    private final AtomicBoolean enabled;
    private final Map<String, PerformanceMetrics> metricsMap;

    /**
     * 创建性能监控器
     * 
     * @param name 监控器名称
     */
    public PerformanceMonitor(String name) {
        this.name = name;
        this.enabled = new AtomicBoolean(true);
        this.metricsMap = new ConcurrentHashMap<>();
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
        if (!enabled.get()) {
            return;
        }
        getOrCreateMetrics(operationName).record(durationMs);
    }

    /**
     * 记录指标
     * 
     * @param metricName 指标名称
     * @param value 指标值
     */
    public void recordMetric(String metricName, double value) {
        if (!enabled.get()) {
            return;
        }
        getOrCreateMetrics(metricName).recordValue(value);
    }

    /**
     * 记录缓存命中
     * 
     * @param cacheName 缓存名称
     */
    public void recordCacheHit(String cacheName) {
        if (!enabled.get()) {
            return;
        }
        getOrCreateMetrics("cache:" + cacheName).recordHit();
    }

    /**
     * 记录缓存未命中
     * 
     * @param cacheName 缓存名称
     */
    public void recordCacheMiss(String cacheName) {
        if (!enabled.get()) {
            return;
        }
        getOrCreateMetrics("cache:" + cacheName).recordMiss();
    }

    /**
     * 生成性能报告
     * 
     * @return 性能报告
     */
    public PerformanceReport generateReport() {
        return new PerformanceReport(name, Map.copyOf(metricsMap));
    }

    /**
     * 重置所有统计
     */
    public void reset() {
        metricsMap.clear();
    }

    /**
     * 检查是否启用
     * 
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 设置启用状态
     * 
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    /**
     * 获取监控器名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取指定操作的指标
     * 
     * @param operationName 操作名称
     * @return 性能指标，如果不存在返回 null
     */
    public PerformanceMetrics getMetrics(String operationName) {
        return metricsMap.get(operationName);
    }

    /**
     * 获取所有操作名称
     */
    public java.util.Set<String> getOperationNames() {
        return metricsMap.keySet();
    }

    private PerformanceMetrics getOrCreateMetrics(String name) {
        return metricsMap.computeIfAbsent(name, k -> new PerformanceMetrics(k));
    }
}