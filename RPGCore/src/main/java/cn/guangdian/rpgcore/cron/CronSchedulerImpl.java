package cn.guangdian.rpgcore.cron;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.CronScheduler;
import it.sauronsoftware.cron4j.Scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CronSchedulerImpl implements CronScheduler {

    private final RPGCore plugin;
    private final Scheduler scheduler;
    private final Map<Long, TaskInfo> tasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(1);

    public CronSchedulerImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.scheduler = new Scheduler();
        this.scheduler.start();
    }

    @Override
    public long schedule(String cronExpression, Runnable task) {
        return schedule(cronExpression, task, "CronTask-" + taskIdGenerator.get());
    }

    @Override
    public long schedule(String cronExpression, Runnable task, String taskName) {
        long taskId = taskIdGenerator.incrementAndGet();

        try {
            String scheduleId = scheduler.schedule(cronExpression, () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().warning("[CronScheduler] Task " + taskName + " failed: " + e.getMessage());
                }
            });

            TaskInfo info = new TaskInfo(taskId, taskName, cronExpression, scheduleId);
            tasks.put(taskId, info);

            plugin.getLogger().info("[CronScheduler] Scheduled task: " + taskName + " with cron: " + cronExpression);
            return taskId;

        } catch (Exception e) {
            plugin.getLogger().warning("[CronScheduler] Failed to schedule task: " + taskName + " - " + e.getMessage());
            return -1;
        }
    }

    @Override
    public long scheduleDaily(LocalDateTime time) {
        return scheduleDaily(time, 
            String.valueOf(time.getHour()), 
            String.valueOf(time.getMinute()), 
            String.valueOf(time.getSecond()));
    }

    @Override
    public long scheduleDaily(LocalDateTime time, String hour, String minute, String second) {
        String cron = second + " " + minute + " " + hour + " * * *";
        return schedule(cron, () -> {}, "DailyTask-" + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @Override
    public long scheduleWeekly(LocalDateTime dayAndTime, Runnable task) {
        int dayOfWeek = convertDayOfWeek(dayAndTime.getDayOfWeek().getValue());
        String cron = String.format("%d %d %d * * %d",
                dayAndTime.getSecond(),
                dayAndTime.getMinute(),
                dayAndTime.getHour(),
                dayOfWeek);
        return schedule(cron, task, "WeeklyTask");
    }

    @Override
    public long scheduleMonthly(LocalDateTime dayAndTime, Runnable task) {
        String cron = String.format("%d %d %d %d * *",
                dayAndTime.getSecond(),
                dayAndTime.getMinute(),
                dayAndTime.getHour(),
                dayAndTime.getDayOfMonth());
        return schedule(cron, task, "MonthlyTask");
    }

    @Override
    public boolean cancelTask(long taskId) {
        TaskInfo info = tasks.remove(taskId);
        if (info != null) {
            try {
                scheduler.deschedule(info.scheduleId);
                plugin.getLogger().info("[CronScheduler] Cancelled task: " + info.name);
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("[CronScheduler] Failed to cancel task: " + info.name);
                return false;
            }
        }
        return false;
    }

    @Override
    public void cancelAll() {
        for (TaskInfo info : tasks.values()) {
            try {
                scheduler.deschedule(info.scheduleId);
            } catch (Exception e) {
                plugin.getLogger().warning("[CronScheduler] Failed to cancel task: " + info.name);
            }
        }
        tasks.clear();
        plugin.getLogger().info("[CronScheduler] Cancelled all tasks");
    }

    @Override
    public int getActiveTaskCount() {
        return tasks.size();
    }

    @Override
    public String getNextRunTime(String cronExpression) {
        return "N/A";
    }

    public void shutdown() {
        scheduler.stop();
    }

    private int convertDayOfWeek(int javaDayOfWeek) {
        return javaDayOfWeek == 7 ? 0 : javaDayOfWeek;
    }

    private static class TaskInfo {
        final long taskId;
        final String name;
        final String cronExpression;
        final String scheduleId;

        TaskInfo(long taskId, String name, String cronExpression, String scheduleId) {
            this.taskId = taskId;
            this.name = name;
            this.cronExpression = cronExpression;
            this.scheduleId = scheduleId;
        }
    }
}
