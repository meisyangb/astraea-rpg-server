package cn.guangdian.rpgcore.api;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLog {

    void log(Player player, String action, String details);

    void log(UUID playerId, String playerName, String action, String details);

    void log(String action, String details);

    List<LogEntry> getLogs(int limit);

    List<LogEntry> getLogs(String action, int limit);

    List<LogEntry> getPlayerLogs(UUID playerId, int limit);

    List<LogEntry> getLogsBetween(Instant start, Instant end);

    void clearOldLogs(Instant before);

    int getLogCount();

    record LogEntry(
        long id,
        Instant timestamp,
        UUID playerId,
        String playerName,
        String action,
        String details
    ) {}
}
