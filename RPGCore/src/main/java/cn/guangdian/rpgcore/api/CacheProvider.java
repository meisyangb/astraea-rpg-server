package cn.guangdian.rpgcore.api;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存提供者接口 - 统一缓存管理
 * 
 * <p>CacheProvider 提供了一个统一的缓存抽象层，支持TTL过期和LRU淘汰策略。
 * 整合现有的多种缓存实现（EquipmentCacheManager、LuckPermsCacheManager等）。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 存入缓存（30分钟过期）
 * cache.put("player:" + playerId + ":stats", playerStats, Duration.ofMinutes(30));
 * 
 * // 获取缓存
 * PlayerStats stats = cache.get("player:" + playerId + ":stats", PlayerStats.class);
 * 
 * // 带加载器的获取（缓存未命中时自动加载）
 * PlayerStats stats = cache.getOrLoad("player:" + playerId + ":stats", 
 *     PlayerStats.class, 
 *     () -> loadFromDatabase(playerId),
 *     Duration.ofMinutes(30));
 * 
 * // 失效缓存
 * cache.invalidate("player:" + playerId + ":stats");
 * cache.invalidatePattern("player:" + playerId + ":*");
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface CacheProvider {

    // ==================== 基本操作 ====================

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值，如果不存在返回 null
     * @param <T> 值类型
     */
    <T> T get(String key, Class<T> type);

    /**
     * 获取缓存值（Optional包装）
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 包含缓存值的 Optional
     * @param <T> 值类型
     */
    default <T> Optional<T> getOptional(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type));
    }

    /**
     * 存入缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @param <T> 值类型
     */
    <T> void put(String key, T value, Duration ttl);

    /**
     * 存入缓存（使用默认TTL）
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param <T> 值类型
     */
    default <T> void put(String key, T value) {
        put(key, value, Duration.ofMinutes(30));
    }

    /**
     * 获取或加载缓存
     * 
     * <p>如果缓存存在则返回，不存在则使用加载器加载并存入缓存。</p>
     * 
     * @param key 缓存键
     * @param type 值类型
     * @param loader 加载器
     * @param ttl 过期时间
     * @return 缓存值或加载的值
     * @param <T> 值类型
     */
    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl);

    // ==================== 失效操作 ====================

    /**
     * 使缓存失效
     * 
     * @param key 缓存键
     */
    void invalidate(String key);

    /**
     * 批量失效（模式匹配）
     * 
     * <p>支持通配符模式，如 "player:*:stats" 会匹配所有玩家的 stats 缓存。</p>
     * 
     * @param pattern 缓存键模式
     */
    void invalidatePattern(String pattern);

    /**
     * 清空所有缓存
     */
    void clear();

    // ==================== 统计信息 ====================

    /**
     * 获取缓存统计
     * 
     * @return 缓存统计信息
     */
    CacheStats getStats();

    /**
     * 获取缓存大小
     * 
     * @return 缓存中的条目数
     */
    int size();

    /**
     * 检查缓存是否包含指定键
     * 
     * @param key 缓存键
     * @return 如果包含返回 true
     */
    boolean containsKey(String key);

    // ==================== 配置操作 ====================

    /**
     * 设置默认TTL
     * 
     * @param ttl 默认过期时间
     */
    void setDefaultTTL(Duration ttl);

    /**
     * 设置最大缓存大小
     * 
     * @param maxSize 最大缓存条目数
     */
    void setMaxSize(int maxSize);
}