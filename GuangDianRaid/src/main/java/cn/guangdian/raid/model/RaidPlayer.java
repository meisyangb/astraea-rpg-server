package cn.guangdian.raid.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidPlayer {

    private final UUID playerId;
    private String playerName;
    private RaidPlayerState state;
    private int kills;
    private int intelCollected;
    private int damageDealt;
    private int damageTaken;
    private int revivesUsed;

    private Location joinLocation;
    private Map<String, Object> metadata;

    public RaidPlayer(Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.state = RaidPlayerState.ALIVE;
        this.kills = 0;
        this.intelCollected = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        this.revivesUsed = 0;
        this.joinLocation = player.getLocation().clone();
        this.metadata = new HashMap<>();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public boolean isAlive() {
        return state == RaidPlayerState.ALIVE;
    }

    public boolean isDead() {
        return state == RaidPlayerState.DEAD;
    }

    public void addKill() {
        kills++;
    }

    public void addIntel(int value) {
        intelCollected += value;
    }

    public void addDamageDealt(int damage) {
        damageDealt += damage;
    }

    public void addDamageTaken(int damage) {
        damageTaken += damage;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public RaidPlayerState getState() { return state; }
    public int getKills() { return kills; }
    public int getIntelCollected() { return intelCollected; }
    public int getDamageDealt() { return damageDealt; }
    public int getDamageTaken() { return damageTaken; }
    public int getRevivesUsed() { return revivesUsed; }
    public Location getJoinLocation() { return joinLocation; }

    public void setState(RaidPlayerState state) { this.state = state; }
    public void setRevivesUsed(int revivesUsed) { this.revivesUsed = revivesUsed; }
}
