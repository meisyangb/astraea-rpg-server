package cn.guangdian.collection.model;

public class KillRecord {
    
    private final String entryId;
    private int killCount = 0;
    
    public KillRecord(String entryId) {
        this.entryId = entryId;
    }
    
    public String getEntryId() { return entryId; }
    public int getKillCount() { return killCount; }
    
    public void addKill() {
        this.killCount++;
    }
    
    public void setKillCount(int count) {
        this.killCount = count;
    }
}
