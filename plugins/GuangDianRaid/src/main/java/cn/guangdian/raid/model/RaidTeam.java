package cn.guangdian.raid.model;

import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class RaidTeam {

    private final UUID teamId;
    private final Map<UUID, RaidPlayer> members;
    private UUID leaderId;
    private int maxRevives;
    private int usedRevives;

    public RaidTeam(Player leader) {
        this.teamId = UUID.randomUUID();
        this.members = new HashMap<>();
        this.leaderId = leader.getUniqueId();
        this.maxRevives = 3;
        this.usedRevives = 0;
        addMember(leader);
    }

    public void addMember(Player player) {
        members.put(player.getUniqueId(), new RaidPlayer(player));
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public RaidPlayer getMember(UUID playerId) {
        return members.get(playerId);
    }

    public Collection<RaidPlayer> getMembers() {
        return members.values();
    }

    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (RaidPlayer rp : members.values()) {
            Player player = rp.getPlayer();
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public int size() {
        return members.size();
    }

    public boolean isFull(int maxSize) {
        return members.size() >= maxSize;
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public boolean canRevive() {
        return usedRevives < maxRevives;
    }

    public void useRevive() {
        usedRevives++;
    }

    public int getRemainingRevives() {
        return maxRevives - usedRevives;
    }

    public void broadcastMessage(Component message) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        MiniMessageService miniMessage = MiniMessageService.getInstance();

        Component titleComp = miniMessage.colorize(title);
        Component subtitleComp = miniMessage.colorize(subtitle);

        Title.Times times = Title.Times.times(
            Duration.ofMillis((long) fadeIn * 50),
            Duration.ofMillis((long) stay * 50),
            Duration.ofMillis((long) fadeOut * 50)
        );

        Title titleObj = Title.title(titleComp, subtitleComp, times);

        for (Player player : getOnlinePlayers()) {
            player.showTitle(titleObj);
        }
    }

    public void broadcastSound(org.bukkit.Sound sound, float volume, float pitch) {
        for (Player player : getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public UUID getTeamId() { return teamId; }
    public UUID getLeaderId() { return leaderId; }
    public int getMaxRevives() { return maxRevives; }
    public int getUsedRevives() { return usedRevives; }
}
