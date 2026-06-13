package cn.guangdian.armorstats.storage;

import cn.guangdian.armorstats.GuangDianArmorStats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 异步执行器服务
 * 提供异步I/O执行能力，避免阻塞主线程
 * 
 * 功能:
 * - 异步保存玩家数据
 * - 管理待保存队列
 * - 合并重复保存请求
 * - 服务器关闭时等待所有保存完成
 */
public class AsyncExecutorService {
    
    private final GuangDianArmorStats plugin;
    
    // 异步线程池
    private final ExecutorService executor;
    
    // 待保存队列: UUID -> CompletableFuture
    private final Map<UUID, CompletableFuture<Void>> pendingSaves;
    
    // 保存失败计数
    private final Map<UUID, Integer> saveFailures;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param threadPoolSize 线程池大小（默认2）
     */
    public AsyncExecutorService(GuangDianArmorStats plugin, int threadPoolSize) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(
            threadPoolSize,
            new ThreadFactory() {
                private int counter = 0;
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "ArmorStats-AsyncSave-" + counter++);
                    thread.setDaemon(true);
                    return thread;
                }
            }
        );
        this.pendingSaves = new ConcurrentHashMap<>();
        this.saveFailures = new ConcurrentHashMap<>();
    }
    
    /**
     * 异步保存玩家数据
     * 
     * 特性:
     * - 如果该玩家已有待保存任务，则合并请求
     * - 保存失败时记录错误日志并保留内存数据
     * - 保存完成后从队列移除
     * 
     * @param playerUuid 玩家UUID
     * @param saveTask 保存任务（Runnable）
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerUuid, Runnable saveTask) {
        // 检查是否已有待保存任务
        CompletableFuture<Void> existingFuture = pendingSaves.get(playerUuid);
        if (existingFuture != null && !existingFuture.isDone()) {
            // 合并请求: 等待现有任务完成后再执行新任务
            return existingFuture.thenRunAsync(saveTask, executor)
                .whenComplete((result, throwable) -> {
                    handleCompletion(playerUuid, throwable);
                });
        }
        
        // 创建新的异步任务
        CompletableFuture<Void> future = CompletableFuture.runAsync(saveTask, executor)
            .whenComplete((result, throwable) -> {
                handleCompletion(playerUuid, throwable);
            });
        
        // 添加到待保存队列
        pendingSaves.put(playerUuid, future);
        
        return future;
    }
    
    /**
     * 处理保存完成
     * 
     * @param playerUuid 玩家UUID
     * @param throwable 异常（如果有）
     */
    private void handleCompletion(UUID playerUuid, Throwable throwable) {
        if (throwable != null) {
            // 保存失败
            plugin.getLogger().severe("保存玩家数据失败: " + playerUuid + ", " + throwable.getMessage());
            
            // 记录失败次数
            int failCount = saveFailures.getOrDefault(playerUuid, 0) + 1;
            saveFailures.put(playerUuid, failCount);
            
            if (failCount >= 3) {
                plugin.getLogger().severe("玩家数据保存失败3次，请检查磁盘空间: " + playerUuid);
            }
        } else {
            // 保存成功，清除失败计数
            saveFailures.remove(playerUuid);
        }
        
        // 从待保存队列移除
        pendingSaves.remove(playerUuid);
    }
    
    /**
     * 等待所有保存完成
     * 用于服务器关闭时
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否所有保存都完成
     */
    public boolean awaitAllSaves(long timeout, TimeUnit unit) {
        if (pendingSaves.isEmpty()) {
            return true;
        }
        
        plugin.getLogger().info("等待 " + pendingSaves.size() + " 个玩家数据保存完成...");
        
        // 收集所有待保存任务
        CompletableFuture<?>[] futures = pendingSaves.values().toArray(new CompletableFuture[0]);
        
        try {
            // 等待所有任务完成
            CompletableFuture.allOf(futures).get(timeout, unit);
            plugin.getLogger().info("所有玩家数据保存完成");
            return true;
        } catch (TimeoutException e) {
            plugin.getLogger().warning("等待保存超时，仍有 " + pendingSaves.size() + " 个任务未完成");
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("等待保存时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 优雅关闭执行器
     * 
     * 步骤:
     * 1. 停止接受新任务
     * 2. 等待现有任务完成
     * 3. 强制关闭（如果超时）
     */
    public void shutdown() {
        plugin.getLogger().info("关闭异步执行器...");
        
        // 停止接受新任务
        executor.shutdown();
        
        try {
            // 等待现有任务完成（最多30秒）
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("异步任务未在30秒内完成，强制关闭");
                executor.shutdownNow();
                
                // 再等待5秒
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    plugin.getLogger().severe("无法关闭异步执行器");
                }
            }
        } catch (InterruptedException e) {
            plugin.getLogger().severe("关闭异步执行器时被中断");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        plugin.getLogger().info("异步执行器已关闭");
    }
    
    /**
     * 获取待保存队列大小
     * 
     * @return 待保存任务数量
     */
    public int getPendingSaveCount() {
        return pendingSaves.size();
    }
    
    /**
     * 获取保存失败次数
     * 
     * @param playerUuid 玩家UUID
     * @return 失败次数
     */
    public int getFailureCount(UUID playerUuid) {
        return saveFailures.getOrDefault(playerUuid, 0);
    }
    
    /**
     * 清理玩家数据
     * 玩家退出时调用
     * 
     * @param playerUuid 玩家UUID
     */
    public void cleanup(UUID playerUuid) {
        saveFailures.remove(playerUuid);
        // 注意: 不移除pendingSaves，让任务自然完成
    }
}
