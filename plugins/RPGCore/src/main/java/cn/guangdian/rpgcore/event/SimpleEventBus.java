package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.event.events.PlayerDataLoadEvent;
import cn.guangdian.rpgcore.event.events.PlayerDataSaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 高性能事件总线实现 - 批量处理优化版
 *
 * <p>基于内存的事件总线实现，支持同步和异步事件发布。</p>
 *
 * <h3>性能优化：</h3>
 * <ul>
 *   <li>处理器在注册时按优先级排序，发布时无需再排序</li>
 *   <li>使用 CopyOnWriteArrayList 保证并发安全</li>
 *   <li>批量事件处理 - 减少高频事件的线程切换开销</li>
 *   <li>高优先级事件快速通道 - 关键事件立即同步处理</li>
 *   <li>事件分组批量分发 - 同类型事件合并处理</li>
 * </ul>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class SimpleEventBus implements EventBus {

    private final Logger logger;
    private final JavaPlugin plugin;

    // 存储已排序的处理器列表，避免每次发布时重新排序
    private final ConcurrentHashMap<Class<? extends CoreEvent>, SortedHandlerList> sortedHandlers;

    // 用于保护排序操作的锁
    private final Object sortLock = new Object();
    
    // ==================== 批量处理优化 ====================
    
    /**
     * 批量事件队列
     */
    private final ConcurrentLinkedQueue<PendingEvent> pendingEvents;
    
    /**
     * 批量处理调度器
     */
    private ScheduledExecutorService batchScheduler;
    
    /**
     * 批量处理任务Future
     */
    private ScheduledFuture<?> batchTask;
    
    /**
     * 批量处理配置
     */
    private static final int BATCH_MAX_SIZE = 100;        // 最大批量大小
    private static final long BATCH_INTERVAL_MS = 50;     // 批量处理间隔（毫秒）

    // ==================== 动态调整配置 ====================

    /**
     * 动态调整阈值
     */
    private static final int LOW_QUEUE_THRESHOLD = 20;     // 低队列阈值
    private static final int HIGH_QUEUE_THRESHOLD = 100;   // 高队列阈值
    private static final int CRITICAL_QUEUE_THRESHOLD = 500; // 临界队列阈值

    /**
     * 动态调整后的批量大小（初始值）
     */
    private volatile int dynamicBatchSize = BATCH_MAX_SIZE;

    /**
     * 动态调整后的处理间隔（初始值）
     */
    private volatile long dynamicBatchInterval = BATCH_INTERVAL_MS;

    /**
     * 是否启用批量处理
     */
    private volatile boolean batchEnabled = true;

    /**
     * 是否启用动态调整
     */
    private volatile boolean dynamicAdjustEnabled = true;

    /**
     * 上次调整时间
     */
    private volatile long lastAdjustTime = System.currentTimeMillis();

    /**
     * 调整冷却时间（毫秒）
     */
    private static final long ADJUST_COOLDOWN_MS = 1000;
    
    /**
     * 统计信息
     */
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong batchEventsProcessed = new AtomicLong(0);

    /**
     * 创建事件总线
     *
     * @param plugin 插件实例
     */
    public SimpleEventBus(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.sortedHandlers = new ConcurrentHashMap<>();
        this.pendingEvents = new ConcurrentLinkedQueue<>();
        
        // 启动批量处理任务
        startBatchProcessor();
    }
    
    /**
     * 启动批量处理器
     */
    private void startBatchProcessor() {
        if (batchScheduler != null) {
            return;
        }
        
        batchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RPGCore-EventBus-Batch");
            t.setDaemon(true);
            return t;
        });
        
        batchTask = batchScheduler.scheduleWithFixedDelay(
            this::processBatch,
            BATCH_INTERVAL_MS,
            BATCH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("[EventBus] 批量处理器已启动，间隔: " + BATCH_INTERVAL_MS + "ms");
    }
    
    /**
     * 停止批量处理器
     */
    public void shutdown() {
        if (batchTask != null) {
            batchTask.cancel(false);
        }
        if (batchScheduler != null) {
            batchScheduler.shutdown();
        }
        
        // 处理剩余事件
        processRemainingEvents();
        
        logger.info("[EventBus] 批量处理器已关闭");
    }
    
    /**
     * 处理剩余事件（关闭时调用）
     */
    private void processRemainingEvents() {
        PendingEvent e;
        while ((e = pendingEvents.poll()) != null) {
            try {
                dispatchSingle(e.event);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error processing remaining event", ex);
            }
        }
    }

    @Override
    public <T extends CoreEvent> void publish(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        totalEventsProcessed.incrementAndGet();

        // 高优先级事件直接同步处理
        if (isHighPriorityEvent(event)) {
            dispatchSingle(event);
            return;
        }
        
        // 批量处理模式：入队等待批量处理
        if (batchEnabled) {
            pendingEvents.offer(new PendingEvent(event));
        } else {
            // 批量处理禁用时直接分发
            dispatchSingle(event);
        }
    }
    
    /**
     * 判断是否是高优先级事件
     * 高优先级事件立即同步处理，不进入批量队列
     */
    private boolean isHighPriorityEvent(CoreEvent event) {
        // 玩家数据加载/保存等关键事件同步处理
        return event instanceof PlayerDataLoadEvent || 
               event instanceof PlayerDataSaveEvent;
    }
    
    /**
     * 批量处理任务
     */
    private void processBatch() {
        if (pendingEvents.isEmpty()) {
            return;
        }

        // 动态调整批量参数
        if (dynamicAdjustEnabled) {
            adjustBatchParameters();
        }

        List<PendingEvent> batch = new ArrayList<>(dynamicBatchSize);

        // 取出一批事件
        PendingEvent e;
        while (batch.size() < dynamicBatchSize && (e = pendingEvents.poll()) != null) {
            batch.add(e);
        }

        if (batch.isEmpty()) {
            return;
        }

        batchEventsProcessed.addAndGet(batch.size());

        // 按事件类型分组
        Map<Class<?>, List<PendingEvent>> grouped = batch.stream()
            .collect(Collectors.groupingBy(pe -> pe.event.getClass()));

        // 批量分发
        for (Map.Entry<Class<?>, List<PendingEvent>> entry : grouped.entrySet()) {
            dispatchBatch(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 动态调整批量处理参数
     *
     * <p>根据队列深度动态调整批量大小和处理频率：</p>
     * <ul>
     *   <li>队列深度低（<20）：减小批量，降低延迟</li>
     *   <li>队列深度中等（20-100）：标准配置</li>
     *   <li>队列深度高（100-500）：增大批量，提升吞吐</li>
     *   <li>队列深度临界（>500）：紧急模式，最大化吞吐</li>
     * </ul>
     */
    private void adjustBatchParameters() {
        long now = System.currentTimeMillis();

        // 冷却检查，避免频繁调整
        if (now - lastAdjustTime < ADJUST_COOLDOWN_MS) {
            return;
        }

        int queueSize = pendingEvents.size();
        int oldBatchSize = dynamicBatchSize;
        long oldInterval = dynamicBatchInterval;

        if (queueSize < LOW_QUEUE_THRESHOLD) {
            // 低负载：减小批量，降低延迟
            dynamicBatchSize = 20;
            dynamicBatchInterval = 25; // 更快处理
        } else if (queueSize < HIGH_QUEUE_THRESHOLD) {
            // 正常负载：标准配置
            dynamicBatchSize = BATCH_MAX_SIZE;
            dynamicBatchInterval = BATCH_INTERVAL_MS;
        } else if (queueSize < CRITICAL_QUEUE_THRESHOLD) {
            // 高负载：增大批量，提升吞吐
            dynamicBatchSize = 200;
            dynamicBatchInterval = 30; // 加快处理
        } else {
            // 临界负载：紧急模式
            dynamicBatchSize = 500;
            dynamicBatchInterval = 10; // 最快处理
            logger.warning("[EventBus] Critical queue depth: " + queueSize + ", entering emergency mode");
        }

        // 如果间隔变化，重新调度
        if (dynamicBatchInterval != oldInterval && batchTask != null) {
            rescheduleBatchTask();
        }

        // 记录调整
        if (dynamicBatchSize != oldBatchSize) {
            lastAdjustTime = now;
            logger.fine("[EventBus] Adjusted batch params: size=" + dynamicBatchSize +
                ", interval=" + dynamicBatchInterval + "ms, queue=" + queueSize);
        }
    }

    /**
     * 重新调度批量任务
     */
    private void rescheduleBatchTask() {
        if (batchTask != null) {
            batchTask.cancel(false);
        }
        if (batchScheduler != null && !batchScheduler.isShutdown()) {
            batchTask = batchScheduler.scheduleWithFixedDelay(
                this::processBatch,
                dynamicBatchInterval,
                dynamicBatchInterval,
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * 批量分发同类型事件
     */
    @SuppressWarnings("unchecked")
    private void dispatchBatch(Class<?> eventType, List<PendingEvent> batch) {
        SortedHandlerList handlerList = sortedHandlers.get(eventType);
        if (handlerList == null || handlerList.isEmpty()) {
            return;
        }
        
        List<EventHandlerWrapper<?>> handlers = handlerList.getHandlers();
        
        for (EventHandlerWrapper<?> wrapper : handlers) {
            for (PendingEvent pe : batch) {
                try {
                    // 检查事件是否已取消
                    if (pe.event.isCancelled() && wrapper.ignoreCancelled) {
                        continue;
                    }
                    
                    EventHandler<CoreEvent> handler = (EventHandler<CoreEvent>) wrapper.handler;
                    handler.handle(pe.event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in batch handler for " + eventType.getSimpleName(), e);
                }
            }
        }
    }
    
    /**
     * 单事件分发（高优先级事件使用）
     */
    @SuppressWarnings("unchecked")
    private void dispatchSingle(CoreEvent event) {
        Class<? extends CoreEvent> eventType = event.getClass();
        SortedHandlerList handlerList = sortedHandlers.get(eventType);

        if (handlerList == null || handlerList.isEmpty()) {
            return;
        }

        for (EventHandlerWrapper<?> wrapper : handlerList.getHandlers()) {
            if (event.isCancelled() && wrapper.ignoreCancelled) {
                continue;
            }

            try {
                EventHandler<CoreEvent> handler = (EventHandler<CoreEvent>) wrapper.handler;
                handler.handle(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error handling event " + event.getEventName(), e);
            }
        }
    }

    @Override
    public <T extends CoreEvent> void publishAsync(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (plugin != null && Bukkit.isPrimaryThread()) {
            // 在主线程调用时，入队批量处理
            pendingEvents.offer(new PendingEvent(event));
            totalEventsProcessed.incrementAndGet();
        } else {
            // 已经在异步线程，入队等待处理
            pendingEvents.offer(new PendingEvent(event));
            totalEventsProcessed.incrementAndGet();
        }
    }

    @Override
    public <T extends CoreEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        synchronized (sortLock) {
            SortedHandlerList handlerList = sortedHandlers.computeIfAbsent(
                eventType, k -> new SortedHandlerList()
            );

            EventHandlerWrapper<T> wrapper = new EventHandlerWrapper<>(handler,
                handler.getPriority(), handler.ignoreCancelled());

            handlerList.addHandler(wrapper);
        }

        logger.fine("Subscribed handler for event: " + eventType.getSimpleName());
    }

    @Override
    public void unsubscribe(EventHandler<?> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        synchronized (sortLock) {
            for (SortedHandlerList handlerList : sortedHandlers.values()) {
                handlerList.removeHandler(handler);
            }
        }
    }

    @Override
    public <T extends CoreEvent> void unsubscribeAll(Class<T> eventType) {
        if (eventType == null) {
            return;
        }

        synchronized (sortLock) {
            sortedHandlers.remove(eventType);
        }

        logger.fine("Unsubscribed all handlers for event: " + eventType.getSimpleName());
    }

    @Override
    public boolean hasSubscribers(Class<? extends CoreEvent> eventType) {
        SortedHandlerList handlerList = sortedHandlers.get(eventType);
        return handlerList != null && !handlerList.isEmpty();
    }

    @Override
    public int getSubscriberCount(Class<? extends CoreEvent> eventType) {
        SortedHandlerList handlerList = sortedHandlers.get(eventType);
        return handlerList == null ? 0 : handlerList.size();
    }

    /**
     * 清空所有处理器
     */
    public void clear() {
        synchronized (sortLock) {
            sortedHandlers.clear();
        }
        pendingEvents.clear();
    }

    /**
     * 获取所有事件类型的处理器总数
     */
    public int getTotalHandlerCount() {
        int count = 0;
        for (SortedHandlerList handlerList : sortedHandlers.values()) {
            count += handlerList.size();
        }
        return count;
    }
    
    /**
     * 获取待处理事件数量
     */
    public int getPendingEventCount() {
        return pendingEvents.size();
    }
    
    /**
     * 获取统计信息
     */
    public EventBusStats getStats() {
        return new EventBusStats(
            totalEventsProcessed.get(),
            batchEventsProcessed.get(),
            pendingEvents.size(),
            sortedHandlers.size(),
            getTotalHandlerCount(),
            dynamicBatchSize,
            dynamicBatchInterval,
            getQueueStatus()
        );
    }
    
    /**
     * 设置是否启用批量处理
     */
    public void setBatchEnabled(boolean enabled) {
        this.batchEnabled = enabled;
    }

    /**
     * 设置是否启用动态调整
     */
    public void setDynamicAdjustEnabled(boolean enabled) {
        this.dynamicAdjustEnabled = enabled;
    }

    /**
     * 获取当前动态批量大小
     */
    public int getDynamicBatchSize() {
        return dynamicBatchSize;
    }

    /**
     * 获取当前动态处理间隔
     */
    public long getDynamicBatchInterval() {
        return dynamicBatchInterval;
    }

    /**
     * 获取队列状态描述
     */
    public String getQueueStatus() {
        int queueSize = pendingEvents.size();
        if (queueSize < LOW_QUEUE_THRESHOLD) {
            return "LOW";
        } else if (queueSize < HIGH_QUEUE_THRESHOLD) {
            return "NORMAL";
        } else if (queueSize < CRITICAL_QUEUE_THRESHOLD) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 事件总线统计信息
     */
    public static class EventBusStats {
        public final long totalEventsProcessed;
        public final long batchEventsProcessed;
        public final int pendingEvents;
        public final int eventTypes;
        public final int totalHandlers;
        public final int currentBatchSize;
        public final long currentInterval;
        public final String queueStatus;

        EventBusStats(long total, long batch, int pending, int types, int handlers,
                      int batchSize, long interval, String status) {
            this.totalEventsProcessed = total;
            this.batchEventsProcessed = batch;
            this.pendingEvents = pending;
            this.eventTypes = types;
            this.totalHandlers = handlers;
            this.currentBatchSize = batchSize;
            this.currentInterval = interval;
            this.queueStatus = status;
        }

        @Override
        public String toString() {
            return String.format("EventBusStats{total=%d, batch=%d, pending=%d, types=%d, handlers=%d, batchSize=%d, interval=%dms, status=%s}",
                totalEventsProcessed, batchEventsProcessed, pendingEvents, eventTypes, totalHandlers,
                currentBatchSize, currentInterval, queueStatus);
        }
    }
    
    /**
     * 待处理事件包装
     */
    private static class PendingEvent {
        final CoreEvent event;
        final long timestamp;
        
        PendingEvent(CoreEvent event) {
            this.event = event;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 已排序的处理器列表
     *
     * <p>维护一个按优先级排序的处理器列表，添加新处理器时立即排序。</p>
     */
    private static class SortedHandlerList {
        // 使用 CopyOnWriteArrayList 保证并发读取安全
        private final CopyOnWriteArrayList<EventHandlerWrapper<?>> handlers;
        // 标记是否需要重新排序
        private final AtomicBoolean needsSort = new AtomicBoolean(false);

        SortedHandlerList() {
            this.handlers = new CopyOnWriteArrayList<>();
        }

        void addHandler(EventHandlerWrapper<?> wrapper) {
            handlers.add(wrapper);
            // 添加后立即排序
            sortHandlers();
        }

        void removeHandler(EventHandler<?> handler) {
            handlers.removeIf(w -> w.handler.equals(handler));
        }

        List<EventHandlerWrapper<?>> getHandlers() {
            // 如果需要排序，先排序再返回
            if (needsSort.get()) {
                sortHandlers();
            }
            return handlers;
        }

        boolean isEmpty() {
            return handlers.isEmpty();
        }

        int size() {
            return handlers.size();
        }

        private void sortHandlers() {
            if (handlers.size() <= 1) {
                needsSort.set(false);
                return;
            }

            // 创建新的排序列表并替换
            List<EventHandlerWrapper<?>> sorted = new ArrayList<>(handlers);
            sorted.sort(Comparator.comparingInt(w -> w.priority.getOrder()));

            // CopyOnWriteArrayList 的 set 操作会创建新副本
            handlers.clear();
            handlers.addAll(sorted);

            needsSort.set(false);
        }
    }

    /**
     * 事件处理器包装类
     */
    private static class EventHandlerWrapper<T extends CoreEvent> {
        final EventHandler<T> handler;
        final EventPriority priority;
        final boolean ignoreCancelled;

        EventHandlerWrapper(EventHandler<T> handler, EventPriority priority, boolean ignoreCancelled) {
            this.handler = handler;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }
    }
}