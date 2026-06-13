package cn.guangdian.dungeon.model.stage;

public class CompletionCondition {
    private CompletionType type;
    private int targetCount;
    private String targetMob;
    
    public CompletionCondition() {}
    
    public CompletionCondition(CompletionType type) {
        this.type = type;
    }
    
    public CompletionType getType() { return type; }
    public void setType(CompletionType type) { this.type = type; }
    
    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }
    
    public String getTargetMob() { return targetMob; }
    public void setTargetMob(String targetMob) { this.targetMob = targetMob; }
}
