package cn.guangdian.quest.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务目标
 */
public class QuestObjective {
    
    private final int index;                // 目标索引
    private final ObjectiveType type;       // 目标类型
    private final String target;            // 目标（怪物ID/物品ID/NPC ID/位置）
    private final int amount;               // 所需数量
    private final String description;       // 目标描述
    
    // 可选参数
    private String world;                   // 世界名（REACH类型）
    private double x, y, z;                 // 坐标（REACH类型）
    private double radius;                  // 半径（REACH类型）
    
    // 对话内容（TALK类型）
    private List<String> dialogue;          // NPC对话内容
    
    /**
     * 目标类型枚举
     */
    public enum ObjectiveType {
        KILL,       // 击杀怪物
        COLLECT,    // 收集物品（自动拾取触发）
        SUBMIT,     // 提交物品（手动触发，消耗物品）
        TALK,       // 与NPC对话
        REACH,      // 到达位置
        USE,        // 使用物品/技能
        BREAK,      // 破坏方块
        CRAFT,      // 合成物品
        FISH        // 钓鱼
    }
    
    public QuestObjective(int index, ObjectiveType type, String target, int amount, String description) {
        this.index = index;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.description = description;
        this.dialogue = new ArrayList<>();
    }
    
    /**
     * 从配置加载任务目标
     */
    public static QuestObjective fromConfig(int index, ConfigurationSection section) {
        ObjectiveType type = ObjectiveType.valueOf(section.getString("type", "KILL").toUpperCase());
        String target = section.getString("target", "");
        int amount = section.getInt("amount", 1);
        String description = section.getString("description", "");

        QuestObjective objective = new QuestObjective(index, type, target, amount, description);

        // 位置相关参数
        objective.world = section.getString("world");
        objective.x = section.getDouble("x");
        objective.y = section.getDouble("y");
        objective.z = section.getDouble("z");
        objective.radius = section.getDouble("radius", 5.0);

        // 对话内容（TALK类型）
        if (section.contains("dialogue")) {
            objective.dialogue = section.getStringList("dialogue");
            System.out.println("[QuestObjective] 读取dialogue字段: " + objective.dialogue.size() + " 行");
        } else {
            System.out.println("[QuestObjective] 配置中没有dialogue字段!");
        }

        // 调试日志
        if (type == ObjectiveType.TALK) {
            System.out.println("[QuestObjective] 加载TALK目标: target=" + target + ", dialogue=" + objective.dialogue);
        }

        return objective;
    }
    
    // Getters
    public int getIndex() { return index; }
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }
    public List<String> getDialogue() { return dialogue; }

    // Setter
    public void setDialogue(List<String> dialogue) { this.dialogue = dialogue; }
    
    /**
     * 获取进度显示文本
     */
    public String getProgressText(int current) {
        return description + " (" + current + "/" + amount + ")";
    }
    
    /**
     * 检查是否完成
     */
    public boolean isComplete(int current) {
        return current >= amount;
    }
}