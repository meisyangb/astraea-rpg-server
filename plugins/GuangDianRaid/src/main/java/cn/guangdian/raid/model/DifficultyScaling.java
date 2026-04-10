package cn.guangdian.raid.model;

import org.bukkit.configuration.ConfigurationSection;

public class DifficultyScaling {

    private double healthMultiplier;
    private double damageMultiplier;
    private int additionalMobs;
    private double intelBonusMultiplier;
    private double rewardMultiplier;

    public DifficultyScaling() {
        this.healthMultiplier = 1.0;
        this.damageMultiplier = 1.0;
        this.additionalMobs = 0;
        this.intelBonusMultiplier = 1.0;
        this.rewardMultiplier = 1.0;
    }

    public static DifficultyScaling fromConfig(ConfigurationSection section) {
        DifficultyScaling scaling = new DifficultyScaling();
        if (section == null) return scaling;

        scaling.healthMultiplier = section.getDouble("health_multiplier", 1.0);
        scaling.damageMultiplier = section.getDouble("damage_multiplier", 1.0);
        scaling.additionalMobs = section.getInt("additional_mobs", 0);
        scaling.intelBonusMultiplier = section.getDouble("intel_bonus_multiplier", 1.0);
        scaling.rewardMultiplier = section.getDouble("reward_multiplier", 1.0);

        return scaling;
    }

    public DifficultyScaling scaleForPlayers(int playerCount) {
        DifficultyScaling scaled = new DifficultyScaling();
        double playerScale = 1.0 + (playerCount - 1) * 0.15;
        scaled.healthMultiplier = this.healthMultiplier * playerScale;
        scaled.damageMultiplier = this.damageMultiplier * (1.0 + (playerCount - 1) * 0.1);
        scaled.additionalMobs = this.additionalMobs + (playerCount - 1);
        scaled.intelBonusMultiplier = this.intelBonusMultiplier;
        scaled.rewardMultiplier = this.rewardMultiplier * (1.0 + (playerCount - 1) * 0.2);
        return scaled;
    }

    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public int getAdditionalMobs() { return additionalMobs; }
    public double getIntelBonusMultiplier() { return intelBonusMultiplier; }
    public double getRewardMultiplier() { return rewardMultiplier; }
}
