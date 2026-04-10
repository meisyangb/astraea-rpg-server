package cn.guangdian.battlepass.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Season {
    
    private int seasonId;
    private String seasonName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxLevel;
    private List<BattlePassLevel> levels;
    private boolean active;
    
    public Season() {
        this.levels = new ArrayList<>();
        this.active = false;
    }
    
    public Season(int seasonId, String seasonName, LocalDateTime startTime, LocalDateTime endTime, int maxLevel) {
        this.seasonId = seasonId;
        this.seasonName = seasonName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxLevel = maxLevel;
        this.levels = new ArrayList<>();
        this.active = false;
    }
    
    public boolean isActive() {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
    
    public long getRemainingDays() {
        if (!isActive()) return 0;
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, endTime);
        return duration.toDays();
    }
    
    public long getRemainingHours() {
        if (!isActive()) return 0;
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, endTime);
        return duration.toHours();
    }
    
    public int getSeasonId() {
        return seasonId;
    }
    
    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }
    
    public String getSeasonName() {
        return seasonName;
    }
    
    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
    
    public List<BattlePassLevel> getLevels() {
        return levels;
    }
    
    public void setLevels(List<BattlePassLevel> levels) {
        this.levels = levels;
    }
    
    public void addLevel(BattlePassLevel level) {
        this.levels.add(level);
    }
    
    public BattlePassLevel getLevel(int level) {
        if (level < 1 || level > levels.size()) return null;
        return levels.get(level - 1);
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setActiveNow(boolean active) {
        this.active = active;
    }
}
