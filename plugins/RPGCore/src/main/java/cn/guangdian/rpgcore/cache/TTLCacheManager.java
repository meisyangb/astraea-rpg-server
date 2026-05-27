package cn.guangdian.rpgcore.cache;

import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.CacheStats;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * TTL缓存管理器（模式选择器）
 *
 * <p>根据配置选择不同的缓存实现策略。</p>
 *
 * <h3>可用模式：</h3>
 * <ul>
 *   <li><b>lightweight</b> - 轻量模式，适用于中小型服务器（默认）</li>
 *   <li><b>high_performance</b> - 高性能模式，适用于大型服务器</li>
 * </ul>
 *
 * <h3>选择建议：</h3>
 * <table border="1">
 *   <tr><th>场景</th><th>推荐模式</th></tr>
 *   <tr><td>在线玩家 &lt; 50</td><td>lightweight</td></tr>
 *   <tr><td>在线玩家 50-100</td><td>lightweight 或 high_performance</td></tr>
 *   <tr><td>在线玩家 &gt; 100</td><td>high_performance</td></tr>
 *   <tr><td>缓存条目 &lt; 1000</td><td>lightweight</td></tr>
 *   <tr><td>缓存条目 &gt; 1000</td><td>high_performance</td></tr>
 * </table>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class TTLCacheManager implements CacheProvider {

    /**
     * 缓存模式枚举
     */
    public enum Mode {
        /**
         * 轻量模式 - 适用于中小型服务器
         * <ul>
         *   <li>无锁读取</li>
         *   <li>简单淘汰</li>
         *   <li>低内存占用</li>
         * </ul>
         */
        LIGHTWEIGHT,

        /**
         * 高性能模式 - 适用于大型服务器
         * <ul>
         *   <li>O(1) LRU淘汰</li>
         *   <li>Pattern缓存</li>
         *   <li>增量清理</li>
         * </ul>
         */
        HIGH_PERFORMANCE
    }

    // 委托给实际的实现
    private final CacheProvider delegate;
    private final Mode mode;

    /**
     * 创建缓存管理器（默认轻量模式）
     */
    public TTLCacheManager(int maxSize, Duration defaultTTL, boolean recordStats) {
        this(maxSize, defaultTTL, recordStats, Mode.LIGHTWEIGHT);
    }

    /**
     * 创建缓存管理器（指定模式）
     *
     * @param maxSize 最大缓存大小
     * @param defaultTTL 默认过期时间
     * @param recordStats 是否记录统计
     * @param mode 缓存模式
     */
    public TTLCacheManager(int maxSize, Duration defaultTTL, boolean recordStats, Mode mode) {
        this.mode = mode;

        switch (mode) {
            case HIGH_PERFORMANCE:
                this.delegate = new HighPerformanceCacheProvider(maxSize, defaultTTL, recordStats);
                break;
            case LIGHTWEIGHT:
            default:
                this.delegate = new LightweightCacheProvider(maxSize, defaultTTL, recordStats);
                break;
        }
    }

    /**
     * 创建缓存管理器（默认配置，轻量模式）
     */
    public TTLCacheManager() {
        this(1000, Duration.ofMinutes(30), true, Mode.LIGHTWEIGHT);
    }

    /**
     * 创建缓存管理器（指定模式）
     *
     * @param mode 缓存模式
     */
    public TTLCacheManager(Mode mode) {
        this(1000, Duration.ofMinutes(30), true, mode);
    }

    /**
     * 获取当前缓存模式
     *
     * @return 缓存模式
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * 获取底层实现类名
     *
     * @return 实现类简单名称
     */
    public String getImplementationName() {
        return delegate.getClass().getSimpleName();
    }

    // ==================== 委托方法 ====================

    @Override
    public <T> T get(String key, Class<T> type) {
        return delegate.get(key, type);
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        delegate.put(key, value, ttl);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        return delegate.getOrLoad(key, type, loader, ttl);
    }

    @Override
    public void invalidate(String key) {
        delegate.invalidate(key);
    }

    @Override
    public void invalidatePattern(String pattern) {
        delegate.invalidatePattern(pattern);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public CacheStats getStats() {
        return delegate.getStats();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean containsKey(String key) {
        return delegate.containsKey(key);
    }

    @Override
    public void setDefaultTTL(Duration ttl) {
        delegate.setDefaultTTL(ttl);
    }

    @Override
    public void setMaxSize(int maxSize) {
        delegate.setMaxSize(maxSize);
    }
}