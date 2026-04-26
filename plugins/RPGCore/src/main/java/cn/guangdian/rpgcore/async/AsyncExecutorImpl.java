package cn.guangdian.rpgcore.async;

import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 异步执行器实现
 * 
 * <p>基于线程池的异步执行器，支持玩家数据保存的自动合并。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class AsyncExecutorImpl implements AsyncExecutor {

    private final Logger logger;
    private final JavaPlugin plugin;
    private final ExecutorService executor;
    private final Map<UUID, CompletableFuture<Void>> pendingSaves;
    private final AtomicInteger pendingTaskCount;
    private final AtomicInteger threadNumber;
    private volatile boolean shutdown = false;

    /**
     * 创建异步执行器（基础配置）
     *
     * @param plugin 插件实例
     * @param threadPoolSize 线程池大小
     */
    public AsyncExecutorImpl(JavaPlugin plugin, int threadPoolSize) {
        this(plugin, threadPoolSize, 1000, 60, false, "RPGCore-Async-");
    }

    /**
     * 创建异步执行器（完整配置）
     *
     * @param plugin 插件实例
     * @param threadPoolSize 线程池大小
     * @param queueCapacity 队列容量（0表示无界队列）
     * @param keepAliveSeconds 线程保活时间（秒）
     * @param allowCoreThreadTimeout 是否允许核心线程超时
     * @param threadNamePrefix 线程名称前缀
     */
    public AsyncExecutorImpl(JavaPlugin plugin, int threadPoolSize, int queueCapacity,
                             long keepAliveSeconds, boolean allowCoreThreadTimeout,
                             String threadNamePrefix) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.threadNumber = new AtomicInteger(1);

        // 创建有界或无界队列
        BlockingQueue<Runnable> workQueue = queueCapacity > 0
            ? new LinkedBlockingQueue<>(queueCapacity)
            : new LinkedBlockingQueue<>();

        // 创建 ThreadPoolExecutor
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            workQueue,
            r -> {
                Thread thread = new Thread(r, threadNamePrefix + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((t, ex) -> {
                    logger.log(Level.WARNING, "Async thread " + t.getName() + " crashed", ex);
                });
                return thread;
            },
            (r, executor) -> {
                logger.warning("异步任务队列已满，拒绝执行任务 (队列容量: " + queueCapacity + ", 活跃线程: " + executor.getActiveCount() + ")");
                throw new RejectedExecutionException("异步任务队列已满，无法接受新任务");
            }
        );

        // 允许核心线程超时
        threadPool.allowCoreThreadTimeOut(allowCoreThreadTimeout);

        this.executor = threadPool;
        this.pendingSaves = new ConcurrentHashMap<>();
        this.pendingTaskCount = new AtomicInteger(0);

        logger.info("AsyncExecutor initialized with " + threadPoolSize + " threads, " +
            (queueCapacity > 0 ? "queue capacity: " + queueCapacity : "unbounded queue"));
    }

    @Override
    public <T> CompletableFuture<T> execute(Callable<T> task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Executor is shut down"));
        }

        pendingTaskCount.incrementAndGet();
        CompletableFuture<T> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Async task failed", e);
                future.completeExceptionally(e);
            } finally {
                pendingTaskCount.decrementAndGet();
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> execute(Runnable task) {
        return execute(() -> {
            task.run();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> submitPlayerSave(UUID playerId, Runnable saveTask) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Executor is shut down"));
        }

        if (playerId == null || saveTask == null) {
            return CompletableFuture.completedFuture(null);
        }

        // 检查是否有待处理的保存任务
        CompletableFuture<Void> existingFuture = pendingSaves.get(playerId);
        if (existingFuture != null && !existingFuture.isDone()) {
            // 合并请求：等待现有任务完成后再执行
            return existingFuture.thenRunAsync(saveTask, executor);
        }

        // 创建新的保存任务
        pendingTaskCount.incrementAndGet();
        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingSaves.put(playerId, future);

        executor.submit(() -> {
            try {
                saveTask.run();
                future.complete(null);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Player save task failed for " + playerId, e);
                future.completeExceptionally(e);
            } finally {
                pendingTaskCount.decrementAndGet();
                pendingSaves.remove(playerId);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> getPendingSave(UUID playerId) {
        return playerId == null ? null : pendingSaves.get(playerId);
    }

    @Override
    public boolean cancelPendingSave(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        CompletableFuture<Void> future = pendingSaves.get(playerId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(false);
            if (cancelled) {
                pendingSaves.remove(playerId);
                pendingTaskCount.decrementAndGet();
            }
            return cancelled;
        }
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        try {
            // 先等待所有待处理的玩家保存完成
            long startTime = System.currentTimeMillis();
            long timeoutMillis = unit.toMillis(timeout);

            // 使用 CompletableFuture 等待所有 pendingSaves 完成，避免忙等待
            if (!pendingSaves.isEmpty()) {
                CompletableFuture<Void> allSaves = CompletableFuture.allOf(
                    pendingSaves.values().toArray(new CompletableFuture[0])
                );
                try {
                    allSaves.get(timeoutMillis, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.warning("Some player saves did not complete within timeout: " + 
                        pendingSaves.size() + " pending");
                }
            }

            long remainingMillis = timeoutMillis - (System.currentTimeMillis() - startTime);
            if (remainingMillis <= 0) {
                return executor.isTerminated();
            }

            // 等待线程池终止
            return executor.awaitTermination(remainingMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        logger.info("AsyncExecutor shutdown initiated");
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> remaining = executor.shutdownNow();
        pendingSaves.clear();
        logger.info("AsyncExecutor shutdown now, " + remaining.size() + " tasks remaining");
        return new ArrayList<>(remaining);
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public int getPendingTaskCount() {
        return pendingTaskCount.get();
    }

    @Override
    public int getActiveThreadCount() {
        if (executor instanceof ThreadPoolExecutor tpe) {
            return tpe.getActiveCount();
        }
        return 0;
    }

    @Override
    public int getPoolSize() {
        if (executor instanceof ThreadPoolExecutor tpe) {
            return tpe.getPoolSize();
        }
        return 0;
    }
}