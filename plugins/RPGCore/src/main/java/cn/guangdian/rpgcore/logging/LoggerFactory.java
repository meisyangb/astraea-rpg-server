package cn.guangdian.rpgcore.logging;

import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SLF4J 日志工厂 - 推荐使用
 *
 * <p>提供统一的 SLF4J 日志实例获取。替代 Bukkit.getLogger() 和自定义日志实现。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取日志记录器
 * private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
 *
 * // 使用日志
 * logger.debug("调试信息");
 * logger.info("普通信息: {}", data);
 * logger.warn("警告信息", exception);
 * logger.error("错误信息", throwable);
 *
 * // 使用占位符
 * logger.info("玩家 {} 执行了命令 {}", playerName, command);
 * }</pre>
 *
 * <h3>与 Bukkit Logger 对比：</h3>
 * <table border="1">
 *   <tr><th>特性</th><th>Bukkit Logger</th><th>SLF4J</th></tr>
 *   <tr><td>占位符支持</td><td>❌</td><td>✅</td></tr>
 *   <tr><td>日志级别控制</td><td>有限</td><td>完整</td></tr>
 *   <tr><td>结构化日志</td><td>❌</td><td>✅</td></tr>
 *   <tr><td>性能</td><td>一般</td><td>高</td></tr>
 *   <tr><td>异步支持</td><td>❌</td><td>✅</td></tr>
 * </table>
 *
 * @author GuangDian
 * @since 2.0.0
 * @deprecated 使用 SLF4J 替代 Bukkit Logger 和自定义日志实现
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public final class LoggerFactory {

    private static final Map<String, Logger> LOGGER_CACHE = new ConcurrentHashMap<>();

    private LoggerFactory() {
        // 工具类，禁止实例化
    }

    /**
     * 获取日志记录器
     *
     * @param clazz 类
     * @return 日志记录器
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * 获取日志记录器
     *
     * @param name 日志名称
     * @return 日志记录器
     */
    public static Logger getLogger(String name) {
        return LOGGER_CACHE.computeIfAbsent(name, org.slf4j.LoggerFactory::getLogger);
    }

    /**
     * 获取 RPGCore 日志记录器
     *
     * @return 日志记录器
     */
    public static Logger getRPGCoreLogger() {
        return getLogger("cn.guangdian.rpgcore");
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        LOGGER_CACHE.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存的日志记录器数量
     */
    public static int getCacheSize() {
        return LOGGER_CACHE.size();
    }
}
