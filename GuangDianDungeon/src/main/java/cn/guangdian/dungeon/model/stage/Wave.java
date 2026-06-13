package cn.guangdian.dungeon.model.stage;

import java.util.ArrayList;
import java.util.List;

public class Wave {
    private String id;
    private List<MobSpawn> spawns;
    private CompletionCondition completion;
    private WaveTrigger nextWaveTrigger;
    private int timeLimit;
    private int expReward;
    private String completionMessage;
    private String startMessage;
    private int killCount;
    private int spawnedMobCount;
    private boolean completed;
    private boolean active;
    private long startTime;
    
    public Wave() {
        this.spawns = new ArrayList<>();
        this.killCount = 0;
        this.spawnedMobCount = 0;
        this.completed = false;
        this.active = false;
        this.nextWaveTrigger = WaveTrigger.onKillComplete();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public List<MobSpawn> getSpawns() { return spawns; }
    public void setSpawns(List<MobSpawn> spawns) { this.spawns = spawns; }
    
    public CompletionCondition getCompletion() { return completion; }
    public void setCompletion(CompletionCondition completion) { this.completion = completion; }
    
    public WaveTrigger getNextWaveTrigger() { return nextWaveTrigger; }
    public void setNextWaveTrigger(WaveTrigger nextWaveTrigger) { this.nextWaveTrigger = nextWaveTrigger; }
    
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    
    public int getExpReward() { return expReward; }
    public void setExpReward(int expReward) { this.expReward = expReward; }
    
    public String getCompletionMessage() { return completionMessage; }
    public void setCompletionMessage(String completionMessage) { this.completionMessage = completionMessage; }
    
    public String getStartMessage() { return startMessage; }
    public void setStartMessage(String startMessage) { this.startMessage = startMessage; }
    
    public int getKillCount() { return killCount; }
    public void setKillCount(int killCount) { this.killCount = killCount; }
    public void incrementKillCount() { this.killCount++; }
    
    public int getSpawnedMobCount() { return spawnedMobCount; }
    public void setSpawnedMobCount(int spawnedMobCount) { this.spawnedMobCount = spawnedMobCount; }
    public void addSpawnedMobs(int count) { this.spawnedMobCount += count; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public void activate() {
        this.active = true;
        this.startTime = System.currentTimeMillis();
    }
    
    public boolean checkCompletion() {
        if (completion == null) return false;
        
        switch (completion.getType()) {
            case KILL_ALL:
                return killCount >= spawnedMobCount && spawnedMobCount > 0;
            case KILL_COUNT:
                return killCount >= completion.getTargetCount();
            case BOSS_KILL:
                return killCount >= spawnedMobCount && spawnedMobCount > 0;
            default:
                return false;
        }
    }
    
    public boolean shouldTriggerNextWave() {
        if (nextWaveTrigger == null) return checkCompletion();
        
        switch (nextWaveTrigger.getType()) {
            case ON_KILL_COMPLETE:
                return checkCompletion();
            case ON_TIME:
                if (startTime > 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    return elapsed >= nextWaveTrigger.getDelaySeconds();
                }
                return false;
            default:
                return checkCompletion();
        }
    }
}
