package cn.guangdian.armorstats.debug;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调试日志管理器
 * 
 * 功能:
 * - 记录所有属性解析操作的详细日志
 * - 追踪主线程/异步线程调用
 * - 按玩家过滤日志
 * - 输出到文件和控制台
 * 
 * 使用方法:
 * DebugLogManager.log(player, "PARSE_START", "开始解析装备", details);
 */
public class DebugLogManager {

    private static GuangDianArmorStats plugin;
    private static boolean enabled = false;
    private static boolean logToConsole = true;
    private static boolean logToFile = true;
    private static boolean logThreadInfo = true;
    private static boolean logStackTrace = false;
    
    // 日志过滤：只记录指定玩家的日志（空=记录所有）
    private static final Set<UUID> filterPlayers = new HashSet<>();
    
    // 日志队列（异步写入）
    private static final ConcurrentLinkedQueue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean flushing = new AtomicBoolean(false);
    
    // 日志文件
    private static File logFile;
    private static PrintWriter fileWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 统计
    private static long totalLogs = 0;
    private static long droppedLogs = 0;
    
    // 日志级别
    public enum Level {
        TRACE,      // 最详细，包含调用栈
        DEBUG,      // 调试信息
        INFO,       // 一般信息
        WARNING,    // 警告
        ERROR       // 错误
    }
    
    // 操作类型（方便搜索和过滤）
    public static class Operation {
        // 玩家事件
        public static final String PLAYER_JOIN = "PLAYER_JOIN";
        public static final String PLAYER_QUIT = "PLAYER_QUIT";
        public static final String PLAYER_RESPAWN = "PLAYER_RESPAWN";
        
        // 装备操作
        public static final String EQUIPMENT_CHANGE = "EQUIPMENT_CHANGE";
        public static final String ARMOR_EQUIP = "ARMOR_EQUIP";
        public static final String ARMOR_UNEQUIP = "ARMOR_UNEQUIP";
        public static final String WEAPON_SWITCH = "WEAPON_SWITCH";
        public static final String OFFHAND_CHANGE = "OFFHAND_CHANGE";
        
        // 属性解析
        public static final String PARSE_START = "PARSE_START";
        public static final String PARSE_LORE = "PARSE_LORE";
        public static final String PARSE_GEM = "PARSE_GEM";
        public static final String PARSE_COMPLETE = "PARSE_COMPLETE";
        public static final String PARSE_ERROR = "PARSE_ERROR";
        
        // 属性合并
        public static final String STATS_MERGE = "STATS_MERGE";
        public static final String STATS_RESET = "STATS_RESET";
        public static final String STATS_APPLY = "STATS_APPLY";
        
        // 缓存操作
        public static final String CACHE_HIT = "CACHE_HIT";
        public static final String CACHE_MISS = "CACHE_MISS";
        public static final String CACHE_INVALIDATE = "CACHE_INVALIDATE";
        
        // 伤害计算
        public static final String DAMAGE_CALC = "DAMAGE_CALC";
        public static final String DAMAGE_ATTACK = "DAMAGE_ATTACK";
        public static final String DAMAGE_DEFENSE = "DAMAGE_DEFENSE";
        public static final String DAMAGE_CRIT = "DAMAGE_CRIT";
        public static final String DAMAGE_FINAL = "DAMAGE_FINAL";
        
        // 异步操作
        public static final String ASYNC_START = "ASYNC_START";
        public static final String ASYNC_COMPLETE = "ASYNC_COMPLETE";
        public static final String ASYNC_ERROR = "ASYNC_ERROR";
        
        // 数据保存
        public static final String SAVE_START = "SAVE_START";
        public static final String SAVE_COMPLETE = "SAVE_COMPLETE";
        public static final String LOAD_START = "LOAD_START";
        public static final String LOAD_COMPLETE = "LOAD_COMPLETE";
        
        // 血量操作
        public static final String HEALTH_CHANGE = "HEALTH_CHANGE";
        public static final String HEALTH_SYNC = "HEALTH_SYNC";
        
        // GUI操作
        public static final String GUI_OPEN = "GUI_OPEN";
        public static final String GUI_CLICK = "GUI_CLICK";
        public static final String GEM_INLAY = "GEM_INLAY";
    }
    
