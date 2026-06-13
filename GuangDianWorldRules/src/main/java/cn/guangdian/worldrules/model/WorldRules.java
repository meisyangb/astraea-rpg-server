package cn.guangdian.worldrules.model;

import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class WorldRules {

    private final String worldName;

    // 死亡相关
    private boolean keepInventory = false;
    private boolean keepExp = false;

    // 生物刷新相关
    private boolean disableNaturalSpawn = false;
    private boolean disableMonsterSpawn = false;
    private boolean disableAnimalSpawn = false;

    // 环境相关
    private boolean disableWeatherChange = false;
    private boolean disableTimeChange = false;

    // 玩家状态相关
    private boolean disableHunger = false;
    private boolean disableFallDamage = false;
    private boolean disableFireDamage = false;
    private boolean disableDrowningDamage = false;

    // 破坏相关
    private boolean disableExplosionBlockDamage = false;
    private boolean disableMobGriefing = false;

    // PVP
    private boolean pvp = true;

    // 物品相关
    private boolean disableItemDrop = false;
    private boolean disableItemPickup = false;

    // 方块操作相关
    private boolean disableBlockBreak = false;
    private boolean disableBlockPlace = false;
    private boolean disableBlockInteract = false;

    // 液体流动相关
    private boolean disableLiquidFlow = false;

    // 特定生物类型禁用
    private final Set<String> disabledMobs = new HashSet<>();

    public WorldRules(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    // Getters and Setters
    public boolean isKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public boolean isKeepExp() {
        return keepExp;
    }

    public void setKeepExp(boolean keepExp) {
        this.keepExp = keepExp;
    }

    public boolean isDisableNaturalSpawn() {
        return disableNaturalSpawn;
    }

    public void setDisableNaturalSpawn(boolean disableNaturalSpawn) {
        this.disableNaturalSpawn = disableNaturalSpawn;
    }

    public boolean isDisableMonsterSpawn() {
        return disableMonsterSpawn;
    }

    public void setDisableMonsterSpawn(boolean disableMonsterSpawn) {
        this.disableMonsterSpawn = disableMonsterSpawn;
    }

    public boolean isDisableAnimalSpawn() {
        return disableAnimalSpawn;
    }

    public void setDisableAnimalSpawn(boolean disableAnimalSpawn) {
        this.disableAnimalSpawn = disableAnimalSpawn;
    }

    public boolean isDisableWeatherChange() {
        return disableWeatherChange;
    }

    public void setDisableWeatherChange(boolean disableWeatherChange) {
        this.disableWeatherChange = disableWeatherChange;
    }

    public boolean isDisableTimeChange() {
        return disableTimeChange;
    }

    public void setDisableTimeChange(boolean disableTimeChange) {
        this.disableTimeChange = disableTimeChange;
    }

    public boolean isDisableHunger() {
        return disableHunger;
    }

    public void setDisableHunger(boolean disableHunger) {
        this.disableHunger = disableHunger;
    }

    public boolean isDisableFallDamage() {
        return disableFallDamage;
    }

    public void setDisableFallDamage(boolean disableFallDamage) {
        this.disableFallDamage = disableFallDamage;
    }

    public boolean isDisableFireDamage() {
        return disableFireDamage;
    }

    public void setDisableFireDamage(boolean disableFireDamage) {
        this.disableFireDamage = disableFireDamage;
    }

    public boolean isDisableDrowningDamage() {
        return disableDrowningDamage;
    }

    public void setDisableDrowningDamage(boolean disableDrowningDamage) {
        this.disableDrowningDamage = disableDrowningDamage;
    }

    public boolean isDisableExplosionBlockDamage() {
        return disableExplosionBlockDamage;
    }

    public void setDisableExplosionBlockDamage(boolean disableExplosionBlockDamage) {
        this.disableExplosionBlockDamage = disableExplosionBlockDamage;
    }

    public boolean isDisableMobGriefing() {
        return disableMobGriefing;
    }

    public void setDisableMobGriefing(boolean disableMobGriefing) {
        this.disableMobGriefing = disableMobGriefing;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public boolean isDisableItemDrop() {
        return disableItemDrop;
    }

    public void setDisableItemDrop(boolean disableItemDrop) {
        this.disableItemDrop = disableItemDrop;
    }

    public boolean isDisableItemPickup() {
        return disableItemPickup;
    }

    public void setDisableItemPickup(boolean disableItemPickup) {
        this.disableItemPickup = disableItemPickup;
    }

    public boolean isDisableBlockBreak() {
        return disableBlockBreak;
    }

    public void setDisableBlockBreak(boolean disableBlockBreak) {
        this.disableBlockBreak = disableBlockBreak;
    }

    public boolean isDisableBlockPlace() {
        return disableBlockPlace;
    }

    public void setDisableBlockPlace(boolean disableBlockPlace) {
        this.disableBlockPlace = disableBlockPlace;
    }

    public boolean isDisableBlockInteract() {
        return disableBlockInteract;
    }

    public void setDisableBlockInteract(boolean disableBlockInteract) {
        this.disableBlockInteract = disableBlockInteract;
    }

    public boolean isDisableLiquidFlow() {
        return disableLiquidFlow;
    }

    public void setDisableLiquidFlow(boolean disableLiquidFlow) {
        this.disableLiquidFlow = disableLiquidFlow;
    }

    public Set<String> getDisabledMobs() {
        return disabledMobs;
    }

    public void addDisabledMob(String mobType) {
        this.disabledMobs.add(mobType.toUpperCase());
    }

    public void removeDisabledMob(String mobType) {
        this.disabledMobs.remove(mobType.toUpperCase());
    }

    public boolean isMobDisabled(String mobType) {
        return disabledMobs.contains(mobType.toUpperCase());
    }

    public void setDisabledMobs(Set<String> disabledMobs) {
        this.disabledMobs.clear();
        for (String mob : disabledMobs) {
            this.disabledMobs.add(mob.toUpperCase());
        }
    }

    /**
     * 检查是否允许该生物类型刷新
     */
    public boolean canSpawn(String mobType) {
        if (disabledMobs.contains(mobType.toUpperCase())) {
            return false;
        }
        if (disableNaturalSpawn) {
            return false;
        }
        return true;
    }

    /**
     * 从另一个规则对象复制所有设置
     */
    public void copyFrom(WorldRules other) {
        this.keepInventory = other.keepInventory;
        this.keepExp = other.keepExp;
        this.disableNaturalSpawn = other.disableNaturalSpawn;
        this.disableMonsterSpawn = other.disableMonsterSpawn;
        this.disableAnimalSpawn = other.disableAnimalSpawn;
        this.disableWeatherChange = other.disableWeatherChange;
        this.disableTimeChange = other.disableTimeChange;
        this.disableHunger = other.disableHunger;
        this.disableFallDamage = other.disableFallDamage;
        this.disableFireDamage = other.disableFireDamage;
        this.disableDrowningDamage = other.disableDrowningDamage;
        this.disableExplosionBlockDamage = other.disableExplosionBlockDamage;
        this.disableMobGriefing = other.disableMobGriefing;
        this.pvp = other.pvp;
        this.disableItemDrop = other.disableItemDrop;
        this.disableItemPickup = other.disableItemPickup;
        this.disableBlockBreak = other.disableBlockBreak;
        this.disableBlockPlace = other.disableBlockPlace;
        this.disableBlockInteract = other.disableBlockInteract;
        this.disableLiquidFlow = other.disableLiquidFlow;
        this.disabledMobs.clear();
        this.disabledMobs.addAll(other.disabledMobs);
    }
}
