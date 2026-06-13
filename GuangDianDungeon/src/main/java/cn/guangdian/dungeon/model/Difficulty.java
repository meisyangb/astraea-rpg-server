package cn.guangdian.dungeon.model;

public class Difficulty {

    private final String id;
    private final String name;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double mobCountMultiplier;
    private final double rewardMultiplier;
    private final double expMultiplier;
    private final int timeLimitModifier;
    private final int maxDeathsModifier;

    public Difficulty(String id, String name, double healthMultiplier, double damageMultiplier,
                     double mobCountMultiplier, double rewardMultiplier, double expMultiplier,
                     int timeLimitModifier, int maxDeathsModifier) {
        this.id = id;
        this.name = name;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.mobCountMultiplier = mobCountMultiplier;
        this.rewardMultiplier = rewardMultiplier;
        this.expMultiplier = expMultiplier;
        this.timeLimitModifier = timeLimitModifier;
        this.maxDeathsModifier = maxDeathsModifier;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getMobCountMultiplier() { return mobCountMultiplier; }
    public double getRewardMultiplier() { return rewardMultiplier; }
    public double getExpMultiplier() { return expMultiplier; }
    public int getTimeLimitModifier() { return timeLimitModifier; }
    public int getMaxDeathsModifier() { return maxDeathsModifier; }

    public int adjustTimeLimit(int baseTime) {
        return Math.max(60, baseTime + timeLimitModifier);
    }

    public int adjustMaxDeaths(int baseDeaths) {
        return Math.max(0, baseDeaths + maxDeathsModifier);
    }
}
