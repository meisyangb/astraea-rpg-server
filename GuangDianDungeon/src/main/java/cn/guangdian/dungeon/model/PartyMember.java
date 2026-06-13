package cn.guangdian.dungeon.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyMember {

    private final UUID playerId;
    private boolean isLeader;
    private boolean ready;
    private long joinTime;

    public PartyMember(UUID playerId, boolean isLeader) {
        this.playerId = playerId;
        this.isLeader = isLeader;
        this.ready = isLeader;
        this.joinTime = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isLeader() { return isLeader; }
    public boolean isReady() { return ready; }
    public long getJoinTime() { return joinTime; }

    public void setLeader(boolean leader) { this.isLeader = leader; }
    public void setReady(boolean ready) { this.ready = ready; }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public String getName() {
        Player player = getPlayer();
        return player != null ? player.getName() : "Unknown";
    }
}
