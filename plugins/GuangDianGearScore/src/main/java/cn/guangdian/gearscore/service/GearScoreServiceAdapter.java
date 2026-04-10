package cn.guangdian.gearscore.service;

import cn.guangdian.gearscore.GuangDianGearScore;
import cn.guangdian.gearscore.api.GearScoreService;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GearScoreServiceAdapter implements GearScoreService {

    private final GuangDianGearScore plugin;
    private boolean usingRPGCore = false;

    public GearScoreServiceAdapter(GuangDianGearScore plugin) {
        this.plugin = plugin;
        
        if (RPGCore.getInstance() != null) {
            RPGCore.getInstance().getServiceRegistry().registerService(GearScoreService.class, this);
            usingRPGCore = true;
            plugin.getLogger().info("已注册到 RPGCore ServiceRegistry");
        }
    }

    public void unregister() {
        if (usingRPGCore && RPGCore.getInstance() != null) {
            RPGCore.getInstance().getServiceRegistry().unregisterService(GearScoreService.class);
            plugin.getLogger().info("已从 RPGCore ServiceRegistry 注销");
        }
    }

    public boolean isUsingRPGCore() {
        return usingRPGCore;
    }

    @Override
    public long getPlayerScore(UUID uuid) {
        return plugin.getPlayerScore(uuid);
    }

    @Override
    public long getPlayerScore(Player player) {
        return plugin.getPlayerScore(player);
    }

    @Override
    public int getPlayerRank(UUID uuid) {
        return plugin.getPlayerRank(uuid);
    }

    @Override
    public List<Map.Entry<UUID, Long>> getTopPlayers(int count) {
        return plugin.getTopPlayers(count);
    }

    @Override
    public String getTopPlayerName(int index) {
        return plugin.getTopPlayerName(index);
    }

    @Override
    public long getTopPlayerScore(int index) {
        return plugin.getTopPlayerScore(index);
    }

    @Override
    public void updatePlayerScore(Player player) {
        plugin.updatePlayerScore(player);
    }
}
