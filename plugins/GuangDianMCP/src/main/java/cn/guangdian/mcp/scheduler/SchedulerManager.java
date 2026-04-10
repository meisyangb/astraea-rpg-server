package cn.guangdian.mcp.scheduler;

import cn.guangdian.mcp.GuangDianMCP;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulerManager {
    
    private final GuangDianMCP plugin;
    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private int taskId = -1;
    
    public SchedulerManager(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        if (!enabled) return;
        
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 20L, 20L).getTaskId();
        plugin.getLogger().info("定时任务管理器已启动，当前任务数: " + tasks.size());
    }
    
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        tasks.clear();
        plugin.getLogger().info("定时任务管理器已停止");
    }
    
    public void reload() {
        stop();
        start();
    }
    
    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        
        for (ScheduledTask task : tasks.values()) {
            if (task.shouldExecute(now)) {
                executeTask(task);
                task.updateLastExecution(now);
            }
        }
    }
    
    private void executeTask(ScheduledTask task) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                switch (task.getType()) {
                    case "restart" -> executeRestart(task);
                    case "backup" -> executeBackup(task);
                    case "command" -> executeCommand(task);
                    case "clear_entities" -> executeClearEntities(task);
                    case "announcement" -> executeAnnouncement(task);
                    case "save_all" -> executeSaveAll(task);
                    case "clear_lag" -> executeClearLag(task);
                    default -> plugin.getLogger().warning("未知任务类型: " + task.getType());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("执行定时任务失败: " + task.getName() + " - " + e.getMessage());
            }
        });
    }
    
    private void executeRestart(ScheduledTask task) {
        String warningMessage = task.getWarningMessage();
        if (warningMessage != null) {
            Bukkit.broadcastMessage(warningMessage);
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.spigot().restart();
        }, task.getWarningSeconds() * 20L);
    }
    
    private void executeBackup(ScheduledTask task) {
        plugin.getLogger().info("开始执行定时备份...");
        
        File serverDir = plugin.getServer().getWorldContainer().getParentFile();
        File backupDir = new File(serverDir, "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        File thisBackup = new File(backupDir, "backup-" + timestamp);
        thisBackup.mkdirs();
        
        for (World world : Bukkit.getWorlds()) {
            world.save();
            File worldFolder = world.getWorldFolder();
            File worldBackup = new File(thisBackup, world.getName());
            try {
                copyFolder(worldFolder.toPath(), worldBackup.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("备份世界失败: " + world.getName() + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("定时备份完成: " + thisBackup.getName());
        
        if (task.getEventPusher() != null) {
            JsonObject data = new JsonObject();
            data.addProperty("type", "backup");
            data.addProperty("path", thisBackup.getAbsolutePath());
            data.addProperty("timestamp", System.currentTimeMillis());
            task.getEventPusher().pushCustomEvent("scheduled_task", data);
        }
    }
    
    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source)
            .filter(path -> !path.toFile().getName().equals("session.lock"))
            .forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path targetPath = target.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    // ignore
                }
            });
    }
    
    private void executeCommand(ScheduledTask task) {
        String command = task.getCommand();
        if (command != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("执行定时命令: " + command);
        }
    }
    
    private void executeClearEntities(ScheduledTask task) {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                    total++;
                }
            }
        }
        plugin.getLogger().info("定时清理实体: 清除 " + total + " 个实体");
    }
    
    private void executeAnnouncement(ScheduledTask task) {
        String message = task.getMessage();
        if (message != null) {
            Bukkit.broadcastMessage(message);
        }
    }
    
    private void executeSaveAll(ScheduledTask task) {
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        Bukkit.savePlayers();
        plugin.getLogger().info("定时保存完成");
    }
    
    private void executeClearLag(ScheduledTask task) {
        int items = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType().name().equals("DROPPED_ITEM")) {
                    entity.remove();
                    items++;
                }
            }
        }
        plugin.getLogger().info("定时清理掉落物: 清除 " + items + " 个物品");
        
        if (items > 0) {
            Bukkit.broadcastMessage("§a[系统] 已清理 " + items + " 个地面掉落物");
        }
    }
    
    public void addTask(ScheduledTask task) {
        tasks.put(task.getName(), task);
        plugin.getLogger().info("添加定时任务: " + task.getName() + " (" + task.getType() + ")");
    }
    
    public void removeTask(String name) {
        ScheduledTask removed = tasks.remove(name);
        if (removed != null) {
            plugin.getLogger().info("移除定时任务: " + name);
        }
    }
    
    public ScheduledTask getTask(String name) {
        return tasks.get(name);
    }
    
    public Collection<ScheduledTask> getAllTasks() {
        return tasks.values();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static class ScheduledTask {
        private final String name;
        private final String type;
        private String command;
        private String message;
        private String warningMessage;
        private int warningSeconds = 60;
        private cn.guangdian.mcp.server.EventPusher eventPusher;
        
        private int hour = -1;
        private int minute = 0;
        private long intervalSeconds = -1;
        private LocalDateTime lastExecution;
        private boolean enabled = true;
        
        public ScheduledTask(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        public boolean shouldExecute(LocalDateTime now) {
            if (!enabled) return false;
            
            if (intervalSeconds > 0) {
                if (lastExecution == null) {
                    return true;
                }
                long secondsSinceLast = ChronoUnit.SECONDS.between(lastExecution, now);
                return secondsSinceLast >= intervalSeconds;
            }
            
            if (hour >= 0) {
                return now.getHour() == hour && now.getMinute() == minute && now.getSecond() == 0;
            }
            
            return false;
        }
        
        public void updateLastExecution(LocalDateTime now) {
            this.lastExecution = now;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getCommand() { return command; }
        public String getMessage() { return message; }
        public String getWarningMessage() { return warningMessage; }
        public int getWarningSeconds() { return warningSeconds; }
        public cn.guangdian.mcp.server.EventPusher getEventPusher() { return eventPusher; }
        
        public ScheduledTask setCommand(String command) { this.command = command; return this; }
        public ScheduledTask setMessage(String message) { this.message = message; return this; }
        public ScheduledTask setWarningMessage(String msg) { this.warningMessage = msg; return this; }
        public ScheduledTask setWarningSeconds(int seconds) { this.warningSeconds = seconds; return this; }
        public ScheduledTask setEventPusher(cn.guangdian.mcp.server.EventPusher pusher) { this.eventPusher = pusher; return this; }
        
        public ScheduledTask setDailyTime(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
            return this;
        }
        
        public ScheduledTask setIntervalSeconds(long seconds) {
            this.intervalSeconds = seconds;
            return this;
        }
        
        public ScheduledTask setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("type", type);
            if (command != null) json.addProperty("command", command);
            if (message != null) json.addProperty("message", message);
            if (hour >= 0) {
                json.addProperty("hour", hour);
                json.addProperty("minute", minute);
            }
            if (intervalSeconds > 0) {
                json.addProperty("intervalSeconds", intervalSeconds);
            }
            if (lastExecution != null) {
                json.addProperty("lastExecution", lastExecution.toString());
            }
            return json;
        }
    }
}
