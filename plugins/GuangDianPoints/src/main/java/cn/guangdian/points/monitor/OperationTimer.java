package cn.guangdian.points.monitor;

/**
 * 操作计时器
 * 支持try-with-resources自动计时
 */
public class OperationTimer implements AutoCloseable {

    /**
     * 空操作计时器（用于监控禁用时）
     */
    public static final OperationTimer NOOP = new OperationTimer();

    private final PerformanceMonitor monitor;
    private final String operationName;
    private final long startTime;
    private boolean closed = false;

    /**
     * 创建空操作计时器
     */
    private OperationTimer() {
        this.monitor = null;
        this.operationName = null;
        this.startTime = 0;
    }

    /**
     * 创建操作计时器
     *
     * @param monitor 性能监控器
     * @param operationName 操作名称
     */
    public OperationTimer(PerformanceMonitor monitor, String operationName) {
        this.monitor = monitor;
        this.operationName = operationName;
        this.startTime = System.nanoTime();
    }

    /**
     * 获取已耗时（毫秒）
     *
     * @return 已耗时
     */
    public long getElapsedMillis() {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    /**
     * 获取已耗时（纳秒）
     *
     * @return 已耗时
     */
    public long getElapsedNanos() {
        return System.nanoTime() - startTime;
    }

    /**
     * 停止计时并记录
     *
     * @return 耗时（毫秒）
     */
    public long stop() {
        if (closed) return 0;

        long duration = getElapsedMillis();
        if (monitor != null && operationName != null) {
            monitor.recordOperation(operationName, duration);
        }
        closed = true;
        return duration;
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 检查是否已关闭
     *
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
}