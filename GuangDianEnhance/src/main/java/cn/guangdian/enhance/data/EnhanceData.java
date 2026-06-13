package cn.guangdian.enhance.data;

public class EnhanceData {

    private int level;
    private int maxLevel;
    private long lastEnhanceTime;
    private int totalAttempts;
    private int successfulAttempts;
    private int failedAttempts;

    public EnhanceData() {
        this.level = 0;
        this.maxLevel = 15;
        this.lastEnhanceTime = 0;
        this.totalAttempts = 0;
        this.successfulAttempts = 0;
        this.failedAttempts = 0;
    }

    public EnhanceData(int level, int maxLevel) {
        this.level = level;
        this.maxLevel = maxLevel;
        this.lastEnhanceTime = System.currentTimeMillis();
        this.totalAttempts = 0;
        this.successfulAttempts = 0;
        this.failedAttempts = 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, maxLevel));
        this.lastEnhanceTime = System.currentTimeMillis();
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public long getLastEnhanceTime() {
        return lastEnhanceTime;
    }

    public void setLastEnhanceTime(long lastEnhanceTime) {
        this.lastEnhanceTime = lastEnhanceTime;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void incrementTotalAttempts() {
        this.totalAttempts++;
    }

    public int getSuccessfulAttempts() {
        return successfulAttempts;
    }

    public void incrementSuccessfulAttempts() {
        this.successfulAttempts++;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public double getSuccessRate() {
        if (totalAttempts == 0) return 0.0;
        return (double) successfulAttempts / totalAttempts * 100.0;
    }

    public boolean canEnhance() {
        return level < maxLevel;
    }

    public void incrementLevel() {
        if (level < maxLevel) {
            level++;
            lastEnhanceTime = System.currentTimeMillis();
        }
    }

    public void decrementLevel() {
        if (level > 0) {
            level--;
            lastEnhanceTime = System.currentTimeMillis();
        }
    }

    public void reset() {
        level = 0;
        totalAttempts = 0;
        successfulAttempts = 0;
        failedAttempts = 0;
        lastEnhanceTime = System.currentTimeMillis();
    }
}
