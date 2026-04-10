package cn.guangdian.rpgcore.api;

/**
 * 缓存统计信息
 * 
 * <p>记录缓存的命中率、大小等统计信息。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class CacheStats {

    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long size;
    private final long maxSize;

    public CacheStats(long hitCount, long missCount, long evictionCount, long size, long maxSize) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.evictionCount = evictionCount;
        this.size = size;
        this.maxSize = maxSize;
    }

    /**
     * 获取缓存命中次数
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * 获取缓存未命中次数
     */
    public long getMissCount() {
        return missCount;
    }

    /**
     * 获取缓存驱逐次数
     */
    public long getEvictionCount() {
        return evictionCount;
    }

    /**
     * 获取当前缓存大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 获取最大缓存大小
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * 获取总请求次数
     */
    public long getRequestCount() {
        return hitCount + missCount;
    }

    /**
     * 获取缓存命中率
     * 
     * @return 命中率（0.0 - 1.0）
     */
    public double getHitRate() {
        long total = getRequestCount();
        return total == 0 ? 0.0 : (double) hitCount / total;
    }

    /**
     * 获取缓存未命中率
     * 
     * @return 未命中率（0.0 - 1.0）
     */
    public double getMissRate() {
        return 1.0 - getHitRate();
    }

    /**
     * 获取缓存使用率
     * 
     * @return 使用率（0.0 - 1.0）
     */
    public double getUsageRate() {
        return maxSize == 0 ? 0.0 : (double) size / maxSize;
    }

    @Override
    public String toString() {
        return String.format(
            "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, size=%d/%d, evictions=%d}",
            hitCount, missCount, getHitRate() * 100, size, maxSize, evictionCount
        );
    }

    /**
     * 创建空的统计信息
     */
    public static CacheStats empty() {
        return new CacheStats(0, 0, 0, 0, 0);
    }

    /**
     * 合并两个统计信息
     */
    public CacheStats merge(CacheStats other) {
        return new CacheStats(
            this.hitCount + other.hitCount,
            this.missCount + other.missCount,
            this.evictionCount + other.evictionCount,
            Math.max(this.size, other.size),
            Math.max(this.maxSize, other.maxSize)
        );
    }
}