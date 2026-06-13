package cn.guangdian.portal.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Portal {

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final Material frameMaterial;
    private Location destination;
    private String destinationPortal;
    private String permission;
    private boolean enabled;
    private final Set<Location> portalArea;

    public Portal(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2, Material frameMaterial) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.frameMaterial = frameMaterial;
        this.enabled = true;
        this.portalArea = new HashSet<>();
        calculatePortalArea();
    }

    private void calculatePortalArea() {
        portalArea.clear();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    portalArea.add(new Location(null, x, y, z));
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public Material getFrameMaterial() {
        return frameMaterial;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public String getDestinationPortal() {
        return destinationPortal;
    }

    public void setDestinationPortal(String destinationPortal) {
        this.destinationPortal = destinationPortal;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInside(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().getName().equals(worldName)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public List<Location> getFrameBlocks(World world) {
        List<Location> blocks = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean isFrame = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    if (isFrame) {
                        blocks.add(new Location(world, x, y, z));
                    }
                }
            }
        }

        return blocks;
    }

    public void createFrame(World world) {
        for (Location loc : getFrameBlocks(world)) {
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                block.setType(frameMaterial);
            }
        }
    }

    public void fillPortal(World world, Material material) {
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                        block.setType(material);
                    }
                }
            }
        }
    }

    public void clearPortal(World world) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("minX", minX);
        section.set("minY", minY);
        section.set("minZ", minZ);
        section.set("maxX", maxX);
        section.set("maxY", maxY);
        section.set("maxZ", maxZ);
        section.set("frame-material", frameMaterial.name());
        section.set("enabled", enabled);

        if (destination != null) {
            section.set("destination.world", destination.getWorld() != null ? destination.getWorld().getName() : worldName);
            section.set("destination.x", destination.getX());
            section.set("destination.y", destination.getY());
            section.set("destination.z", destination.getZ());
            section.set("destination.yaw", destination.getYaw());
            section.set("destination.pitch", destination.getPitch());
        }

        if (destinationPortal != null) {
            section.set("destination-portal", destinationPortal);
        }

        if (permission != null) {
            section.set("permission", permission);
        }
    }

    public static Portal loadFromConfig(String name, ConfigurationSection section) {
        String worldName = section.getString("world");
        int minX = section.getInt("minX");
        int minY = section.getInt("minY");
        int minZ = section.getInt("minZ");
        int maxX = section.getInt("maxX");
        int maxY = section.getInt("maxY");
        int maxZ = section.getInt("maxZ");
        Material frameMaterial = Material.valueOf(section.getString("frame-material", "OBSIDIAN"));
        boolean enabled = section.getBoolean("enabled", true);

        Portal portal = new Portal(name, worldName, minX, minY, minZ, maxX, maxY, maxZ, frameMaterial);
        portal.setEnabled(enabled);

        if (section.contains("destination")) {
            String destWorld = section.getString("destination.world", worldName);
            double destX = section.getDouble("destination.x");
            double destY = section.getDouble("destination.y");
            double destZ = section.getDouble("destination.z");
            float destYaw = (float) section.getDouble("destination.yaw", 0);
            float destPitch = (float) section.getDouble("destination.pitch", 0);

            World world = Bukkit.getWorld(destWorld);
            if (world != null) {
                portal.destination = new Location(world, destX, destY, destZ, destYaw, destPitch);
            }
        }

        if (section.contains("destination-portal")) {
            portal.destinationPortal = section.getString("destination-portal");
        }

        if (section.contains("permission")) {
            portal.permission = section.getString("permission");
        }

        return portal;
    }

    public String getBoundsString() {
        return String.format("[%s] (%d,%d,%d) -> (%d,%d,%d)", 
            worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public String getDestinationString() {
        if (destination != null) {
            return String.format("[%s] (%.1f, %.1f, %.1f)",
                destination.getWorld() != null ? destination.getWorld().getName() : "未知",
                destination.getX(), destination.getY(), destination.getZ());
        } else if (destinationPortal != null) {
            return "→ " + destinationPortal;
        }
        return "未设置";
    }

    @Override
    public String toString() {
        return String.format("Portal{name='%s', world='%s', bounds=%s, dest=%s, enabled=%s}",
            name, worldName, getBoundsString(), getDestinationString(), enabled);
    }
}
