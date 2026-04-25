package cn.guangdian.rpgcore.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 核心事件基类 - 所有 RPGCore 事件的父类
 * 
 * <p>CoreEvent 继承 Bukkit Event，实现与 Bukkit 事件系统的完全兼容。
 * 所有自定义事件都应继承此类。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class PointsChangeEvent extends CoreEvent {
 *     private static final HandlerList HANDLERS = new HandlerList();
 *     private final UUID playerId;
 *     private final long oldBalance;
 *     private final long newBalance;
 *     
 *     public PointsChangeEvent(UUID playerId, long oldBalance, long newBalance) {
 *         super();
 *         this.playerId = playerId;
 *         this.oldBalance = oldBalance;
 *         this.newBalance = newBalance;
 *     }
 *     
 *     @Override
 *     public HandlerList getHandlers() {
 *         return HANDLERS;
 *     }
 *     
 *     public static HandlerList getHandlerList() {
 *         return HANDLERS;
 *     }
 *     
 *     // getters...
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class CoreEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * 事件是否被取消
     */
    private boolean cancelled = false;

    /**
     * 事件时间戳
     */
    private final long timestamp;

    /**
     * 创建同步事件
     */
    public CoreEvent() {
        this(false);
    }

    /**
     * 创建事件
     * 
     * @param async 是否为异步事件
     */
    public CoreEvent(boolean async) {
        super(async);
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * 获取 HandlerList（Bukkit 要求）
     * 
     * @return HandlerList 实例
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * 检查事件是否被取消
     * 
     * @return 如果事件被取消返回 true
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 设置事件是否取消
     * 
     * @param cancelled 是否取消
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * 获取事件时间戳
     * 
     * @return 事件创建时间（毫秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取事件名称
     * 
     * @return 事件名称（默认为类名）
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s{cancelled=%s, timestamp=%d}",
            getEventName(), cancelled, timestamp);
    }
}
