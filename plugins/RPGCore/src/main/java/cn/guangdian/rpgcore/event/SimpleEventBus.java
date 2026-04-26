package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单事件总线实现 - Bukkit 代理模式
 *
 * <p>此实现将事件代理到 Bukkit 事件系统，实现与所有插件的互通。</p>
 * <p><b>注意：</b>EventBus 已废弃，建议直接使用 Bukkit 事件系统。</p>
 *
 * @author GuangDian
 * @since 2.0.0
 * @deprecated 使用 Bukkit 原生事件系统替代
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class SimpleEventBus implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(SimpleEventBus.class);

    private final JavaPlugin plugin;
    private final Map<Class<? extends CoreEvent>, Listener> registeredListeners = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsPublished = new AtomicLong(0);

    /**
     * 创建事件总线
     *
     * @param plugin 插件实例
     */
    public SimpleEventBus(JavaPlugin plugin) {
        this.plugin = plugin;
        logger.info("EventBus initialized (Bukkit Proxy Mode)");
    }

    @Override
    public <T extends CoreEvent> void publish(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        totalEventsPublished.incrementAndGet();

        // 代理到 Bukkit 事件系统！
        // 这样所有插件都能收到事件
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
        } else {
            // 如果在异步线程，调度到主线程
            // 使用 RPGCore SyncScheduler
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSync(() -> {
                    Bukkit.getPluginManager().callEvent(event);
                });
            } else {
                // 降级：使用 Bukkit 异步调度器
                Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                    Bukkit.getPluginManager().callEvent(event);
                });
            }
        }
    }

    @Override
    public <T extends CoreEvent> void publishAsync(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        totalEventsPublished.incrementAndGet();

        // 使用 Paper 的 AsyncScheduler
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    @Override
    public <T extends CoreEvent> void subscribe(Class<T> eventType, cn.guangdian.rpgcore.event.EventHandler<T> handler) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        // 创建 Bukkit 监听器
        Listener listener = new Listener() {};

        // 使用 EventExecutor 适配 RPGCore EventHandler 到 Bukkit
        EventExecutor executor = (listener1, event) -> {
            if (!(event instanceof CoreEvent coreEvent)) {
                return;
            }
            if (eventType.isInstance(event)) {
                try {
                    @SuppressWarnings("unchecked")
                    T typedEvent = (T) event;
                    if (!typedEvent.isCancelled() || !handler.ignoreCancelled()) {
                        handler.handle(typedEvent);
                    }
                } catch (Exception e) {
                    logger.error("Error handling event " + event.getEventName(), e);
                }
            }
        };

        // 映射优先级
        EventPriority bukkitPriority = mapPriority(handler.getPriority());

        // 注册到 Bukkit
        Bukkit.getPluginManager().registerEvent(
            eventType,
            listener,
            bukkitPriority,
            executor,
            plugin,
            !handler.ignoreCancelled()
        );

        registeredListeners.put(eventType, listener);
        logger.debug("Subscribed handler for event: {}", eventType.getSimpleName());
    }

    @Override
    public void unsubscribe(cn.guangdian.rpgcore.event.EventHandler<?> handler) {
        // Bukkit 没有直接取消订阅单个 handler 的方法
        // 需要取消整个监听器的注册
        logger.warn("Unsubscribe single handler not supported in proxy mode, use unsubscribeAll instead");
    }

    @Override
    public <T extends CoreEvent> void unsubscribeAll(Class<T> eventType) {
        Listener listener = registeredListeners.remove(eventType);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            logger.debug("Unsubscribed all handlers for event: {}", eventType.getSimpleName());
        }
    }

    @Override
    public boolean hasSubscribers(Class<? extends CoreEvent> eventType) {
        // 在代理模式下，总是返回 true（Bukkit 系统可能有其他插件监听）
        return true;
    }

    @Override
    public int getSubscriberCount(Class<? extends CoreEvent> eventType) {
        // 代理模式下无法准确获取订阅者数量
        return -1;
    }

    /**
     * 清空所有处理器
     */
    public void clear() {
        for (Listener listener : registeredListeners.values()) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();
        logger.info("EventBus cleared");
    }

    /**
     * 获取总发布事件数
     */
    public long getTotalEventsPublished() {
        return totalEventsPublished.get();
    }

    /**
     * 映射 RPGCore 优先级到 Bukkit 优先级
     */
    private org.bukkit.event.EventPriority mapPriority(cn.guangdian.rpgcore.event.EventPriority priority) {
        if (priority == null) {
            return org.bukkit.event.EventPriority.NORMAL;
        }
        return switch (priority) {
            case FIRST -> org.bukkit.event.EventPriority.HIGHEST;
            case EARLY -> org.bukkit.event.EventPriority.HIGH;
            case NORMAL -> org.bukkit.event.EventPriority.NORMAL;
            case LATE -> org.bukkit.event.EventPriority.LOW;
            case LAST -> org.bukkit.event.EventPriority.LOWEST;
        };
    }
}
