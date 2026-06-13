package cn.guangdian.mobs.aggro.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface AggroService {

    double getAggro(LivingEntity entity, Player player);

    void addAggro(LivingEntity entity, Player player, double amount);

    void setAggro(LivingEntity entity, Player player, double amount);

    void removeAggro(LivingEntity entity, Player player);

    void clearAggro(LivingEntity entity);

    void clearAllAggro(Player player);

    Player getTopAggroTarget(LivingEntity entity);

    Map<UUID, Double> getAllAggro(LivingEntity entity);

    void transferAggro(LivingEntity entity, Player from, Player to, double percentage);

    boolean hasAggro(LivingEntity entity, Player player);

    int getAggroRank(LivingEntity entity, Player player);

    double getTotalAggro(LivingEntity entity);

    void setAggroDecay(LivingEntity entity, double decayRate);

    void forceTarget(LivingEntity entity, Player target);

    boolean isAvailable();
}
