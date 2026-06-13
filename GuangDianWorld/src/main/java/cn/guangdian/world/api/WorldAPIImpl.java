package cn.guangdian.world.api;

import cn.guangdian.world.manager.WorldManager;
import cn.guangdian.world.model.GDWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class WorldAPIImpl implements WorldAPI {

    private final WorldManager worldManager;

    public WorldAPIImpl(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public GDWorld getWorld(String name) {
        return worldManager.getWorld(name);
    }

    @Override
    public GDWorld getWorld(World world) {
        return worldManager.getWorld(world);
    }

    @Override
    public Collection<GDWorld> getAllWorlds() {
        return worldManager.getAllWorlds();
    }

    @Override
    public List<String> getWorldNames() {
        return worldManager.getWorldNames();
    }

    @Override
    public int getWorldCount() {
        return worldManager.getWorldCount();
    }

    @Override
    public boolean worldExists(String name) {
        return worldManager.getWorld(name) != null;
    }

    @Override
    public boolean isWorldLoaded(String name) {
        GDWorld world = worldManager.getWorld(name);
        return world != null && world.isLoaded();
    }

    @Override
    public GDWorld createWorld(String name, World.Environment environment) {
        return worldManager.createWorld(name, environment);
    }

    @Override
    public boolean loadWorld(String name) {
        GDWorld world = worldManager.getWorld(name);
        if (world == null) return false;
        return worldManager.loadWorld(world);
    }

    @Override
    public boolean unloadWorld(String name) {
        return worldManager.unloadWorld(name);
    }

    @Override
    public boolean deleteWorld(String name) {
        return worldManager.deleteWorld(name);
    }

    @Override
    public boolean teleportToWorld(Player player, String worldName) {
        return worldManager.teleportToWorld(player, worldName);
    }

    @Override
    public void setSpawnPoint(String worldName, Location location) {
        worldManager.setSpawnPoint(worldName, location);
    }

    @Override
    public String getWorldDisplayName(String worldName) {
        GDWorld world = worldManager.getWorld(worldName);
        return world != null ? world.getDisplayName() : worldName;
    }

    @Override
    public String getRespawnWorld(String worldName) {
        GDWorld world = worldManager.getWorld(worldName);
        return world != null ? world.getRespawnWorld() : null;
    }
}
