package cn.guangdian.dungeon.model.stage;

public class StageTrigger {
    private TriggerType type;
    private String targetStage;
    private String targetMob;
    private int count;
    private int delaySeconds;
    
    public StageTrigger() {}
    
    public StageTrigger(TriggerType type) {
        this.type = type;
    }
    
    public TriggerType getType() { return type; }
    public void setType(TriggerType type) { this.type = type; }
    
    public String getTargetStage() { return targetStage; }
    public void setTargetStage(String targetStage) { this.targetStage = targetStage; }
    
    public String getTargetMob() { return targetMob; }
    public void setTargetMob(String targetMob) { this.targetMob = targetMob; }
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }
}
