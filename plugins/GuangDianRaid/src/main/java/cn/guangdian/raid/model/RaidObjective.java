package cn.guangdian.raid.model;

import java.util.Map;

public class RaidObjective {

    public enum ObjectiveType {
        COLLECT_INTEL,
        UNLOCK_AREA,
        KILL_MOBS,
        KILL_BOSS,
        REACH_EXTRACTION,
        SURVIVE,
        INTERACT
    }

    private final int index;
    private final ObjectiveType type;
    private final String target;
    private final int amount;
    private final String description;

    private String areaId;
    private double x, y, z;

    public RaidObjective(int index, ObjectiveType type, String target, int amount, String description) {
        this.index = index;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.description = description;
    }

    public static RaidObjective fromMap(int index, Map<?, ?> map) {
        try {
            ObjectiveType type = ObjectiveType.valueOf(map.get("type").toString().toUpperCase());
            String target = map.get("target").toString();
            int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
            String description = map.containsKey("description") ? map.get("description").toString() : "";

            RaidObjective objective = new RaidObjective(index, type, target, amount, description);

            if (map.containsKey("area")) {
                objective.areaId = map.get("area").toString();
            }
            if (map.containsKey("x")) {
                objective.x = ((Number) map.get("x")).doubleValue();
                objective.y = ((Number) map.get("y")).doubleValue();
                objective.z = ((Number) map.get("z")).doubleValue();
            }

            return objective;
        } catch (Exception e) {
            return null;
        }
    }

    public int getIndex() { return index; }
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getAreaId() { return areaId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
