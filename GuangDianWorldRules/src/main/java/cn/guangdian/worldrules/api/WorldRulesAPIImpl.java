package cn.guangdian.worldrules.api;

import cn.guangdian.worldrules.manager.WorldRulesManager;
import cn.guangdian.worldrules.model.WorldRules;
import org.bukkit.World;

import java.util.Set;

public class WorldRulesAPIImpl implements WorldRulesAPI {

    private final WorldRulesManager worldRulesManager;

    public WorldRulesAPIImpl(WorldRulesManager worldRulesManager) {
        this.worldRulesManager = worldRulesManager;
    }

    @Override
    public WorldRules getWorldRules(String worldName) {
        return worldRulesManager.getWorldRules(worldName);
    }

    @Override
    public WorldRules getWorldRules(World world) {
        return worldRulesManager.getWorldRules(world);
    }

    @Override
    public boolean hasWorldRules(String worldName) {
        return worldRulesManager.hasWorldRules(worldName);
    }

    @Override
    public WorldRules getDefaultRules() {
        return worldRulesManager.getDefaultRules();
    }

    @Override
    public Set<String> getConfiguredWorlds() {
        return worldRulesManager.getConfiguredWorlds();
    }

    @Override
    public boolean isKeepInventory(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.isKeepInventory();
    }

    @Override
    public boolean isKeepExp(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.isKeepExp();
    }

    @Override
    public boolean isDisableNaturalSpawn(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.isDisableNaturalSpawn();
    }

    @Override
    public boolean isDisableMonsterSpawn(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.isDisableMonsterSpawn();
    }

    @Override
    public boolean isDisableAnimalSpawn(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.isDisableAnimalSpawn();
    }

    @Override
    public boolean isPvpDisabled(String worldName) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return !rules.isPvp();
    }

    @Override
    public boolean canMobSpawn(String worldName, String mobType) {
        WorldRules rules = worldRulesManager.getWorldRules(worldName);
        return rules.canSpawn(mobType);
    }

    @Override
    public void reloadRules() {
        worldRulesManager.loadRules();
    }

    @Override
    public void reloadWorldRules(String worldName) {
        worldRulesManager.reloadWorldRules(worldName);
    }
}
