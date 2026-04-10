package cn.guangdian.rpgcore.integration;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 统一调度器助手
 * 
 * <p>为所有插件提供统一的任务调度接口，自动使用 RPGCore AsyncExecutor 或降级到 Bukkit Scheduler。</p>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 异步执行
 * UnifiedScheduler.runAsync(plugin, () -> {
 *     // 异步任务
 * });
 * 
 * // 定时异步任务
 * UnifiedScheduler.runAsyncTimer(plugin, () -> {
 *     // 定时清理任务
 * }, 20L, 1200L); // 延迟1秒，每分钟执行
 * 
 * // 带返回值的异步任务
 * CompletableFuture<String> future = UnifiedScheduler.supplyAsync(plugin, () -> {
 *     return "结果";
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public final class UnifiedScheduler {

    private UnifiedScheduler() {}

    /**
     * 获取 AsyncExecutor（如果可用）
     * 
     * @return AsyncExecutor 或 null
     */
    public static AsyncExecutor getAsyncExecutor() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                return RPGCore.getInstance().getAsyncExecutor();
            } catch (Exception e) {
                // RPGCore 不可用时返回 null
            }
        }
        return null;
    }

    /**
     * 异步执行任务
     * 
     * @param plugin 插件实例
     * @param task 任务
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        AsyncExecutor executor = getAsyncExecutor();
        if (executor != null) {
            executor.execute(task);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * 异步执行任务（带返回值）
     * 
     * @param plugin 插件实例
     * @param task 任务
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsync(Plugin plugin, java.util.concurrent.Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsync(plugin, () -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * 延迟异步执行任务
     * 
     * @param plugin 插件实例
     * @param task 任务
     * @param delay 延迟（ticks）
     */
    public static void runAsyncLater(Plugin plugin, Runnable task, long delay) {
        AsyncExecutor executor = getAsyncExecutor();
        if (executor != null) {
            // RPGCore AsyncExecutor 没有延迟方法，使用 Bukkit 调度器包装
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                executor.execute(task);
            }, delay);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }

    /**
     * 定时异步执行任务
     * 
     * @param plugin 插件实例
     * @param task 任务
     * @param delay 延迟（ticks）
     * @param period 周期（ticks）
     */
    public static void runAsyncTimer(Plugin plugin, Runnable task, long delay, long period) {
        AsyncExecutor executor = getAsyncExecutor();
        if (executor != null) {
            // 使用 RPGCore AsyncExecutor 执行
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                executor.execute(task);
            }, delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        }
    }

    /**
     * 同步执行任务（主线程）
     * 
     * @param plugin 插件实例
     * @param task 任务
     */
    public static void runSync(Plugin plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延迟同步执行任务
     * 
     * @param plugin 插件实例
     * @param task 任务
     * @param delay 延迟（ticks）
     */
    public static void runSyncLater(Plugin plugin, Runnable task, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * 定时同步执行任务
     * 
     * @param plugin 插件实例
     * @param task 任务
     * @param delay 延迟（ticks）
     * @param period 周期（ticks）
     */
    public static void runSyncTimer(Plugin plugin, Runnable task, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    /**
     * 异步保存玩家数据（合并保存）
     * 
     * <p>使用 RPGCore 的玩家数据保存合并机制，减少频繁 IO。</p>
     * 
     * @param plugin 插件实例
     * @param playerId 玩家 UUID
     * @param saveTask 保存任务
     */
    public static void savePlayerDataAsync(Plugin plugin, java.util.UUID playerId, Runnable saveTask) {
        AsyncExecutor executor = getAsyncExecutor();
        if (executor != null) {
            executor.submitPlayerSave(playerId, saveTask);
        } else {
            runAsync(plugin, saveTask);
        }
    }

    /**
     * 等待所有异步任务完成
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否成功等待完成
     */
    public static boolean awaitTermination(long timeout, TimeUnit unit) {
        AsyncExecutor executor = getAsyncExecutor();
        if (executor != null) {
            return executor.awaitTermination(timeout, unit);
        }
        return true; // 无 AsyncExecutor 时直接返回成功
    }

    /**
     * 检查是否使用 RPGCore AsyncExecutor
     * 
     * @return 是否使用 RPGCore
     */
    public static boolean isUsingRPGCore() {
        return getAsyncExecutor() != null;
    }
}