package cn.guangdian.npc.dialogue;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC 对话选项
 */
public class DialogueOption {

    private final String id;
    private final String text;
    private final String nextNodeId;
    private final String action;
    private final Map<String, Object> conditions;

    public DialogueOption(String id, String text, String nextNodeId, String action) {
        this.id = id;
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
        this.conditions = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void addCondition(String key, Object value) {
        this.conditions.put(key, value);
    }

    public boolean hasNextNode() {
        return nextNodeId != null && !nextNodeId.isEmpty();
    }

    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }

    public static DialogueOption fromConfig(String id, ConfigurationSection section) {
        if (section == null) return null;

        String text = section.getString("text", "选项");
        String nextNode = section.getString("next", null);
        String action = section.getString("action", null);

        DialogueOption option = new DialogueOption(id, text, nextNode, action);

        ConfigurationSection conditionsSection = section.getConfigurationSection("conditions");
        if (conditionsSection != null) {
            for (String key : conditionsSection.getKeys(false)) {
                option.addCondition(key, conditionsSection.get(key));
            }
        }

        return option;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        if (nextNodeId != null) {
            map.put("next", nextNodeId);
        }
        if (action != null) {
            map.put("action", action);
        }
        if (!conditions.isEmpty()) {
            map.put("conditions", conditions);
        }
        return map;
    }
}
