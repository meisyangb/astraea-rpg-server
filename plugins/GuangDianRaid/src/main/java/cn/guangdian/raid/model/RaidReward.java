package cn.guangdian.raid.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaidReward {

    private int basePoints;
    private int baseExp;
    private List<ItemReward> itemRewards;
    private int bonusPerIntel;
    private int bonusPerKill;
    private double timeBonusMultiplier;

    public RaidReward() {
        this.itemRewards = new ArrayList<>();
        this.timeBonusMultiplier = 1.5;
    }

    public static RaidReward fromConfig(ConfigurationSection section) {
        RaidReward reward = new RaidReward();
        if (section == null) return reward;

        reward.basePoints = section.getInt("base_points", 100);
        reward.baseExp = section.getInt("base_exp", 50);
        reward.bonusPerIntel = section.getInt("bonus_per_intel", 10);
        reward.bonusPerKill = section.getInt("bonus_per_kill", 5);
        reward.timeBonusMultiplier = section.getDouble("time_bonus_multiplier", 1.5);

        List<Map<?, ?>> itemsList = section.getMapList("items");
        for (Map<?, ?> itemMap : itemsList) {
            try {
                String type = itemMap.get("type").toString();
                double chance = itemMap.containsKey("chance") ? 
                    ((Number) itemMap.get("chance")).doubleValue() : 1.0;
                int amount = itemMap.containsKey("amount") ? 
                    ((Number) itemMap.get("amount")).intValue() : 1;
                reward.itemRewards.add(new ItemReward(type, chance, amount));
            } catch (Exception ignored) {}
        }

        return reward;
    }

    public RaidReward multiply(double multiplier) {
        RaidReward newReward = new RaidReward();
        newReward.basePoints = (int) (this.basePoints * multiplier);
        newReward.baseExp = (int) (this.baseExp * multiplier);
        newReward.bonusPerIntel = (int) (this.bonusPerIntel * multiplier);
        newReward.bonusPerKill = (int) (this.bonusPerKill * multiplier);
        newReward.timeBonusMultiplier = this.timeBonusMultiplier;
        newReward.itemRewards = new ArrayList<>(this.itemRewards);
        return newReward;
    }

    public int getBasePoints() { return basePoints; }
    public int getBaseExp() { return baseExp; }
    public List<ItemReward> getItemRewards() { return itemRewards; }
    public int getBonusPerIntel() { return bonusPerIntel; }
    public int getBonusPerKill() { return bonusPerKill; }
    public double getTimeBonusMultiplier() { return timeBonusMultiplier; }

    public static class ItemReward {
        private final String itemType;
        private final double chance;
        private final int amount;

        public ItemReward(String itemType, double chance, int amount) {
            this.itemType = itemType;
            this.chance = chance;
            this.amount = amount;
        }

        public String getItemType() { return itemType; }
        public double getChance() { return chance; }
        public int getAmount() { return amount; }
    }
}
