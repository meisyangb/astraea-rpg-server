package cn.guangdian.rpgcore.monitor;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.logging.AsyncLogger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AlertManager {

    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(30);

    private final RPGCore plugin;
    private final AsyncLogger asyncLogger;
    private final ConcurrentLinkedQueue<Alert> recentAlerts;
    private final Map<String, AlertRule> rules;
    private final AtomicBoolean running;
    private volatile boolean enabled;

    public AlertManager(RPGCore plugin, AsyncLogger asyncLogger) {
        this.plugin = plugin;
        this.asyncLogger = asyncLogger;
        this.recentAlerts = new ConcurrentLinkedQueue<>();
        this.rules = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(true);
        this.enabled = true;

        registerDefaultRules();
    }

    private void registerDefaultRules() {
        registerRule("tps.low", 15.0, AlertLevel.WARNING, "TPS低于15，当前: {value}");
        registerRule("tps.critical", 14.0, AlertLevel.CRITICAL, "TPS严重过低，当前: {value}");

        registerRule("mspt.high", 50.0, AlertLevel.WARNING, "MSPT过高，当前: {value}ms");
        registerRule("mspt.critical", 100.0, AlertLevel.CRITICAL, "MSPT严重过高，当前: {value}ms");

        registerRule("cpu.high", 80.0, AlertLevel.WARNING, "CPU使用率过高，当前: {value}%");
        registerRule("memory.high", 85.0, AlertLevel.WARNING, "内存使用率过高，当前: {value}%");
        registerRule("memory.critical", 95.0, AlertLevel.CRITICAL, "内存使用率严重过高，当前: {value}%");

        registerRule("db.slow_query", 1000.0, AlertLevel.WARNING, "数据库慢查询，当前: {value}ms");
        registerRule("db.connection_timeout", 0.0, AlertLevel.CRITICAL, "数据库连接超时!");
    }

    public void registerRule(String name, double threshold, AlertLevel level, String message) {
        rules.put(name, new AlertRule(name, threshold, level, message));
    }

    public void checkTps(double tps) {
        if (!enabled) return;

        AlertRule lowRule = rules.get("tps.low");
        AlertRule criticalRule = rules.get("tps.critical");

        if (tps < criticalRule.threshold) {
            triggerAlert(criticalRule.level, criticalRule.message.replace("{value}", String.format("%.2f", tps)));
        } else if (tps < lowRule.threshold) {
            triggerAlert(lowRule.level, lowRule.message.replace("{value}", String.format("%.2f", tps)));
        }
    }

    public void checkMspt(double mspt) {
        if (!enabled) return;

        AlertRule highRule = rules.get("mspt.high");
        AlertRule criticalRule = rules.get("mspt.critical");

        if (mspt > criticalRule.threshold) {
            triggerAlert(criticalRule.level, criticalRule.message.replace("{value}", String.format("%.2f", mspt)));
        } else if (mspt > highRule.threshold) {
            triggerAlert(highRule.level, highRule.message.replace("{value}", String.format("%.2f", mspt)));
        }
    }

    public void checkMemory(double usagePercent) {
        if (!enabled) return;

        AlertRule highRule = rules.get("memory.high");
        AlertRule criticalRule = rules.get("memory.critical");

        if (usagePercent > criticalRule.threshold) {
            triggerAlert(criticalRule.level, criticalRule.message.replace("{value}", String.format("%.1f", usagePercent)));
        } else if (usagePercent > highRule.threshold) {
            triggerAlert(highRule.level, highRule.message.replace("{value}", String.format("%.1f", usagePercent)));
        }
    }

    public void checkCpu(double usagePercent) {
        if (!enabled) return;

        AlertRule rule = rules.get("cpu.high");
        if (usagePercent > rule.threshold) {
            triggerAlert(rule.level, rule.message.replace("{value}", String.format("%.1f", usagePercent)));
        }
    }

    public void checkDbQueryTime(long queryTimeMs) {
        if (!enabled) return;

        AlertRule rule = rules.get("db.slow_query");
        if (queryTimeMs > rule.threshold) {
            triggerAlert(rule.level, rule.message.replace("{value}", String.format("%d", queryTimeMs)));
        }
    }

    public void triggerAlert(AlertLevel level, String message) {
        Alert alert = new Alert(level, message, LocalDateTime.now());
        recentAlerts.offer(alert);

        while (recentAlerts.size() > 100) {
            recentAlerts.poll();
        }

        switch (level) {
            case CRITICAL -> asyncLogger.severe("[ALERT] " + message);
            case WARNING -> asyncLogger.warning("[ALERT] " + message);
            default -> asyncLogger.info("[ALERT] " + message);
        }

        if (level == AlertLevel.CRITICAL) {
            plugin.getLogger().warning("[ALERT] " + message);
        }
    }

    public void triggerAlert(String ruleName, double currentValue) {
        AlertRule rule = rules.get(ruleName);
        if (rule == null) return;

        String message = rule.message.replace("{value}", String.format("%.2f", currentValue));
        triggerAlert(rule.level, message);
    }

    public void recordSlowOperation(String operationName, long durationMs) {
        if (!enabled) return;

        AlertRule rule = rules.get("db.slow_query");
        if (durationMs > rule.threshold) {
            triggerAlert(rule.level, "慢操作 [" + operationName + "]: " + durationMs + "ms");
        }
    }

    public ConcurrentLinkedQueue<Alert> getRecentAlerts() {
        return recentAlerts;
    }

    public int getRecentAlertCount() {
        return recentAlerts.size();
    }

    public void clearAlerts() {
        recentAlerts.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        running.set(false);
        clearAlerts();
    }

    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }

    public static class Alert {
        private final AlertLevel level;
        private final String message;
        private final LocalDateTime timestamp;

        public Alert(AlertLevel level, String message, LocalDateTime timestamp) {
            this.level = level;
            this.message = message;
            this.timestamp = timestamp;
        }

        public AlertLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return timestamp + " [" + level + "] " + message;
        }
    }

    public static class AlertRule {
        final String name;
        final double threshold;
        final AlertLevel level;
        final String message;

        public AlertRule(String name, double threshold, AlertLevel level, String message) {
            this.name = name;
            this.threshold = threshold;
            this.level = level;
            this.message = message;
        }
    }
}