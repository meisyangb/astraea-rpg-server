package cn.guangdian.mobs.model;

import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 掉落表数据模型
 */
public class DropTable {

    private String id;                    // 掉落表ID
    private String displayName;           // 显示名称
    private List<DropItem> items;         // 掉落物品列表
    private double expMin;                // 最小经验
    private double expMax;                // 最大经验
    private double moneyMin;              // 最小金钱
    private double moneyMax;              // 最大金钱

    public DropTable(String id) {
        this.id = id;
        this.items = new ArrayList<>();
        this.expMin = 0;
        this.expMax = 0;
        this.moneyMin = 0;
        this.moneyMax = 0;
    }

    /**
     * 掉落物品项
     */
    public static class DropItem {
        private String itemId;            // 物品ID (Material 名)
        private int amountMin;            // 最小数量
        private int amountMax;            // 最大数量
        private double chance;            // 掉落几率 (0-1)

        public DropItem(String itemId, int amountMin, int amountMax, double chance) {
            this.itemId = itemId;
            this.amountMin = amountMin;
            this.amountMax = amountMax;
            this.chance = chance;
        }

        // Getters
        public String getItemId() { return itemId; }
        public int getAmountMin() { return amountMin; }
        public int getAmountMax() { return amountMax; }
        public double getChance() { return chance; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<DropItem> getItems() { return items; }
    public void setItems(List<DropItem> items) { this.items = items; }

    public double getExpMin() { return expMin; }
    public void setExpMin(double expMin) { this.expMin = expMin; }

    public double getExpMax() { return expMax; }
    public void setExpMax(double expMax) { this.expMax = expMax; }

    public double getMoneyMin() { return moneyMin; }
    public void setMoneyMin(double moneyMin) { this.moneyMin = moneyMin; }

    public double getMoneyMax() { return moneyMax; }
    public void setMoneyMax(double moneyMax) { this.moneyMax = moneyMax; }

    /**
     * 添加掉落物品
     */
    public void addItem(String itemId, int amountMin, int amountMax, double chance) {
        items.add(new DropItem(itemId, amountMin, amountMax, chance));
    }

    /**
     * 验证掉落表是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }

    @Override
    public String toString() {
        return "DropTable{" +
                "id='" + id + '\'' +
                ", items=" + items.size() +
                '}';
    }
}
