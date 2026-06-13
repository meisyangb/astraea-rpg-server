package cn.guangdian.rpgcore.monitor;

import java.util.Map;

/**
 * 性能报告
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PerformanceReport {

    private final String monitorName;
    private final Map<String, PerformanceMetrics> metrics;
    private final long generatedAt;

    /**
     * 创建性能报告
     * 
     * @param monitorName 监控器名称
     * @param metrics 指标映射
     */
    public PerformanceReport(String monitorName, Map<String, PerformanceMetrics> metrics) {
        this.monitorName = monitorName;
        this.metrics = metrics;
        this.generatedAt = System.currentTimeMillis();
    }

    /**
     * 获取监控器名称
     */
    public String getMonitorName() {
        return monitorName;
    }

    /**
     * 获取所有指标
     */
    public Map<String, PerformanceMetrics> getMetrics() {
        return metrics;
    }

    /**
     * 获取指定指标
     * 
     * @param name 指标名称
     * @return 性能指标，如果不存在返回 null
     */
    public PerformanceMetrics getMetrics(String name) {
        return metrics.get(name);
    }

    /**
     * 获取生成时间
     */
    public long getGeneratedAt() {
        return generatedAt;
    }

    /**
     * 获取指标数量
     */
    public int getMetricsCount() {
        return metrics.size();
    }

    /**
     * 获取总操作次数
     */
    public long getTotalOperationCount() {
        return metrics.values().stream()
            .filter(m -> !m.getName().startsWith("cache:"))
            .mapToLong(PerformanceMetrics::getCount)
            .sum();
    }

    /**
     * 获取平均耗时
     */
    public double getOverallAverageTime() {
        double totalTime = 0;
        long totalCount = 0;
        
        for (PerformanceMetrics m : metrics.values()) {
            if (!m.getName().startsWith("cache:")) {
                totalTime += m.getTotalTime();
                totalCount += m.getCount();
            }
        }
        
        return totalCount == 0 ? 0 : totalTime / totalCount;
    }

    /**
     * 获取整体缓存命中率
     */
    public double getOverallCacheHitRate() {
        long totalHits = 0;
        long totalMisses = 0;
        
        for (PerformanceMetrics m : metrics.values()) {
            if (m.getName().startsWith("cache:")) {
                totalHits += m.getCacheHits();
                totalMisses += m.getCacheMisses();
            }
        }
        
        long total = totalHits + totalMisses;
        return total == 0 ? 0 : (double) totalHits / total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== Performance Report: ").append(monitorName).append(" ==========\n");
        sb.append("Generated at: ").append(new java.util.Date(generatedAt)).append("\n");
        sb.append("Total operations: ").append(getTotalOperationCount()).append("\n");
        sb.append("Overall average time: ").append(String.format("%.2f", getOverallAverageTime())).append("ms\n");
        sb.append("Overall cache hit rate: ").append(String.format("%.2f", getOverallCacheHitRate() * 100)).append("%\n");
        sb.append("\n--- Detailed Metrics ---\n");
        
        for (PerformanceMetrics m : metrics.values()) {
            sb.append(m.toString()).append("\n");
        }
        
        sb.append("================================================");
        return sb.toString();
    }
}