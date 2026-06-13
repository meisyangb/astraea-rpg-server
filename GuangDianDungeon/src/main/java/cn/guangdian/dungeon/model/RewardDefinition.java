package cn.guangdian.dungeon.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RewardDefinition {

    private final int exp;
    private final List<ItemStack> items;

    public RewardDefinition(int exp, List<ItemStack> items) {
        this.exp = exp;
        this.items = items != null ? items : new ArrayList<>();
    }

    public int getExp() { return exp; }
    public List<ItemStack> getItems() { return items; }

    public boolean isEmpty() {
        return exp <= 0 && items.isEmpty();
    }
}
