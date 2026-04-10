package cn.guangdian.raid.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class ExtractionPoint {

    private final String id;
    private Location location;
    private double radius;
    private int extractionTime;
    private boolean requiresIntel;
    private int minIntelRequired;

    public ExtractionPoint(String id) {
        this.id = id;
        this.radius = 3.0;
        this.extractionTime = 10;
        this.requiresIntel = false;
        this.minIntelRequired = 0;
    }

    public static ExtractionPoint fromConfig(String id, ConfigurationSection section) {
        ExtractionPoint point = new ExtractionPoint(id);
        if (section == null) return point;

        ConfigurationSection locSection = section.getConfigurationSection("location");
        if (locSection != null) {
            String worldName = locSection.getString("world", "world");
            double x = locSection.getDouble("x");
            double y = locSection.getDouble("y");
            double z = locSection.getDouble("z");
            point.location = new Location(null, x, y, z);
            point.location.setWorld(org.bukkit.Bukkit.getWorld(worldName));
        }

        point.radius = section.getDouble("radius", 3.0);
        point.extractionTime = section.getInt("extraction_time", 10);
        point.requiresIntel = section.getBoolean("requires_intel", false);
        point.minIntelRequired = section.getInt("min_intel", 0);

        return point;
    }

    public boolean isInZone(Location playerLocation) {
        if (location == null || playerLocation == null) return false;
        if (!location.getWorld().equals(playerLocation.getWorld())) return false;

        double distanceSquared = location.distanceSquared(playerLocation);
        return distanceSquared <= radius * radius;
    }

    public String getId() { return id; }
    public Location getLocation() { return location; }
    public double getRadius() { return radius; }
    public int getExtractionTime() { return extractionTime; }
    public boolean isRequiresIntel() { return requiresIntel; }
    public int getMinIntelRequired() { return minIntelRequired; }

    public void setLocation(Location location) { this.location = location; }
}
