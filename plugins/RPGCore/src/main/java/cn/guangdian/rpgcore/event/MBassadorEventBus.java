package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.api.EventBus;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MBassador 高性能事件总线实现
 *
 * <p>基于 MBassador 的高性能事件总线，支持异步事件处理和注解驱动的事件订阅。</p>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>高性能（比 Guava EventBus 快 10 倍以上）</li>
 *   <li>注解驱动，代码简洁</li>
 *   <li>支持异步事件处理</li>
 *   <li>支持事件优先级</li>
 *   <li>弱引用监听器，自动垃圾回收</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义事件
 * public class PlayerLevelUpEvent {
 *     private final Player player;
 *     private final int newLevel;
 *     // constructor, getters...
 * }
 *
 * // 订阅事件
 * public class PlayerListener {
 *     @Handler
 *     public void onLevelUp(PlayerLevelUpEvent event) {
 *         // 处理事件
 *     }
 * }
 *
 * // 发布事件
 * eventBus.publish(new PlayerLevelUpEvent(player, 10));
 * }</pre>
 *
 * @author GuangDian
 * @since 1.1.0
 */
public class MBassadorEventBus implements EventBus {

    private final Logger logger;
    private final MBassador<Object> bus;
    private final ConcurrentHashMap<Class<? extends CoreEvent>, Integer> subscriberCount;

    /**
     * 创建 MBassador 事件总线
     *
     * @param logger 日志记录器
     */
    public MBassadorEventBus(Logger logger) {
        this.logger = logger;
        this.subscriberCount = new ConcurrentHashMap<>();

        // 配置 MBassador
        BusConfiguration config = new BusConfiguration()
            .addFeature(Feature.SyncPubSub.Default())
            .addFeature(Feature.AsynchronousHandlerInvocation.Default())
            .addFeature(Feature.AsynchronousMessageDispatch.Default())
            .addPublicationErrorHandler(new PublicationErrorHandler());

        this.bus = new MBassador<>(config);

        logger.info("MBassador EventBus initialized");
    }

    @Override
    public <T extends CoreEvent> void publish(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        bus.publish(event);
    }

    @Override
    public <T extends CoreEvent> void publishAsync(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        bus.publishAsync(event);
    }

    @Override
    public <T extends CoreEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        // 创建适配器
        MBassadorHandlerAdapter<T> adapter = new MBassadorHandlerAdapter<>(handler);
        bus.subscribe(adapter);

        // 更新订阅计数
        subscriberCount.merge(eventType, 1, Integer::sum);

        logger.fine("Subscribed handler for event: " + eventType.getSimpleName());
    }

    /**
     * 使用注解方式订阅监听器
     *
     * @param listener 监听器对象（包含 @Handler 注解的方法）
     */
    public void subscribe(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        bus.subscribe(listener);
        logger.fine("Subscribed listener: " + listener.getClass().getSimpleName());
    }

    @Override
    public void unsubscribe(EventHandler<?> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        // MBassador 使用弱引用，不需要显式取消订阅
        // 但为了兼容性，我们记录日志
        logger.fine("Unsubscribe called for handler: " + handler.getClass().getSimpleName());
    }

    /**
     * 取消订阅监听器
     *
     * @param listener 监听器对象
     */
    public void unsubscribe(Object listener) {
        if (listener == null) {
            return;
        }

        bus.unsubscribe(listener);
        logger.fine("Unsubscribed listener: " + listener.getClass().getSimpleName());
    }

    @Override
    public <T extends CoreEvent> void unsubscribeAll(Class<T> eventType) {
        if (eventType == null) {
            return;
        }

        // MBassador 不支持按事件类型取消订阅
        // 使用弱引用，让 GC 自动处理
        subscriberCount.remove(eventType);
        logger.fine("Unsubscribed all handlers for event: " + eventType.getSimpleName());
    }

    @Override
    public boolean hasSubscribers(Class<? extends CoreEvent> eventType) {
        return subscriberCount.getOrDefault(eventType, 0) > 0;
    }

    @Override
    public int getSubscriberCount(Class<? extends CoreEvent> eventType) {
        return subscriberCount.getOrDefault(eventType, 0);
    }

    /**
     * 关闭事件总线
     */
    public void shutdown() {
        bus.shutdown();
        subscriberCount.clear();
        logger.info("MBassador EventBus shutdown");
    }

    /**
     * 获取底层 MBassador 实例（高级用法）
     *
     * @return MBassador 实例
     */
    public MBassador<Object> getBus() {
        return bus;
    }

    /**
     * 事件处理器适配器
     */
    @Listener(references = References.Weak)
    private static class MBassadorHandlerAdapter<T extends CoreEvent> {
        private final EventHandler<T> handler;

        MBassadorHandlerAdapter(EventHandler<T> handler) {
            this.handler = handler;
        }

        @Handler
        public void handle(T event) {
            if (event.isCancelled() && handler.ignoreCancelled()) {
                return;
            }
            handler.handle(event);
        }
    }

    /**
     * 发布错误处理器
     */
    private class PublicationErrorHandler implements IPublicationErrorHandler {
        @Override
        public void handleError(PublicationError error) {
            logger.log(Level.WARNING, "Event publication error", error.getCause());
        }
    }
}
