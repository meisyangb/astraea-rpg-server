package cn.guangdian.worldrules.manager;

import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 区域管理器
 */
public class RegionManager {

    private final GuangDianWorldRules plugin;
    private final Map<String, ProtectedRegion> regions = new HashMap<>();
    private File regionsFile;

    public RegionManager(GuangDianWorldRules plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
    }

    /**
     * 加载所有区域
     */
    public void loadRegions() {
        regions.clear();

        if (!regionsFile.exists()) {
            plugin.saveResource("regions.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");

        if (regionsSection == null) {
            return;
        }

        for (String regionName : regionsSection.getKeys(false)) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
            if (regionSection == null) {
                continue;
            }

            try {
                ProtectedRegion region = loadRegion(regionName, regionSection);
                regions.put(regionName.toLowerCase(), region);
            } catch (Exception e) {
                plugin.getLogger().warning("加载区域 " + regionName + " 失败: " + e.getMessage());
            }
        }

        plugin.getLogger().info("已加载 " + regions.size() + " 个保护区域");
    }

    /**
     * 从配置加载单个区域
     */
    private ProtectedRegion loadRegion(String name, ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) {
            throw new IllegalArgumentException("区域缺少 world 配置");
        }

        ConfigurationSection minSection = section.getConfigurationSection("min");
        ConfigurationSection maxSection = section.getConfigurationSection("max");

        if (minSection == null || maxSection == null) {
            throw new IllegalArgumentException("区域缺少 min/max 坐标配置");
        }

        int minX = minSection.getInt("x");
        int minY = minSection.getInt("y");
        int minZ = minSection.getInt("z");
        int maxX = maxSection.getInt("x");
        int maxY = maxSection.getInt("y");
        int maxZ = maxSection.getInt("z");

        ProtectedRegion region = new ProtectedRegion(name, worldName, minX, minY, minZ, maxX, maxY, maxZ);

        // 加载规则覆盖
        if (section.contains("allow-break")) {
            region.setAllowBreak(section.getBoolean("allow-break"));
        }
        if (section.contains("allow-place")) {
            region.setAllowPlace(section.getBoolean("allow-place"));
        }
        if (section.contains("allow-interact")) {
            region.setAllowInteract(section.getBoolean("allow-interact"));
        }
        if (section.contains("allow-pvp")) {
            region.setAllowPVP(section.getBoolean("allow-pvp"));
        }
        if (section.contains("allow-item-drop")) {
            region.setAllowItemDrop(section.getBoolean("allow-item-drop"));
        }
        if (section.contains("allow-item-pickup")) {
            region.setAllowItemPickup(section.getBoolean("allow-item-pickup"));
        }
        
        // 加载刷怪控制规则
        if (section.contains("disable-natural-spawn")) {
            region.setDisableNaturalSpawn(section.getBoolean("disable-natural-spawn"));
        }
        if (section.contains("disable-monster-spawn")) {
            region.setDisableMonsterSpawn(section.getBoolean("disable-monster-spawn"));
        }
        if (section.contains("disable-animal-spawn")) {
            region.setDisableAnimalSpawn(section.getBoolean("disable-animal-spawn"));
        }
        
        // 加载死亡规则
        if (section.contains("keep-inventory")) {
            region.setKeepInventory(section.getBoolean("keep-inventory"));
        }
        if (section.contains("keep-exp")) {
            region.setKeepExp(section.getBoolean("keep-exp"));
        }

        // 加载提示信息
        if (section.contains("enter-title")) {
            region.setEnterTitle(section.getString("enter-title"));
        }
        if (section.contains("enter-subtitle")) {
            region.setEnterSubtitle(section.getString("enter-subtitle"));
        }
        if (section.contains("leave-title")) {
            region.setLeaveTitle(section.getString("leave-title"));
        }
        if (section.contains("leave-subtitle")) {
            region.setLeaveSubtitle(section.getString("leave-subtitle"));
        }

        return region;
    }

    /**
     * 保存所有区域
     */
    public void saveRegions() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
            String regionName = entry.getKey();
            ProtectedRegion region = entry.getValue();

            String path = "regions." + regionName;
            config.set(path + ".world", region.getWorldName());
            config.set(path + ".min.x", region.getMinX());
            config.set(path + ".min.y", region.getMinY());
            config.set(path + ".min.z", region.getMinZ());
            config.set(path + ".max.x", region.getMaxX());
            config.set(path + ".max.y", region.getMaxY());
            config.set(path + ".max.z", region.getMaxZ());

            // 保存规则覆盖
            if (region.getAllowBreak() != null) {
                config.set(path + ".allow-break", region.getAllowBreak());
            }
            if (region.getAllowPlace() != null) {
                config.set(path + ".allow-place", region.getAllowPlace());
            }
            if (region.getAllowInteract() != null) {
                config.set(path + ".allow-interact", region.getAllowInteract());
            }
            if (region.getAllowPVP() != null) {
                config.set(path + ".allow-pvp", region.getAllowPVP());
            }
            if (region.getAllowItemDrop() != null) {
                config.set(path + ".allow-item-drop", region.getAllowItemDrop());
            }
            if (region.getAllowItemPickup() != null) {
                config.set(path + ".allow-item-pickup", region.getAllowItemPickup());
            }

            // 保存提示信息
            if (region.getEnterTitle() != null) {
                config.set(path + ".enter-title", region.getEnterTitle());
            }
            if (region.getEnterSubtitle() != null) {
                config.set(path + ".enter-subtitle", region.getEnterSubtitle());
            }
            if (region.getLeaveTitle() != null) {
                config.set(path + ".leave-title", region.getLeaveTitle());
            }
            if (region.getLeaveSubtitle() != null) {
                config.set(path + ".leave-subtitle", region.getLeaveSubtitle());
            }
        }

        try {
            config.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存区域配置失败: " + e.getMessage());
        }
    }

    /**
     * 添加区域
     */
    public void addRegion(ProtectedRegion region) {
        regions.put(region.getName().toLowerCase(), region);
        saveRegions();
    }

    /**
     * 删除区域
     */
    public void removeRegion(String name) {
        if (regions.remove(name.toLowerCase()) != null) {
            saveRegions();
        }
    }

    /**
     * 获取区域
     */
    public ProtectedRegion getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    /**
     * 获取所有区域
     */
    public Collection<ProtectedRegion> getAllRegions() {
        return regions.values();
    }

    /**
     * 获取位置所在的所有区域
     */
    public List<ProtectedRegion> getRegionsAt(Location location) {
        List<ProtectedRegion> result = new ArrayList<>();
        for (ProtectedRegion region : regions.values()) {
            if (region.contains(location)) {
                result.add(region);
            }
        }
        return result;
    }

    /**
     * 获取位置所在的所有区域（按坐标）
     */
    public List<ProtectedRegion> getRegionsAt(int x, int y, int z, String world) {
        List<ProtectedRegion> result = new ArrayList<>();
        for (ProtectedRegion region : regions.values()) {
            if (region.contains(x, y, z, world)) {
                result.add(region);
            }
        }
        return result;
    }

    /**
     * 检查位置是否在任何区域内
     */
    public boolean isInAnyRegion(Location location) {
        for (ProtectedRegion region : regions.values()) {
            if (region.contains(location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取区域数量
     */
    public int getRegionCount() {
        return regions.size();
    }
}
