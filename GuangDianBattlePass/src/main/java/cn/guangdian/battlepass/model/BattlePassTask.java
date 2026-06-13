package cn.guangdian.battlepass.model;

public class BattlePassTask {
    
    private String taskId;
    private String taskName;
    private TaskType taskType;
    private int requiredAmount;
    private int expReward;
    private String description;
    private boolean daily;
    private boolean weekly;
    private String target;
    private int dayOfWeek;
    
    public enum TaskType {
        KILL_MOB,
        KILL_PLAYER,
        BREAK_BLOCK,
        PLACE_BLOCK,
        CRAFT_ITEM,
        ENCHANT_ITEM,
        COMPLETE_QUEST,
        COMPLETE_DUNGEON,
        PLAY_TIME,
        LOGIN,
        CHAT,
        CUSTOM
    }
    
    public BattlePassTask() {
    }
    
    public BattlePassTask(String taskId, String taskName, TaskType taskType, int requiredAmount, int expReward) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.requiredAmount = requiredAmount;
        this.expReward = expReward;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public int getRequiredAmount() {
        return requiredAmount;
    }
    
    public void setRequiredAmount(int requiredAmount) {
        this.requiredAmount = requiredAmount;
    }
    
    public int getExpReward() {
        return expReward;
    }
    
    public void setExpReward(int expReward) {
        this.expReward = expReward;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isDaily() {
        return daily;
    }
    
    public void setDaily(boolean daily) {
        this.daily = daily;
    }
    
    public boolean isWeekly() {
        return weekly;
    }
    
    public void setWeekly(boolean weekly) {
        this.weekly = weekly;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public int getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
}
