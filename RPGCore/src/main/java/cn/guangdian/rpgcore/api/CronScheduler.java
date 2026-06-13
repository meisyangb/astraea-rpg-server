package cn.guangdian.rpgcore.api;

import java.time.LocalDateTime;
import java.util.function.Supplier;

public interface CronScheduler {

    long schedule(String cronExpression, Runnable task);

    long schedule(String cronExpression, Runnable task, String taskName);

    long scheduleDaily(LocalDateTime time);

    long scheduleDaily(LocalDateTime time, String hour, String minute, String second);

    long scheduleWeekly(LocalDateTime dayAndTime, Runnable task);

    long scheduleMonthly(LocalDateTime dayAndTime, Runnable task);

    boolean cancelTask(long taskId);

    void cancelAll();

    int getActiveTaskCount();

    String getNextRunTime(String cronExpression);

    interface CronExpression {
        int getSecond();
        int getMinute();
        int getHour();
        int getDayOfMonth();
        int getMonth();
        int getDayOfWeek();

        boolean matches(LocalDateTime dateTime);
    }

    record ScheduledTaskInfo(
        long taskId,
        String name,
        String cronExpression,
        LocalDateTime nextRun,
        boolean active
    ) {}
}
