package cn.guangdian.rpgcore.logging;

import cn.guangdian.rpgcore.api.GameLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 高性能异步日志实现
 * 
 * <p>特性：
 * <ul>
 *   <li>完整的统计信息（队列大小、总日志数、丢弃数）</li>
 *   <li>分级日志控制（可动态调整级别）</li>
 *   <li>结构化日志支持（JSON格式）</li>
 *   <li>性能指标收集</li>
 *   <li>日志采样（高频日志防刷屏）</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class AsyncLogger implements GameLogger {

    private static final String LOGGER_NAME = "cn.guangdian.rpgcore";
    private final Logger logger;

    // 统计信息
    private final LongAdder totalLogged = new LongAdder();
    private final LongAdder totalDropped = new LongAdder();
    private final LongAdder totalDebug = new LongAdder();
    private final LongAdder totalInfo = new LongAdder();
    private final LongAdder totalWarning = new LongAdder();
    private final LongAdder totalSevere = new LongAdder();

    // 当前日志级别
    private volatile LogLevel currentLevel = LogLevel.INFO;

    // 采样控制（防止高频日志刷屏）
    private final Map<String, LogSampler> samplers = new ConcurrentHashMap<>();
    private final long defaultSampleIntervalMs = 5000; // 默认5秒采样间隔

    // 性能指标
    private final AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());
    private final LongAdder totalLogTime = new LongAdder();

    // 时间格式化器
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG(0), INFO(1), WARNING(2), SEVERE(3);

        private final int level;

        LogLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isEnabledFor(LogLevel minLevel) {
            return this.level >= minLevel.level;
        }
    }

    /**
     * 日志采样器
     */
    private static class LogSampler {
        private final AtomicLong lastLogTime = new AtomicLong(0);
        private final long intervalMs;
        private final LongAdder skippedCount = new LongAdder();

        LogSampler(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        boolean shouldLog() {
            long now = System.currentTimeMillis();
            long last = lastLogTime.get();
            if (now - last >= intervalMs) {
                if (lastLogTime.compareAndSet(last, now)) {
                    return true;
                }
            }
            skippedCount.increment();
            return false;
        }

        long getSkippedCount() {
            return skippedCount.sum();
        }
    }

    public AsyncLogger() {
        this.logger = LoggerFactory.getLogger(LOGGER_NAME);
    }

    @Override
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    @Override
    public void warning(String message) {
        log(LogLevel.WARNING, message, null);
    }

    @Override
    public void severe(String message) {
        log(LogLevel.SEVERE, message, null);
    }

    @Override
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    /**
     * 带异常信息的日志
     */
    public void info(String message, Throwable throwable) {
        log(LogLevel.INFO, message, throwable);
    }

    public void warning(String message, Throwable throwable) {
        log(LogLevel.WARNING, message, throwable);
    }

    public void severe(String message, Throwable throwable) {
        log(LogLevel.SEVERE, message, throwable);
    }

    public void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, message, throwable);
    }

    /**
     * 核心日志方法
     */
    private void log(LogLevel level, String message, Throwable throwable) {
        long startTime = System.nanoTime();

        try {
            // 检查级别
            if (!level.isEnabledFor(currentLevel)) {
                totalDropped.increment();
                return;
            }

            // 记录统计
            totalLogged.increment();
            switch (level) {
                case DEBUG -> totalDebug.increment();
                case INFO -> totalInfo.increment();
                case WARNING -> totalWarning.increment();
                case SEVERE -> totalSevere.increment();
            }

            // 输出日志
            if (throwable != null) {
                switch (level) {
                    case DEBUG -> logger.debug(message, throwable);
                    case INFO -> logger.info(message, throwable);
                    case WARNING -> logger.warn(message, throwable);
                    case SEVERE -> logger.error(message, throwable);
                }
            } else {
                switch (level) {
                    case DEBUG -> logger.debug(message);
                    case INFO -> logger.info(message);
                    case WARNING -> logger.warn(message);
                    case SEVERE -> logger.error(message);
                }
            }

        } finally {
            // 记录性能指标
            long duration = System.nanoTime() - startTime;
            totalLogTime.add(duration);
            lastLogTime.set(System.currentTimeMillis());
        }
    }

    /**
     * 采样日志（防止高频日志刷屏）
     */
    public void infoSampled(String samplerKey, String message) {
        infoSampled(samplerKey, message, defaultSampleIntervalMs);
    }

    public void infoSampled(String samplerKey, String message, long intervalMs) {
        LogSampler sampler = samplers.computeIfAbsent(samplerKey, k -> new LogSampler(intervalMs));
        if (sampler.shouldLog()) {
            long skipped = sampler.getSkippedCount();
            if (skipped > 0) {
                info(message + " (期间跳过 " + skipped + " 条相似日志)");
            } else {
                info(message);
            }
        }
    }

    public void warningSampled(String samplerKey, String message) {
        warningSampled(samplerKey, message, defaultSampleIntervalMs);
    }

    public void warningSampled(String samplerKey, String message, long intervalMs) {
        LogSampler sampler = samplers.computeIfAbsent(samplerKey, k -> new LogSampler(intervalMs));
        if (sampler.shouldLog()) {
            long skipped = sampler.getSkippedCount();
            if (skipped > 0) {
                warning(message + " (期间跳过 " + skipped + " 条相似日志)");
            } else {
                warning(message);
            }
        }
    }

    /**
     * 结构化日志（JSON格式）
     */
    public void infoStructured(String event, Map<String, Object> data) {
        String json = buildStructuredLog("INFO", event, data);
        info(json);
    }

    public void warningStructured(String event, Map<String, Object> data) {
        String json = buildStructuredLog("WARNING", event, data);
        warning(json);
    }

    public void severeStructured(String event, Map<String, Object> data) {
        String json = buildStructuredLog("SEVERE", event, data);
        severe(json);
    }

    private String buildStructuredLog(String level, String event, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestamp\":\"").append(TIME_FORMATTER.format(Instant.now())).append("\",");
        sb.append("\"level\":\"").append(level).append("\",");
        sb.append("\"event\":\"").append(escapeJson(event)).append("\",");
        sb.append("\"data\":{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ==================== 级别控制 ====================

    public void setLevel(LogLevel level) {
        this.currentLevel = level;
        info("日志级别已切换为: " + level);
    }

    public LogLevel getLevel() {
        return currentLevel;
    }

    public boolean isDebugEnabled() {
        return LogLevel.DEBUG.isEnabledFor(currentLevel);
    }

    // ==================== 统计信息 ====================

    @Override
    public int getQueueSize() {
        // 由于使用 Logback 的 AsyncAppender，队列大小由 Logback 管理
        // 这里返回一个估算值或从 MDC 获取
        return 0; // Logback 不暴露队列大小
    }

    @Override
    public long getTotalLogged() {
        return totalLogged.sum();
    }

    @Override
    public long getTotalDropped() {
        return totalDropped.sum();
    }

    public long getTotalDebug() {
        return totalDebug.sum();
    }

    public long getTotalInfo() {
        return totalInfo.sum();
    }

    public long getTotalWarning() {
        return totalWarning.sum();
    }

    public long getTotalSevere() {
        return totalSevere.sum();
    }

    /**
     * 获取平均日志处理时间（纳秒）
     */
    public long getAverageLogTimeNs() {
        long total = totalLogged.sum();
        return total > 0 ? totalLogTime.sum() / total : 0;
    }

    /**
     * 获取最后一次日志时间
     */
    public long getLastLogTime() {
        return lastLogTime.get();
    }

    /**
     * 获取完整统计信息
     */
    public LogStats getStats() {
        return new LogStats(
                getTotalLogged(),
                getTotalDropped(),
                getTotalDebug(),
                getTotalInfo(),
                getTotalWarning(),
                getTotalSevere(),
                getAverageLogTimeNs(),
                currentLevel
        );
    }

    @Override
    public void shutdown() {
        info("日志系统正在关闭...");
        samplers.clear();
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalLogged.reset();
        totalDropped.reset();
        totalDebug.reset();
        totalInfo.reset();
        totalWarning.reset();
        totalSevere.reset();
        totalLogTime.reset();
        samplers.clear();
        info("日志统计已重置");
    }

    /**
     * 日志统计信息记录
     */
    public record LogStats(
            long totalLogged,
            long totalDropped,
            long totalDebug,
            long totalInfo,
            long totalWarning,
            long totalSevere,
            long averageLogTimeNs,
            LogLevel currentLevel
    ) {
        @Override
        public String toString() {
            return String.format(
                    "LogStats{total=%d, dropped=%d, debug=%d, info=%d, warning=%d, severe=%d, avgTime=%dns, level=%s}",
                    totalLogged, totalDropped, totalDebug, totalInfo, totalWarning, totalSevere,
                    averageLogTimeNs, currentLevel
            );
        }
    }
}
