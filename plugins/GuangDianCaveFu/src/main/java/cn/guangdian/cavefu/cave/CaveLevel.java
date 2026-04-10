package cn.guangdian.cavefu.cave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 洞府等级配置
 */
public class CaveLevel {
    private final int level;
    private final String name;
    private final int size;
    private final int height;
    private final List<String> upgradeCost;

    public CaveLevel(int level, String name, int size, int height, List<String> upgradeCost) {
        this.level = level;
        this.name = name;
        this.size = size;
        this.height = height;
        this.upgradeCost = upgradeCost != null ? upgradeCost : new ArrayList<>();
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getHeight() {
        return height;
    }

    public List<String> getUpgradeCost() {
        return upgradeCost;
    }

    public boolean isMaxLevel() {
        return upgradeCost.isEmpty();
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("size", size);
        map.put("height", height);
        map.put("upgrade-cost", upgradeCost);
        return map;
    }

    public static CaveLevel deserialize(int level, Map<String, Object> data) {
        String name = (String) data.getOrDefault("name", "等级" + level);
        int size = ((Number) data.getOrDefault("size", 4)).intValue();
        int height = ((Number) data.getOrDefault("height", 6)).intValue();

        List<String> upgradeCost = new ArrayList<>();
        Object costObj = data.get("upgrade-cost");
        if (costObj instanceof List) {
            for (Object item : (List<?>) costObj) {
                if (item instanceof String) {
                    upgradeCost.add((String) item);
                }
            }
        }

        return new CaveLevel(level, name, size, height, upgradeCost);
    }
}