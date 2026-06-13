package cn.guangdian.dungeon.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDungeonData {

    private final UUID playerId;
    private final Map<String, ClearRecord> clearRecords;
    private final Map<String, Long> cooldowns;

    public PlayerDungeonData(UUID playerId) {
        this.playerId = playerId;
        this.clearRecords = new HashMap<>();
        this.cooldowns = new HashMap<>();
    }

    public UUID getPlayerId() { return playerId; }
    public Map<String, ClearRecord> getClearRecords() { return clearRecords; }
    public Map<String, Long> getCooldowns() { return cooldowns; }

    public boolean hasCleared(String key) {
        ClearRecord record = clearRecords.get(key);
        return record != null && record.clearCount > 0;
    }

    public void markCleared(String key, long time) {
        ClearRecord record = clearRecords.computeIfAbsent(key, k -> new ClearRecord());
        if (record.clearCount == 0) {
            record.firstClearTime = time;
        }
        record.clearCount++;
    }

    public int getClearCount(String key) {
        ClearRecord record = clearRecords.get(key);
        return record != null ? record.clearCount : 0;
    }

    public long getBestTime(String key) {
        ClearRecord record = clearRecords.get(key);
        return record != null ? record.bestTime : 0;
    }

    public void setBestTime(String key, long time) {
        ClearRecord record = clearRecords.computeIfAbsent(key, k -> new ClearRecord());
        if (record.bestTime == 0 || time < record.bestTime) {
            record.bestTime = time;
        }
    }

    public int getBestScore(String key) {
        ClearRecord record = clearRecords.get(key);
        return record != null ? record.bestScore : 0;
    }

    public void setBestScore(String key, int score) {
        ClearRecord record = clearRecords.computeIfAbsent(key, k -> new ClearRecord());
        if (score > record.bestScore) {
            record.bestScore = score;
        }
    }

    public void setClearRecord(String key, long firstClearTime, int clearCount, 
                               long bestTime, int bestScore) {
        ClearRecord record = clearRecords.computeIfAbsent(key, k -> new ClearRecord());
        record.firstClearTime = firstClearTime;
        record.clearCount = clearCount;
        record.bestTime = bestTime;
        record.bestScore = bestScore;
    }

    public boolean isOnCooldown(String dungeonId) {
        Long endTime = cooldowns.get(dungeonId);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public long getRemainingCooldown(String dungeonId) {
        Long endTime = cooldowns.get(dungeonId);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public void setCooldownEnd(String dungeonId, long endTime) {
        cooldowns.put(dungeonId, endTime);
    }

    public void setCooldown(String dungeonId, int seconds) {
        cooldowns.put(dungeonId, System.currentTimeMillis() + seconds * 1000L);
    }

    public void clearCooldown(String dungeonId) {
        cooldowns.remove(dungeonId);
    }

    public static class ClearRecord {
        public long firstClearTime;
        public int clearCount;
        public long bestTime;
        public int bestScore;

        public ClearRecord() {
            this.firstClearTime = 0;
            this.clearCount = 0;
            this.bestTime = 0;
            this.bestScore = 0;
        }
    }
}
