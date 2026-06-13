package cn.guangdian.mobs.aggro.adapter;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.aggro.api.AggroService;
import cn.guangdian.mobs.aggro.manager.AggroManager;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class AggroServiceAdapter implements AggroService {

    private final GuangDianMobs plugin;
    private final AggroManager aggroManager;

    public AggroServiceAdapter(GuangDianMobs plugin, AggroManager aggroManager) {
        this.plugin = plugin;
        this.aggroManager = aggroManager;

        RPGCore rpgCore = plugin.getRPGCore();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(AggroService.class, this);
        }
    }

    public void unregister() {
        RPGCore rpgCore = plugin.getRPGCore();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(AggroService.class);
        }
    }

    @Override
    public double getAggro(LivingEntity entity, Player player) {
        return aggroManager.getAggro(entity, player);
    }

    @Override
    public void addAggro(LivingEntity entity, Player player, double amount) {
        aggroManager.addAggro(entity, player, amount);
    }

    @Override
    public void setAggro(LivingEntity entity, Player player, double amount) {
        aggroManager.setAggro(entity, player, amount);
    }

    @Override
    public void removeAggro(LivingEntity entity, Player player) {
        aggroManager.removeAggro(entity, player);
    }

    @Override
    public void clearAggro(LivingEntity entity) {
        aggroManager.clearAggro(entity);
    }

    @Override
    public void clearAllAggro(Player player) {
        aggroManager.clearAllAggro(player);
    }

    @Override
    public Player getTopAggroTarget(LivingEntity entity) {
        return aggroManager.getTopAggroTarget(entity);
    }

    @Override
    public Map<UUID, Double> getAllAggro(LivingEntity entity) {
        return aggroManager.getAllAggro(entity);
    }

    @Override
    public void transferAggro(LivingEntity entity, Player from, Player to, double percentage) {
        aggroManager.transferAggro(entity, from, to, percentage);
    }

    @Override
    public boolean hasAggro(LivingEntity entity, Player player) {
        return aggroManager.hasAggro(entity, player);
    }

    @Override
    public int getAggroRank(LivingEntity entity, Player player) {
        return aggroManager.getAggroRank(entity, player);
    }

    @Override
    public double getTotalAggro(LivingEntity entity) {
        return aggroManager.getTotalAggro(entity);
    }

    @Override
    public void setAggroDecay(LivingEntity entity, double decayRate) {
        aggroManager.setAggroDecay(entity, decayRate);
    }

    @Override
    public void forceTarget(LivingEntity entity, Player target) {
        aggroManager.forceTarget(entity, target);
    }

    @Override
    public boolean isAvailable() {
        return aggroManager.isAvailable();
    }
}
