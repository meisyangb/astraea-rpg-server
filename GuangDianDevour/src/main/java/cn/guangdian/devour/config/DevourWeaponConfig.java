package cn.guangdian.devour.config;

import org.bukkit.Material;

/**
 * 吞噬剑配置
 */
public class DevourWeaponConfig {
    private final String name;
    private final int maxSlots;
    private final String mythicType;
    private final Material material;
    
    public DevourWeaponConfig(String name, int maxSlots, String mythicType, Material material) {
        this.name = name;
        this.maxSlots = maxSlots;
        this.mythicType = mythicType;
        this.material = material;
    }
    
    public String getName() { return name; }
    public int getMaxSlots() { return maxSlots; }
    public String getMythicType() { return mythicType; }
    public Material getMaterial() { return material; }
}
