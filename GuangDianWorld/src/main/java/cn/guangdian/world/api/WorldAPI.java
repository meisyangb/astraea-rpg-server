package cn.guangdian.world.api;

import cn.guangdian.world.model.GDWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public interface WorldAPI {

    GDWorld getWorld(String name);

    GDWorld getWorld(World world);

    Collection<GDWorld> getAllWorlds();

    List<String> getWorldNames();

    int getWorldCount();

    boolean worldExists(String name);

    boolean isWorldLoaded(String name);

    GDWorld createWorld(String name, World.Environment environment);

    boolean loadWorld(String name);

    boolean unloadWorld(String name);

    boolean deleteWorld(String name);

    boolean teleportToWorld(Player player, String worldName);

    void setSpawnPoint(String worldName, Location location);

    String getWorldDisplayName(String worldName);

    String getRespawnWorld(String worldName);
}
