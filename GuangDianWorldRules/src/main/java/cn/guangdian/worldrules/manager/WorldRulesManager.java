package cn.guangdian.worldrules.manager;

import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.WorldRules;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldRulesManager {

    private final GuangDianWorldRules plugin;
    private final Map<String, WorldRules> worldRulesMap = new HashMap<>();
    private WorldRules defaultRules;

    public WorldRulesManager(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    public void loadRules() {
        worldRulesMap.clear();

        FileConfiguration config = plugin.getConfigManager().getConfig();

        // 加载默认规则
        defaultRules = loadRulesFromSection(config.getConfigurationSection("default-rules"), "__default__");
        if (defaultRules == null) {
            defaultRules = new WorldRules("__default__");
        }

        // 加载特定世界规则
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldRules rules = loadRulesFromSection(worldSection, worldName);
                    if (rules != null) {
                        // 继承默认规则
                        WorldRules mergedRules = new WorldRules(worldName);
                        mergedRules.copyFrom(defaultRules);
                        // 覆盖特定设置
                        mergeWorldRules(mergedRules, worldSection);
                        worldRulesMap.put(worldName.toLowerCase(), mergedRules);

                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getLogger().info("已加载世界 " + worldName + " 的规则配置");
                        }
                    }
                }
            }
        }

        // 加载生物控制配置
        loadSpawnControl(config);
    }

    private WorldRules loadRulesFromSection(ConfigurationSection section, String worldName) {
        if (section == null) {
            return null;
        }

        WorldRules rules = new WorldRules(worldName);

        rules.setKeepInventory(section.getBoolean("keep-inventory", false));
        rules.setKeepExp(section.getBoolean("keep-exp", false));
        rules.setDisableNaturalSpawn(section.getBoolean("disable-natural-spawn", false));
        rules.setDisableMonsterSpawn(section.getBoolean("disable-monster-spawn", false));
        rules.setDisableAnimalSpawn(section.getBoolean("disable-animal-spawn", false));
        rules.setDisableWeatherChange(section.getBoolean("disable-weather-change", false));
        rules.setDisableTimeChange(section.getBoolean("disable-time-change", false));
        rules.setDisableHunger(section.getBoolean("disable-hunger", false));
        rules.setDisableFallDamage(section.getBoolean("disable-fall-damage", false));
        rules.setDisableFireDamage(section.getBoolean("disable-fire-damage", false));
        rules.setDisableDrowningDamage(section.getBoolean("disable-drowning-damage", false));
        rules.setDisableExplosionBlockDamage(section.getBoolean("disable-explosion-block-damage", false));
        rules.setDisableMobGriefing(section.getBoolean("disable-mob-griefing", false));
        rules.setPvp(section.getBoolean("pvp", true));
        rules.setDisableItemDrop(section.getBoolean("disable-item-drop", false));
        rules.setDisableItemPickup(section.getBoolean("disable-item-pickup", false));
        rules.setDisableBlockBreak(section.getBoolean("disable-block-break", false));
        rules.setDisableBlockPlace(section.getBoolean("disable-block-place", false));
        rules.setDisableBlockInteract(section.getBoolean("disable-block-interact", false));
        rules.setDisableLiquidFlow(section.getBoolean("disable-liquid-flow", false));

        return rules;
    }

    private void mergeWorldRules(WorldRules rules, ConfigurationSection section) {
        if (section.contains("keep-inventory")) {
            rules.setKeepInventory(section.getBoolean("keep-inventory"));
        }
        if (section.contains("keep-exp")) {
            rules.setKeepExp(section.getBoolean("keep-exp"));
        }
        if (section.contains("disable-natural-spawn")) {
            rules.setDisableNaturalSpawn(section.getBoolean("disable-natural-spawn"));
        }
        if (section.contains("disable-monster-spawn")) {
            rules.setDisableMonsterSpawn(section.getBoolean("disable-monster-spawn"));
        }
        if (section.contains("disable-animal-spawn")) {
            rules.setDisableAnimalSpawn(section.getBoolean("disable-animal-spawn"));
        }
        if (section.contains("disable-weather-change")) {
            rules.setDisableWeatherChange(section.getBoolean("disable-weather-change"));
        }
        if (section.contains("disable-time-change")) {
            rules.setDisableTimeChange(section.getBoolean("disable-time-change"));
        }
        if (section.contains("disable-hunger")) {
            rules.setDisableHunger(section.getBoolean("disable-hunger"));
        }
        if (section.contains("disable-fall-damage")) {
            rules.setDisableFallDamage(section.getBoolean("disable-fall-damage"));
        }
        if (section.contains("disable-fire-damage")) {
            rules.setDisableFireDamage(section.getBoolean("disable-fire-damage"));
        }
        if (section.contains("disable-drowning-damage")) {
            rules.setDisableDrowningDamage(section.getBoolean("disable-drowning-damage"));
        }
        if (section.contains("disable-explosion-block-damage")) {
            rules.setDisableExplosionBlockDamage(section.getBoolean("disable-explosion-block-damage"));
        }
        if (section.contains("disable-mob-griefing")) {
            rules.setDisableMobGriefing(section.getBoolean("disable-mob-griefing"));
        }
        if (section.contains("pvp")) {
            rules.setPvp(section.getBoolean("pvp"));
        }
        if (section.contains("disable-item-drop")) {
            rules.setDisableItemDrop(section.getBoolean("disable-item-drop"));
        }
        if (section.contains("disable-item-pickup")) {
            rules.setDisableItemPickup(section.getBoolean("disable-item-pickup"));
        }
        if (section.contains("disable-block-break")) {
            rules.setDisableBlockBreak(section.getBoolean("disable-block-break"));
        }
        if (section.contains("disable-block-place")) {
            rules.setDisableBlockPlace(section.getBoolean("disable-block-place"));
        }
        if (section.contains("disable-block-interact")) {
            rules.setDisableBlockInteract(section.getBoolean("disable-block-interact"));
        }
        if (section.contains("disable-liquid-flow")) {
            rules.setDisableLiquidFlow(section.getBoolean("disable-liquid-flow"));
        }
    }

    private void loadSpawnControl(FileConfiguration config) {
        ConfigurationSection spawnControl = config.getConfigurationSection("spawn-control");
        if (spawnControl == null) {
            return;
        }

        // 全局禁用生物
        java.util.List<String> globalDisabled = spawnControl.getStringList("disabled-mobs");
        if (!globalDisabled.isEmpty()) {
            for (String mob : globalDisabled) {
                defaultRules.addDisabledMob(mob);
            }
        }

        // 特定世界禁用生物
        ConfigurationSection worldSpecific = spawnControl.getConfigurationSection("world-specific");
        if (worldSpecific != null) {
            for (String worldName : worldSpecific.getKeys(false)) {
                java.util.List<String> disabledMobs = worldSpecific.getStringList(worldName);
                WorldRules rules = worldRulesMap.get(worldName.toLowerCase());
                if (rules == null) {
                    rules = new WorldRules(worldName);
                    rules.copyFrom(defaultRules);
                    worldRulesMap.put(worldName.toLowerCase(), rules);
                }
                for (String mob : disabledMobs) {
                    rules.addDisabledMob(mob);
                }
            }
        }
    }

    public void saveRules() {
        // 规则是只读的，从配置文件加载，不需要保存
    }

    /**
     * 获取世界的规则配置
     */
    public WorldRules getWorldRules(String worldName) {
        WorldRules rules = worldRulesMap.get(worldName.toLowerCase());
        if (rules != null) {
            return rules;
        }
        // 返回默认规则的副本
        WorldRules defaultCopy = new WorldRules(worldName);
        defaultCopy.copyFrom(defaultRules);
        return defaultCopy;
    }

    /**
     * 获取世界的规则配置
     */
    public WorldRules getWorldRules(World world) {
        if (world == null) {
            WorldRules defaultCopy = new WorldRules("unknown");
            defaultCopy.copyFrom(defaultRules);
            return defaultCopy;
        }
        return getWorldRules(world.getName());
    }

    /**
     * 设置世界的规则
     */
    public void setWorldRules(String worldName, WorldRules rules) {
        worldRulesMap.put(worldName.toLowerCase(), rules);
    }

    /**
     * 获取默认规则
     */
    public WorldRules getDefaultRules() {
        return defaultRules;
    }

    /**
     * 获取已配置规则的世界数量
     */
    public int getWorldRulesCount() {
        return worldRulesMap.size();
    }

    /**
     * 获取所有已配置规则的世界名称
     */
    public Set<String> getConfiguredWorlds() {
        return Collections.unmodifiableSet(worldRulesMap.keySet());
    }

    /**
     * 重新加载某个世界的规则
     */
    public void reloadWorldRules(String worldName) {
        worldRulesMap.remove(worldName.toLowerCase());
        loadRules();
    }

    /**
     * 检查世界是否已配置规则
     */
    public boolean hasWorldRules(String worldName) {
        return worldRulesMap.containsKey(worldName.toLowerCase());
    }
}
