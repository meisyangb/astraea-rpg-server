package cn.guangdian.dungeon.model.stage;

import java.util.ArrayList;
import java.util.List;

public class Stage {
    private String id;
    private String name;
    private StageType type;
    private StageTrigger trigger;
    private List<Wave> waves;
    private List<StageAction> onComplete;
    private int currentWaveIndex;
    private boolean active;
    private boolean completed;
    
    public Stage() {
        this.waves = new ArrayList<>();
        this.onComplete = new ArrayList<>();
        this.currentWaveIndex = 0;
        this.active = false;
        this.completed = false;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public StageType getType() { return type; }
    public void setType(StageType type) { this.type = type; }
    
    public StageTrigger getTrigger() { return trigger; }
    public void setTrigger(StageTrigger trigger) { this.trigger = trigger; }
    
    public List<Wave> getWaves() { return waves; }
    public void setWaves(List<Wave> waves) { this.waves = waves; }
    
    public List<StageAction> getOnComplete() { return onComplete; }
    public void setOnComplete(List<StageAction> onComplete) { this.onComplete = onComplete; }
    
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public void setCurrentWaveIndex(int currentWaveIndex) { this.currentWaveIndex = currentWaveIndex; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public Wave getCurrentWave() {
        if (currentWaveIndex < waves.size()) {
            return waves.get(currentWaveIndex);
        }
        return null;
    }
    
    public Wave getNextWave() {
        if (currentWaveIndex + 1 < waves.size()) {
            return waves.get(currentWaveIndex + 1);
        }
        return null;
    }
    
    public boolean hasNextWave() {
        return currentWaveIndex + 1 < waves.size();
    }
    
    public boolean advanceWave() {
        if (hasNextWave()) {
            currentWaveIndex++;
            return true;
        }
        return false;
    }
    
    public boolean checkStageCompletion() {
        for (Wave wave : waves) {
            if (!wave.isCompleted()) {
                return false;
            }
        }
        return true;
    }
    
    public enum StageType {
        COMBAT,
        BOSS,
        PUZZLE,
        PARKOUR,
        WAIT
    }
}
