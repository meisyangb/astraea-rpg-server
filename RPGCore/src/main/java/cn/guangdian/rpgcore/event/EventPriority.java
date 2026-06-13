package cn.guangdian.rpgcore.event;

/**
 * 事件处理器优先级
 * 
 * <p>定义事件处理器的执行顺序，优先级高的处理器先执行。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public enum EventPriority {

    /**
     * 最先执行
     * <p>用于需要在其他处理器之前处理事件的场景，如事件监控。</p>
     */
    FIRST(0),

    /**
     * 较早执行
     * <p>用于需要在大部分处理器之前处理事件的场景。</p>
     */
    EARLY(1),

    /**
     * 普通优先级（默认）
     */
    NORMAL(2),

    /**
     * 较晚执行
     * <p>用于需要在大部分处理器之后处理事件的场景。</p>
     */
    LATE(3),

    /**
     * 最后执行
     * <p>用于需要在所有处理器之后处理事件的场景，如事件日志记录。</p>
     */
    LAST(4);

    private final int order;

    EventPriority(int order) {
        this.order = order;
    }

    /**
     * 获取执行顺序
     * 
     * @return 顺序值（越小越先执行）
     */
    public int getOrder() {
        return order;
    }
}