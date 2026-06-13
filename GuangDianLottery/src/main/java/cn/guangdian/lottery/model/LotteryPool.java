package cn.guangdian.lottery.model;

import org.bukkit.Material;
import java.util.List;

public class LotteryPool {
    
    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final int iconCustomModelData;
    private final int cooldownSeconds;
    private final String permission;
    private final String currencyType;
    private final int cost;
    private final List<Prize> prizes;
    private final double totalWeight;
    
    public LotteryPool(String id, String displayName, Material iconMaterial, int iconCustomModelData,
                       int cooldownSeconds, String permission, String currencyType, int cost,
                       List<Prize> prizes, double totalWeight) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.iconCustomModelData = iconCustomModelData;
        this.cooldownSeconds = cooldownSeconds;
        this.permission = permission;
        this.currencyType = currencyType;
        this.cost = cost;
        this.prizes = prizes;
        this.totalWeight = totalWeight;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIconMaterial() { return iconMaterial; }
    public int getIconCustomModelData() { return iconCustomModelData; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public String getPermission() { return permission; }
    public String getCurrencyType() { return currencyType; }
    public int getCost() { return cost; }
    public List<Prize> getPrizes() { return prizes; }
    public double getTotalWeight() { return totalWeight; }
}
