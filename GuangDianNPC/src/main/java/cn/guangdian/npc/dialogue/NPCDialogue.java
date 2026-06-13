package cn.guangdian.npc.dialogue;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC 对话定义
 */
public class NPCDialogue {

    private final String id;
    private final String npcId;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;
    private final String displayName;
    private final boolean useChatFrame;

    public NPCDialogue(String id, String npcId) {
        this.id = id;
        this.npcId = npcId;
        this.startNodeId = "start";
        this.nodes = new HashMap<>();
        this.displayName = null;
        this.useChatFrame = true;
    }

    public NPCDialogue(String id, String npcId, String startNodeId, String displayName, boolean useChatFrame) {
        this.id = id;
        this.npcId = npcId;
        this.startNodeId = startNodeId != null ? startNodeId : "start";
        this.nodes = new HashMap<>();
        this.displayName = displayName;
        this.useChatFrame = useChatFrame;
    }

    public String getId() {
        return id;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isUseChatFrame() {
        return useChatFrame;
    }

    public Map<String, DialogueNode> getNodes() {
        return nodes;
    }

    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }

    public void addNode(DialogueNode node) {
        if (node != null) {
            this.nodes.put(node.getId(), node);
        }
    }

    public boolean hasNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    public static NPCDialogue fromConfig(String id, ConfigurationSection section) {
        if (section == null) return null;

        String npcId = section.getString("npc_id", "");
        String startNode = section.getString("start_node", "start");
        String displayName = section.getString("display_name", null);
        boolean useChatFrame = section.getBoolean("use_chat_frame", true);

        NPCDialogue dialogue = new NPCDialogue(id, npcId, startNode, displayName, useChatFrame);

        ConfigurationSection nodesSection = section.getConfigurationSection("nodes");
        if (nodesSection != null) {
            for (String nodeId : nodesSection.getKeys(false)) {
                ConfigurationSection nodeSection = nodesSection.getConfigurationSection(nodeId);
                if (nodeSection != null) {
                    DialogueNode node = DialogueNode.fromConfig(nodeId, nodeSection);
                    if (node != null) {
                        dialogue.addNode(node);
                    }
                }
            }
        }

        return dialogue;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("npc_id", npcId);
        map.put("start_node", startNodeId);
        if (displayName != null) {
            map.put("display_name", displayName);
        }
        map.put("use_chat_frame", useChatFrame);

        Map<String, Object> nodesMap = new HashMap<>();
        for (DialogueNode node : nodes.values()) {
            nodesMap.put(node.getId(), node.serialize());
        }
        map.put("nodes", nodesMap);

        return map;
    }
}
