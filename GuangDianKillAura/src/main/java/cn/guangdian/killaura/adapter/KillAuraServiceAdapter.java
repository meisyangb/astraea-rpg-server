package cn.guangdian.killaura.adapter;

import cn.guangdian.killaura.GuangDianKillAura;
import cn.guangdian.killaura.api.KillAuraService;
import cn.guangdian.killaura.manager.AttackManager;
import cn.guangdian.killaura.model.TargetStrategy;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class KillAuraServiceAdapter implements KillAuraService {

    private final GuangDianKillAura plugin;
    private final AttackManager attackManager;
    private final boolean useRPGCore;

    public KillAuraServiceAdapter(GuangDianKillAura plugin, AttackManager attackManager) {
        this.plugin = plugin;
        this.attackManager = attackManager;
        this.useRPGCore = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                rpgCore.getServiceRegistry().registerService(KillAuraService.class, this);
                plugin.logInfo("已注册到 RPGCore: KillAuraService");
            } catch (Exception e) {
                plugin.logWarning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isKillAuraEnabled(UUID playerId) {
        return attackManager.isKillAuraEnabled(playerId);
    }

    @Override
    public void setKillAuraEnabled(UUID playerId, boolean enabled) {
        attackManager.setKillAuraEnabled(playerId, enabled);
    }

    @Override
    public boolean toggleKillAura(UUID playerId) {
        return attackManager.toggleKillAura(playerId);
    }

    @Override
    public LivingEntity getCurrentTarget(UUID playerId) {
        return attackManager.getCurrentTarget(playerId);
    }

    @Override
    public TargetStrategy getTargetStrategy(UUID playerId) {
        return attackManager.getTargetStrategy(playerId);
    }

    @Override
    public void setTargetStrategy(UUID playerId, TargetStrategy strategy) {
        attackManager.setTargetStrategy(playerId, strategy);
    }

    @Override
    public double getAttackRange(UUID playerId) {
        return attackManager.getAttackRange(playerId);
    }

    @Override
    public void setAttackRange(UUID playerId, double range) {
        attackManager.setAttackRange(playerId, range);
    }

    @Override
    public int getKillCount(UUID playerId) {
        return attackManager.getKillCount(playerId);
    }

    @Override
    public void resetKillCount(UUID playerId) {
        attackManager.resetKillCount(playerId);
    }

    @Override
    public double getTotalDamage(UUID playerId) {
        return attackManager.getTotalDamage(playerId);
    }

    @Override
    public int getActivePlayerCount() {
        return attackManager.getActivePlayerCount();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                rpgCore.getServiceRegistry().unregisterService(KillAuraService.class);
                plugin.logInfo("已从 RPGCore 注销: KillAuraService");
            } catch (Exception e) {
                plugin.logWarning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}
