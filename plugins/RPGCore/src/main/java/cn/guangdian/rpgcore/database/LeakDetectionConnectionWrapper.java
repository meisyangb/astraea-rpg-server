package cn.guangdian.rpgcore.database;

import org.bukkit.Bukkit;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 连接泄漏检测包装器
 * 
 * <p>包装数据库连接，检测潜在的连接泄漏问题。</p>
 * 
 * @author GuangDian
 * @since 2.0.0
 */
public class LeakDetectionConnectionWrapper implements Connection {

    private static final Logger logger = Logger.getLogger("RPGCore-LeakDetection");
    
    private static final ConcurrentHashMap<Long, ConnectionInfo> ACTIVE_CONNECTIONS = new ConcurrentHashMap<>();
    private static final AtomicLong TOTAL_LEAKS_DETECTED = new AtomicLong(0);
    private static final long LEAK_CHECK_INTERVAL_MS = 60000;
    
    static {
        Thread leakChecker = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(LEAK_CHECK_INTERVAL_MS);
                    checkForLeaks();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "RPGCore-LeakChecker");
        leakChecker.setDaemon(true);
        leakChecker.start();
    }

    private final Connection delegate;
    private final long connectionId;
    private final long createTime;
    private final StackTraceElement[] creationStack;
    private volatile boolean closed = false;
    private volatile long lastAccessTime;

    public LeakDetectionConnectionWrapper(Connection delegate) {
        this.delegate = delegate;
        this.connectionId = System.identityHashCode(this);
        this.createTime = System.currentTimeMillis();
        this.lastAccessTime = createTime;
        this.creationStack = Thread.currentThread().getStackTrace();
        
        ACTIVE_CONNECTIONS.put(connectionId, new ConnectionInfo(
            connectionId, createTime, creationStack
        ));
        
        logger.fine("连接创建: ID=" + connectionId + ", 活跃连接数=" + ACTIVE_CONNECTIONS.size());
    }

    private void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        
        closed = true;
        ACTIVE_CONNECTIONS.remove(connectionId);
        
        long duration = System.currentTimeMillis() - createTime;
        logger.fine("连接关闭: ID=" + connectionId + ", 存活时间=" + duration + "ms, 剩余连接数=" + ACTIVE_CONNECTIONS.size());
        
        delegate.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed || delegate.isClosed();
    }

    private static void checkForLeaks() {
        long now = System.currentTimeMillis();
        long leakThreshold = 300000;
        
        ACTIVE_CONNECTIONS.forEach((id, info) -> {
            long age = now - info.createTime;
            if (age > leakThreshold) {
                TOTAL_LEAKS_DETECTED.incrementAndGet();
                
                logger.warning("检测到潜在连接泄漏!");
                logger.warning("  连接ID: " + id);
                logger.warning("  存活时间: " + (age / 1000) + "秒");
                logger.warning("  创建堆栈:");
                
                for (StackTraceElement element : info.creationStack) {
                    logger.warning("    at " + element.toString());
                }
            }
        });
        
        if (!ACTIVE_CONNECTIONS.isEmpty()) {
            logger.info("当前活跃连接数: " + ACTIVE_CONNECTIONS.size());
        }
    }

    public static int getActiveConnectionCount() {
        return ACTIVE_CONNECTIONS.size();
    }

    public static long getTotalLeaksDetected() {
        return TOTAL_LEAKS_DETECTED.get();
    }

    public static void logConnectionStats() {
        logger.info("========== 连接池统计 ==========");
        logger.info("活跃连接数: " + ACTIVE_CONNECTIONS.size());
        logger.info("检测到的泄漏次数: " + TOTAL_LEAKS_DETECTED.get());
        
        if (!ACTIVE_CONNECTIONS.isEmpty()) {
            long now = System.currentTimeMillis();
            ACTIVE_CONNECTIONS.forEach((id, info) -> {
                long age = (now - info.createTime) / 1000;
                logger.info("  连接 " + id + ": 存活 " + age + "秒");
            });
        }
        logger.info("================================");
    }

    private static class ConnectionInfo {
        final long connectionId;
        final long createTime;
        final StackTraceElement[] creationStack;

        ConnectionInfo(long connectionId, long createTime, StackTraceElement[] creationStack) {
            this.connectionId = connectionId;
            this.createTime = createTime;
            this.creationStack = creationStack;
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        updateAccessTime();
        return delegate.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        updateAccessTime();
        return delegate.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        updateAccessTime();
        return delegate.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        updateAccessTime();
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        updateAccessTime();
        return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        updateAccessTime();
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        updateAccessTime();
        delegate.rollback();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        updateAccessTime();
        return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        updateAccessTime();
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        updateAccessTime();
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        updateAccessTime();
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        updateAccessTime();
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        updateAccessTime();
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        updateAccessTime();
        return delegate.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        updateAccessTime();
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        updateAccessTime();
        delegate.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        updateAccessTime();
        return delegate.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        updateAccessTime();
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        updateAccessTime();
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        updateAccessTime();
        delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        updateAccessTime();
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        updateAccessTime();
        return delegate.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        updateAccessTime();
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        updateAccessTime();
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        updateAccessTime();
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        updateAccessTime();
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        updateAccessTime();
        return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        updateAccessTime();
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        updateAccessTime();
        return delegate.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        updateAccessTime();
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        updateAccessTime();
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        updateAccessTime();
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        updateAccessTime();
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        updateAccessTime();
        return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        updateAccessTime();
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        updateAccessTime();
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        updateAccessTime();
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        updateAccessTime();
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        updateAccessTime();
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        updateAccessTime();
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        updateAccessTime();
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        updateAccessTime();
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        updateAccessTime();
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        updateAccessTime();
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        updateAccessTime();
        return delegate.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        updateAccessTime();
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        updateAccessTime();
        return delegate.isWrapperFor(iface);
    }
}
