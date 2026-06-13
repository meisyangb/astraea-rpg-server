package cn.guangdian.npc.dialogue;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 玩家对话状态
 */
public class PlayerDialogueState {

    private final UUID playerUUID;
    private final String dialogueId;
    private final String npcId;
    private String currentNodeId;
    private final long startTime;
    private boolean waitingForInput;

    public PlayerDialogueState(Player player, String dialogueId, String npcId, String startNodeId) {
        this.playerUUID = player.getUniqueId();
        this.dialogueId = dialogueId;
        this.npcId = npcId;
        this.currentNodeId = startNodeId;
        this.startTime = System.currentTimeMillis();
        this.waitingForInput = true;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    public void setWaitingForInput(boolean waitingForInput) {
        this.waitingForInput = waitingForInput;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }
}
