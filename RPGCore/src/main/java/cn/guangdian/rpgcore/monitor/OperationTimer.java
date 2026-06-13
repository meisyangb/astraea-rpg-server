package cn.guangdian.rpgcore.monitor;

/**
 * 操作计时器
 * 
 * <p>实现 AutoCloseable 接口，支持 try-with-resources 自动计时。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * try (OperationTimer timer = monitor.startOperation("loadPlayerData")) {
 *     // 业务逻辑
 * } // 自动记录耗时
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
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
     */
    public void stop() {
        if (closed || monitor == null) {
            return;
        }
        
        long durationMs = getElapsedMillis();
        monitor.recordOperation(operationName, durationMs);
        closed = true;
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 检查是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 获取操作名称
     */
    public String getOperationName() {
        return operationName;
    }
}