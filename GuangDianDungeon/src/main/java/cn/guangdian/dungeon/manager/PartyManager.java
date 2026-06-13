package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.event.*;
import cn.guangdian.dungeon.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final GuangDianDungeon plugin;
    private final Map<UUID, DungeonParty> parties;
    private final Map<UUID, DungeonParty> playerPartyMap;
    private final Map<UUID, PartyInvite> pendingInvites;

    public PartyManager(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.parties = new ConcurrentHashMap<>();
        this.playerPartyMap = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
    }

    public DungeonParty createParty(Player leader, DungeonTemplate template) {
        if (playerPartyMap.containsKey(leader.getUniqueId())) {
            return null;
        }

        int maxPlayers = template.getSettings().getMaxPlayers();
        DungeonParty party = new DungeonParty(UUID.randomUUID(), leader, maxPlayers);

        parties.put(party.getPartyId(), party);
        playerPartyMap.put(leader.getUniqueId(), party);

        Bukkit.getPluginManager().callEvent(new PartyCreateEvent(party));

        plugin.sendMessage(leader, "party-created");

        return party;
    }

    public void invitePlayer(Player inviter, Player target) {
        DungeonParty party = getPlayerParty(inviter).orElse(null);
        if (party == null) {
            plugin.sendMessage(inviter, "not-in-party");
            return;
        }

        if (!party.isLeader(inviter)) {
            plugin.sendMessage(inviter, "not-leader");
            return;
        }

        if (party.isMember(target)) {
            return;
        }

        if (isInParty(target)) {
            return;
        }

        PartyInvite invite = new PartyInvite(
            party.getPartyId(),
            inviter.getUniqueId(),
            target.getUniqueId(),
            System.currentTimeMillis() + 60000
        );

        pendingInvites.put(target.getUniqueId(), invite);

        plugin.sendMessage(target, "party-invite-received", "{player}", inviter.getName());
        plugin.sendMessage(inviter, "party-invite-sent", "{player}", target.getName());
    }

    public void acceptInvite(Player player) {
        PartyInvite invite = pendingInvites.remove(player.getUniqueId());

        if (invite == null || invite.isExpired()) {
            plugin.sendMessage(player, "party-invite-expired");
            return;
        }

        DungeonParty party = parties.get(invite.getPartyId());
        if (party == null || party.isFull()) {
            plugin.sendMessage(player, "party-full");
            return;
        }

        party.addMember(player);
        playerPartyMap.put(player.getUniqueId(), party);

        Bukkit.getPluginManager().callEvent(new PartyJoinEvent(party, player));

        party.broadcast(plugin.color("<green>" + player.getName() + " 加入了队伍"));
    }

    public void declineInvite(Player player) {
        pendingInvites.remove(player.getUniqueId());
    }

    public void leaveParty(Player player) {
        DungeonParty party = getPlayerParty(player).orElse(null);
        if (party == null) {
            plugin.sendMessage(player, "not-in-party");
            return;
        }

        boolean wasLeader = party.isLeader(player);
        party.removeMember(player);
        playerPartyMap.remove(player.getUniqueId());

        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, player));

        if (party.isEmpty()) {
            disbandParty(party);
        } else if (wasLeader) {
            Player newLeader = party.getMembers().get(0).getPlayer();
            if (newLeader != null) {
                party.setLeader(newLeader);
                party.broadcast(plugin.color("<yellow>新队长: " + newLeader.getName()));
            }
        }

        plugin.sendMessage(player, "party-left");
    }

    public void kickPlayer(Player leader, Player target) {
        DungeonParty party = getPlayerParty(leader).orElse(null);
        if (party == null || !party.isLeader(leader)) {
            plugin.sendMessage(leader, "not-leader");
            return;
        }

        if (!party.isMember(target)) {
            return;
        }

        party.removeMember(target);
        playerPartyMap.remove(target.getUniqueId());

        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, target));

        plugin.sendMessage(target, "party-kicked");
        party.broadcast(plugin.color("<yellow>" + target.getName() + " 被踢出队伍"));
    }

    public void transferLeader(Player currentLeader, Player newLeader) {
        DungeonParty party = getPlayerParty(currentLeader).orElse(null);
        if (party == null || !party.isLeader(currentLeader)) {
            plugin.sendMessage(currentLeader, "not-leader");
            return;
        }

        if (!party.isMember(newLeader)) {
            return;
        }

        PartyMember currentMember = party.getMember(currentLeader.getUniqueId());
        PartyMember newMember = party.getMember(newLeader.getUniqueId());

        if (currentMember != null) currentMember.setLeader(false);
        if (newMember != null) newMember.setLeader(true);

        party.setLeader(newLeader);

        party.broadcast(plugin.color("<yellow>队长已转让给 " + newLeader.getName()));
    }

    public void disbandParty(DungeonParty party) {
        Bukkit.getPluginManager().callEvent(new PartyDisbandEvent(party));

        // 如果队伍在副本中，清理副本状态
        if (party.isInDungeon() && party.getActiveSessionId() != null) {
            var session = plugin.getSessionManager().getSession(party.getActiveSessionId());
            if (session != null) {
                plugin.getSessionManager().endSession(session, false);
            }
        }

        for (PartyMember member : party.getMembers()) {
            playerPartyMap.remove(member.getPlayerId());
            Player player = member.getPlayer();
            if (player != null) {
                plugin.sendMessage(player, "party-disbanded");
            }
        }

        party.setActiveSessionId(null);
        parties.remove(party.getPartyId());
    }

    public Optional<DungeonParty> getParty(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }

    public Optional<DungeonParty> getPlayerParty(Player player) {
        return Optional.ofNullable(playerPartyMap.get(player.getUniqueId()));
    }

    public Optional<PartyInvite> getPendingInvite(Player player) {
        return Optional.ofNullable(pendingInvites.get(player.getUniqueId()));
    }

    public boolean isInParty(Player player) {
        return playerPartyMap.containsKey(player.getUniqueId());
    }

    public boolean isPartyLeader(Player player) {
        DungeonParty party = playerPartyMap.get(player.getUniqueId());
        return party != null && party.isLeader(player);
    }

    public Collection<DungeonParty> getAllParties() {
        return Collections.unmodifiableCollection(parties.values());
    }

    public void cleanupAll() {
        for (DungeonParty party : new ArrayList<>(parties.values())) {
            try {
                disbandParty(party);
            } catch (Exception e) {
                plugin.getLogger().warning("清理队伍失败: " + e.getMessage());
            }
        }
    }

    public static class PartyInvite {
        private final UUID partyId;
        private final UUID inviter;
        private final UUID target;
        private final long expireTime;

        public PartyInvite(UUID partyId, UUID inviter, UUID target, long expireTime) {
            this.partyId = partyId;
            this.inviter = inviter;
            this.target = target;
            this.expireTime = expireTime;
        }

        public UUID getPartyId() { return partyId; }
        public UUID getInviter() { return inviter; }
        public UUID getTarget() { return target; }
        public long getExpireTime() { return expireTime; }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
