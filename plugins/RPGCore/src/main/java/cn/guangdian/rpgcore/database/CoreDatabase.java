package cn.guangdian.rpgcore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * 共享数据库连接池
 * 
 * <p>所有 GuangDian 系列插件共用此连接池，避免每个插件各自管理连接。</p>
 * 
 * <p>使用方式：</p>
 * <pre>
 * // 获取连接
 * try (Connection conn = CoreDatabase.getConnection()) {
 *     // 执行数据库操作
 * }
 * 
 * // 异步操作
 * CoreDatabase.getConnectionAsync().thenAccept(conn -> {
 *     // 异步执行
 * });
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class CoreDatabase {

    private static HikariDataSource sharedPool;
    private static boolean enabled = false;
    private static JavaPlugin plugin;

    private CoreDatabase() {}

    /**
     * 初始化共享数据库连接池
     * 
     * @param pluginInstance 插件实例
     * @param jdbcUrl JDBC 连接 URL
     * @param username 用户名
     * @param password 密码
     * @param maxPoolSize 最大连接数
     * @return 是否初始化成功
     */
    public static synchronized boolean initialize(JavaPlugin pluginInstance, 
                                                   String jdbcUrl, 
                                                   String username, 
                                                   String password, 
                                                   int maxPoolSize) {
        if (enabled) {
            pluginInstance.getLogger().warning("[CoreDatabase] 连接池已初始化，跳过重复初始化");
            return true;
        }

        plugin = pluginInstance;

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("RPGCore-SharedPool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("netTimeoutForStreamingResults", "0");

            sharedPool = new HikariDataSource(config);
            enabled = true;

            plugin.getLogger().info("[CoreDatabase] 共享数据库连接池已初始化");
            plugin.getLogger().info("[CoreDatabase] 连接池名称: " + config.getPoolName());
            plugin.getLogger().info("[CoreDatabase] 最大连接数: " + maxPoolSize);

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[CoreDatabase] 初始化失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
            return false;
        }
    }

    /**
     * 使用配置文件初始化
     * 
     * @param pluginInstance 插件实例
     * @return 是否初始化成功
     */
    public static synchronized boolean initialize(JavaPlugin pluginInstance) {
        var config = pluginInstance.getConfig();
        
        String jdbcUrl = config.getString("database.url", 
            "jdbc:mysql://localhost:3306/mc_rpg?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        int maxPoolSize = config.getInt("database.max-pool-size", 20);

        return initialize(pluginInstance, jdbcUrl, username, password, maxPoolSize);
    }

    /**
     * 获取数据库连接
     * 
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     * @throws IllegalStateException 如果连接池未初始化
     */
    public static Connection getConnection() throws SQLException {
        if (!enabled || sharedPool == null) {
            throw new IllegalStateException("CoreDatabase 未初始化或已关闭");
        }
        return sharedPool.getConnection();
    }

    /**
     * 异步获取数据库连接
     * 
     * @return 包含连接的 CompletableFuture
     */
    public static CompletableFuture<Connection> getConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("异步获取数据库连接失败", e);
            }
        });
    }

    /**
     * 检查连接池是否已启用
     * 
     * @return 是否已启用
     */
    public static boolean isEnabled() {
        return enabled && sharedPool != null && !sharedPool.isClosed();
    }

    /**
     * 获取连接池状态信息
     * 
     * @return 状态信息字符串
     */
    public static String getPoolStatus() {
        if (!enabled || sharedPool == null) {
            return "未初始化";
        }
        
        return String.format("活跃连接: %d, 空闲连接: %d, 总连接: %d, 等待线程: %d",
            sharedPool.getHikariPoolMXBean().getActiveConnections(),
            sharedPool.getHikariPoolMXBean().getIdleConnections(),
            sharedPool.getHikariPoolMXBean().getTotalConnections(),
            sharedPool.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }

    /**
     * 关闭连接池
     */
    public static synchronized void shutdown() {
        if (sharedPool != null && !sharedPool.isClosed()) {
            sharedPool.close();
            plugin.getLogger().info("[CoreDatabase] 共享数据库连接池已关闭");
        }
        enabled = false;
        sharedPool = null;
    }

    /**
     * 获取连接池数据源（高级用法）
     * 
     * @return HikariDataSource 实例
     */
    public static HikariDataSource getDataSource() {
        return sharedPool;
    }
}
