package cn.guangdian.rpgcore.event;

import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * RPGCore 事件拦截器
 *
 * <p>提供事件拦截、过滤、转换等高级功能：</p>
 * <ul>
 *   <li>事件前置检查（权限、条件）</li>
 *   <li>事件转换（A事件 → B事件）</li>
 *   <li>事件拦截（黑名单、白名单）</li>
 *   <li>事件增强（添加额外数据）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 注册事件拦截器
 * EventInterceptor interceptor = new EventInterceptor(plugin);
 * interceptor.register();
 *
 * // 添加事件过滤器
 * interceptor.addFilter(PlayerStatsChangedEvent.class, event -> {
 *     return event.getPlayer().hasPermission("rpg.stats.track");
 * });
 *
 * // 添加事件转换器
 * interceptor.addTransformer(PlayerStatsChangedEvent.class, event -> {
 *     // 转换或增强事件
 *     return new EnhancedStatsEvent(event);
 * });
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class EventInterceptor implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(EventInterceptor.class);

    // 事件过滤器：返回 false 表示拦截该事件
    private final Map<Class<? extends Event>, List<Predicate<Event>>> filters = new ConcurrentHashMap<>();

    // 事件转换器：将事件转换为另一个事件（或增强）
    private final Map<Class<? extends Event>, List<EventTransformer>> transformers = new ConcurrentHashMap<>();

    // 事件监听器：在事件处理前后执行
    private final Map<Class<? extends Event>, List<EventListener>> listeners = new ConcurrentHashMap<>();

    // 黑名单：这些事件会被完全拦截
    private final Set<Class<? extends Event>> blacklist = ConcurrentHashMap.newKeySet();

    // 白名单：如果设置了白名单，只有白名单内的事件能通过
    private final Set<Class<? extends Event>> whitelist = ConcurrentHashMap.newKeySet();
    private volatile boolean useWhitelist = false;

    // 是否启用拦截
    private volatile boolean enabled = true;

    /**
     * 创建事件拦截器
     */
    public EventInterceptor() {
    }

    /**
     * 启用拦截器
     */
    public void enable() {
        this.enabled = true;
        logger.info("事件拦截器已启用");
    }

    /**
     * 禁用拦截器
     */
    public void disable() {
        this.enabled = false;
        logger.info("事件拦截器已禁用");
    }

    /**
     * 检查拦截器是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 拦截事件
     *
     * <p>在事件发布前调用，返回 false 表示拦截该事件。</p>
     *
     * @param event 要检查的事件
     * @return true 表示允许通过，false 表示拦截
     */
    public boolean intercept(Event event) {
        if (!enabled) {
            return true;
        }

        Class<? extends Event> eventType = event.getClass();

        // 1. 黑名单检查
        if (isBlacklisted(eventType)) {
            logger.debug("事件被黑名单拦截: {}", event.getEventName());
            return false;
        }

        // 2. 白名单检查
        if (useWhitelist && !isWhitelisted(eventType)) {
            logger.debug("事件不在白名单中: {}", event.getEventName());
            return false;
        }

        // 3. 过滤器检查
        List<Predicate<Event>> eventFilters = filters.get(eventType);
        if (eventFilters != null) {
            for (Predicate<Event> filter : eventFilters) {
                if (!filter.test(event)) {
                    logger.debug("事件被过滤器拦截: {}", event.getEventName());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 转换事件
     *
     * <p>在事件发布后调用，可以转换或增强事件。</p>
     *
     * @param event 原始事件
     * @return 转换后的事件（如果不需要转换则返回原事件）
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> T transform(T event) {
        if (!enabled) {
            return event;
        }

        Class<? extends Event> eventType = event.getClass();
        List<EventTransformer> eventTransformers = transformers.get(eventType);

        if (eventTransformers != null) {
            Event current = event;
            for (EventTransformer transformer : eventTransformers) {
                try {
                    current = transformer.transform(current);
                    if (current == null) {
                        logger.warn("事件转换器返回 null: {}", eventType.getSimpleName());
                        return event; // 返回原始事件
                    }
                } catch (Exception e) {
                    logger.error("事件转换异常: {}", eventType.getSimpleName(), e);
                    return event; // 返回原始事件
                }
            }
            return (T) current;
        }

        return event;
    }

    /**
     * 添加事件过滤器
     *
     * @param eventType 事件类型
     * @param filter 过滤器（返回 false 拦截事件）
     */
    public <T extends Event> void addFilter(Class<T> eventType, Predicate<T> filter) {
        @SuppressWarnings("unchecked")
        List<Predicate<Event>> list = filters.computeIfAbsent(eventType, k -> new ArrayList<>());
        @SuppressWarnings("unchecked")
        Predicate<Event> rawFilter = (Predicate<Event>) filter;
        list.add(rawFilter);
        logger.debug("添加事件过滤器: {}", eventType.getSimpleName());
    }

    /**
     * 移除事件过滤器
     */
    public <T extends Event> void removeFilter(Class<T> eventType, Predicate<T> filter) {
        List<Predicate<Event>> list = filters.get(eventType);
        if (list != null) {
            list.remove(filter);
        }
    }

    /**
     * 添加事件转换器
     *
     * @param eventType 事件类型
     * @param transformer 转换器
     */
    public <T extends Event> void addTransformer(Class<T> eventType, EventTransformer<T> transformer) {
        @SuppressWarnings("unchecked")
        List<EventTransformer> list = transformers.computeIfAbsent(eventType, k -> new ArrayList<>());
        @SuppressWarnings("unchecked")
        EventTransformer rawTransformer = transformer;
        list.add(rawTransformer);
        logger.debug("添加事件转换器: {}", eventType.getSimpleName());
    }

    /**
     * 移除事件转换器
     */
    public <T extends Event> void removeTransformer(Class<T> eventType, EventTransformer<T> transformer) {
        List<EventTransformer> list = transformers.get(eventType);
        if (list != null) {
            list.remove(transformer);
        }
    }

    /**
     * 添加事件监听器
     *
     * @param eventType 事件类型
     * @param listener 监听器
     */
    public <T extends Event> void addListener(Class<T> eventType, EventListener<T> listener) {
        @SuppressWarnings("unchecked")
        List<EventListener> list = listeners.computeIfAbsent(eventType, k -> new ArrayList<>());
        @SuppressWarnings("unchecked")
        EventListener rawListener = listener;
        list.add(rawListener);
        logger.debug("添加事件监听器: {}", eventType.getSimpleName());
    }

    /**
     * 将事件类型加入黑名单
     */
    public void addToBlacklist(Class<? extends Event> eventType) {
        blacklist.add(eventType);
        logger.info("事件加入黑名单: {}", eventType.getSimpleName());
    }

    /**
     * 将事件类型从黑名单移除
     */
    public void removeFromBlacklist(Class<? extends Event> eventType) {
        blacklist.remove(eventType);
        logger.info("事件从黑名单移除: {}", eventType.getSimpleName());
    }

    /**
     * 检查事件是否在黑名单中
     */
    public boolean isBlacklisted(Class<? extends Event> eventType) {
        return blacklist.contains(eventType);
    }

    /**
     * 将事件类型加入白名单
     */
    public void addToWhitelist(Class<? extends Event> eventType) {
        whitelist.add(eventType);
        logger.info("事件加入白名单: {}", eventType.getSimpleName());
    }

    /**
     * 将事件类型从白名单移除
     */
    public void removeFromWhitelist(Class<? extends Event> eventType) {
        whitelist.remove(eventType);
        logger.info("事件从白名单移除: {}", eventType.getSimpleName());
    }

    /**
     * 检查事件是否在白名单中
     */
    public boolean isWhitelisted(Class<? extends Event> eventType) {
        return whitelist.contains(eventType);
    }

    /**
     * 启用白名单模式
     */
    public void enableWhitelist() {
        this.useWhitelist = true;
        logger.info("白名单模式已启用");
    }

    /**
     * 禁用白名单模式
     */
    public void disableWhitelist() {
        this.useWhitelist = false;
        logger.info("白名单模式已禁用");
    }

    /**
     * 清除所有配置
     */
    public void clear() {
        filters.clear();
        transformers.clear();
        listeners.clear();
        blacklist.clear();
        whitelist.clear();
        useWhitelist = false;
        logger.info("事件拦截器配置已清除");
    }

    // ==================== 接口定义 ====================

    /**
     * 事件转换器接口
     */
    @FunctionalInterface
    public interface EventTransformer<T extends Event> {
        /**
         * 转换事件
         *
         * @param event 原始事件
         * @return 转换后的事件
         */
        Event transform(T event);
    }

    /**
     * 事件监听器接口
     */
    public interface EventListener<T extends Event> {
        /**
         * 事件处理前调用
         */
        default void onBefore(T event) {}

        /**
         * 事件处理后调用
         */
        default void onAfter(T event) {}
    }
}
