package cn.guangdian.raid.adapter;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.api.RaidService;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.Raid;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RaidServiceAdapter implements RaidService {

    private final GuangDianRaid plugin;

    public RaidServiceAdapter(GuangDianRaid plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(RaidService.class, this);
        }
    }

    @Override
    public Optional<RaidInstance> getPlayerRaid(UUID playerId) {
        if (plugin.getInstanceManager() == null) return Optional.empty();
        return plugin.getInstanceManager().getPlayerInstance(playerId);
    }

    @Override
    public boolean isInRaid(UUID playerId) {
        return getPlayerRaid(playerId).isPresent();
    }

    @Override
    public Optional<RaidInstance> getInstance(String instanceId) {
        if (plugin.getInstanceManager() == null) return Optional.empty();
        return Optional.ofNullable(plugin.getInstanceManager().getInstance(instanceId));
    }

    @Override
    public void startRaid(String raidId, List<Player> players) {
        if (plugin.getInstanceManager() == null) return;
        plugin.getInstanceManager().createInstance(raidId, players);
    }

    @Override
    public void forceEndRaid(String instanceId) {
        if (plugin.getInstanceManager() == null) return;
        RaidInstance instance = plugin.getInstanceManager().getInstance(instanceId);
        if (instance != null) {
            instance.fail("强制结束");
        }
    }

    @Override
    public Optional<Raid> getRaidDefinition(String raidId) {
        if (plugin.getConfigManager() == null) return Optional.empty();
        return Optional.ofNullable(plugin.getConfigManager().getRaid(raidId));
    }

    @Override
    public List<String> getAvailableRaids() {
        if (plugin.getConfigManager() == null) return List.of();
        return plugin.getConfigManager().getRaidIds();
    }

    @Override
    public int getActiveRaidCount() {
        if (plugin.getInstanceManager() == null) return 0;
        return plugin.getInstanceManager().getActiveCount();
    }

    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(RaidService.class);
        }
    }
}
