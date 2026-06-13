package cn.guangdian.points.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 性能报告类
 * 生成和输出性能报告
 */
public class PerformanceReport {

    private final String moduleName;
    private final Map<String, PerformanceMetrics> metrics;
    private final long startTime;
    private final long reportTime;

    /**
     * 创建性能报告
     *
     * @param moduleName 模块名称
     * @param metrics 指标映射
     * @param startTime 开始时间
     */
    public PerformanceReport(String moduleName, Map<String, PerformanceMetrics> metrics, long startTime) {
        this.moduleName = moduleName;
        this.metrics = metrics;
        this.startTime = startTime;
        this.reportTime = System.currentTimeMillis();
    }

    /**
     * 获取运行时间（毫秒）
     *
     * @return 运行时间
     */
    public long getUptime() {
        return reportTime - startTime;
    }

    /**
     * 获取格式化的运行时间
     *
     * @return 格式化的运行时间
     */
    public String getFormattedUptime() {
        long uptime = getUptime();
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (uptime % (60 * 1000)) / 1000;

        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 生成格式化报告
     *
     * @return 格式化报告字符串
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(reportTime));

        sb.append("========== 性能监控报告 ==========\n");
        sb.append("模块: ").append(moduleName).append("\n");
        sb.append("生成时间: ").append(timestamp).append("\n");
        sb.append("运行时长: ").append(getFormattedUptime()).append("\n");
        sb.append("----------------------------------\n");

        long totalOps = 0;
        long totalDuration = 0;
        long totalCacheHits = 0;
        long totalCacheMisses = 0;
        long totalLockAcquired = 0;
        long totalLockTimeout = 0;

        for (Map.Entry<String, PerformanceMetrics> entry : metrics.entrySet()) {
            PerformanceMetrics m = entry.getValue();
            sb.append(m.toDetailedString());

            totalOps += m.getOperationCount();
            totalDuration += m.getTotalDuration();
            totalCacheHits += m.getCacheHits();
            totalCacheMisses += m.getCacheMisses();
            totalLockAcquired += m.getLockAcquired();
            totalLockTimeout += m.getLockTimeout();
        }

        sb.append("==================================\n");
        sb.append("汇总统计:\n");
        sb.append("  总操作次数: ").append(totalOps).append("\n");

        if (totalOps > 0) {
            sb.append(String.format("  平均耗时: %.2f ms\n", (double) totalDuration / totalOps));
        }

        long cacheTotal = totalCacheHits + totalCacheMisses;
        if (cacheTotal > 0) {
            sb.append(String.format("  缓存命中率: %.1f%% (%d/%d)\n",
                (double) totalCacheHits / cacheTotal * 100,
                totalCacheHits, cacheTotal));
        }

        long lockTotal = totalLockAcquired + totalLockTimeout;
        if (lockTotal > 0) {
            sb.append(String.format("  锁成功率: %.1f%% (%d/%d)\n",
                (double) totalLockAcquired / lockTotal * 100,
                totalLockAcquired, lockTotal));
        }

        sb.append("==================================");

        return sb.toString();
    }

    /**
     * 生成简要报告
     *
     * @return 简要报告字符串
     */
    public String toBriefString() {
        long totalOps = 0;
        long totalDuration = 0;
        long totalCacheHits = 0;
        long totalCacheMisses = 0;

        for (PerformanceMetrics m : metrics.values()) {
            totalOps += m.getOperationCount();
            totalDuration += m.getTotalDuration();
            totalCacheHits += m.getCacheHits();
            totalCacheMisses += m.getCacheMisses();
        }

        double avgDuration = totalOps > 0 ? (double) totalDuration / totalOps : 0;
        double hitRate = (totalCacheHits + totalCacheMisses) > 0
            ? (double) totalCacheHits / (totalCacheHits + totalCacheMisses) * 100 : 0;

        return String.format("[%s] uptime=%s, ops=%d, avg=%.2fms, cache=%.1f%%",
            moduleName, getFormattedUptime(), totalOps, avgDuration, hitRate);
    }

    /**
     * 写入文件
     *
     * @param file 目标文件
     * @throws IOException 写入失败时抛出
     */
    public void writeToFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(toFormattedString());
            writer.write("\n\n");
        }
    }

    /**
     * 获取模块名称
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * 获取指标映射
     */
    public Map<String, PerformanceMetrics> getMetrics() {
        return metrics;
    }

    /**
     * 获取开始时间
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取报告时间
     */
    public long getReportTime() {
        return reportTime;
    }
}