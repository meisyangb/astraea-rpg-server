package cn.guangdian.rpgcore.audit;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AuditLog;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class AuditLogImpl implements AuditLog {

    private final RPGCore plugin;
    private final Queue<LogEntry> logs = new ConcurrentLinkedQueue<>();
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final File logFile;
    private static final int MAX_CACHED_LOGS = 1000;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public AuditLogImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "audit.log");
        plugin.getLogger().info("[AuditLog] Initialized, log file: " + logFile.getAbsolutePath());
    }

    @Override
    public void log(Player player, String action, String details) {
        log(player.getUniqueId(), player.getName(), action, details);
    }

    @Override
    public void log(UUID playerId, String playerName, String action, String details) {
        LogEntry entry = new LogEntry(
                idGenerator.incrementAndGet(),
                Instant.now(),
                playerId,
                playerName,
                action,
                details
        );

        logs.offer(entry);
        if (logs.size() > MAX_CACHED_LOGS) {
            logs.poll();
        }

        writeToFile(formatEntry(entry));
    }

    @Override
    public void log(String action, String details) {
        LogEntry entry = new LogEntry(
                idGenerator.incrementAndGet(),
                Instant.now(),
                null,
                "SYSTEM",
                action,
                details
        );

        logs.offer(entry);
        if (logs.size() > MAX_CACHED_LOGS) {
            logs.poll();
        }

        writeToFile(formatEntry(entry));
    }

    @Override
    public List<LogEntry> getLogs(int limit) {
        List<LogEntry> result = new ArrayList<>();
        int count = 0;
        for (LogEntry entry : logs) {
            if (count >= limit) break;
            result.add(entry);
            count++;
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public List<LogEntry> getLogs(String action, int limit) {
        List<LogEntry> result = new ArrayList<>();
        int count = 0;
        for (LogEntry entry : logs) {
            if (count >= limit) break;
            if (entry.action().equals(action)) {
                result.add(entry);
                count++;
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public List<LogEntry> getPlayerLogs(UUID playerId, int limit) {
        List<LogEntry> result = new ArrayList<>();
        int count = 0;
        for (LogEntry entry : logs) {
            if (count >= limit) break;
            if (entry.playerId() != null && entry.playerId().equals(playerId)) {
                result.add(entry);
                count++;
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public List<LogEntry> getLogsBetween(Instant start, Instant end) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : logs) {
            if (!entry.timestamp().isBefore(start) && !entry.timestamp().isAfter(end)) {
                result.add(entry);
            }
        }
        return result;
    }

    @Override
    public void clearOldLogs(Instant before) {
        logs.removeIf(entry -> entry.timestamp().isBefore(before));
        plugin.getLogger().info("[AuditLog] Cleared logs before " + FORMATTER.format(before));
    }

    @Override
    public int getLogCount() {
        return logs.size();
    }

    private String formatEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.id()).append("] ");
        sb.append(FORMATTER.format(entry.timestamp())).append(" | ");
        if (entry.playerId() != null) {
            sb.append(entry.playerName()).append(" (").append(entry.playerId()).append(") | ");
        } else {
            sb.append("SYSTEM | ");
        }
        sb.append("[").append(entry.action()).append("] ");
        sb.append(entry.details());
        sb.append("\n");
        return sb.toString();
    }

    private void writeToFile(String line) {
        try {
            Files.writeString(logFile.toPath(), line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("[AuditLog] Failed to write to file: " + e.getMessage());
        }
    }
}
