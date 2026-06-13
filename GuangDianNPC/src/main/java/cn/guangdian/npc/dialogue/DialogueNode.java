package cn.guangdian.npc.dialogue;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NPC 对话节点
 */
public class DialogueNode {

    private final String id;
    private final String npcText;
    private final List<DialogueOption> options;
    private final String action;
    private final boolean endDialogue;
    private final int autoContinueDelay;
    private final String autoContinueNode;

    public DialogueNode(String id, String npcText) {
        this.id = id;
        this.npcText = npcText;
        this.options = new ArrayList<>();
        this.action = null;
        this.endDialogue = false;
        this.autoContinueDelay = -1;
        this.autoContinueNode = null;
    }

    public DialogueNode(String id, String npcText, String action, boolean endDialogue, 
                        int autoContinueDelay, String autoContinueNode) {
        this.id = id;
        this.npcText = npcText;
        this.options = new ArrayList<>();
        this.action = action;
        this.endDialogue = endDialogue;
        this.autoContinueDelay = autoContinueDelay;
        this.autoContinueNode = autoContinueNode;
    }

    public String getId() {
        return id;
    }

    public String getNpcText() {
        return npcText;
    }

    public List<DialogueOption> getOptions() {
        return options;
    }

    public String getAction() {
        return action;
    }

    public boolean isEndDialogue() {
        return endDialogue;
    }

    public int getAutoContinueDelay() {
        return autoContinueDelay;
    }

    public String getAutoContinueNode() {
        return autoContinueNode;
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }

    public boolean isAutoContinue() {
        return autoContinueDelay > 0 && autoContinueNode != null;
    }

    public void addOption(DialogueOption option) {
        if (option != null) {
            this.options.add(option);
        }
    }

    public DialogueOption getOptionByIndex(int index) {
        if (index >= 0 && index < options.size()) {
            return options.get(index);
        }
        return null;
    }

    public static DialogueNode fromConfig(String id, ConfigurationSection section) {
        if (section == null) return null;

        String npcText = section.getString("text", "...");
        String action = section.getString("action", null);
        boolean endDialogue = section.getBoolean("end", false);
        int autoDelay = section.getInt("auto_continue.delay", -1);
        String autoNode = section.getString("auto_continue.next", null);

        DialogueNode node = new DialogueNode(id, npcText, action, endDialogue, autoDelay, autoNode);

        ConfigurationSection optionsSection = section.getConfigurationSection("options");
        if (optionsSection != null) {
            for (String optionId : optionsSection.getKeys(false)) {
                ConfigurationSection optionSection = optionsSection.getConfigurationSection(optionId);
                if (optionSection != null) {
                    DialogueOption option = DialogueOption.fromConfig(optionId, optionSection);
                    if (option != null) {
                        node.addOption(option);
                    }
                }
            }
        }

        return node;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", npcText);
        if (action != null) {
            map.put("action", action);
        }
        if (endDialogue) {
            map.put("end", true);
        }
        if (isAutoContinue()) {
            Map<String, Object> autoContinue = new HashMap<>();
            autoContinue.put("delay", autoContinueDelay);
            autoContinue.put("next", autoContinueNode);
            map.put("auto_continue", autoContinue);
        }
        if (!options.isEmpty()) {
            Map<String, Object> optionsMap = new HashMap<>();
            for (DialogueOption option : options) {
                optionsMap.put(option.getId(), option.serialize());
            }
            map.put("options", optionsMap);
        }
        return map;
    }
}
