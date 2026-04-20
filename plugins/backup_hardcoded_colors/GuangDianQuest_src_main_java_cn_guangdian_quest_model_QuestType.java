package cn.guangdian.quest.model;

/**
 * 任务类型枚举
 */
public enum QuestType {
    
    /**
     * 主线任务 - 剧情推进、解锁系统
     */
    MAIN("主线任务", "&6&l【主线】", true, false),
    
    /**
     * 支线任务 - NPC委托、探索任务
     */
    SIDE("支线任务", "&b&l【支线】", true, false),
    
    /**
     * 每日任务 - 每日重置，限制数量
     */
    DAILY("每日任务", "&e&l【每日】", false, true),
    
    /**
     * 成就任务 - 长期目标，累计奖励
     */
    ACHIEVEMENT("成就任务", "&d&l【成就】", true, false);
    
    private final String displayName;
    private final String prefix;
    private final boolean permanent;
    private final boolean dailyReset;
    
    QuestType(String displayName, String prefix, boolean permanent, boolean dailyReset) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.permanent = permanent;
        this.dailyReset = dailyReset;
    }
    
    public String getDisplayName() { return displayName; }
    public String getPrefix() { return prefix; }
    public boolean isPermanent() { return permanent; }
    public boolean isDailyReset() { return dailyReset; }
    
    public static QuestType fromString(String str) {
        if (str == null) return SIDE;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SIDE;
        }
    }
}