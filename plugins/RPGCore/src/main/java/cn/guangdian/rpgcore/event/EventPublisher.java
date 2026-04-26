package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.context.PluginContext;
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.lang.StackWalker.StackFrame;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * RPGCore 统一事件发布器
 *
 * <p>所有子插件必须通过此类发布事件，实现统一管控：</p>
 * <ul>
 *   <li>统一日志记录</li>
 *   <li>性能监控与告警</li>
 *   <li>权限检查</li>
 *   <li>频率限制</li>
 *   <li>批量处理优化</li>
 *   <li>异步发布支持</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 标准发布
 * EventPublisher.publish(new PlayerStatsChangedEvent(player, oldStats, newStats));
 *
 * // 异步发布（非关键事件）
 * EventPublisher.publishAsync(new PlayerStatsChangedEvent(player, oldStats, newStats));
 *
 * // 批量发布（高频场景）
 * List<Event> events = Arrays.asList(event1, event2, event3);
 * EventPublisher.publishBatch(events);
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    // 性能监控阈值（纳秒）
    private static final long PERFORMANCE_THRESHOLD_NS = 1_000_000L; // 1ms
    private static final long PERFORMANCE_WARNING_NS = 10_000_000L;  // 10ms
    private static final long PERFORMANCE_CRITICAL_NS = 50_000_000L; // 50ms

    // 频率限制配置
    private static final int DEFAULT_RATE_LIMIT = 100; // 每秒最多100个事件
    private static final Map<Class<? extends Event>, Integer> rateLimits = new ConcurrentHashMap<>();

    // 被禁止发布事件的插件列表
    private static final Set<String> BLOCKED_PLUGINS = ConcurrentHashMap.newKeySet();

    // 事件统计
    private static final Map<Class<? extends Event>, EventStats> eventStats = new ConcurrentHashMap<>();
    private static final AtomicInteger totalPublished = new AtomicInteger(0);

    // 批量处理队列
    private static final Map<Class<? extends Event>, List<Event>> batchQueues = new ConcurrentHashMap<>();
    private static volatile boolean batchMode = false;

    // 高频事件快速路径缓存
    private static final Set<Class<? extends Event>> fastPathEvents = ConcurrentHashMap.newKeySet();
    private static final AtomicLong fastPathThreshold = new AtomicLong(1000); // 1μs

    /**
     * 发布事件（标准方式）
     *
     * <p>所有子插件必须通过此方法发布事件，不要直接调用 Bukkit API。</p>
     *
     * @param event 要发布的事件
     */
    public static void publish(Event event) {
        if (event == null) {
            logger.warn("尝试发布 null 事件，已忽略");
            return;
        }

        Class<? extends Event> eventType = event.getClass();

        // 快速路径：高频系统事件直接发布，跳过管控
        if (fastPathEvents.contains(eventType)) {
            publishFastPath(event);
            return;
        }

        // 管控检查
        if (!prePublishChecks(event, eventType)) {
            return;
        }

        // 执行发布并监控性能
        doPublishWithMonitoring(event, eventType);
    }

    /**
     * 快速路径发布（跳过管控）
     */
    private static void publishFastPath(Event event) {
        try {
            Bukkit.getPluginManager().callEvent(event);
            totalPublished.incrementAndGet();
        } catch (Exception e) {
            logger.error("事件发布异常: {}", event.getEventName(), e);
        }
    }

    /**
     * 发布前检查（权限、频率限制、批量模式）
     */
    private static boolean prePublishChecks(Event event, Class<? extends Event> eventType) {
        // 1. 权限检查
        if (!checkPermission(event)) {
            logger.debug("事件发布被拒绝: {}", event.getEventName());
            return false;
        }

        // 2. 频率限制检查
        if (!checkRateLimit(eventType)) {
            logger.warn("事件频率超限: {}，已丢弃", event.getEventName());
            return false;
        }

        // 3. 批量模式检查
        if (batchMode && isBatchable(eventType)) {
            addToBatch(event);
            return false;
        }

        return true;
    }

    /**
     * 执行发布并监控性能
     */
    private static void doPublishWithMonitoring(Event event, Class<? extends Event> eventType) {
        long startTime = System.nanoTime();

        try {
            Bukkit.getPluginManager().callEvent(event);

            long durationNs = System.nanoTime() - startTime;

            // 记录统计
            recordStats(eventType, durationNs);

            // 性能告警
            checkPerformance(event, durationNs);

            // 自动快速路径优化
            if (durationNs < fastPathThreshold.get()) {
                fastPathEvents.add(eventType);
            }

        } catch (Exception e) {
            logger.error("事件发布异常: {}", event.getEventName(), e);
        }
    }

    /**
     * 异步发布事件
     *
     * <p>适用于非关键事件，不阻塞主线程。</p>
     *
     * @param event 要发布的事件
     */
    public static void publishAsync(Event event) {
        if (event == null) {
            logger.warn("尝试异步发布 null 事件，已忽略");
            return;
        }

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runAsync(() -> publish(event));
        } else {
            // 降级：同步发布
            logger.warn("RPGCore 不可用，降级为同步发布: {}", event.getEventName());
            publish(event);
        }
    }

    /**
     * 延迟发布事件
     *
     * @param event 要发布的事件
     * @param delayTicks 延迟 tick 数
     */
    public static void publishLater(Event event, long delayTicks) {
        if (event == null) {
            logger.warn("尝试延迟发布 null 事件，已忽略");
            return;
        }

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> publish(event), delayTicks);
        } else {
            logger.warn("RPGCore 不可用，无法延迟发布: {}", event.getEventName());
        }
    }

    /**
     * 批量发布事件
     *
     * <p>自动合并同类事件，减少调用次数。</p>
     *
     * @param events 事件列表
     */
    public static void publishBatch(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        // 按事件类型分组
        Map<Class<? extends Event>, List<Event>> grouped = events.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Event::getClass));

        grouped.forEach((type, list) -> {
            if (isMergeable(type)) {
                // 合并同类事件
                Event merged = mergeEvents(type, list);
                if (merged != null) {
                    publish(merged);
                }
            } else {
                // 逐个发布
                list.forEach(EventPublisher::publish);
            }
        });
    }

    /**
     * 开始批量模式
     *
     * <p>在批量模式下，事件会被缓存，需要调用 flushBatch() 统一发布。</p>
     */
    public static void beginBatch() {
        batchMode = true;
        batchQueues.clear();
        logger.debug("开始批量模式");
    }

    /**
     * 结束批量模式并发布所有缓存的事件
     */
    public static void endBatch() {
        batchMode = false;

        // 发布所有缓存的事件
        batchQueues.forEach((type, list) -> {
            if (isMergeable(type)) {
                Event merged = mergeEvents(type, list);
                if (merged != null) {
                    publish(merged);
                }
            } else {
                list.forEach(EventPublisher::publish);
            }
        });

        batchQueues.clear();
        logger.debug("结束批量模式，已发布所有缓存事件");
    }

    /**
     * 设置事件频率限制
     *
     * @param eventType 事件类型
     * @param limitPerSecond 每秒最大数量
     */
    public static void setRateLimit(Class<? extends Event> eventType, int limitPerSecond) {
        rateLimits.put(eventType, limitPerSecond);
        logger.info("设置事件频率限制: {} = {}/s", eventType.getSimpleName(), limitPerSecond);
    }

    /**
     * 获取事件统计信息
     *
     * @return 事件统计快照
     */
    public static Map<Class<? extends Event>, EventStats> getStatsSnapshot() {
        return new HashMap<>(eventStats);
    }

    /**
     * 获取总发布次数
     */
    public static int getTotalPublished() {
        return totalPublished.get();
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        eventStats.clear();
        totalPublished.set(0);
        logger.info("事件统计已重置");
    }

    // ==================== 私有方法 ====================

    private static boolean checkPermission(Event event) {
        String eventName = event.getEventName();
        
        if (isSystemEvent(eventName)) {
            return true;
        }

        String pluginName = PluginContext.getCurrentPluginName();
        
        if (pluginName == null || "Unknown".equals(pluginName)) {
            logger.debug("无法获取事件发布者插件名称，允许发布系统事件: {}", eventName);
            return true;
        }

        if (BLOCKED_PLUGINS.contains(pluginName)) {
            logger.warn("插件 {} 被禁止发布事件，拒绝发布: {}", pluginName, eventName);
            return false;
        }

        return true;
    }

    private static boolean isSystemEvent(String eventName) {
        return eventName.startsWith("org.bukkit.event") ||
               eventName.startsWith("cn.guangdian.rpgcore.event");
    }

    private static boolean checkRateLimit(Class<? extends Event> eventType) {
        int limit = rateLimits.getOrDefault(eventType, DEFAULT_RATE_LIMIT);
        EventStats stats = eventStats.computeIfAbsent(eventType, k -> new EventStats());

        // 检查当前秒内的发布次数
        long currentSecond = System.currentTimeMillis() / 1000;
        if (stats.lastSecond != currentSecond) {
            stats.lastSecond = currentSecond;
            stats.countThisSecond = 0;
        }

        if (stats.countThisSecond >= limit) {
            return false;
        }

        stats.countThisSecond++;
        return true;
    }

    private static boolean isBatchable(Class<? extends Event> eventType) {
        if (eventType.isAnnotationPresent(Batchable.class)) {
            return true;
        }
        return eventType.getSimpleName().contains("Stats") ||
               eventType.getSimpleName().contains("Health");
    }

    private static void addToBatch(Event event) {
        batchQueues.computeIfAbsent(event.getClass(), k -> new ArrayList<>()).add(event);
    }

    private static boolean isMergeable(Class<? extends Event> eventType) {
        if (eventType.isAnnotationPresent(Mergeable.class)) {
            return true;
        }
        return eventType.getSimpleName().contains("StatsChanged") ||
               eventType.getSimpleName().contains("HealthChanged");
    }

    @SuppressWarnings("unchecked")
    private static Event mergeEvents(Class<? extends Event> type, List<Event> events) {
        if (events.isEmpty()) {
            return null;
        }

        if (events.size() == 1) {
            return events.get(0);
        }

        Event lastEvent = events.get(events.size() - 1);

        if (type.getSimpleName().contains("StatsChanged") ||
            type.getSimpleName().contains("HealthChanged")) {
            return new MergedStatsEvent(lastEvent, events.size());
        }

        return lastEvent;
    }

    private static class MergedStatsEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Event originalEvent;
        private final int mergedCount;

        public MergedStatsEvent(Event originalEvent, int mergedCount) {
            super(!Bukkit.isPrimaryThread());
            this.originalEvent = originalEvent;
            this.mergedCount = mergedCount;
        }

        public Event getOriginalEvent() {
            return originalEvent;
        }

        public int getMergedCount() {
            return mergedCount;
        }

        public String getOriginalEventName() {
            return originalEvent.getEventName();
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    private static void recordStats(Class<? extends Event> eventType, long durationNs) {
        EventStats stats = eventStats.computeIfAbsent(eventType, k -> new EventStats());
        stats.totalCount++;
        stats.totalDurationNs += durationNs;
        stats.maxDurationNs = Math.max(stats.maxDurationNs, durationNs);
        stats.minDurationNs = Math.min(stats.minDurationNs, durationNs);
        totalPublished.incrementAndGet();
    }

    private static void checkPerformance(Event event, long durationNs) {
        String eventName = event.getEventName();

        if (durationNs > PERFORMANCE_CRITICAL_NS) {
            logger.error("事件处理严重超时: {} - {}ms", eventName, durationNs / 1_000_000);
        } else if (durationNs > PERFORMANCE_WARNING_NS) {
            logger.warn("事件处理耗时过长: {} - {}ms", eventName, durationNs / 1_000_000);
        } else if (durationNs > PERFORMANCE_THRESHOLD_NS && logger.isDebugEnabled()) {
            logger.debug("事件处理耗时: {} - {}μs", eventName, durationNs / 1000);
        }
    }

    /**
     * 事件统计信息
     */
    public static class EventStats {
        public long totalCount = 0;
        public long totalDurationNs = 0;
        public long maxDurationNs = 0;
        public long minDurationNs = Long.MAX_VALUE;
        public long lastSecond = 0;
        public int countThisSecond = 0;

        public double getAverageDurationMs() {
            if (totalCount == 0) return 0;
            return (totalDurationNs / totalCount) / 1_000_000.0;
        }

        @Override
        public String toString() {
            return String.format("EventStats{count=%d, avg=%.2fms, max=%.2fms, min=%.2fms}",
                totalCount, getAverageDurationMs(), maxDurationNs / 1_000_000.0, minDurationNs / 1_000_000.0);
        }
    }
}
