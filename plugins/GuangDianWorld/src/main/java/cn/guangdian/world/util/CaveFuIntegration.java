package cn.guangdian.world.util;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CaveFuIntegration {

    private static GuangDianWorld plugin;

    public static void init(GuangDianWorld plugin) {
        CaveFuIntegration.plugin = plugin;
    }

    public static boolean isCaveFuWorld(String worldName) {
        return "CaveFuWorld".equals(worldName);
    }

    public static boolean isCaveFuWorld(World world) {
        return world != null && isCaveFuWorld(world.getName());
    }

    public static GDWorld getCaveFuWorld() {
        return plugin.getWorldManager().getWorld("CaveFuWorld");
    }

    public static boolean isCaveFuWorldLoaded() {
        GDWorld world = getCaveFuWorld();
        return world != null && world.isLoaded();
    }

    public static boolean teleportToCaveFuWorld(Player player) {
        return plugin.getWorldManager().teleportToWorld(player, "CaveFuWorld");
    }

    public static Location getCaveFuSpawnLocation() {
        GDWorld world = getCaveFuWorld();
        if (world == null || !world.isLoaded()) return null;
        
        Location spawn = world.getSpawnLocation();
        if (spawn != null) {
            spawn = spawn.clone();
            spawn.setWorld(world.getBukkitWorld());
        }
        return spawn;
    }

    public static void ensureCaveFuWorldLoaded() {
        GDWorld world = getCaveFuWorld();
        if (world != null && !world.isLoaded()) {
            plugin.getWorldManager().loadWorld("CaveFuWorld");
        }
    }

    public static World getBukkitCaveFuWorld() {
        return Bukkit.getWorld("CaveFuWorld");
    }
}
