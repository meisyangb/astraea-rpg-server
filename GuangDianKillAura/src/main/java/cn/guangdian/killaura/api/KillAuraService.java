package cn.guangdian.killaura.api;

import cn.guangdian.killaura.model.TargetStrategy;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public interface KillAuraService {

    boolean isKillAuraEnabled(UUID playerId);

    void setKillAuraEnabled(UUID playerId, boolean enabled);

    boolean toggleKillAura(UUID playerId);

    LivingEntity getCurrentTarget(UUID playerId);

    TargetStrategy getTargetStrategy(UUID playerId);

    void setTargetStrategy(UUID playerId, TargetStrategy strategy);

    double getAttackRange(UUID playerId);

    void setAttackRange(UUID playerId, double range);

    int getKillCount(UUID playerId);

    void resetKillCount(UUID playerId);

    double getTotalDamage(UUID playerId);

    int getActivePlayerCount();

    boolean isAvailable();
}