    /**
     * 初始化日志管理器
     */
    public static void initialize(GuangDianArmorStats pluginInstance) {
        plugin = pluginInstance;
        
        // 从配置读取设置
        var config = plugin.getConfig();
        var debugSection = config.getConfigurationSection("debug");
        
        if (debugSection != null) {
            enabled = debugSection.getBoolean("enabled", false);
            logToConsole = debugSection.getBoolean("log_to_console", true);
            logToFile = debugSection.getBoolean("log_to_file", true);
            logThreadInfo = debugSection.getBoolean("log_thread_info", true);
            logStackTrace = debugSection.getBoolean("log_stack_trace", false);
            
            // 读取过滤玩家列表
            List<String> filterList = debugSection.getStringList("filter_players");
            for (String name : filterList) {
                // 稍后解析为UUID
            }
        }
        
        if (enabled && logToFile) {
            initLogFile();
        }
        
        if (enabled) {
            plugin.getLogger().info("调试日志已启用");
            plugin.getLogger().info("  控制台输出: " + logToConsole);
            plugin.getLogger().info("  文件输出: " + logToFile);
            plugin.getLogger().info("  线程信息: " + logThreadInfo);
        }
    }
    
    /**
     * 初始化日志文件
     */
    private static void initLogFile() {
        File logFolder = new File(plugin.getDataFolder(), "debug_logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        
        String fileName = "debug_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".log";
        logFile = new File(logFolder, fileName);
        
        try {
            fileWriter = new PrintWriter(new FileWriter(logFile, true), true);
            fileWriter.println("========== GuangDianArmorStats Debug Log ==========");
            fileWriter.println("Started: " + new Date());
            fileWriter.println("==================================================");
        } catch (IOException e) {
            plugin.getLogger().severe("无法创建调试日志文件: " + e.getMessage());
            logToFile = false;
        }
    }
    
    /**
     * 关闭日志管理器
     */
    public static void shutdown() {
        if (fileWriter != null) {
            flush();
            fileWriter.println("========== Log End ==========");
            fileWriter.close();
        }
        
        plugin.getLogger().info("调试日志统计: 总计=" + totalLogs + ", 丢弃=" + droppedLogs);
    }
    
    /**
     * 记录日志（带玩家）
     */
    public static void log(Player player, String operation, String message) {
        log(player, Level.DEBUG, operation, message, null);
    }
    
    /**
     * 记录日志（带玩家和详情）
     */
    public static void log(Player player, String operation, String message, Map<String, Object> details) {
        log(player, Level.DEBUG, operation, message, details);
    }
    
    /**
     * 记录日志（带级别，4参数）
     */
    public static void log(Player player, Level level, String operation, String message) {
        log(player, level, operation, message, null);
    }
    
    /**
     * 记录日志（完整参数）
     */
    public static void log(Player player, Level level, String operation, String message, Map<String, Object> details) {
        if (!enabled) return;
        
        // 过滤玩家
        if (!filterPlayers.isEmpty() && player != null) {
            if (!filterPlayers.contains(player.getUniqueId())) {
                return;
            }
        }
        
        // 构建日志条目
        LogEntry entry = new LogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.level = level;
        entry.operation = operation;
        entry.playerName = player != null ? player.getName() : "SYSTEM";
        entry.playerUuid = player != null ? player.getUniqueId().toString() : "N/A";
        entry.message = message;
        entry.details = details;
        entry.threadName = Thread.currentThread().getName();
        entry.isMainThread = Bukkit.isPrimaryThread();
        
        if (logStackTrace && level == Level.TRACE) {
            entry.stackTrace = Thread.currentThread().getStackTrace();
        }
        
        totalLogs++;
        
        // 输出到控制台
        if (logToConsole) {
            outputToConsole(entry);
        }
        
        // 加入队列等待写入文件
        if (logToFile) {
            logQueue.offer(entry);
            
            // 队列过大时触发刷新
            if (logQueue.size() > 1000) {
                flushAsync();
            }
        }
    }
    
    /**
     * 快捷方法：记录属性解析
     */
    public static void logParse(Player player, String itemDesc, Map<String, Object> attrs) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("item", itemDesc);
        details.put("attributes", attrs);
        details.put("thread", Thread.currentThread().getName());
        details.put("isMainThread", Bukkit.isPrimaryThread());
        log(player, Level.DEBUG, Operation.PARSE_COMPLETE, "属性解析完成", details);
    }
    
