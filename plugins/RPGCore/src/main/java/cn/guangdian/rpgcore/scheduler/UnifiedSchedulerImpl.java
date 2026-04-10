package cn.guangdian.rpgcore.scheduler;

import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UnifiedSchedulerImpl implements SyncScheduler {
    
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<Long, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0);
    private volatile boolean shutdown = false;
    
    public UnifiedSchedulerImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void runSync(Runnable task) {
        if (shutdown) return;
        Bukkit.getScheduler().runTask(plugin, task);
    }
    
    @Override
    public long runSyncLater(Runnable task, long delayTicks) {
        if (shutdown) return -1;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        long taskId = taskIdGenerator.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return taskId;
    }
    
    @Override
    public long runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (shutdown) return -1;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        long taskId = taskIdGenerator.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return taskId;
    }
    
    @Override
    public long runAsync(Runnable task) {
        if (shutdown) return -1;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        long taskId = taskIdGenerator.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return taskId;
    }
    
    @Override
    public long runAsyncLater(Runnable task, long delayTicks) {
        if (shutdown) return -1;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        long taskId = taskIdGenerator.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return taskId;
    }
    
    @Override
    public long runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (shutdown) return -1;
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        long taskId = taskIdGenerator.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return taskId;
    }
    
    @Override
    public void cancelTask(long taskId) {
        BukkitTask task = tasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }
    
    @Override
    public void cancelAllTasks() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }
    
    @Override
    public int getActiveTaskCount() {
        return tasks.size();
    }
    
    public void shutdown() {
        shutdown = true;
        cancelAllTasks();
    }
}
