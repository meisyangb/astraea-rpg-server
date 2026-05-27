package cn.guangdian.rpgcore.monitoring;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.CacheStats;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.monitor.PerformanceMonitor;
import cn.guangdian.rpgcore.monitor.PerformanceMetrics;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsExporter {

    private static final String METRIC_PREFIX = "rpgcore_";
    private final RPGCore rpgCore;
    private final AtomicLong startTime;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;

    public MetricsExporter(RPGCore rpgCore) {
        this.rpgCore = rpgCore;
        this.startTime = new AtomicLong(System.currentTimeMillis());
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    public String exportPrometheusFormat() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);

        out.println("# HELP " + METRIC_PREFIX + "info RPGCore Information");
        out.println("# TYPE " + METRIC_PREFIX + "info gauge");
        out.println(METRIC_PREFIX + "info_version{version=\"" + rpgCore.getDescription().getVersion() + "\"} 1");

        exportJVMMetrics(out);
        exportCacheMetrics(out);
        exportPerformanceMetrics(out);

        return writer.toString();
    }

    private void exportJVMMetrics(PrintWriter out) {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        out.println("# HELP " + METRIC_PREFIX + "jvm_memory_heap_used JVM heap memory used");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_memory_heap_used gauge");
        out.println(METRIC_PREFIX + "jvm_memory_heap_used " + heap.getUsed());

        out.println("# HELP " + METRIC_PREFIX + "jvm_memory_heap_max JVM heap memory max");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_memory_heap_max gauge");
        out.println(METRIC_PREFIX + "jvm_memory_heap_max " + heap.getMax());

        out.println("# HELP " + METRIC_PREFIX + "jvm_memory_heap_committed JVM heap memory committed");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_memory_heap_committed gauge");
        out.println(METRIC_PREFIX + "jvm_memory_heap_committed " + heap.getCommitted());

        out.println("# HELP " + METRIC_PREFIX + "jvm_memory_nonheap_used JVM non-heap memory used");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_memory_nonheap_used gauge");
        out.println(METRIC_PREFIX + "jvm_memory_nonheap_used " + nonHeap.getUsed());

        out.println("# HELP " + METRIC_PREFIX + "jvm_threads_current JVM current thread count");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_threads_current gauge");
        out.println(METRIC_PREFIX + "jvm_threads_current " + threadBean.getThreadCount());

        out.println("# HELP " + METRIC_PREFIX + "jvm_threads_peak JVM peak thread count");
        out.println("# TYPE " + METRIC_PREFIX + "jvm_threads_peak gauge");
        out.println(METRIC_PREFIX + "jvm_threads_peak " + threadBean.getPeakThreadCount());

        long uptime = System.currentTimeMillis() - startTime.get();
        out.println("# HELP " + METRIC_PREFIX + "uptime_seconds Uptime in seconds");
        out.println("# TYPE " + METRIC_PREFIX + "uptime_seconds gauge");
        out.println(METRIC_PREFIX + "uptime_seconds " + (uptime / 1000.0));
    }

    private void exportCacheMetrics(PrintWriter out) {
        CacheProvider cacheProvider = rpgCore.getCacheProvider();
        if (cacheProvider == null) {
            return;
        }

        out.println("# HELP " + METRIC_PREFIX + "cache_size Cache entry count");
        out.println("# TYPE " + METRIC_PREFIX + "cache_size gauge");

        try {
            CacheStats stats = cacheProvider.getStats();
            out.println(METRIC_PREFIX + "cache_size{type=\"total\"} " + stats.getSize());
            out.println(METRIC_PREFIX + "cache_hits_total " + stats.getHitCount());
            out.println(METRIC_PREFIX + "cache_misses_total " + stats.getMissCount());
            out.println("# TYPE " + METRIC_PREFIX + "cache_hit_rate gauge");
            out.println(METRIC_PREFIX + "cache_hit_rate " + stats.getHitRate());
        } catch (Exception e) {
            out.println(METRIC_PREFIX + "cache_size{type=\"total\"} 0");
        }
    }

    private void exportPerformanceMetrics(PrintWriter out) {
        PerformanceMonitor monitor = rpgCore.getPerformanceMonitor();
        if (monitor != null) {
            out.println("# HELP " + METRIC_PREFIX + "performance_operations_total Total operation count");
            out.println("# TYPE " + METRIC_PREFIX + "performance_operations_total gauge");
            out.println(METRIC_PREFIX + "performance_operations_total " + monitor.getOperationNames().size());

            for (String opName : monitor.getOperationNames()) {
                PerformanceMetrics metrics = monitor.getMetrics(opName);
                if (metrics != null) {
                    String label = opName.replaceAll("[^a-zA-Z0-9]", "_");
                    out.println(METRIC_PREFIX + "performance_" + label + "_total " + metrics.getCount());
                    out.println(METRIC_PREFIX + "performance_" + label + "_avg_ms " + metrics.getAverageTime());
                }
            }
        }
    }

    public String exportJsonFormat() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        json.append("\"rpgcore\":{");
        json.append("\"version\":\"").append(rpgCore.getDescription().getVersion()).append("\",");
        json.append("\"uptime\":").append(System.currentTimeMillis() - startTime.get()).append(",");

        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        json.append("\"memory\":{");
        json.append("\"heapUsed\":").append(heap.getUsed()).append(",");
        json.append("\"heapMax\":").append(heap.getMax()).append(",");
        json.append("\"heapCommitted\":").append(heap.getCommitted());
        json.append("},");

        json.append("\"threads\":{");
        json.append("\"current\":").append(threadBean.getThreadCount()).append(",");
        json.append("\"peak\":").append(threadBean.getPeakThreadCount());
        json.append("},");

        CacheProvider cacheProvider = rpgCore.getCacheProvider();
        if (cacheProvider != null) {
            try {
                CacheStats stats = cacheProvider.getStats();
                json.append("\"cache\":{");
                json.append("\"size\":").append(stats.getSize()).append(",");
                json.append("\"hits\":").append(stats.getHitCount()).append(",");
                json.append("\"misses\":").append(stats.getMissCount()).append(",");
                json.append("\"hitRate\":").append(stats.getHitRate());
                json.append("}");
            } catch (Exception e) {
                json.append("\"cache\":{}");
            }
        }

        json.append("}}");
        return json.toString();
    }

    public void logCurrentMetrics() {
        rpgCore.getLogger().info("=== RPGCore Metrics Report ===");
        rpgCore.getLogger().info("Uptime: " + formatDuration(System.currentTimeMillis() - startTime.get()));

        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        rpgCore.getLogger().info("Heap Memory: " +
                formatBytes(heap.getUsed()) + " / " + formatBytes(heap.getMax()));

        rpgCore.getLogger().info("Threads: " + threadBean.getThreadCount() +
                " (Peak: " + threadBean.getPeakThreadCount() + ")");

        CacheProvider cacheProvider = rpgCore.getCacheProvider();
        if (cacheProvider != null) {
            try {
                CacheStats stats = cacheProvider.getStats();
                rpgCore.getLogger().info("Cache: " + stats.getSize() + " entries, " +
                        "Hit Rate: " + String.format("%.2f%%", stats.getHitRate() * 100));
            } catch (Exception ignored) {
            }
        }

        rpgCore.getLogger().info("================================");
    }

    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        long kb = bytes / 1024;
        long mb = kb / 1024;
        long gb = mb / 1024;
        if (gb > 0) return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
        if (mb > 0) return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
        if (kb > 0) return String.format("%.2f KB", bytes / 1024.0);
        return bytes + " B";
    }
}
