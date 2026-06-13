package cn.guangdian.enhance.stone;

import org.bukkit.inventory.ItemStack;

public class EnhanceStone {

    private final String id;
    private final StoneType type;
    private final double value;
    private final int tier;
    private final boolean consumable;
    private final ItemStack displayItem;
    
    public EnhanceStone(String id, StoneType type, double value, int tier, boolean consumable, ItemStack displayItem) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.tier = tier;
        this.consumable = consumable;
        this.displayItem = displayItem;
    }
    
    public String getId() {
        return id;
    }
    
    public StoneType getType() {
        return type;
    }
    
    public double getValue() {
        return value;
    }
    
    public int getTier() {
        return tier;
    }
    
    public boolean isConsumable() {
        return consumable;
    }
    
    public ItemStack getDisplayItem() {
        return displayItem != null ? displayItem.clone() : null;
    }
    
    public double applyEffect(double baseValue) {
        return switch (type.getEffectType()) {
            case SUCCESS_RATE_BONUS -> Math.min(1.0, baseValue + value);
            case GUARANTEE_SUCCESS -> 1.0;
            case LUCK_BONUS -> baseValue + value;
            default -> baseValue;
        };
    }
    
    public boolean preventsDegrade() {
        return type.getEffectType() == StoneEffectType.PREVENT_DEGRADE ||
               type.getEffectType() == StoneEffectType.PREVENT_DESTROY;
    }
    
    public boolean preventsDestroy() {
        return type.getEffectType() == StoneEffectType.PREVENT_DESTROY;
    }
    
    public boolean guaranteesSuccess() {
        return type.getEffectType() == StoneEffectType.GUARANTEE_SUCCESS;
    }
    
    @Override
    public String toString() {
        return String.format("EnhanceStone{id=%s, type=%s, value=%.2f, tier=%d}", 
            id, type, value, tier);
    }
}
