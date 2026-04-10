package cn.guangdian.rpgcore.api;

import java.util.concurrent.TimeUnit;

public interface SyncScheduler {
    
    void runSync(Runnable task);
    
    long runSyncLater(Runnable task, long delayTicks);
    
    long runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    long runAsync(Runnable task);
    
    long runAsyncLater(Runnable task, long delayTicks);
    
    long runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    void cancelTask(long taskId);
    
    void cancelAllTasks();
    
    int getActiveTaskCount();
}
