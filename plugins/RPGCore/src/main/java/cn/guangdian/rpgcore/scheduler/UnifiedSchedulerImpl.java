package cn.guangdian.rpgcore.scheduler;

import cn.guangdian.rpgcore.api.SyncScheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UnifiedSchedulerImpl implements SyncScheduler {

    private final JavaPlugin plugin;
    private final AsyncScheduler asyncScheduler;
    private final GlobalRegionScheduler globalRegionScheduler;
    private final ConcurrentHashMap<Long, ScheduledTask> asyncTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ScheduledTask> syncTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0);
    private volatile boolean shutdown = false;

    public UnifiedSchedulerImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        this.asyncScheduler = Bukkit.getAsyncScheduler();
        this.globalRegionScheduler = Bukkit.getGlobalRegionScheduler();
    }

    @Override
    public void runSync(Runnable task) {
        if (shutdown) return;
        globalRegionScheduler.run(plugin, scheduledTask -> task.run());
    }

    @Override
    public long runSyncLater(Runnable task, long delayTicks) {
        if (shutdown) return -1;
        long taskId = taskIdGenerator.incrementAndGet();
        ScheduledTask scheduledTask = globalRegionScheduler.runDelayed(plugin, scheduledTask1 -> {
            if (!shutdown) {
                task.run();
            }
        }, delayTicks);
        syncTasks.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public long runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (shutdown) return -1;
        long taskId = taskIdGenerator.incrementAndGet();
        ScheduledTask scheduledTask = globalRegionScheduler.runAtFixedRate(plugin, scheduledTask1 -> {
            if (!shutdown) {
                task.run();
            }
        }, delayTicks, periodTicks);
        syncTasks.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public long runAsync(Runnable task) {
        if (shutdown) return -1;
        long taskId = taskIdGenerator.incrementAndGet();
        ScheduledTask scheduledTask = asyncScheduler.runNow(plugin, scheduledTask1 -> {
            if (!shutdown) {
                task.run();
            }
        });
        asyncTasks.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public long runAsyncLater(Runnable task, long delayTicks) {
        if (shutdown) return -1;
        long taskId = taskIdGenerator.incrementAndGet();
        long delayMs = delayTicks * 50;
        ScheduledTask scheduledTask = asyncScheduler.runDelayed(plugin, scheduledTask1 -> {
            if (!shutdown) {
                task.run();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        asyncTasks.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public long runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (shutdown) return -1;
        long taskId = taskIdGenerator.incrementAndGet();
        long delayMs = delayTicks * 50;
        long periodMs = periodTicks * 50;
        ScheduledTask scheduledTask = asyncScheduler.runAtFixedRate(plugin, scheduledTask1 -> {
            if (!shutdown) {
                task.run();
            }
        }, delayMs, periodMs, TimeUnit.MILLISECONDS);
        asyncTasks.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public void cancelTask(long taskId) {
        ScheduledTask asyncTask = asyncTasks.remove(taskId);
        if (asyncTask != null) {
            asyncTask.cancel();
            return;
        }

        ScheduledTask syncTask = syncTasks.remove(taskId);
        if (syncTask != null) {
            syncTask.cancel();
        }
    }

    @Override
    public void cancelAllTasks() {
        for (ScheduledTask task : asyncTasks.values()) {
            task.cancel();
        }
        asyncTasks.clear();

        for (ScheduledTask task : syncTasks.values()) {
            task.cancel();
        }
        syncTasks.clear();
    }

    @Override
    public int getActiveTaskCount() {
        return asyncTasks.size() + syncTasks.size();
    }

    public void shutdown() {
        shutdown = true;
        cancelAllTasks();
    }
}