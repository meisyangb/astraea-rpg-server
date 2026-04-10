package cn.guangdian.raid.instance;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.model.Raid;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RaidInstanceManager {

    private final GuangDianRaid plugin;
    private final Map<String, RaidInstance> instances;
    private final Map<UUID, String> playerInstances;
    private int instanceCounter;

    public RaidInstanceManager(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.instances = new ConcurrentHashMap<>();
        this.playerInstances = new ConcurrentHashMap<>();
        this.instanceCounter = 0;
    }

    public RaidInstance createInstance(String raidId, List<Player> players) {
        if (players == null || players.isEmpty()) {
            return null;
        }

        Raid raid = plugin.getConfigManager().getRaid(raidId);
        if (raid == null) {
            players.get(0).sendMessage("§c副本不存在: " + raidId);
            return null;
        }

        for (Player player : players) {
            if (isPlayerInRaid(player.getUniqueId())) {
                players.get(0).sendMessage("§c玩家 " + player.getName() + " 已在其他副本中");
                return null;
            }
        }

        if (players.size() < raid.getMinPlayers()) {
            players.get(0).sendMessage("§c人数不足，最少需要 " + raid.getMinPlayers() + " 人");
            return null;
        }

        if (players.size() > raid.getMaxPlayers()) {
            players.get(0).sendMessage("§c人数过多，最多允许 " + raid.getMaxPlayers() + " 人");
            return null;
        }

        String instanceId = generateInstanceId(raidId);
        RaidInstance instance = new RaidInstance(instanceId, raid, players, plugin);

        instances.put(instanceId, instance);
        for (Player player : players) {
            playerInstances.put(player.getUniqueId(), instanceId);
        }

        instance.start();

        return instance;
    }

    private String generateInstanceId(String raidId) {
        return raidId + "_" + (++instanceCounter) + "_" + System.currentTimeMillis();
    }

    public Optional<RaidInstance> getPlayerInstance(UUID playerId) {
        String instanceId = playerInstances.get(playerId);
        if (instanceId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(instances.get(instanceId));
    }

    public RaidInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }

    public boolean isPlayerInRaid(UUID playerId) {
        return playerInstances.containsKey(playerId);
    }

    public void removeInstance(String instanceId) {
        RaidInstance instance = instances.remove(instanceId);
        if (instance != null) {
            for (UUID playerId : instance.getTeam().getMembers().stream()
                    .map(p -> p.getPlayerId()).toList()) {
                playerInstances.remove(playerId);
            }
        }
    }

    public void removePlayer(UUID playerId) {
        String instanceId = playerInstances.remove(playerId);
        if (instanceId != null) {
            RaidInstance instance = instances.get(instanceId);
            if (instance != null) {
                instance.getTeam().removeMember(playerId);
            }
        }
    }

    public int getActiveCount() {
        return instances.size();
    }

    public Collection<RaidInstance> getAllInstances() {
        return instances.values();
    }

    public void shutdownAll() {
        for (RaidInstance instance : instances.values()) {
            instance.shutdown();
        }
        instances.clear();
        playerInstances.clear();
    }
}
