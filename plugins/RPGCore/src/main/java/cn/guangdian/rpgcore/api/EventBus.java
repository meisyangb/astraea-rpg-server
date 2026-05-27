package cn.guangdian.rpgcore.api;

import cn.guangdian.rpgcore.event.CoreEvent;
import cn.guangdian.rpgcore.event.EventHandler;

/**
 * 事件总线接口 - 解耦插件间通信
 * 
 * <p>EventBus 提供了一个发布-订阅模式的事件系统，用于模块间的解耦通信。
 * 替代现有的反射调用方式，提供类型安全的事件传递机制。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 发布事件
 * eventBus.publish(new PointsChangeEvent(playerId, oldBalance, newBalance));
 * 
 * // 异步发布事件
 * eventBus.publishAsync(new PlayerDataLoadEvent(playerId));
 * 
 * // 订阅事件
 * eventBus.subscribe(PointsChangeEvent.class, event -> {
 *     getLogger().info("玩家 " + event.getPlayerId() + " 余额变化");
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface EventBus {

    /**
     * 发布事件（同步）
     * 
     * <p>事件将在当前线程中同步处理，所有订阅者处理完成后才返回。
     * 适用于需要立即处理的事件。</p>
     * 
     * @param event 事件对象
     * @param <T> 事件类型
     * @throws IllegalArgumentException 如果 event 为 null
     */
    <T extends CoreEvent> void publish(T event);

    /**
     * 发布事件（异步）
     * 
     * <p>事件将在异步线程中处理，不阻塞当前线程。
     * 适用于耗时操作或非关键事件。</p>
     * 
     * @param event 事件对象
     * @param <T> 事件类型
     * @throws IllegalArgumentException 如果 event 为 null
     */
    <T extends CoreEvent> void publishAsync(T event);

    /**
     * 订阅事件
     * 
     * <p>注册一个事件处理器，当指定类型的事件发布时会被调用。</p>
     * 
     * @param eventType 事件类型的 Class 对象
     * @param handler 事件处理器
     * @param <T> 事件类型
     * @throws IllegalArgumentException 如果 eventType 或 handler 为 null
     */
    <T extends CoreEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);

    /**
     * 取消订阅
     * 
     * <p>移除之前注册的事件处理器。</p>
     * 
     * @param handler 要移除的事件处理器
     * @throws IllegalArgumentException 如果 handler 为 null
     */
    void unsubscribe(EventHandler<?> handler);

    /**
     * 取消订阅指定类型的所有处理器
     * 
     * @param eventType 事件类型的 Class 对象
     * @param <T> 事件类型
     */
    <T extends CoreEvent> void unsubscribeAll(Class<T> eventType);

    /**
     * 检查是否有指定事件类型的订阅者
     * 
     * @param eventType 事件类型的 Class 对象
     * @return 如果有订阅者返回 true
     */
    boolean hasSubscribers(Class<? extends CoreEvent> eventType);

    /**
     * 获取指定事件类型的订阅者数量
     * 
     * @param eventType 事件类型的 Class 对象
     * @return 订阅者数量
     */
    int getSubscriberCount(Class<? extends CoreEvent> eventType);
}