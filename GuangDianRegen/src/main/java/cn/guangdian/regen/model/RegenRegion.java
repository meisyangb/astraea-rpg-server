package cn.guangdian.regen.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 刷新区域模型
 */
public class RegenRegion {

    private final String name;
    private final RegenType type;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    // 方块配置: Material -> RegenBlock
    private final Map<Material, RegenBlock> blockConfigs = new HashMap<>();

    // 树木配置(仅林场使用)
    private final Map<Material, TreeConfig> treeConfigs = new HashMap<>();

    private boolean enabled = true;

    public RegenRegion(String name, RegenType type, String worldName,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.type = type;
        this.worldName = worldName;

        // 确保最小/最大值正确
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    /**
     * 检查位置是否在区域内
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * 检查坐标是否在区域内
     */
    public boolean contains(int x, int y, int z, String world) {
        if (!world.equals(worldName)) {
            return false;
        }

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * 获取区域体积
     */
    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /**
     * 添加方块配置
     */
    public void addBlockConfig(Material material, RegenBlock config) {
        blockConfigs.put(material, config);
    }

    /**
     * 获取方块配置
     */
    public RegenBlock getBlockConfig(Material material) {
        return blockConfigs.get(material);
    }

    /**
     * 检查是否配置了该方块
     */
    public boolean hasBlockConfig(Material material) {
        return blockConfigs.containsKey(material);
    }

    /**
     * 添加树木配置
     */
    public void addTreeConfig(Material material, TreeConfig config) {
        treeConfigs.put(material, config);
    }

    /**
     * 获取树木配置
     */
    public TreeConfig getTreeConfig(Material material) {
        return treeConfigs.get(material);
    }

    // Getters

    public String getName() {
        return name;
    }

    public RegenType getType() {
        return type;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Map<Material, RegenBlock> getBlockConfigs() {
        return blockConfigs;
    }

    public Map<Material, TreeConfig> getTreeConfigs() {
        return treeConfigs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return String.format("RegenRegion{name=%s, type=%s, world=%s, min=(%d,%d,%d), max=(%d,%d,%d), enabled=%s}",
                name, type, worldName, minX, minY, minZ, maxX, maxY, maxZ, enabled);
    }

    /**
     * 树木配置
     */
    public static class TreeConfig {
        private final int delay;
        private final int minHeight;
        private final int maxHeight;

        public TreeConfig(int delay, int minHeight, int maxHeight) {
            this.delay = delay;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        public int getDelay() {
            return delay;
        }

        public int getMinHeight() {
            return minHeight;
        }

        public int getMaxHeight() {
            return maxHeight;
        }
    }
}
