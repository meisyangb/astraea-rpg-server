package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.bukkit.Bukkit;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EventBus 代理实现 - 过渡期兼容方案
 *
 * <p>此类提供 EventBus 接口的代理实现，将调用转发到 Bukkit 事件系统。
 * 用于向后兼容旧代码，新代码应直接使用 EventPublisher 或 Bukkit API。</p>
 *
 * <p><b>⚠️ 此类为过渡期方案，将在未来版本移除。</b></p>
 *
 * @author GuangDian
 * @since 2.0.0
 * @deprecated 使用 {@link EventPublisher} 或 Bukkit 原生事件系统替代
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class EventBusProxy implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBusProxy.class);
    private static final AtomicBoolean warningLogged = new AtomicBoolean(false);

    private final Map<EventHandler<?>, org.bukkit.event.Listener> listenerMap = new ConcurrentHashMap<>();

    public EventBusProxy() {
        logDeprecationWarning();
    }

    private void logDeprecationWarning() {
        if (warningLogged.compareAndSet(false, true)) {
            logger.warn("========================================");
            logger.warn("EventBus 已废弃，请迁移到 Bukkit 事件系统");
            logger.warn("旧代码: eventBus.publish(event)");
            logger.warn("新代码: Bukkit.getPluginManager().callEvent(event)");
            logger.warn("或使用: EventPublisher.publish(event)");
            logger.warn("========================================");
        }
    }

    @Override
    public <T extends CoreEvent> void publish(T event) {
        if (event == null) {
            logger.warn("尝试发布 null 事件，已忽略");
            return;
        }
        logger.debug("[EventBus代理] 发布事件: {}", event.getEventName());
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public <T extends CoreEvent> void publishAsync(T event) {
        if (event == null) {
            logger.warn("尝试异步发布 null 事件，已忽略");
            return;
        }
        logger.debug("[EventBus代理] 异步发布事件: {}", event.getEventName());
        EventPublisher.publishAsync(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CoreEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        if (eventType == null || handler == null) {
            logger.warn("订阅事件参数无效: eventType={}, handler={}", eventType, handler);
            return;
        }

        org.bukkit.event.Listener proxyListener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onEvent(T event) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("事件处理器执行异常: {}", event.getEventName(), e);
                }
            }
        };

        listenerMap.put(handler, proxyListener);

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            Bukkit.getPluginManager().registerEvents(proxyListener, rpgCore);
            logger.debug("[EventBus代理] 订阅事件: {}", eventType.getSimpleName());
        } else {
            logger.warn("[EventBus代理] RPGCore 未初始化，无法注册监听器");
        }
    }

    @Override
    public void unsubscribe(EventHandler<?> handler) {
        if (handler == null) {
            return;
        }

        org.bukkit.event.Listener listener = listenerMap.remove(handler);
        if (listener != null) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
            logger.debug("[EventBus代理] 取消订阅事件处理器");
        }
    }

    @Override
    public <T extends CoreEvent> void unsubscribeAll(Class<T> eventType) {
        logger.debug("[EventBus代理] 取消订阅所有: {}", eventType.getSimpleName());
    }

    @Override
    public boolean hasSubscribers(Class<? extends CoreEvent> eventType) {
        return !listenerMap.isEmpty();
    }

    @Override
    public int getSubscriberCount(Class<? extends CoreEvent> eventType) {
        return listenerMap.size();
    }

    /**
     * 清理所有监听器
     */
    public void shutdown() {
        for (org.bukkit.event.Listener listener : listenerMap.values()) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
        listenerMap.clear();
        logger.debug("[EventBus代理] 已清理所有监听器");
    }
}