    /**
     * 快捷方法：记录缓存命中
     */
    public static void logCacheHit(Player player, String itemHash) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("hash", itemHash);
        log(player, Level.DEBUG, Operation.CACHE_HIT, "缓存命中", details);
    }
    
    /**
     * 快捷方法：记录缓存未命中
     */
    public static void logCacheMiss(Player player, String itemHash) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("hash", itemHash);
        log(player, Level.DEBUG, Operation.CACHE_MISS, "缓存未命中，需要解析", details);
    }
    
    /**
     * 快捷方法：记录装备变化
     */
    public static void logEquipmentChange(Player player, String slot, String fromItem, String toItem) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("slot", slot);
        details.put("from", fromItem);
        details.put("to", toItem);
        log(player, Level.INFO, Operation.EQUIPMENT_CHANGE, "装备变化", details);
    }
    
    /**
     * 快捷方法：记录伤害计算
     */
    public static void logDamage(Player attacker, Player target, double baseDamage, double finalDamage, Map<String, Object> details) {
        Map<String, Object> fullDetails = new LinkedHashMap<>();
        fullDetails.put("attacker", attacker.getName());
        fullDetails.put("target", target.getName());
        fullDetails.put("baseDamage", baseDamage);
        fullDetails.put("finalDamage", finalDamage);
        fullDetails.putAll(details);
        log(attacker, Level.DEBUG, Operation.DAMAGE_FINAL, "伤害计算完成", fullDetails);
    }
    
    /**
     * 快捷方法：记录线程警告
     */
    public static void warnThreadSafety(String operation, String message) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("thread", Thread.currentThread().getName());
        details.put("isMainThread", Bukkit.isPrimaryThread());
        details.put("warning", "潜在的线程安全问题");
        log(null, Level.WARNING, operation, message, details);
    }
    
    /**
     * 输出到控制台
     */
    private static void outputToConsole(LogEntry entry) {
        String threadInfo = logThreadInfo ? 
            "[" + (entry.isMainThread ? "MAIN" : "ASYNC") + ":" + entry.threadName + "] " : "";
        
        String playerInfo = entry.playerName.equals("SYSTEM") ? "" : "[" + entry.playerName + "] ";
        
        String detailStr = "";
        if (entry.details != null && !entry.details.isEmpty()) {
            detailStr = " " + entry.details.toString();
        }
        
        String logMessage = String.format("[%s] %s%s[%s] %s%s",
            dateFormat.format(new Date(entry.timestamp)),
            threadInfo,
            playerInfo,
            entry.operation,
            entry.message,
            detailStr
        );
        
        switch (entry.level) {
            case ERROR:
                plugin.getLogger().severe(logMessage);
                break;
            case WARNING:
                plugin.getLogger().warning(logMessage);
                break;
            default:
                plugin.getLogger().info(logMessage);
        }
    }
    
    /**
     * 异步刷新日志到文件
     */
    public static void flushAsync() {
        if (flushing.compareAndSet(false, true)) {
            // 使用统一调度器，自动使用 RPGCore AsyncExecutor 或降级
            cn.guangdian.rpgcore.integration.UnifiedScheduler.runAsync(plugin, () -> {
                flush();
                flushing.set(false);
            });
        }
    }
    
    /**
     * 刷新日志到文件
     */
    public static void flush() {
        if (fileWriter == null) return;
        
        LogEntry entry;
        int count = 0;
        while ((entry = logQueue.poll()) != null) {
            writeEntryToFile(entry);
            count++;
        }
        
        if (count > 0) {
            fileWriter.flush();
        }
    }
    
    /**
     * 写入日志条目到文件
     */
    private static void writeEntryToFile(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] ", dateFormat.format(new Date(entry.timestamp))));
        sb.append(String.format("[%s] ", entry.level));
        sb.append(String.format("[%s] ", entry.operation));
        sb.append(String.format("[Thread:%s|%s] ", entry.threadName, entry.isMainThread ? "MAIN" : "ASYNC"));
        sb.append(String.format("[Player:%s] ", entry.playerName));
        sb.append(entry.message);
        
        if (entry.details != null && !entry.details.isEmpty()) {
            sb.append(" | Details: ");
            sb.append(entry.details.toString());
        }
        
        if (entry.stackTrace != null) {
            sb.append("\n  StackTrace:");
            for (int i = 2; i < Math.min(entry.stackTrace.length, 10); i++) {
                StackTraceElement ste = entry.stackTrace[i];
                sb.append("\n    at ").append(ste.toString());
            }
        }
        
        fileWriter.println(sb.toString());
    }
    
    /**
     * 设置是否启用
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    /**
     * 添加玩家到过滤列表
     */
    public static void addFilterPlayer(UUID uuid) {
        filterPlayers.add(uuid);
    }
    
    /**
     * 移除玩家过滤
     */
    public static void removeFilterPlayer(UUID uuid) {
        filterPlayers.remove(uuid);
    }
    
    /**
     * 清空玩家过滤
     */
    public static void clearFilterPlayers() {
        filterPlayers.clear();
    }
    
    /**
     * 是否已启用
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取统计信息
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("enabled", enabled);
        stats.put("totalLogs", totalLogs);
        stats.put("droppedLogs", droppedLogs);
        stats.put("queueSize", logQueue.size());
        stats.put("filterPlayers", filterPlayers.size());
        return stats;
    }
    
    /**
     * 日志条目内部类
     */
    private static class LogEntry {
        long timestamp;
        Level level;
        String operation;
        String playerName;
        String playerUuid;
        String message;
        Map<String, Object> details;
        String threadName;
        boolean isMainThread;
        StackTraceElement[] stackTrace;
    }
}