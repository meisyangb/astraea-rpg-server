package cn.guangdian.collection.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionReward {
    
    private final String id;
    private String name;
    private String description;
    private RewardCondition condition;
    private double money = 0;
    private long points = 0;
    private final List<String> commands = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    
    public CollectionReward(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RewardCondition getCondition() { return condition; }
    public double getMoney() { return money; }
    public long getPoints() { return points; }
    public List<String> getCommands() { return commands; }
    public List<String> getMessages() { return messages; }
    
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCondition(RewardCondition condition) { this.condition = condition; }
    public void setMoney(double money) { this.money = money; }
    public void setPoints(long points) { this.points = points; }
    
    public void addCommand(String command) {
        commands.add(command);
    }
    
    public void addMessage(String message) {
        messages.add(message);
    }
    
    public static class RewardCondition {
        private ConditionType type;
        private String category;
        private String entryId;
        private int count;
        
        public enum ConditionType {
            CATEGORY_COMPLETE,
            ITEM_COLLECT,
            KILL_TARGET,
            ITEM_COUNT,
            KILL_COUNT
        }
        
        public ConditionType getType() { return type; }
        public String getCategory() { return category; }
        public String getEntryId() { return entryId; }
        public int getCount() { return count; }
        
        public void setType(ConditionType type) { this.type = type; }
        public void setCategory(String category) { this.category = category; }
        public void setEntryId(String entryId) { this.entryId = entryId; }
        public void setCount(int count) { this.count = count; }
    }
}
