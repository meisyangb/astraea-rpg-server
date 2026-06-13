package cn.guangdian.dungeon.model;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonParty {

    private final UUID partyId;
    private Player leader;
    private final List<PartyMember> members;
    private final int maxMembers;
    private PartyState state;
    private String activeSessionId;

    public DungeonParty(UUID partyId, Player leader, int maxMembers) {
        this.partyId = partyId;
        this.leader = leader;
        this.members = new ArrayList<>();
        this.maxMembers = maxMembers;
        this.state = PartyState.CREATED;
        this.activeSessionId = null;
        this.members.add(new PartyMember(leader.getUniqueId(), true));
    }

    public UUID getPartyId() { return partyId; }
    public Player getLeader() { return leader; }
    public List<PartyMember> getMembers() { return members; }
    public int getMemberCount() { return members.size(); }
    public int getMaxMembers() { return maxMembers; }
    public PartyState getState() { return state; }
    public String getActiveSessionId() { return activeSessionId; }

    public void setLeader(Player leader) { this.leader = leader; }
    public void setState(PartyState state) { this.state = state; }
    public void setActiveSessionId(String sessionId) { this.activeSessionId = sessionId; }

    public boolean isInDungeon() {
        return activeSessionId != null && state == PartyState.IN_DUNGEON;
    }

    public boolean isLeader(Player player) {
        return leader.getUniqueId().equals(player.getUniqueId());
    }

    public boolean isMember(Player player) {
        return members.stream().anyMatch(m -> m.getPlayerId().equals(player.getUniqueId()));
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public void addMember(Player player) {
        if (!isMember(player) && !isFull()) {
            members.add(new PartyMember(player.getUniqueId(), false));
        }
    }

    public void removeMember(Player player) {
        members.removeIf(m -> m.getPlayerId().equals(player.getUniqueId()));
    }

    public PartyMember getMember(UUID playerId) {
        return members.stream()
            .filter(m -> m.getPlayerId().equals(playerId))
            .findFirst()
            .orElse(null);
    }

    public List<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>();
        for (PartyMember member : members) {
            Player player = member.getPlayer();
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    public void broadcast(net.kyori.adventure.text.Component message) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public boolean isReady() {
        return members.stream().allMatch(PartyMember::isReady);
    }

    public void setReady(Player player, boolean ready) {
        PartyMember member = getMember(player.getUniqueId());
        if (member != null) {
            member.setReady(ready);
        }
    }
}
