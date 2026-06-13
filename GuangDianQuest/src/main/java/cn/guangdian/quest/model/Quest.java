package cn.guangdian.quest.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务模型
 */
public class Quest {
    
    private final String id;
    private String name;
    private QuestType type;
    private List<String> description;
    private List<QuestObjective> objectives;
    private QuestReward reward;
    
    private List<String> prerequisites;
    private int requiredLevel;
    
    private String questLine;
    private int order;
    
    private int timeLimit;
    private int dailyWeight;
    private boolean repeatable;
    private int cooldown;
    
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
    
    public static Quest fromConfig(String id, ConfigurationSection section) {
        Quest quest = new Quest(id);
        
        quest.name = section.getString("name", id);
        quest.type = QuestType.fromString(section.getString("type", "SIDE"));
        quest.description = section.getStringList("description");
        
        List<Map<?, ?>> objectivesList = section.getMapList("objectives");
        for (int i = 0; i < objectivesList.size(); i++) {
            Map<?, ?> objMap = objectivesList.get(i);
            QuestObjective objective = parseObjectiveFromMap(i, objMap);
            if (objective != null) {
                quest.objectives.add(objective);
            }
        }
        
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            quest.reward = QuestReward.fromConfig(rewardSection);
        } else {
            quest.reward = new QuestReward();
        }
        
        quest.prerequisites = section.getStringList("prerequisites");
        quest.requiredLevel = section.getInt("required_level", 0);
        quest.questLine = section.getString("questline");
        quest.order = section.getInt("order", 0);
        quest.timeLimit = section.getInt("time_limit", 0);
        quest.dailyWeight = section.getInt("daily_weight", 1);
        quest.repeatable = section.getBoolean("repeatable", false);
        quest.cooldown = section.getInt("cooldown", 0);
        
        return quest;
    }
    
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

            // 读取 dialogue 字段
            if (map.containsKey("dialogue")) {
                Object dialogueObj = map.get("dialogue");
                if (dialogueObj instanceof List) {
                    List<String> dialogue = new ArrayList<>();
                    for (Object line : (List<?>) dialogueObj) {
                        dialogue.add(line.toString());
                    }
                    objective.setDialogue(dialogue);
                    System.out.println("[Quest] 读取dialogue字段: " + dialogue.size() + " 行");
                }
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
    
    public int getObjectiveCount() { return objectives.size(); }
    public QuestObjective getObjective(int index) {
        if (index < 0 || index >= objectives.size()) return null;
        return objectives.get(index);
    }
    public String getFullName() { return type.getPrefix() + " " + name; }
    public boolean hasPrerequisites() { return !prerequisites.isEmpty(); }
}