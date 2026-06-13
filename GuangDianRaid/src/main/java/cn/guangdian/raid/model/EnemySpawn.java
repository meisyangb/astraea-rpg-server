package cn.guangdian.raid.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnemySpawn {

    private String mobType;
    private int count;
    private List<Location> locations;
    private double healthMultiplier;
    private double damageMultiplier;

    public EnemySpawn() {
        this.count = 1;
        this.locations = new ArrayList<>();
        this.healthMultiplier = 1.0;
        this.damageMultiplier = 1.0;
    }

    public static EnemySpawn fromMap(Map<?, ?> map) {
        EnemySpawn spawn = new EnemySpawn();
        try {
            spawn.mobType = map.get("type").toString();
            spawn.count = map.containsKey("count") ? ((Number) map.get("count")).intValue() : 1;
            spawn.healthMultiplier = map.containsKey("health_mult") ? 
                ((Number) map.get("health_mult")).doubleValue() : 1.0;
            spawn.damageMultiplier = map.containsKey("damage_mult") ? 
                ((Number) map.get("damage_mult")).doubleValue() : 1.0;

            Object locs = map.get("locations");
            if (locs instanceof List) {
                for (Object locObj : (List<?>) locs) {
                    if (locObj instanceof Map) {
                        Map<?, ?> locMap = (Map<?, ?>) locObj;
                        double x = locMap.containsKey("x") ? ((Number) locMap.get("x")).doubleValue() : 0;
                        double y = locMap.containsKey("y") ? ((Number) locMap.get("y")).doubleValue() : 0;
                        double z = locMap.containsKey("z") ? ((Number) locMap.get("z")).doubleValue() : 0;
                        spawn.locations.add(new Location(null, x, y, z));
                    }
                }
            }
            return spawn;
        } catch (Exception e) {
            return null;
        }
    }

    public String getMobType() { return mobType; }
    public int getCount() { return count; }
    public List<Location> getLocations() { return locations; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
}
