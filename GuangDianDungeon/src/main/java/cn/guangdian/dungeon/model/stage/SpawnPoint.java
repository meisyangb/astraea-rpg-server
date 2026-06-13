package cn.guangdian.dungeon.model.stage;

import org.bukkit.Location;

public class SpawnPoint {
    private String id;
    private Location location;
    private double radius;
    
    public SpawnPoint() {}
    
    public SpawnPoint(String id, Location location, double radius) {
        this.id = id;
        this.location = location;
        this.radius = radius;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
}
