package cn.guangdian.quest.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务奖励
 */
public class QuestReward {
    
    private int points;                      // 点券奖励
    private int experience;                  // 经验奖励
    private Map<String, Integer> items;      // 物品奖励 (物品ID -> 数量)
    private List<String> commands;           // 命令奖励
    private List<String> messages;           // 完成消息
    
    public QuestReward() {
        this.items = new HashMap<>();
        this.commands = new ArrayList<>();
        this.messages = new ArrayList<>();
    }
    
    /**
     * 从配置加载奖励
     */
    public static QuestReward fromConfig(ConfigurationSection section) {
        QuestReward reward = new QuestReward();
        if (section == null) return reward;

        reward.points = section.getInt("points", 0);
        reward.experience = section.getInt("experience", 0);

        // 物品奖励 - 支持多种格式
        if (section.isList("items")) {
            List<?> itemsList = section.getList("items");
            if (itemsList != null) {
                for (Object item : itemsList) {
                    if (item instanceof String) {
                        // 简单字符串格式: "ItemId:数量" 或 "type:ItemId:数量"
                        parseSimpleItemFormat(reward, (String) item);
                    } else if (item instanceof Map) {
                        // Map格式: {type: xxx, id: xxx, amount: x}
                        parseMapItemFormat(reward, (Map<?, ?>) item);
                    }
                }
            }
        }

        // 命令奖励
        reward.commands = section.getStringList("commands");

        // 完成消息
        reward.messages = section.getStringList("messages");

        return reward;
    }

    /**
     * 解析简单字符串格式的物品
     * 支持格式:
     * - "ItemId:数量" (默认rpgitems)
     * - "type:ItemId:数量" (指定类型)
     * - "type:ItemId" (数量默认1)
     */
    private static void parseSimpleItemFormat(QuestReward reward, String itemStr) {
        String[] parts = itemStr.split(":");
        String type;
        String id;
        int amount = 1;

        if (parts.length == 1) {
            // 只有ID，默认rpgitems类型
            type = "rpgitems";
            id = parts[0];
        } else if (parts.length == 2) {
            // 可能是 "ItemId:数量" 或 "type:ItemId"
            try {
                amount = Integer.parseInt(parts[1]);
                type = "rpgitems";
                id = parts[0];
            } catch (NumberFormatException e) {
                // 第二部分不是数字，说明是 "type:ItemId"
                type = parts[0];
                id = parts[1];
                amount = 1;
            }
        } else {
            // "type:ItemId:数量"
            type = parts[0];
            id = parts[1];
            try {
                amount = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        String itemKey = type + ":" + id;
        reward.items.merge(itemKey, amount, Integer::sum);
    }

    /**
     * 解析Map格式的物品
     */
    private static void parseMapItemFormat(QuestReward reward, Map<?, ?> itemMap) {
        Object typeObj = itemMap.get("type");
        String type = typeObj != null ? typeObj.toString() : "rpgitems";

        Object idObj = itemMap.get("id");
        Object materialObj = itemMap.get("material");
        String id = idObj != null ? idObj.toString() :
                    (materialObj != null ? materialObj.toString() : "unknown");

        Object amountObj = itemMap.get("amount");
        int amount = amountObj != null ? ((Number) amountObj).intValue() : 1;

        String itemKey = type + ":" + id;
        reward.items.merge(itemKey, amount, Integer::sum);
    }
    
    // Getters
    public int getPoints() { return points; }
    public int getExperience() { return experience; }
    public Map<String, Integer> getItems() { return items; }
    public List<String> getCommands() { return commands; }
    public List<String> getMessages() { return messages; }
    
    // Setters
    public void setPoints(int points) { this.points = points; }
    public void setExperience(int experience) { this.experience = experience; }
    
    /**
     * 检查是否有奖励
     */
    public boolean hasRewards() {
        return points > 0 || experience > 0 || !items.isEmpty() || !commands.isEmpty();
    }
    
    /**
     * 获取奖励摘要
     */
    public String getSummary() {
        List<String> parts = new ArrayList<>();
        if (points > 0) parts.add("点券 x" + points);
        if (experience > 0) parts.add("经验 x" + experience);
        if (!items.isEmpty()) parts.add("物品 x" + items.size());
        return String.join(", ", parts);
    }
}