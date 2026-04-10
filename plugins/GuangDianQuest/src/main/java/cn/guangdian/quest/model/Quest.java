package cn.guangdian.quest.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务模型
 */
public class Quest {
    
    private final String id;                     // 任务ID
    private String name;                         // 任务名称
    private QuestType type;                      // 任务类型
    private List<String> description;            // 任务描述
    private List<QuestObjective> objectives;     // 任务目标列表
    private QuestReward reward;                  // 任务奖励
    
    // 前置条件
    private List<String> prerequisites;          // 前置任务ID
    private int requiredLevel;                   // 等级要求
    
    // 任务线
    private String questLine;                    // 所属任务线ID
    private int order;                           // 任务线中的顺序
    
    // 时间限制
    private int timeLimit;                       // 时间限制（秒），0表示无限制
    
    // 每日任务配置
    private int dailyWeight;                     // 每日任务权重（用于随机抽取）
    
    // 可重复
    private boolean repeatable;                  // 是否可重复接取
    private int cooldown;                        // 冷却时间（秒）
    
    public Quest(String id) {
        this.id = id;
        this.type = QuestType.SIDE;
        this.description = new ArrayList<>();
        this.objectives = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
        this.requiredLevel = 0;
        this.timeLimit = 0;
        this.dailyWeight = 1;
        this.repeatable = false;
        this.cooldown = 0;
    }
    
    /**
     * 从配置加载任务
     */
    public static Quest fromConfig(String id, ConfigurationSection section) {
        Quest quest = new Quest(id);
        
        quest.name = section.getString("name", id);
        quest.type = QuestType.fromString(section.getString("type", "SIDE"));
        quest.description = section.getStringList("description");
        
        // 加载目标
        List<Map<?, ?>> objectivesList = section.getMapList("objectives");
        for (int i = 0; i < objectivesList.size(); i++) {
            // 需要转换为ConfigurationSection
            // 简化处理：使用Map解析
            Map<?, ?> objMap = objectivesList.get(i);
            QuestObjective objective = parseObjectiveFromMap(i, objMap);
            if (objective != null) {
                quest.objectives.add(objective);
            }
        }
        
        // 加载奖励
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            quest.reward = QuestReward.fromConfig(rewardSection);
        } else {
            quest.reward = new QuestReward();
        }
        
        // 前置条件
        quest.prerequisites = section.getStringList("prerequisites");
        quest.requiredLevel = section.getInt("required_level", 0);
        
        // 任务线
        quest.questLine = section.getString("questline");
        quest.order = section.getInt("order", 0);
        
        // 时间限制
        quest.timeLimit = section.getInt("time_limit", 0);
        
        // 每日任务权重
        quest.dailyWeight = section.getInt("daily_weight", 1);
        
        // 可重复
        quest.repeatable = section.getBoolean("repeatable", false);
        quest.cooldown = section.getInt("cooldown", 0);
        
        return quest;
    }
    
    /**
     * 从Map解析任务目标
     */
    private static QuestObjective parseObjectiveFromMap(int index, Map<?, ?> map) {
        try {
            QuestObjective.ObjectiveType type = QuestObjective.ObjectiveType.valueOf(
                map.get("type").toString().toUpperCase()
            );
            String target = map.get("target").toString();
            int amount = ((Number) map.get("amount")).intValue();
            String description = map.containsKey("description") ? 
                map.get("description").toString() : "";
            
            QuestObjective objective = new QuestObjective(index, type, target, amount, description);
            
            // 位置参数
            if (map.containsKey("world")) {
                // 需要通过反射或setter设置
            }
            
            return objective;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public QuestType getType() { return type; }
    public List<String> getDescription() { return description; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public QuestReward getReward() { return reward; }
    public List<String> getPrerequisites() { return prerequisites; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getQuestLine() { return questLine; }
    public int getOrder() { return order; }
    public int getTimeLimit() { return timeLimit; }
    public int getDailyWeight() { return dailyWeight; }
    public boolean isRepeatable() { return repeatable; }
    public int getCooldown() { return cooldown; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setType(QuestType type) { this.type = type; }
    public void setDescription(List<String> description) { this.description = description; }
    public void setReward(QuestReward reward) { this.reward = reward; }
    public void setQuestLine(String questLine) { this.questLine = questLine; }
    public void setOrder(int order) { this.order = order; }
    
    /**
     * 获取目标数量
     */
    public int getObjectiveCount() {
        return objectives.size();
    }
    
    /**
     * 根据索引获取目标
     */
    public QuestObjective getObjective(int index) {
        if (index < 0 || index >= objectives.size()) return null;
        return objectives.get(index);
    }
    
    /**
     * 获取完整显示名称
     */
    public String getFullName() {
        return type.getPrefix() + " " + name;
    }
    
    /**
     * 检查是否有前置任务
     */
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
}