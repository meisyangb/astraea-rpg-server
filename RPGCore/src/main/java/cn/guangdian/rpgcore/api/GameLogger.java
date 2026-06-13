package cn.guangdian.rpgcore.api;

import java.util.Map;

/**
 * 游戏日志接口 - 统一日志管理
 * 
 * <p>GameLogger 提供了统一的日志抽象层，支持：
 * <ul>
 *   <li>分级日志（DEBUG/INFO/WARNING/SEVERE）</li>
 *   <li>异常日志</li>
 *   <li>采样日志（防止高频刷屏）</li>
 *   <li>结构化日志（JSON格式）</li>
 *   <li>完整的统计信息</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取日志服务
 * RPGCore rpgCore = RPGCore.getInstance();
 * GameLogger logger = rpgCore.getGameLogger();
 * 
 * // 普通日志
 * logger.info("玩家登录: " + player.getName());
 * logger.warning("配置加载失败");
 * logger.severe("数据库连接异常");
 * 
 * // 带异常的日志
 * logger.severe("处理失败", exception);
 * 
 * // 采样日志（5秒内只输出一次）
 * logger.infoSampled("player-move", "玩家移动事件");
 * 
 * // 结构化日志（JSON格式）
 * logger.infoStructured("player_login", Map.of(
 *     "player", player.getName(),
 *     "uuid", player.getUniqueId().toString(),
 *     "ip", player.getAddress().getHostString()
 * ));
 * 
 * // 查看统计
 * LogStats stats = logger.getStats();
 * logger.info("日志统计: " + stats);
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface GameLogger {

    // ==================== 基础日志 ====================

    /**
     * 输出 INFO 级别日志
     * 
     * @param message 日志消息
     */
    void info(String message);

    /**
     * 输出 WARNING 级别日志
     * 
     * @param message 日志消息
     */
    void warning(String message);

    /**
     * 输出 SEVERE 级别日志
     * 
     * @param message 日志消息
     */
    void severe(String message);

    /**
     * 输出 DEBUG 级别日志
     * 
     * @param message 日志消息
     */
    void debug(String message);

    // ==================== 异常日志 ====================

    /**
     * 输出 INFO 级别日志（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void info(String message, Throwable throwable) {
        info(message + " - " + throwable.getMessage());
    }

    /**
     * 输出 WARNING 级别日志（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void warning(String message, Throwable throwable) {
        warning(message + " - " + throwable.getMessage());
    }

    /**
     * 输出 SEVERE 级别日志（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void severe(String message, Throwable throwable) {
        severe(message + " - " + throwable.getMessage());
    }

    /**
     * 输出 DEBUG 级别日志（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void debug(String message, Throwable throwable) {
        debug(message + " - " + throwable.getMessage());
    }

    // ==================== 采样日志 ====================

    /**
     * 采样输出 INFO 日志（防止高频刷屏）
     * 
     * <p>相同 samplerKey 的日志在默认间隔内只输出一次。</p>
     * 
     * @param samplerKey 采样器标识
     * @param message 日志消息
     */
    default void infoSampled(String samplerKey, String message) {
        info(message);
    }

    /**
     * 采样输出 INFO 日志（自定义间隔）
     * 
     * @param samplerKey 采样器标识
     * @param message 日志消息
     * @param intervalMs 采样间隔（毫秒）
     */
    default void infoSampled(String samplerKey, String message, long intervalMs) {
        info(message);
    }

    /**
     * 采样输出 WARNING 日志
     * 
     * @param samplerKey 采样器标识
     * @param message 日志消息
     */
    default void warningSampled(String samplerKey, String message) {
        warning(message);
    }

    /**
     * 采样输出 WARNING 日志（自定义间隔）
     * 
     * @param samplerKey 采样器标识
     * @param message 日志消息
     * @param intervalMs 采样间隔（毫秒）
     */
    default void warningSampled(String samplerKey, String message, long intervalMs) {
        warning(message);
    }

    // ==================== 结构化日志 ====================

    /**
     * 输出结构化 INFO 日志（JSON格式）
     * 
     * <p>便于日志收集和分析系统解析。</p>
     * 
     * @param event 事件名称
     * @param data 事件数据
     */
    default void infoStructured(String event, Map<String, Object> data) {
        info(event + ": " + data);
    }

    /**
     * 输出结构化 WARNING 日志（JSON格式）
     * 
     * @param event 事件名称
     * @param data 事件数据
     */
    default void warningStructured(String event, Map<String, Object> data) {
        warning(event + ": " + data);
    }

    /**
     * 输出结构化 SEVERE 日志（JSON格式）
     * 
     * @param event 事件名称
     * @param data 事件数据
     */
    default void severeStructured(String event, Map<String, Object> data) {
        severe(event + ": " + data);
    }

    // ==================== 统计信息 ====================

    /**
     * 获取日志队列大小
     * 
     * @return 队列中的日志数量（异步日志器返回估算值）
     */
    int getQueueSize();

    /**
     * 获取总日志数
     * 
     * @return 已输出的日志总数
     */
    long getTotalLogged();

    /**
     * 获取丢弃的日志数
     * 
     * @return 因级别过滤或队列满而丢弃的日志数
     */
    long getTotalDropped();

    /**
     * 获取完整统计信息
     * 
     * @return 日志统计信息对象
     */
    default Object getStats() {
        return new SimpleLogStats(getTotalLogged(), getTotalDropped(), getQueueSize());
    }

    /**
     * 关闭日志系统
     */
    void shutdown();

    /**
     * 简单日志统计记录
     */
    record SimpleLogStats(long totalLogged, long totalDropped, int queueSize) {
        @Override
        public String toString() {
            return String.format("LogStats{total=%d, dropped=%d, queue=%d}", 
                    totalLogged, totalDropped, queueSize);
        }
    }
}
