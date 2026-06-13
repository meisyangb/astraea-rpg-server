package cn.guangdian.regen.manager;

import cn.guangdian.regen.GuangDianRegen;
import cn.guangdian.regen.model.RegenBlock;
import cn.guangdian.regen.model.RegenRegion;
import cn.guangdian.regen.model.RegenType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域管理器
 */
public class RegionManager {

    private final GuangDianRegen plugin;
    private final Map<String, RegenRegion> regions = new ConcurrentHashMap<>();
    private File regionsFile;

    public RegionManager(GuangDianRegen plugin) {
        this.plugin = plugin;
        loadRegions();
    }

    /**
     * 加载区域配置
     */
    public void loadRegions() {
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        if (!regionsFile.exists()) {
            plugin.saveResource("regions.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
        regions.clear();

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
                RegenRegion region = parseRegion(regionName, regionSection);
                regions.put(regionName, region);
            } catch (Exception e) {
                plugin.getLogger().warning("加载区域 " + regionName + " 失败: " + e.getMessage());
            }
        }

        plugin.getLogger().info("已加载 " + regions.size() + " 个区域");
    }

    /**
     * 解析区域配置
     */
    private RegenRegion parseRegion(String name, ConfigurationSection section) {
        String typeStr = section.getString("type", "MINE");
        RegenType type = RegenType.fromString(typeStr);

        String worldName = section.getString("world", "world");

        ConfigurationSection minSection = section.getConfigurationSection("min");
        int minX = minSection.getInt("x");
        int minY = minSection.getInt("y");
        int minZ = minSection.getInt("z");

        ConfigurationSection maxSection = section.getConfigurationSection("max");
        int maxX = maxSection.getInt("x");
        int maxY = maxSection.getInt("y");
        int maxZ = maxSection.getInt("z");

        RegenRegion region = new RegenRegion(name, type, worldName, minX, minY, minZ, maxX, maxY, maxZ);

        // 加载方块配置
        if (type == RegenType.MINE || type == RegenType.FARM) {
            ConfigurationSection blocksSection = section.getConfigurationSection("blocks");
            if (blocksSection != null) {
                for (String materialStr : blocksSection.getKeys(false)) {
                    ConfigurationSection blockSection = blocksSection.getConfigurationSection(materialStr);
                    if (blockSection == null) {
                        continue;
                    }

                    Material material = Material.valueOf(materialStr.toUpperCase());
                    Material replace = Material.valueOf(blockSection.getString("replace", "STONE").toUpperCase());
                    int delay = blockSection.getInt("delay", 300);

                    RegenBlock regenBlock = new RegenBlock(material, replace, delay);

                    // 加载掉落配置
                    List<String> dropsList = blockSection.getStringList("drops");
                    for (String dropStr : dropsList) {
                        RegenBlock.DropConfig drop = parseDropConfig(dropStr);
                        if (drop != null) {
                            regenBlock.addDrop(drop);
                        }
                    }

                    region.addBlockConfig(material, regenBlock);
                }
            }
        }

        // 加载树木配置
        if (type == RegenType.FOREST) {
            ConfigurationSection treesSection = section.getConfigurationSection("trees");
            if (treesSection != null) {
                for (String materialStr : treesSection.getKeys(false)) {
                    ConfigurationSection treeSection = treesSection.getConfigurationSection(materialStr);
                    if (treeSection == null) {
                        continue;
                    }

                    Material material = Material.valueOf(materialStr.toUpperCase());
                    int delay = treeSection.getInt("delay", 600);
                    int minHeight = treeSection.getInt("min_height", 5);
                    int maxHeight = treeSection.getInt("max_height", 8);

                    region.addTreeConfig(material, new RegenRegion.TreeConfig(delay, minHeight, maxHeight));
                }
            }
        }

        region.setEnabled(section.getBoolean("enabled", true));

        return region;
    }

