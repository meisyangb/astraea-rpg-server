package cn.guangdian.rpgcore.api;

import cn.guangdian.rpgcore.event.CoreEvent;
import cn.guangdian.rpgcore.event.EventHandler;

/**
 * 事件总线接口 - 已废弃
 *
 * <p><b>⚠️ 废弃说明：</b></p>
 * <p>此接口已废弃，建议使用 Bukkit 原生事件系统。</p>
 * <p>原因：</p>
 * <ul>
 *   <li>Bukkit 事件系统成熟稳定，所有插件都支持</li>
 *   <li>无需额外学习成本</li>
 *   <li>调试工具完善</li>
 *   <li>生态兼容性最好</li>
 * </ul>
 *
 * <p><b>迁移方案：</b></p>
 * <pre>{@code
 * // 旧代码（废弃）
 * eventBus.publish(new PointsChangeEvent(playerId, oldBalance, newBalance));
 *
 * // 新代码（推荐）
 * Bukkit.getPluginManager().callEvent(new PointsChangeEvent(playerId, oldBalance, newBalance));
 * }</pre>
 *
 * @author GuangDian
 * @since 1.0.0
 * @deprecated 使用 Bukkit 原生事件系统替代。所有插件间通信应通过 Bukkit.getPluginManager().callEvent() 和 @EventHandler 实现。
 */
@Deprecated(since = "2.0.0", forRemoval = true)
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