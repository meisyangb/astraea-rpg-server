package cn.guangdian.dungeon.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ScoreReward {

    private final int minScore;
    private final List<ItemStack> items;

    public ScoreReward(int minScore, List<ItemStack> items) {
        this.minScore = minScore;
        this.items = items;
    }

    public int getMinScore() { return minScore; }
    public List<ItemStack> getItems() { return items; }
}
