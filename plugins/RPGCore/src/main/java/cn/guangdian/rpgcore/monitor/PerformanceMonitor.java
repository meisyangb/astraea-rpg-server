package cn.guangdian.rpgcore.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

/**
 * 性能监控器
 *
 * <p>提供操作计时和性能指标收集功能，支持内存监控、TPS监控、慢操作记录等。</p>
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
    private final Logger logger;

    // 监控配置
    private final AtomicBoolean memoryMonitoring = new AtomicBoolean(true);
    private final AtomicBoolean tpsMonitoring = new AtomicBoolean(true);
    private final AtomicBoolean logSlowOperations = new AtomicBoolean(true);
    private final AtomicLong memoryWarningThresholdMb = new AtomicLong(1024);
    private final AtomicLong slowOperationThresholdMs = new AtomicLong(100);
    private final AtomicReference<Duration> reportInterval = new AtomicReference<>(Duration.ofMinutes(30));

    // 内存监控
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final AtomicLong lastMemoryWarningTime = new AtomicLong(0);

    // 报告定时任务
    private ScheduledExecutorService reportScheduler;
    private ScheduledFuture<?> reportTask;

    /**
     * 创建性能监控器
     *
     * @param name 监控器名称
     */
    public PerformanceMonitor(String name) {
        this(name, Logger.getLogger("RPGCore.PerformanceMonitor"));
    }

    /**
     * 创建性能监控器（带日志）
     *
     * @param name 监控器名称
     * @param logger 日志记录器
     */
    public PerformanceMonitor(String name, Logger logger) {
        this.name = name;
        this.enabled = new AtomicBoolean(true);
        this.metricsMap = new ConcurrentHashMap<>();
        this.logger = logger;
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

        // 检查是否为慢操作
        if (logSlowOperations.get() && durationMs > slowOperationThresholdMs.get()) {
            logger.log(Level.WARNING, String.format("[慢操作] %s 耗时 %d ms (阈值: %d ms)",
                operationName, durationMs, slowOperationThresholdMs.get()));
        }
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

    // ==================== 配置方法 ====================

    /**
     * 设置内存监控启用状态
     */
    public void setMemoryMonitoring(boolean enabled) {
        this.memoryMonitoring.set(enabled);
    }

    /**
     * 设置TPS监控启用状态
     */
    public void setTpsMonitoring(boolean enabled) {
        this.tpsMonitoring.set(enabled);
    }

    /**
     * 设置慢操作记录启用状态
     */
    public void setLogSlowOperations(boolean enabled) {
        this.logSlowOperations.set(enabled);
    }

    /**
     * 设置内存警告阈值（MB）
     */
    public void setMemoryWarningThreshold(long thresholdMb) {
        this.memoryWarningThresholdMb.set(thresholdMb);
    }

    /**
     * 设置慢操作阈值（毫秒）
     */
    public void setSlowOperationThreshold(long thresholdMs) {
        this.slowOperationThresholdMs.set(thresholdMs);
    }

    /**
     * 设置报告输出间隔
     */
    public void setReportInterval(Duration interval) {
        this.reportInterval.set(interval);
        restartReportScheduler();
    }

    // ==================== 监控方法 ====================

    /**
     * 检查内存使用情况
     */
    public void checkMemory() {
        if (!enabled.get() || !memoryMonitoring.get()) {
            return;
        }

        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long usedMb = heapUsage.getUsed() / 1024 / 1024;
        long maxMb = heapUsage.getMax() / 1024 / 1024;

        long threshold = memoryWarningThresholdMb.get();
        long currentTime = System.currentTimeMillis();
        long lastWarning = lastMemoryWarningTime.get();

        // 每分钟最多警告一次
        if (usedMb > threshold && (currentTime - lastWarning) > 60000) {
            lastMemoryWarningTime.set(currentTime);
            logger.log(Level.WARNING, String.format(
                "[内存警告] 堆内存使用: %d MB / %d MB (阈值: %d MB)",
                usedMb, maxMb, threshold));
        }
    }

    /**
     * 记录TPS（应由外部调用，传入计算好的TPS）
     *
     * @param tps 当前TPS
     */
    public void recordTps(double tps) {
        if (!enabled.get() || !tpsMonitoring.get()) {
            return;
        }

        recordMetric("tps", tps);

        // TPS低于阈值时警告
        if (tps < 18.0) {
            logger.log(Level.WARNING, String.format("[TPS警告] 当前TPS: %.2f (阈值: 18.0)", tps));
        }
    }

    /**
     * 启动报告定时任务
     */
    public void startReportScheduler() {
        if (reportScheduler != null && !reportScheduler.isShutdown()) {
            return;
        }

        Duration interval = reportInterval.get();
        if (interval.isZero() || interval.isNegative()) {
            return;
        }

        reportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "RPGCore-PerformanceReport");
            thread.setDaemon(true);
            return thread;
        });

        reportTask = reportScheduler.scheduleAtFixedRate(() -> {
            if (enabled.get()) {
                // 自动采集 TPS
                if (tpsMonitoring.get()) {
                    try {
                        double tps = Bukkit.getTPS()[0];
                        recordTps(tps);
                    } catch (Exception e) {
                        logger.log(Level.FINE, "TPS 采集失败: " + e.getMessage());
                    }
                }
                // 自动检查内存
                checkMemory();
                
                PerformanceReport report = generateReport();
                logger.log(Level.INFO, "[性能报告]\\n" + report.toString());
            }
        }, interval.toMinutes(), interval.toMinutes(), TimeUnit.MINUTES);

        logger.log(Level.INFO, "性能报告定时任务已启动，间隔: " + interval.toMinutes() + " 分钟");
    }

    /**
     * 停止报告定时任务
     */
    public void stopReportScheduler() {
        // 先取消任务
        if (reportTask != null) {
            reportTask.cancel(false);
            reportTask = null;
        }
        
        // 再关闭线程池
        if (reportScheduler != null) {
            reportScheduler.shutdown();
            try {
                if (!reportScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    reportScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                reportScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            reportScheduler = null;
            logger.log(Level.INFO, "性能报告定时任务已停止");
        }
    }

    private void restartReportScheduler() {
        stopReportScheduler();
        startReportScheduler();
    }

    private PerformanceMetrics getOrCreateMetrics(String name) {
        return metricsMap.computeIfAbsent(name, k -> new PerformanceMetrics(k));
    }
}