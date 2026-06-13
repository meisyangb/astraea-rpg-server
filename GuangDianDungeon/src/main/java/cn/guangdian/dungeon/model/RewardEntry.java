package cn.guangdian.dungeon.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RewardEntry {

    private final String itemId;
    private final int amount;
    private final double weight;
    private final double chance;

    /** 完整构造 */
    public RewardEntry(String itemId, int amount, double weight, double chance) {
        this.itemId = itemId;
        this.amount = amount;
        this.weight = weight;
        this.chance = chance;
    }

    /** 简写构造：weight = chance */
    public RewardEntry(String itemId, int amount, double chance) {
        this(itemId, amount, chance, chance);
    }

    public String getItemId() { return itemId; }
    public int getAmount() { return amount; }
    public double getWeight() { return weight; }
    public double getChance() { return chance; }

    public ItemStack toItemStack() {
        try {
            Material mat = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(mat, Math.max(1, amount));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
