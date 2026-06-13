package cn.guangdian.worldrules.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.api.WorldRulesAPI;
import cn.guangdian.worldrules.api.WorldRulesAPIImpl;
import cn.guangdian.worldrules.model.WorldRules;
import org.bukkit.World;

import java.util.Set;

public class WorldRulesServiceAdapter implements WorldRulesAPI {

    private final GuangDianWorldRules plugin;
    private final WorldRulesAPIImpl delegate;

    public WorldRulesServiceAdapter(GuangDianWorldRules plugin, WorldRulesAPIImpl delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
        registerService();
    }

    private void registerService() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(WorldRulesAPI.class, this);
        }
    }

    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(WorldRulesAPI.class);
        }
    }

    @Override
    public WorldRules getWorldRules(String worldName) {
        return delegate.getWorldRules(worldName);
    }

    @Override
    public WorldRules getWorldRules(World world) {
        return delegate.getWorldRules(world);
    }

    @Override
    public boolean hasWorldRules(String worldName) {
        return delegate.hasWorldRules(worldName);
    }

    @Override
    public WorldRules getDefaultRules() {
        return delegate.getDefaultRules();
    }

    @Override
    public Set<String> getConfiguredWorlds() {
        return delegate.getConfiguredWorlds();
    }

    @Override
    public boolean isKeepInventory(String worldName) {
        return delegate.isKeepInventory(worldName);
    }

    @Override
    public boolean isKeepExp(String worldName) {
        return delegate.isKeepExp(worldName);
    }

    @Override
    public boolean isDisableNaturalSpawn(String worldName) {
        return delegate.isDisableNaturalSpawn(worldName);
    }

    @Override
    public boolean isDisableMonsterSpawn(String worldName) {
        return delegate.isDisableMonsterSpawn(worldName);
    }

    @Override
    public boolean isDisableAnimalSpawn(String worldName) {
        return delegate.isDisableAnimalSpawn(worldName);
    }

    @Override
    public boolean isPvpDisabled(String worldName) {
        return delegate.isPvpDisabled(worldName);
    }

    @Override
    public boolean canMobSpawn(String worldName, String mobType) {
        return delegate.canMobSpawn(worldName, mobType);
    }

    @Override
    public void reloadRules() {
        delegate.reloadRules();
    }

    @Override
    public void reloadWorldRules(String worldName) {
        delegate.reloadWorldRules(worldName);
    }
}
