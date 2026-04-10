package cn.guangdian.rpgcore.event;

/**
 * 事件处理器接口
 * 
 * <p>EventHandler 是函数式接口，用于处理特定类型的事件。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用 Lambda 表达式
 * eventBus.subscribe(PointsChangeEvent.class, event -> {
 *     getLogger().info("玩家 " + event.getPlayerId() + " 余额变化");
 * });
 * 
 * // 使用方法引用
 * eventBus.subscribe(PointsChangeEvent.class, this::onPointsChange);
 * }</pre>
 * 
 * @param <T> 事件类型
 * @author GuangDian
 * @since 1.0.0
 */
@FunctionalInterface
public interface EventHandler<T extends CoreEvent> {

    /**
     * 处理事件
     * 
     * @param event 事件对象
     */
    void handle(T event);

    /**
     * 获取处理器优先级
     * 
     * <p>默认返回普通优先级。</p>
     * 
     * @return 优先级
     */
    default EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * 检查是否忽略已取消的事件
     * 
     * <p>默认返回 true，即忽略已取消的事件。</p>
     * 
     * @return 如果忽略已取消的事件返回 true
     */
    default boolean ignoreCancelled() {
        return true;
    }
}