package cn.guangdian.dungeon.model.stage;

import org.bukkit.Location;

public class WaveTrigger {
    private WaveTriggerType type;
    private int delaySeconds;
    private Location targetLocation;
    private double locationRadius;
    private String command;
    private String interactType;
    
    public WaveTrigger() {}
    
    public WaveTrigger(WaveTriggerType type) {
        this.type = type;
    }
    
    public static WaveTrigger onKillComplete() {
        return new WaveTrigger(WaveTriggerType.ON_KILL_COMPLETE);
    }
    
    public static WaveTrigger onTime(int seconds) {
        WaveTrigger trigger = new WaveTrigger(WaveTriggerType.ON_TIME);
        trigger.setDelaySeconds(seconds);
        return trigger;
    }
    
    public static WaveTrigger onLocation(Location location, double radius) {
        WaveTrigger trigger = new WaveTrigger(WaveTriggerType.ON_LOCATION);
        trigger.setTargetLocation(location);
        trigger.setLocationRadius(radius);
        return trigger;
    }
    
    public static WaveTrigger onCommand(String command) {
        WaveTrigger trigger = new WaveTrigger(WaveTriggerType.ON_COMMAND);
        trigger.setCommand(command);
        return trigger;
    }
    
    public static WaveTrigger onInteract(String interactType) {
        WaveTrigger trigger = new WaveTrigger(WaveTriggerType.ON_INTERACT);
        trigger.setInteractType(interactType);
        return trigger;
    }
    
    public WaveTriggerType getType() { return type; }
    public void setType(WaveTriggerType type) { this.type = type; }
    
    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }
    
    public Location getTargetLocation() { return targetLocation; }
    public void setTargetLocation(Location targetLocation) { this.targetLocation = targetLocation; }
    
    public double getLocationRadius() { return locationRadius; }
    public void setLocationRadius(double locationRadius) { this.locationRadius = locationRadius; }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public String getInteractType() { return interactType; }
    public void setInteractType(String interactType) { this.interactType = interactType; }
}
