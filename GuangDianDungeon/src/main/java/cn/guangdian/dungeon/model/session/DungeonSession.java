package cn.guangdian.dungeon.model.session;

import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.stage.Stage;
import cn.guangdian.dungeon.model.stage.Wave;
import org.bukkit.World;

import java.util.*;

public class DungeonSession {
    private String sessionId;
    private String dungeonId;
    private World instanceWorld;
    private String instanceWorldName;
    private DungeonParty party;
    private List<Stage> stages;
    private Map<String, cn.guangdian.dungeon.model.stage.SpawnPoint> spawnPoints;
    private int currentStageIndex;
    private SessionState state;
    private long startTime;
    private long endTime;
    private int totalKills;
    private int totalDeaths;
    private Map<String, Integer> mobKillCounts;
    private Set<UUID> spawnedMobs;
    private String difficulty;
    private int timeLimit;
    
    public DungeonSession() {
        this.stages = new ArrayList<>();
        this.spawnPoints = new HashMap<>();
        this.currentStageIndex = 0;
        this.state = SessionState.WAITING;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.mobKillCounts = new HashMap<>();
        this.spawnedMobs = new HashSet<>();
    }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getDungeonId() { return dungeonId; }
    public void setDungeonId(String dungeonId) { this.dungeonId = dungeonId; }
    
    public World getInstanceWorld() { return instanceWorld; }
    public void setInstanceWorld(World instanceWorld) { this.instanceWorld = instanceWorld; }
    
    public String getInstanceWorldName() { return instanceWorldName; }
    public void setInstanceWorldName(String instanceWorldName) { this.instanceWorldName = instanceWorldName; }
    
    public DungeonParty getParty() { return party; }
    public void setParty(DungeonParty party) { this.party = party; }
    
    public List<Stage> getStages() { return stages; }
    public void setStages(List<Stage> stages) { this.stages = stages; }
    
    public Map<String, cn.guangdian.dungeon.model.stage.SpawnPoint> getSpawnPoints() { return spawnPoints; }
    public void setSpawnPoints(Map<String, cn.guangdian.dungeon.model.stage.SpawnPoint> spawnPoints) { this.spawnPoints = spawnPoints; }
    
    public int getCurrentStageIndex() { return currentStageIndex; }
    public void setCurrentStageIndex(int currentStageIndex) { this.currentStageIndex = currentStageIndex; }
    
    public SessionState getState() { return state; }
    public void setState(SessionState state) { this.state = state; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
    public void incrementTotalKills() { this.totalKills++; }
    
    public int getTotalDeaths() { return totalDeaths; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }
    public void incrementTotalDeaths() { this.totalDeaths++; }
    
    public Map<String, Integer> getMobKillCounts() { return mobKillCounts; }
    public void incrementMobKill(String mobId) {
        mobKillCounts.merge(mobId, 1, Integer::sum);
    }
    
    public Set<UUID> getSpawnedMobs() { return spawnedMobs; }
    public void addSpawnedMob(UUID uuid) { spawnedMobs.add(uuid); }
    public void removeSpawnedMob(UUID uuid) { spawnedMobs.remove(uuid); }
    public boolean isSessionMob(UUID uuid) { return spawnedMobs.contains(uuid); }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    
    public Stage getCurrentStage() {
        if (currentStageIndex < stages.size()) {
            return stages.get(currentStageIndex);
        }
        return null;
    }
    
    public Stage getNextStage() {
        if (currentStageIndex + 1 < stages.size()) {
            return stages.get(currentStageIndex + 1);
        }
        return null;
    }
    
    public boolean hasNextStage() {
        return currentStageIndex + 1 < stages.size();
    }
    
    public boolean advanceStage() {
        if (hasNextStage()) {
            Stage current = getCurrentStage();
            if (current != null) {
                current.setCompleted(true);
                current.setActive(false);
            }
            currentStageIndex++;
            Stage next = getCurrentStage();
            if (next != null) {
                next.setActive(true);
            }
            return true;
        }
        return false;
    }
    
    public Wave getCurrentWave() {
        Stage stage = getCurrentStage();
        return stage != null ? stage.getCurrentWave() : null;
    }
    
    public boolean isAllStagesCompleted() {
        for (Stage stage : stages) {
            if (!stage.isCompleted()) {
                return false;
            }
        }
        return true;
    }
    
    public long getElapsedTime() {
        if (startTime == 0) return 0;
        return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
    }
    
    public enum SessionState {
        WAITING,
        STARTING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        CLEANUP
    }
}