    /**
     * 解析掉落配置
     * 格式: 物品:最小-最大|概率
     * 概率可选，默认为 100（必掉）
     * 示例: DIAMOND:1-3|50  （50%概率掉落1-3个钻石）
     *        EXPERIENCE:10:1-1  （必掉10经验）
     *        COMMAND:rpgitem give {player} 钻石:1|30  （30%概率执行指令1次）
     */
    private RegenBlock.DropConfig parseDropConfig(String str) {
        try {
            // 1. 解析概率 (格式: SPEC|CHANCE)
            double chance = 1.0;
            if (str.contains("|")) {
                String[] chanceParts = str.split("\\|");
                str = chanceParts[0];
                chance = Math.min(1.0, Math.max(0.0, Double.parseDouble(chanceParts[1]) / 100.0));
            }

            String[] parts = str.split(":");
            if (parts.length < 2) {
                return null;
            }

            String first = parts[0];
            String last = parts[parts.length - 1];

            RegenBlock.DropType type;
            String value;
            int minAmount = 1;
            int maxAmount = 1;

            if (first.equalsIgnoreCase("ITEM")) {
                // ITEM:DIAMOND:1-3 或 ITEM:DIAMOND:1-3
                type = RegenBlock.DropType.ITEM;
                value = parts[1];
                if (parts.length >= 3) {
                    String amountStr = parts[parts.length - 1];
                    if (amountStr.contains("-")) {
                        String[] range = amountStr.split("-");
                        minAmount = Integer.parseInt(range[0]);
                        maxAmount = Integer.parseInt(range[1]);
                    } else {
                        minAmount = maxAmount = Integer.parseInt(amountStr);
                    }
                }
            } else if (first.equalsIgnoreCase("EXPERIENCE")) {
                // EXPERIENCE:10:1-3
                type = RegenBlock.DropType.EXPERIENCE;
                value = parts[1];
                if (parts.length >= 3) {
                    String amountStr = parts[parts.length - 1];
                    if (amountStr.contains("-")) {
                        String[] range = amountStr.split("-");
                        minAmount = Integer.parseInt(range[0]);
                        maxAmount = Integer.parseInt(range[1]);
                    } else {
                        minAmount = maxAmount = Integer.parseInt(amountStr);
                    }
                }
            } else if (first.equalsIgnoreCase("COMMAND")) {
                // COMMAND:rpgitem give {player} 钻石:1
                type = RegenBlock.DropType.COMMAND;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length - 1; i++) {
                    if (sb.length() > 0) sb.append(":");
                    sb.append(parts[i]);
                }
                value = sb.toString();
                String amountStr = parts[parts.length - 1];
                if (amountStr.contains("-")) {
                    String[] range = amountStr.split("-");
                    minAmount = Integer.parseInt(range[0]);
                    maxAmount = Integer.parseInt(range[1]);
                } else {
                    minAmount = maxAmount = Integer.parseInt(amountStr);
                }
            } else {
                // 旧格式: DIAMOND:1-3 或 DIAMOND:1-3 （没有ITEM前缀）
                type = RegenBlock.DropType.ITEM;
                value = first;
                // 旧格式中第二个部分就是数量范围
                if (parts.length >= 2 && parts[1].contains("-")) {
                    String[] range = parts[1].split("-");
                    minAmount = Integer.parseInt(range[0]);
                    maxAmount = Integer.parseInt(range[1]);
                } else if (parts.length >= 2) {
                    minAmount = maxAmount = Integer.parseInt(parts[1]);
                }
            }

            return new RegenBlock.DropConfig(type, value, minAmount, maxAmount, chance);
        } catch (Exception e) {
            plugin.getLogger().warning("解析掉落配置失败: " + str + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存区域配置
     */
    public void saveRegions() {
        FileConfiguration config = new YamlConfiguration();

        ConfigurationSection regionsSection = config.createSection("regions");

        for (Map.Entry<String, RegenRegion> entry : regions.entrySet()) {
            String name = entry.getKey();
            RegenRegion region = entry.getValue();

            ConfigurationSection regionSection = regionsSection.createSection(name);
            regionSection.set("type", region.getType().name());
            regionSection.set("world", region.getWorldName());
            regionSection.set("enabled", region.isEnabled());

            ConfigurationSection minSection = regionSection.createSection("min");
            minSection.set("x", region.getMinX());
            minSection.set("y", region.getMinY());
            minSection.set("z", region.getMinZ());

            ConfigurationSection maxSection = regionSection.createSection("max");
            maxSection.set("x", region.getMaxX());
            maxSection.set("y", region.getMaxY());
            maxSection.set("z", region.getMaxZ());

            // 保存方块配置
            if (!region.getBlockConfigs().isEmpty()) {
                ConfigurationSection blocksSection = regionSection.createSection("blocks");
                for (Map.Entry<Material, RegenBlock> blockEntry : region.getBlockConfigs().entrySet()) {
                    Material material = blockEntry.getKey();
                    RegenBlock block = blockEntry.getValue();

                    ConfigurationSection blockSection = blocksSection.createSection(material.name());
                    blockSection.set("replace", block.getReplace().name());
                    blockSection.set("delay", block.getDelay());

                    List<String> dropsList = new ArrayList<>();
                    for (RegenBlock.DropConfig drop : block.getDrops()) {
                        String amountStr = drop.getMinAmount() + "-" + drop.getMaxAmount();
                        String dropStr;
                        if (drop.getType() == RegenBlock.DropType.ITEM) {
                            dropStr = "ITEM:" + drop.getValue() + ":" + amountStr;
                        } else if (drop.getType() == RegenBlock.DropType.EXPERIENCE) {
                            dropStr = "EXPERIENCE:" + drop.getValue() + ":" + amountStr;
                        } else {
                            dropStr = "COMMAND:" + drop.getValue() + ":" + amountStr;
                        }
                        // 如果概率不是100%，追加概率
                        if (drop.getChance() < 1.0) {
                            dropStr += "|" + (int) (drop.getChance() * 100);
                        }
                        dropsList.add(dropStr);
                    }
                    blockSection.set("drops", dropsList);
                }
            }
        }

        try {
            config.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存区域配置失败: " + e.getMessage());
        }
    }

    /**
     * 创建区域
     */
    public boolean createRegion(String name, RegenType type, String worldName,
                                int x1, int y1, int z1, int x2, int y2, int z2) {
        if (regions.containsKey(name)) {
            return false;
        }

        RegenRegion region = new RegenRegion(name, type, worldName, x1, y1, z1, x2, y2, z2);
        regions.put(name, region);
        saveRegions();

        return true;
    }

    /**
     * 删除区域
     */
    public boolean deleteRegion(String name) {
        RegenRegion removed = regions.remove(name);
        if (removed != null) {
            saveRegions();
            return true;
        }
        return false;
    }

    /**
     * 获取区域
     */
    public RegenRegion getRegion(String name) {
        return regions.get(name);
    }

    /**
     * 获取所有区域
     */
    public Map<String, RegenRegion> getRegions() {
        return regions;
    }

    /**
     * 根据位置获取区域
     */
    public RegenRegion getRegionAt(Location location) {
        for (RegenRegion region : regions.values()) {
            if (region.contains(location) && region.isEnabled()) {
                return region;
            }
        }
        return null;
    }

    /**
     * 获取指定世界的区域数量
     */
    public int getRegionCount(String worldName) {
        int count = 0;
        for (RegenRegion region : regions.values()) {
            if (region.getWorldName().equals(worldName)) {
                count++;
            }
        }
        return count;
    }
}
