package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPGCore 事件性能监控器
 *
 * <p>监控事件处理性能，提供告警和统计功能：</p>
 * <ul>
 *   <li>事件处理耗时统计</li>
 *   <li>慢事件告警</li>
 *   <li>高频事件检测</li>
 *   <li>监听器性能排名</li>
 *   <li>实时性能报告</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 注册监控器
 * EventPerformanceMonitor monitor = new EventPerformanceMonitor(plugin);
 * monitor.register();
 *
 * // 获取性能报告
 * String report = monitor.generateReport();
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class EventPerformanceMonitor implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(EventPerformanceMonitor.class);

    // 性能阈值（毫秒）
    private static final double SLOW_EVENT_THRESHOLD_MS = 10.0;
    private static final double VERY_SLOW_EVENT_THRESHOLD_MS = 50.0;
    private static final int HIGH_FREQUENCY_THRESHOLD = 1000; // 每秒1000次

    // 事件统计
    private final Map<Class<? extends Event>, EventPerformanceStats> eventStats = new ConcurrentHashMap<>();

    // 监听器统计
    private final Map<String, ListenerPerformanceStats> listenerStats = new ConcurrentHashMap<>();

    // 总统计
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsSlow = new AtomicLong(0);
    private final AtomicLong totalEventsVerySlow = new AtomicLong(0);

    // 是否启用监控
    private volatile boolean enabled = true;

    // 是否记录详细日志
    private volatile boolean detailedLogging = false;

    /**
     * 创建性能监控器
     */
    public EventPerformanceMonitor() {
    }

    /**
     * 记录事件处理性能
     *
     * @param event 事件
     * @param durationNs 处理耗时（纳秒）
     */
    public void recordEventPerformance(Event event, long durationNs) {
        if (!enabled) {
            return;
        }

        Class<? extends Event> eventType = event.getClass();
        String eventName = event.getEventName();
        double durationMs = durationNs / 1_000_000.0;

        // 更新统计
        EventPerformanceStats stats = eventStats.computeIfAbsent(eventType, k -> new EventPerformanceStats(eventName));
        stats.record(durationNs);

        totalEventsProcessed.incrementAndGet();

        // 检查慢事件
        if (durationMs > VERY_SLOW_EVENT_THRESHOLD_MS) {
            totalEventsVerySlow.incrementAndGet();
            logger.error("严重慢事件: {} - {}ms", eventName, String.format("%.2f", durationMs));
        } else if (durationMs > SLOW_EVENT_THRESHOLD_MS) {
            totalEventsSlow.incrementAndGet();
            if (detailedLogging) {
                logger.warn("慢事件: {} - {}ms", eventName, String.format("%.2f", durationMs));
            }
        }

        // 检查高频事件
        if (stats.getEventsPerSecond() > HIGH_FREQUENCY_THRESHOLD) {
            logger.warn("高频事件: {} - {}次/秒", eventName, stats.getEventsPerSecond());
        }
    }

    /**
     * 记录监听器性能
     *
     * @param listenerName 监听器名称
     * @param eventType 事件类型
     * @param durationNs 处理耗时（纳秒）
     */
    public void recordListenerPerformance(String listenerName, Class<? extends Event> eventType, long durationNs) {
        if (!enabled) {
            return;
        }

        String key = listenerName + "#" + eventType.getSimpleName();
        ListenerPerformanceStats stats = listenerStats.computeIfAbsent(key, k -> new ListenerPerformanceStats(listenerName, eventType.getSimpleName()));
        stats.record(durationNs);
    }

    /**
     * 生成性能报告
     *
     * @return 性能报告字符串
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n========== RPGCore 事件性能报告 ==========\n");
        report.append("生成时间: ").append(new Date()).append("\n");
        report.append("监控状态: ").append(enabled ? "启用" : "禁用").append("\n");
        report.append("\n");

        // 总体统计
        report.append("【总体统计】\n");
        report.append("  总事件数: ").append(totalEventsProcessed.get()).append("\n");
        report.append("  慢事件数: ").append(totalEventsSlow.get()).append("\n");
        report.append("  严重慢事件数: ").append(totalEventsVerySlow.get()).append("\n");
        report.append("  慢事件比例: ").append(String.format("%.2f%%", getSlowEventRatio() * 100)).append("\n");
        report.append("\n");

        // 事件性能排名
        report.append("【事件性能排名（按平均耗时）】\n");
        eventStats.values().stream()
            .sorted(Comparator.comparingDouble(EventPerformanceStats::getAverageDurationMs).reversed())
            .limit(10)
            .forEach(stats -> {
                report.append(String.format("  %-40s 平均: %6.2fms  最大: %6.2fms  次数: %d\n",
                    stats.eventName,
                    stats.getAverageDurationMs(),
                    stats.getMaxDurationMs(),
                    stats.totalCount));
            });
        report.append("\n");

        // 高频事件排名
        report.append("【高频事件排名】\n");
        eventStats.values().stream()
            .sorted(Comparator.comparingLong(EventPerformanceStats::getEventsPerSecond).reversed())
            .limit(10)
            .forEach(stats -> {
                report.append(String.format("  %-40s %d次/秒  总计: %d\n",
                    stats.eventName,
                    stats.getEventsPerSecond(),
                    stats.totalCount));
            });
        report.append("\n");

        // 慢监听器排名
        report.append("【慢监听器排名】\n");
        listenerStats.values().stream()
            .sorted(Comparator.comparingDouble(ListenerPerformanceStats::getAverageDurationMs).reversed())
            .limit(10)
            .forEach(stats -> {
                report.append(String.format("  %-30s %-30s 平均: %6.2fms  次数: %d\n",
                    stats.listenerName,
                    stats.eventName,
                    stats.getAverageDurationMs(),
                    stats.totalCount));
            });
        report.append("\n");

        report.append("==========================================\n");

        return report.toString();
    }

    /**
     * 获取慢事件比例
     */
    public double getSlowEventRatio() {
        long total = totalEventsProcessed.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalEventsSlow.get() / total;
    }

    /**
     * 获取事件统计
     */
    public EventPerformanceStats getEventStats(Class<? extends Event> eventType) {
        return eventStats.get(eventType);
    }

    /**
     * 获取所有事件统计
     */
    public Map<Class<? extends Event>, EventPerformanceStats> getAllEventStats() {
        return new HashMap<>(eventStats);
    }

    /**
     * 启用监控
     */
    public void enable() {
        this.enabled = true;
        logger.info("事件性能监控已启用");
    }

    /**
     * 禁用监控
     */
    public void disable() {
        this.enabled = false;
        logger.info("事件性能监控已禁用");
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 启用详细日志
     */
    public void enableDetailedLogging() {
        this.detailedLogging = true;
        logger.info("事件性能详细日志已启用");
    }

    /**
     * 禁用详细日志
     */
    public void disableDetailedLogging() {
        this.detailedLogging = false;
        logger.info("事件性能详细日志已禁用");
    }

    /**
     * 重置统计
     */
    public void reset() {
        eventStats.clear();
        listenerStats.clear();
        totalEventsProcessed.set(0);
        totalEventsSlow.set(0);
        totalEventsVerySlow.set(0);
        logger.info("事件性能统计已重置");
    }

    // ==================== 统计类 ====================

    /**
     * 事件性能统计
     */
    public static class EventPerformanceStats {
        public final String eventName;
        public long totalCount = 0;
        public long totalDurationNs = 0;
        public long maxDurationNs = 0;
        public long minDurationNs = Long.MAX_VALUE;
        public long lastSecond = 0;
        public int countThisSecond = 0;

        public EventPerformanceStats(String eventName) {
            this.eventName = eventName;
        }

        public void record(long durationNs) {
            totalCount++;
            totalDurationNs += durationNs;
            maxDurationNs = Math.max(maxDurationNs, durationNs);
            minDurationNs = Math.min(minDurationNs, durationNs);

            long currentSecond = System.currentTimeMillis() / 1000;
            if (currentSecond != lastSecond) {
                lastSecond = currentSecond;
                countThisSecond = 1;
            } else {
                countThisSecond++;
            }
        }

        public double getAverageDurationMs() {
            if (totalCount == 0) return 0.0;
            return (totalDurationNs / (double) totalCount) / 1_000_000.0;
        }

        public double getMaxDurationMs() {
            return maxDurationNs / 1_000_000.0;
        }

        public long getEventsPerSecond() {
            long currentSecond = System.currentTimeMillis() / 1000;
            if (currentSecond != lastSecond) {
                return 0;
            }
            return countThisSecond;
        }
    }

    /**
     * 监听器性能统计
     */
    public static class ListenerPerformanceStats {
        public final String listenerName;
        public final String eventName;
        public long totalCount = 0;
        public long totalDurationNs = 0;
        public long maxDurationNs = 0;

        public ListenerPerformanceStats(String listenerName, String eventName) {
            this.listenerName = listenerName;
            this.eventName = eventName;
        }

        public void record(long durationNs) {
            totalCount++;
            totalDurationNs += durationNs;
            maxDurationNs = Math.max(maxDurationNs, durationNs);
        }

        public double getAverageDurationMs() {
            if (totalCount == 0) return 0.0;
            return (totalDurationNs / (double) totalCount) / 1_000_000.0;
        }
    }
}
