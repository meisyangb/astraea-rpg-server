package cn.guangdian.rpgcore.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 数据仓库接口 - 统一数据访问抽象
 * 
 * <p>DataRepository 提供了一个统一的数据访问层接口，支持异步操作。
 * 所有玩家相关的数据存储都应该通过此接口进行抽象。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 加载数据
 * CompletableFuture<PlayerData> future = repository.load(playerId);
 * future.thenAccept(data -> {
 *     // 处理数据
 * });
 * 
 * // 保存数据
 * repository.save(playerId, playerData).thenRun(() -> {
 *     getLogger().info("数据保存成功");
 * });
 * }</pre>
 * 
 * @param <T> 数据类型
 * @author GuangDian
 * @since 1.0.0
 */
public interface DataRepository<T> {

    // ==================== 单条操作 ====================

    /**
     * 加载数据
     * 
     * @param id 数据唯一标识（通常是玩家UUID）
     * @return 包含数据的 CompletableFuture
     */
    CompletableFuture<T> load(UUID id);

    /**
     * 保存数据
     * 
     * @param id 数据唯一标识
     * @param data 要保存的数据
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> save(UUID id, T data);

    /**
     * 删除数据
     * 
     * @param id 数据唯一标识
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> delete(UUID id);

    /**
     * 检查数据是否存在
     * 
     * @param id 数据唯一标识
     * @return 包含布尔结果的 CompletableFuture
     */
    CompletableFuture<Boolean> exists(UUID id);

    // ==================== 批量操作 ====================

    /**
     * 批量加载
     * 
     * @param ids 数据唯一标识集合
     * @return 包含数据映射的 CompletableFuture
     */
    CompletableFuture<Map<UUID, T>> loadAll(Collection<UUID> ids);

    /**
     * 加载所有数据
     * 
     * @return 包含所有数据的 CompletableFuture
     */
    CompletableFuture<Map<UUID, T>> loadAll();

    /**
     * 批量保存
     * 
     * @param data 数据映射
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> saveAll(Map<UUID, T> data);

    /**
     * 获取数据总数
     * 
     * @return 数据总数
     */
    int count();

    // ==================== 缓存操作 ====================

    /**
     * 从缓存获取（同步）
     * 
     * <p>仅从内存缓存中获取，不触发加载。</p>
     * 
     * @param id 数据唯一标识
     * @return 缓存中的数据，如果不存在返回 null
     */
    T getFromCache(UUID id);

    /**
     * 使缓存失效
     * 
     * @param id 数据唯一标识
     */
    void invalidate(UUID id);

    /**
     * 使所有缓存失效
     */
    void invalidateAll();

    /**
     * 预热缓存
     * 
     * <p>将指定ID的数据加载到缓存中。</p>
     * 
     * @param ids 数据唯一标识集合
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> warmup(Collection<UUID> ids);

    /**
     * 获取缓存大小
     * 
     * @return 缓存中的数据数量
     */
    int getCacheSize();

    /**
     * 关闭仓库，释放资源
     */
    void close();
}