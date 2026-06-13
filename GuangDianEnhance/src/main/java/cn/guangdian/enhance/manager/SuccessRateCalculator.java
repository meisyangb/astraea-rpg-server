package cn.guangdian.enhance.manager;

import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.data.EnhanceResult;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class SuccessRateCalculator {

    private final EnhanceConfig config;

    public SuccessRateCalculator(EnhanceConfig config) {
        this.config = config;
    }

    public double calculate(int currentLevel, ItemStack item) {
        double baseRate = config.getBaseSuccessRate();
        double decayFactor = config.getDecayFactor();
        double minRate = config.getMinSuccessRate();
        
        double levelPenalty = currentLevel * decayFactor;
        
        double rate = baseRate - levelPenalty;
        
        rate = applyItemBonus(rate, item, currentLevel);
        
        rate = Math.max(minRate, Math.min(1.0, rate));
        
        return rate;
    }

    private double applyItemBonus(double baseRate, ItemStack item, int level) {
        return baseRate;
    }

    public boolean rollSuccess(double successRate) {
        return ThreadLocalRandom.current().nextDouble() < successRate;
    }

    public double getSuccessRateForLevel(int level) {
        return calculate(level, null);
    }

    public String formatRate(double rate) {
        return String.format("%.1f%%", rate * 100);
    }

    public int getExpectedAttempts(int currentLevel, int targetLevel) {
        if (currentLevel >= targetLevel) {
            return 0;
        }
        
        double totalAttempts = 0;
        for (int level = currentLevel; level < targetLevel; level++) {
            double successRate = calculate(level, null);
            if (successRate <= 0) {
                return Integer.MAX_VALUE;
            }
            totalAttempts += 1.0 / successRate;
        }
        
        return (int) Math.ceil(totalAttempts);
    }

    public double getAverageCost(int currentLevel, int targetLevel, double costPerAttempt) {
        int expectedAttempts = getExpectedAttempts(currentLevel, targetLevel);
        return expectedAttempts * costPerAttempt;
    }
}
