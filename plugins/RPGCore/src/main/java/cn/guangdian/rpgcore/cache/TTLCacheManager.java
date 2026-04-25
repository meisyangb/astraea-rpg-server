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
 *   <li><b>caffeine</b> - 推荐模式，使用 Caffeine 库（业界最佳实践）</li>
 *   <li><b>lightweight</b> - 轻量模式，已废弃，适用于中小型服务器</li>
 *   <li><b>high_performance</b> - 高性能模式，已废弃，适用于大型服务器</li>
 * </ul>
 *
 * <h3>推荐配置：</h3>
 * <pre>{@code
 * // 推荐使用 Caffeine 模式
 * CacheProvider cache = new TTLCacheManager(2000, Duration.ofMinutes(30), true, Mode.CAFFEINE);
 * }</pre>
 *
 * <h3>选择建议：</h3>
 * <table border="1">
 *   <tr><th>场景</th><th>推荐模式</th></tr>
 *   <tr><td>所有场景</td><td>caffeine（推荐）</td></tr>
 *   <tr><td>向后兼容</td><td>lightweight 或 high_performance</td></tr>
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
         * Caffeine 模式 - 推荐
         * <ul>
         *   <li>业界成熟的缓存库</li>
         *   <li>高性能、低延迟</li>
         *   <li>完善的 TTL、LRU、统计功能</li>
         *   <li>线程安全</li>
         * </ul>
         */
        CAFFEINE,

        /**
         * 轻量模式 - 已废弃
         * @deprecated 使用 {@link #CAFFEINE} 替代
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        LIGHTWEIGHT,

        /**
         * 高性能模式 - 已废弃
         * @deprecated 使用 {@link #CAFFEINE} 替代
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        HIGH_PERFORMANCE
    }

    // 委托给实际的实现
    private final CacheProvider delegate;
    private final Mode mode;

    /**
     * 创建缓存管理器（默认 Caffeine 模式 - 推荐）
     */
    public TTLCacheManager(int maxSize, Duration defaultTTL, boolean recordStats) {
        this(maxSize, defaultTTL, recordStats, Mode.CAFFEINE);
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
        this(maxSize, defaultTTL, recordStats, mode, false, false, false, Duration.ZERO);
    }

    /**
     * 创建缓存管理器（完整配置）
     *
     * @param maxSize 最大缓存大小
     * @param defaultTTL 默认过期时间
     * @param recordStats 是否记录统计
     * @param mode 缓存模式
     * @param weakKeys 是否启用弱引用键
     * @param weakValues 是否启用弱引用值
     * @param softValues 是否启用软引用值
     * @param refreshInterval 刷新间隔
     */
    public TTLCacheManager(int maxSize, Duration defaultTTL, boolean recordStats, Mode mode,
                           boolean weakKeys, boolean weakValues, boolean softValues,
                           Duration refreshInterval) {
        this.mode = mode;

        switch (mode) {
            case CAFFEINE:
                this.delegate = new CaffeineCacheProvider(maxSize, defaultTTL, recordStats,
                    weakKeys, weakValues, softValues, refreshInterval);
                break;
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
     * 创建缓存管理器（默认配置，Caffeine 模式）
     */
    public TTLCacheManager() {
        this(1000, Duration.ofMinutes(30), true, Mode.CAFFEINE);
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