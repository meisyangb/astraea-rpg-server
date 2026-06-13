package cn.guangdian.worldrules.model;

import org.bukkit.Location;

/**
 * 保护区域模型
 */
public class ProtectedRegion {

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    // 区域规则（覆盖世界规则）
    private Boolean allowBreak = null;      // null 表示使用世界规则
    private Boolean allowPlace = null;
    private Boolean allowInteract = null;
    private Boolean allowPVP = null;
    private Boolean allowItemDrop = null;
    private Boolean allowItemPickup = null;
    
    // 刷怪控制（覆盖世界规则）
    private Boolean disableNaturalSpawn = null;   // 禁止自然刷新
    private Boolean disableMonsterSpawn = null;   // 禁止怪物刷新
    private Boolean disableAnimalSpawn = null;    // 禁止动物刷新
    
    // 死亡规则（覆盖世界规则）
    private Boolean keepInventory = null;         // 死亡不掉落物品
    private Boolean keepExp = null;               // 死亡不掉落经验

    // 进入/离开提示
    private String enterTitle = null;
    private String enterSubtitle = null;
    private String leaveTitle = null;
    private String leaveSubtitle = null;

    public ProtectedRegion(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
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

    // Getters and Setters

    public String getName() {
        return name;
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

    public Boolean getAllowBreak() {
        return allowBreak;
    }

    public void setAllowBreak(Boolean allowBreak) {
        this.allowBreak = allowBreak;
    }

    public Boolean getAllowPlace() {
        return allowPlace;
    }

    public void setAllowPlace(Boolean allowPlace) {
        this.allowPlace = allowPlace;
    }

    public Boolean getAllowInteract() {
        return allowInteract;
    }

    public void setAllowInteract(Boolean allowInteract) {
        this.allowInteract = allowInteract;
    }

    public Boolean getAllowPVP() {
        return allowPVP;
    }

    public void setAllowPVP(Boolean allowPVP) {
        this.allowPVP = allowPVP;
    }

    public Boolean getAllowItemDrop() {
        return allowItemDrop;
    }

    public void setAllowItemDrop(Boolean allowItemDrop) {
        this.allowItemDrop = allowItemDrop;
    }

    public Boolean getAllowItemPickup() {
        return allowItemPickup;
    }

    public void setAllowItemPickup(Boolean allowItemPickup) {
        this.allowItemPickup = allowItemPickup;
    }
    
    public Boolean getDisableNaturalSpawn() {
        return disableNaturalSpawn;
    }
    
    public void setDisableNaturalSpawn(Boolean disableNaturalSpawn) {
        this.disableNaturalSpawn = disableNaturalSpawn;
    }
    
    public Boolean getDisableMonsterSpawn() {
        return disableMonsterSpawn;
    }
    
    public void setDisableMonsterSpawn(Boolean disableMonsterSpawn) {
        this.disableMonsterSpawn = disableMonsterSpawn;
    }
    
    public Boolean getDisableAnimalSpawn() {
        return disableAnimalSpawn;
    }
    
    public void setDisableAnimalSpawn(Boolean disableAnimalSpawn) {
        this.disableAnimalSpawn = disableAnimalSpawn;
    }
    
    public Boolean getKeepInventory() {
        return keepInventory;
    }
    
    public void setKeepInventory(Boolean keepInventory) {
        this.keepInventory = keepInventory;
    }
    
    public Boolean getKeepExp() {
        return keepExp;
    }
    
    public void setKeepExp(Boolean keepExp) {
        this.keepExp = keepExp;
    }

    public String getEnterTitle() {
        return enterTitle;
    }

    public void setEnterTitle(String enterTitle) {
        this.enterTitle = enterTitle;
    }

    public String getEnterSubtitle() {
        return enterSubtitle;
    }

    public void setEnterSubtitle(String enterSubtitle) {
        this.enterSubtitle = enterSubtitle;
    }

    public String getLeaveTitle() {
        return leaveTitle;
    }

    public void setLeaveTitle(String leaveTitle) {
        this.leaveTitle = leaveTitle;
    }

    public String getLeaveSubtitle() {
        return leaveSubtitle;
    }

    public void setLeaveSubtitle(String leaveSubtitle) {
        this.leaveSubtitle = leaveSubtitle;
    }

    /**
     * 获取区域体积
     */
    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    @Override
    public String toString() {
        return String.format("ProtectedRegion{name=%s, world=%s, min=(%d,%d,%d), max=(%d,%d,%d)}",
                name, worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
