package cn.guangdian.killaura.manager;

import cn.guangdian.killaura.GuangDianKillAura;
import cn.guangdian.killaura.config.KillAuraConfig;
import cn.guangdian.killaura.model.AttackResult;
import cn.guangdian.killaura.model.KillAuraProfile;
import cn.guangdian.killaura.model.TargetStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttackManager {

    private final GuangDianKillAura plugin;
    private final TargetSelector targetSelector;
    private final Map<UUID, KillAuraProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> currentTargets = new ConcurrentHashMap<>();

    private long attackTaskId = -1;

    public AttackManager(GuangDianKillAura plugin) {
        this.plugin = plugin;
        this.targetSelector = new TargetSelector(plugin);
    }

    public void startAttackTask() {
        if (schedulerUnavailable()) {
            plugin.logWarning("RPGCore 调度器不可用，自动攻击任务未启动");
            return;
        }

        attackTaskId = plugin.getScheduler().runSyncRepeating(this::tickAttack, 1L, 1L);
        plugin.logInfo("自动攻击任务已启动，任务ID: " + attackTaskId);
    }

    public void stopAttackTask() {
        if (attackTaskId != -1 && !schedulerUnavailable()) {
            plugin.getScheduler().cancelTask(attackTaskId);
            plugin.logInfo("自动攻击任务已停止");
            attackTaskId = -1;
        }
    }

    private void tickAttack() {
        KillAuraConfig config = plugin.getKillAuraConfig();
        if (!config.isGlobalEnabled()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            KillAuraProfile profile = profiles.get(player.getUniqueId());
            if (profile == null || !profile.isEnabled()) {
                currentTargets.remove(player.getUniqueId());
                continue;
            }

            if (!player.isOnline() || player.isDead()) {
                continue;
            }

            if (!isAttackReady(profile)) {
                continue;
            }

            LivingEntity target = targetSelector.selectTarget(player, profile.getStrategy());
            if (target == null) {
                currentTargets.remove(player.getUniqueId());
                continue;
            }

            currentTargets.put(player.getUniqueId(), target);

            AttackResult result = performAttack(player, target, profile, config);
            if (result.success()) {
                profile.setLastAttackTime(System.currentTimeMillis());
                profile.addDamage(result.damage());

                if (result.killed()) {
                    profile.incrementKillCount();
                }
            }
        }
    }

    private boolean isAttackReady(KillAuraProfile profile) {
        long elapsed = System.currentTimeMillis() - profile.getLastAttackTime();
        long intervalMs = profile.getAttackIntervalTicks() * 50L;
        return elapsed >= intervalMs;
    }

    private AttackResult performAttack(Player player, LivingEntity target, KillAuraProfile profile, KillAuraConfig config) {
        double distance = player.getLocation().distance(target.getLocation());
        if (distance > profile.getAttackRange()) {
            return AttackResult.fail(AttackResult.AttackFailReason.OUT_OF_RANGE);
        }

        if (target.isDead()) {
            return AttackResult.fail(AttackResult.AttackFailReason.TARGET_DEAD);
        }

        if (target.isInvulnerable()) {
            return AttackResult.fail(AttackResult.AttackFailReason.TARGET_INVULNERABLE);
        }

        double damage = calculateDamage(player, target, config);

        if (config.isSwingAnimation()) {
            player.swingMainHand();
        }

        target.damage(damage, player);

        if (config.isDamageIndicator()) {
            playAttackEffects(player, target);
        }

        boolean killed = target.isDead() || target.getHealth() <= 0;
        return AttackResult.success(target, damage, killed);
    }

    private double calculateDamage(Player player, LivingEntity target, KillAuraConfig config) {
        if (config.isUsePlayerDamage()) {
            double attackDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
            return Math.max(0.5, attackDamage);
        }
        return config.getBaseDamage();
    }

    private void playAttackEffects(Player player, LivingEntity target) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 1.2f);
        } catch (Exception ignored) {
        }
    }

    public KillAuraProfile getOrCreateProfile(UUID playerId) {
        return profiles.computeIfAbsent(playerId,
            id -> new KillAuraProfile(id, plugin.getKillAuraConfig().getDefaultAttackRange(),
                plugin.getKillAuraConfig().getDefaultAttackIntervalTicks()));
    }

    public KillAuraProfile getProfile(UUID playerId) {
        return profiles.get(playerId);
    }

    public void removeProfile(UUID playerId) {
        profiles.remove(playerId);
        currentTargets.remove(playerId);
    }

    public boolean isKillAuraEnabled(UUID playerId) {
        KillAuraProfile profile = profiles.get(playerId);
        return profile != null && profile.isEnabled();
    }

    public boolean toggleKillAura(UUID playerId) {
        KillAuraProfile profile = getOrCreateProfile(playerId);
        profile.setEnabled(!profile.isEnabled());
        if (!profile.isEnabled()) {
            currentTargets.remove(playerId);
        }
        return profile.isEnabled();
    }

    public void setKillAuraEnabled(UUID playerId, boolean enabled) {
        KillAuraProfile profile = getOrCreateProfile(playerId);
        profile.setEnabled(enabled);
        if (!enabled) {
            currentTargets.remove(playerId);
        }
    }

    public LivingEntity getCurrentTarget(UUID playerId) {
        return currentTargets.get(playerId);
    }

    public TargetStrategy getTargetStrategy(UUID playerId) {
        KillAuraProfile profile = getProfile(playerId);
        return profile != null ? profile.getStrategy() : TargetStrategy.NEAREST;
    }

    public void setTargetStrategy(UUID playerId, TargetStrategy strategy) {
        getOrCreateProfile(playerId).setStrategy(strategy);
    }

    public double getAttackRange(UUID playerId) {
        KillAuraProfile profile = getProfile(playerId);
        return profile != null ? profile.getAttackRange() : plugin.getKillAuraConfig().getDefaultAttackRange();
    }

    public void setAttackRange(UUID playerId, double range) {
        double maxRange = plugin.getKillAuraConfig().getMaxAttackRange();
        getOrCreateProfile(playerId).setAttackRange(Math.min(range, maxRange));
    }

    public int getKillCount(UUID playerId) {
        KillAuraProfile profile = getProfile(playerId);
        return profile != null ? profile.getKillCount() : 0;
    }

    public void resetKillCount(UUID playerId) {
        KillAuraProfile profile = getProfile(playerId);
        if (profile != null) {
            profile.resetStats();
        }
    }

    public double getTotalDamage(UUID playerId) {
        KillAuraProfile profile = getProfile(playerId);
        return profile != null ? profile.getTotalDamage() : 0;
    }

    public int getActivePlayerCount() {
        return (int) profiles.values().stream().filter(KillAuraProfile::isEnabled).count();
    }

    public void clearAll() {
        profiles.clear();
        currentTargets.clear();
    }

    private boolean schedulerUnavailable() {
        return plugin.getScheduler() == null;
    }
}
