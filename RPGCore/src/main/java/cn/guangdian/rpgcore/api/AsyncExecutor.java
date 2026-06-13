package cn.guangdian.rpgcore.api;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步执行器接口 - 统一异步操作
 * 
 * <p>AsyncExecutor 提供了统一的异步任务执行能力，支持玩家数据保存的自动合并。
 * 整合现有的 AsyncExecutorService 实现。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 执行异步任务
 * CompletableFuture<PlayerData> future = executor.execute(() -> loadFromDatabase(playerId));
 * 
 * // 提交玩家保存（自动合并重复请求）
 * executor.submitPlayerSave(playerId, () -> saveToDatabase(playerId, data));
 * 
 * // 服务器关闭时等待所有保存完成
 * executor.awaitTermination(30, TimeUnit.SECONDS);
 * executor.shutdown();
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface AsyncExecutor {

    // ==================== 基本异步操作 ====================

    /**
     * 执行异步任务
     * 
     * @param task 要执行的任务
     * @return 包含结果的 CompletableFuture
     * @param <T> 返回值类型
     */
    <T> CompletableFuture<T> execute(Callable<T> task);

    /**
     * 执行异步任务（无返回值）
     * 
     * @param task 要执行的任务
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> execute(Runnable task);

    // ==================== 玩家数据保存 ====================

    /**
     * 提交玩家数据保存任务
     * 
     * <p>对于同一玩家的重复保存请求会自动合并，避免重复IO操作。</p>
     * 
     * @param playerId 玩家UUID
     * @param saveTask 保存任务
     * @return 操作完成的 CompletableFuture
     */
    CompletableFuture<Void> submitPlayerSave(UUID playerId, Runnable saveTask);

    /**
     * 获取指定玩家的待保存任务
     * 
     * @param playerId 玩家UUID
     * @return 如果有待保存任务返回对应的 CompletableFuture，否则返回 null
     */
    CompletableFuture<Void> getPendingSave(UUID playerId);

    /**
     * 取消指定玩家的待保存任务
     * 
     * @param playerId 玩家UUID
     * @return 如果成功取消返回 true
     */
    boolean cancelPendingSave(UUID playerId);

    // ==================== 生命周期管理 ====================

    /**
     * 等待所有任务完成
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 如果所有任务在超时前完成返回 true
     */
    boolean awaitTermination(long timeout, TimeUnit unit);

    /**
     * 关闭执行器
     * 
     * <p>不再接受新任务，等待已提交任务完成。</p>
     */
    void shutdown();

    /**
     * 立即关闭执行器
     * 
     * <p>尝试取消所有正在执行的任务。</p>
     * @return 被取消的任务列表
     */
    java.util.List<Runnable> shutdownNow();

    /**
     * 检查执行器是否已关闭
     * 
     * @return 如果已关闭返回 true
     */
    boolean isShutdown();

    /**
     * 检查执行器是否已终止
     * 
     * @return 如果已终止返回 true
     */
    boolean isTerminated();

    // ==================== 状态信息 ====================

    /**
     * 获取待处理任务数
     * 
     * @return 待处理任务数
     */
    int getPendingTaskCount();

    /**
     * 获取活跃线程数
     * 
     * @return 活跃线程数
     */
    int getActiveThreadCount();

    /**
     * 获取线程池大小
     * 
     * @return 线程池大小
     */
    int getPoolSize();
}