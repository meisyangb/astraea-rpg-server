package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.event.CoreEvent;
import cn.guangdian.rpgcore.event.EventHandler;

import java.util.function.Consumer;

/**
 * 事件总线支持类 - 推荐使用 MBassador
 *
 * <p>提供便捷的事件订阅和发布功能。默认使用 MBassador 高性能事件总线，
 * 替代传统的 Bukkit 事件系统和自定义事件总线。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义事件
 * public class PlayerLevelUpEvent extends CoreEvent {
 *     private final Player player;
 *     private final int newLevel;
 *
 *     public PlayerLevelUpEvent(Player player, int newLevel) {
 *         this.player = player;
 *         this.newLevel = newLevel;
 *     }
 *
 *     // Getters...
 * }
 *
 * // 订阅事件
 * EventBusSupport.subscribe(PlayerLevelUpEvent.class, event -> {
 *     Player player = event.getPlayer();
 *     int level = event.getNewLevel();
 *     player.sendMessage("恭喜升级到 " + level + " 级！");
 * });
 *
 * // 发布事件
 * EventBusSupport.publish(new PlayerLevelUpEvent(player, newLevel));
 *
 * // 异步发布
 * EventBusSupport.publishAsync(new PlayerLevelUpEvent(player, newLevel));
 * }</pre>
 *
 * <h3>性能对比：</h3>
 * <table border="1">
 *   <tr><th>特性</th><th>Bukkit Event</th><th>SimpleEventBus</th><th>MBassador</th></tr>
 *   <tr><td>同步性能</td><td>中等</td><td>高</td><td>极高</td></tr>
 *   <tr><td>异步支持</td><td>❌</td><td>✅</td><td>✅</td></tr>
 *   <tr><td>类型安全</td><td>运行时</td><td>编译期</td><td>编译期</td></tr>
 *   <tr><td>内存占用</td><td>高</td><td>低</td><td>低</td></tr>
 *   <tr><td>订阅者数量</td><td>无限制</td><td>无限制</td><td>无限制</td></tr>
 * </table>
 *
 * @author GuangDian
 * @since 2.0.0
 * @see EventBus
 * @see MBassadorEventBus
 * @deprecated 使用 EventBus 接口和 MBassador 替代自定义事件系统
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public final class EventBusSupport {

    private EventBusSupport() {
        // 工具类，禁止实例化
    }

    /**
     * 获取事件总线
     *
     * @return 事件总线
     */
    public static EventBus getEventBus() {
        return RPGCore.getInstance().getEventBus();
    }

    /**
     * 订阅事件
     *
     * @param eventType 事件类型
     * @param handler   处理器
     * @param <T>       事件类型
     */
    public static <T extends CoreEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
        getEventBus().subscribe(eventType, new EventHandler<T>() {
            @Override
            public cn.guangdian.rpgcore.event.EventPriority getPriority() {
                return cn.guangdian.rpgcore.event.EventPriority.NORMAL;
            }

            @Override
            public void handle(T event) {
                handler.accept(event);
            }
        });
    }

    /**
     * 订阅事件（指定优先级）
     *
     * @param eventType 事件类型
     * @param priority  优先级
     * @param handler   处理器
     * @param <T>       事件类型
     */
    public static <T extends CoreEvent> void subscribe(Class<T> eventType,
                                                        cn.guangdian.rpgcore.event.EventPriority priority,
                                                        Consumer<T> handler) {
        getEventBus().subscribe(eventType, new EventHandler<T>() {
            @Override
            public cn.guangdian.rpgcore.event.EventPriority getPriority() {
                return priority;
            }

            @Override
            public void handle(T event) {
                handler.accept(event);
            }
        });
    }

    /**
     * 发布事件（同步）
     *
     * @param event 事件
     * @param <T>   事件类型
     */
    public static <T extends CoreEvent> void publish(T event) {
        getEventBus().publish(event);
    }

    /**
     * 发布事件（异步）
     *
     * @param event 事件
     * @param <T>   事件类型
     */
    public static <T extends CoreEvent> void publishAsync(T event) {
        getEventBus().publishAsync(event);
    }

    /**
     * 检查是否有订阅者
     *
     * @param eventType 事件类型
     * @return 如果有订阅者返回 true
     */
    public static boolean hasSubscribers(Class<? extends CoreEvent> eventType) {
        return getEventBus().hasSubscribers(eventType);
    }

    /**
     * 获取订阅者数量
     *
     * @param eventType 事件类型
     * @return 订阅者数量
     */
    public static int getSubscriberCount(Class<? extends CoreEvent> eventType) {
        return getEventBus().getSubscriberCount(eventType);
    }

    /**
     * 创建事件发布器
     *
     * @param <T> 事件类型
     * @return 事件发布器
     */
    public static <T extends CoreEvent> EventPublisher<T> publisher(Class<T> eventType) {
        return new EventPublisher<>(eventType);
    }

    /**
     * 事件发布器
     *
     * @param <T> 事件类型
     */
    public static class EventPublisher<T extends CoreEvent> {
        private final Class<T> eventType;

        private EventPublisher(Class<T> eventType) {
            this.eventType = eventType;
        }

        /**
         * 发布事件
         *
         * @param event 事件
         */
        public void publish(T event) {
            EventBusSupport.publish(event);
        }

        /**
         * 异步发布事件
         *
         * @param event 事件
         */
        public void publishAsync(T event) {
            EventBusSupport.publishAsync(event);
        }

        /**
         * 检查是否有订阅者
         *
         * @return 如果有订阅者返回 true
         */
        public boolean hasSubscribers() {
            return EventBusSupport.hasSubscribers(eventType);
        }
    }
}
