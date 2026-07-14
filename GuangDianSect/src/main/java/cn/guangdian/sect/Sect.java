package cn.guangdian.sect;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * 门派数据类
 */
public class Sect {
    private final String id;
    private final String name;
    private final String type;
    private final String element;
    private final String description;
    private final String color;
    private final Material icon;
    private final PowerMode powerMode;
    
    public Sect(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.type = config.getString("type", "正道");
        this.element = config.getString("element", "剑");
        this.description = config.getString("description", "");
        this.color = config.getString("color", "<white>");
        
        String iconStr = config.getString("icon", "DIAMOND_SWORD");
        Material mat;
        try {
            mat = Material.valueOf(iconStr);
        } catch (IllegalArgumentException e) {
            mat = Material.DIAMOND_SWORD;
        }
        this.icon = mat;
        
        // 解析变强模式
        ConfigurationSection powerSection = config.getConfigurationSection("power_mode");
        if (powerSection != null) {
            this.powerMode = new PowerMode(powerSection);
        } else {
            this.powerMode = new PowerMode("none");
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getElement() { return element; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
    public Material getIcon() { return icon; }
    public PowerMode getPowerMode() { return powerMode; }
    
    /**
     * 获取变强方式描述
     */
    public String getPowerDescription() {
        return switch (powerMode.getType()) {
            case "enhance_weapon" -> "强化武器";
            case "marriage" -> "结婚羁绊";
            case "heal" -> "治疗救人";
            case "damage" -> "战斗伤害";
            case "kill" -> "击杀敌人";
            case "alchemy" -> "炼制丹药";
            default -> "修炼";
        };
    }
    
    /**
     * 变强模式数据类
     */
    public static class PowerMode {
        private final String type;
        private final Map<String, Object> params = new HashMap<>();
        
        public PowerMode(String type) {
            this.type = type;
        }
        
        public PowerMode(ConfigurationSection config) {
            this.type = config.getString("type", "none");
            for (String key : config.getKeys(false)) {
                if (!key.equals("type")) {
                    params.put(key, config.get(key));
                }
            }
        }
        
        public String getType() { return type; }
        
        public int getInt(String key, int defaultValue) {
            Object value = params.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        }
        
        public double getDouble(String key, double defaultValue) {
            Object value = params.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return defaultValue;
        }
        
        public String getString(String key, String defaultValue) {
            Object value = params.get(key);
            return value != null ? value.toString() : defaultValue;
        }
    }
}