package cn.guangdian.raid.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Intel {

    private final String id;
    private String name;
    private Material material;
    private int customModelData;
    private int value;
    private List<String> unlockAreas;
    private List<Location> fixedLocations;
    private boolean isRandomSpawn;
    private List<String> spawnRegions;

    private transient ItemStack itemStack;

    public Intel(String id) {
        this.id = id;
        this.material = Material.PAPER;
        this.value = 1;
        this.unlockAreas = new ArrayList<>();
        this.fixedLocations = new ArrayList<>();
        this.spawnRegions = new ArrayList<>();
        this.isRandomSpawn = false;
    }

    public static Intel fromConfig(String id, ConfigurationSection section) {
        Intel intel = new Intel(id);
        if (section == null) return intel;

        intel.name = section.getString("name", "§e情报物品");
        String materialStr = section.getString("item", "PAPER");
        try {
            intel.material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            intel.material = Material.PAPER;
        }
        intel.customModelData = section.getInt("custom_model_data", 0);
        intel.value = section.getInt("value", 1);
        intel.unlockAreas = section.getStringList("unlock_areas");

        List<Map<?, ?>> spawnLocations = section.getMapList("spawn_locations");
        for (Map<?, ?> locMap : spawnLocations) {
            if (locMap.containsKey("random") && Boolean.TRUE.equals(locMap.get("random"))) {
                intel.isRandomSpawn = true;
                Object regions = locMap.get("regions");
                if (regions instanceof List) {
                    for (Object region : (List<?>) regions) {
                        intel.spawnRegions.add(region.toString());
                    }
                }
            } else {
                double x = locMap.containsKey("x") ? ((Number) locMap.get("x")).doubleValue() : 0;
                double y = locMap.containsKey("y") ? ((Number) locMap.get("y")).doubleValue() : 0;
                double z = locMap.containsKey("z") ? ((Number) locMap.get("z")).doubleValue() : 0;
                intel.fixedLocations.add(new Location(null, x, y, z));
            }
        }

        return intel;
    }

    public ItemStack createItemStack() {
        if (itemStack == null) {
            itemStack = new ItemStack(material);
            var meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                if (customModelData > 0) {
                    meta.setCustomModelData(customModelData);
                }
                var lore = new java.util.ArrayList<String>();
                lore.add("§7情报价值: §e" + value);
                if (!unlockAreas.isEmpty()) {
                    lore.add("§7可解锁区域: §a" + String.join(", ", unlockAreas));
                }
                lore.add("");
                lore.add("§8右键收集情报");
                meta.setLore(lore);
                itemStack.setItemMeta(meta);
            }
        }
        return itemStack.clone();
    }

    public Location getRandomSpawnLocation(Random random) {
        if (!fixedLocations.isEmpty()) {
            return fixedLocations.get(random.nextInt(fixedLocations.size()));
        }
        return null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public int getValue() { return value; }
    public List<String> getUnlockAreas() { return unlockAreas; }
    public List<Location> getFixedLocations() { return fixedLocations; }
    public boolean isRandomSpawn() { return isRandomSpawn; }
    public List<String> getSpawnRegions() { return spawnRegions; }
}
