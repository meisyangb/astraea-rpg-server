package cn.guangdian.battlepass.model;

import java.util.List;
import java.util.Map;

public class ExpTrigger {
    
    private String triggerId;
    private TriggerType triggerType;
    private String target;
    private List<String> targets;
    private int expAmount;
    private int cooldown;
    private int maxDaily;
    private Map<String, Object> conditions;
    private boolean enabled;
    
    public enum TriggerType {
        KILL_MYTHICMOB,
        KILL_MOB_TYPE,
        OBTAIN_ITEM,
        OBTAIN_MYTHIC_ITEM,
        CRAFT_ITEM,
        ENCHANT_ITEM,
        COMPLETE_DUNGEON,
        COMPLETE_QUEST,
        LEVEL_UP,
        PLAYER_DEATH,
        BLOCK_BREAK,
        BLOCK_PLACE,
        FISHING,
        CUSTOM
    }
    
    public ExpTrigger() {
        this.enabled = true;
        this.cooldown = 0;
        this.maxDaily = -1;
    }
    
    public String getTriggerId() {
        return triggerId;
    }
    
    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }
    
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public List<String> getTargets() {
        return targets;
    }
    
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }
    
    public int getExpAmount() {
        return expAmount;
    }
    
    public void setExpAmount(int expAmount) {
        this.expAmount = expAmount;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
    
    public int getMaxDaily() {
        return maxDaily;
    }
    
    public void setMaxDaily(int maxDaily) {
        this.maxDaily = maxDaily;
    }
    
    public Map<String, Object> getConditions() {
        return conditions;
    }
    
    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean matchesTarget(String targetType) {
        if (target != null && target.equalsIgnoreCase(targetType)) {
            return true;
        }
        if (targets != null && targets.contains(targetType)) {
            return true;
        }
        if (target == null && targets == null) {
            return true;
        }
        return false;
    }
}
