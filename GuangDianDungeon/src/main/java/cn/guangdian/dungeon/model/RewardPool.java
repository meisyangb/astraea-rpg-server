package cn.guangdian.dungeon.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RewardPool {

    private final String id;
    private final List<RewardEntry> entries;
    private final double totalWeight;
    private final RewardRange exp;
    private final RewardRange money;

    public RewardPool(String id, List<RewardEntry> entries) {
        this(id, entries, null, null);
    }

    public RewardPool(String id, List<RewardEntry> entries, RewardRange exp, RewardRange money) {
        this.id = id;
        this.entries = entries != null ? entries : new ArrayList<>();
        this.totalWeight = this.entries.stream().mapToDouble(RewardEntry::getWeight).sum();
        this.exp = exp != null ? exp : new RewardRange(0, 0);
        this.money = money != null ? money : new RewardRange(0, 0);
    }

    public String getId() { return id; }
    public List<RewardEntry> getEntries() { return entries; }
    public double getTotalWeight() { return totalWeight; }
    public RewardRange getExp() { return exp; }
    public RewardRange getMoney() { return money; }

    public ItemStack selectRandom() {
        if (entries.isEmpty() || totalWeight <= 0) return null;

        double random = Math.random() * totalWeight;
        double current = 0;

        for (RewardEntry entry : entries) {
            current += entry.getWeight();
            if (random <= current) {
                return entry.toItemStack();
            }
        }

        return null;
    }

    /**
     * 奖励数值范围
     */
    public static class RewardRange {
        private final double min;
        private final double max;

        public RewardRange(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public double getMin() { return min; }
        public double getMax() { return max; }

        public int randomInt() {
            if (min >= max) return (int) min;
            return (int) (min + Math.random() * (max - min + 1));
        }

        public boolean hasValue() {
            return max > 0;
        }
    }
}
