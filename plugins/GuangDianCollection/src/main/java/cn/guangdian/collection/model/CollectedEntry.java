package cn.guangdian.collection.model;

public class CollectedEntry {
    
    private final String entryId;
    private final long collectedAt;
    private int count = 1;
    
    public CollectedEntry(String entryId, long collectedAt) {
        this.entryId = entryId;
        this.collectedAt = collectedAt;
    }
    
    public String getEntryId() { return entryId; }
    public long getCollectedAt() { return collectedAt; }
    public int getCount() { return count; }
    
    public void incrementCount() {
        this.count++;
    }
}
