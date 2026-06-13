package cn.guangdian.raid.api;

import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.Raid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RaidService {

    Optional<RaidInstance> getPlayerRaid(UUID playerId);

    boolean isInRaid(UUID playerId);

    Optional<RaidInstance> getInstance(String instanceId);

    void startRaid(String raidId, List<org.bukkit.entity.Player> players);

    void forceEndRaid(String instanceId);

    Optional<Raid> getRaidDefinition(String raidId);

    List<String> getAvailableRaids();

    int getActiveRaidCount();
}
