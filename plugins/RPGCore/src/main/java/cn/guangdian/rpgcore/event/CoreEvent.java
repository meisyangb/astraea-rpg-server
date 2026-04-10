package cn.guangdian.rpgcore.event;

/**
 * 核心事件基类 - 所有 RPGCore 事件的父类
 * 
 * <p>CoreEvent 是 RPGCore 事件系统的核心类，所有自定义事件都应继承此类。
 * 支持异步事件和事件取消。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class PointsChangeEvent extends CoreEvent {
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
 *     // getters...
 * }
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class CoreEvent {

    /**
     * 事件是否被取消
     */
    private boolean cancelled = false;

    /**
     * 是否为异步事件
     */
    private final boolean async;

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
        this.async = async;
        this.timestamp = System.currentTimeMillis();
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
     * 检查是否为异步事件
     * 
     * @return 如果是异步事件返回 true
     */
    public boolean isAsync() {
        return async;
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
        return String.format("%s{cancelled=%s, async=%s, timestamp=%d}",
            getEventName(), cancelled, async, timestamp);
    }
}