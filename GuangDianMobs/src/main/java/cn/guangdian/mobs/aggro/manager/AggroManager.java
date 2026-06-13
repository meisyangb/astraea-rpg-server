package cn.guangdian.mobs.aggro.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.aggro.api.AggroService;
import cn.guangdian.mobs.aggro.hook.MythicMobsHook;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仇恨管理器
 *
 * <p>已优化：使用 RPGCore SyncScheduler 进行任务调度</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class AggroManager implements AggroService {

    private final GuangDianMobs plugin;
    private final MythicMobsHook mythicMobsHook;

    private final Map<UUID, Map<UUID, AggroEntry>> aggroTable = new ConcurrentHashMap<>();
    private final Map<UUID, Double> decayRates = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private double defaultDecayRate = 0.05;
    private long decayInterval = 60;
    private double minAggroThreshold = 1.0;
    private double damageAggroMultiplier = 1.0;
    private double healAggroMultiplier = 0.5;
    private double distanceAggroMultiplier = 0.1;
    private int maxAggroTargets = 10;
    private boolean mythicMobsOnly = false;

    private long decayTaskId = -1;

    public AggroManager(GuangDianMobs plugin, MythicMobsHook mythicMobsHook) {
        this.plugin = plugin;
        this.mythicMobsHook = mythicMobsHook;
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("aggro.enabled", true);
        defaultDecayRate = config.getDouble("aggro.decay.rate", 0.05);
        decayInterval = config.getLong("aggro.decay.interval", 60);
        minAggroThreshold = config.getDouble("aggro.threshold.min", 1.0);
        damageAggroMultiplier = config.getDouble("aggro.multiplier.damage", 1.0);
        healAggroMultiplier = config.getDouble("aggro.multiplier.heal", 0.5);
        distanceAggroMultiplier = config.getDouble("aggro.multiplier.distance", 0.1);
        maxAggroTargets = config.getInt("aggro.limits.max_targets", 10);
        mythicMobsOnly = config.getBoolean("aggro.mythicmobs.only", false);

        startDecayTask();

        plugin.getLogger().info("仇恨系统配置已加载");
        plugin.getLogger().info("衰减间隔: " + decayInterval + "秒");
        plugin.getLogger().info("MythicMobs 专属模式: " + (mythicMobsOnly ? "开启" : "关闭"));
    }

    private void startDecayTask() {
        RPGCore rpgCore = plugin.getRPGCore();
        if (rpgCore == null || rpgCore.getScheduler() == null) {
            plugin.getLogger().warning("RPGCore 调度器不可用，仇恨衰减任务未启动");
            return;
        }

        decayTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            if (!enabled) return;

            long now = System.currentTimeMillis();
            int decayedCount = 0;
            int clearedCount = 0;

            for (Map.Entry<UUID, Map<UUID, AggroEntry>> entry : aggroTable.entrySet()) {
                UUID entityId = entry.getKey();
                Map<UUID, AggroEntry> targets = entry.getValue();

                double decayRate = decayRates.getOrDefault(entityId, defaultDecayRate);

                boolean removed = targets.entrySet().removeIf(targetEntry -> {
                    AggroEntry aggroEntry = targetEntry.getValue();
                    long elapsed = now - aggroEntry.lastUpdate;

                    if (elapsed > decayInterval * 1000) {
                        double decay = aggroEntry.aggro * decayRate * (elapsed / 1000.0 / decayInterval);
                        aggroEntry.aggro -= decay;

                        if (aggroEntry.aggro < minAggroThreshold) {
                            return true;
                        }
                        aggroEntry.lastUpdate = now;
                    }
                    return false;
                });

                if (removed) {
                    decayedCount++;
                }

                if (targets.isEmpty()) {
                    aggroTable.remove(entityId);
                    decayRates.remove(entityId);
                    clearedCount++;
                }
            }

            if (decayedCount > 0 || clearedCount > 0) {
                plugin.getLogger().fine("仇恨衰减: " + decayedCount + " 个目标衰减, " + clearedCount + " 个实体清空");
            }
        }, decayInterval * 20L, decayInterval * 20L);

        plugin.getLogger().info("仇恨衰减任务已启动，任务ID: " + decayTaskId);
    }

    @Override
    public double getAggro(LivingEntity entity, Player player) {
        if (!enabled || entity == null || player == null) return 0;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets == null) return 0;

        AggroEntry entry = targets.get(player.getUniqueId());
        return entry != null ? entry.aggro : 0;
    }

    @Override
    public void addAggro(LivingEntity entity, Player player, double amount) {
        if (!enabled || entity == null || player == null) return;

        if (mythicMobsOnly && !isMythicMob(entity)) return;

        UUID entityId = entity.getUniqueId();
        UUID playerId = player.getUniqueId();

        Map<UUID, AggroEntry> targets = aggroTable.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());

        AggroEntry entry = targets.computeIfAbsent(playerId, k -> new AggroEntry());
        entry.aggro += amount;
        entry.lastUpdate = System.currentTimeMillis();

        if (targets.size() > maxAggroTargets) {
            removeLowestAggro(targets);
        }
    }

    @Override
    public void setAggro(LivingEntity entity, Player player, double amount) {
        if (!enabled || entity == null || player == null) return;

        if (mythicMobsOnly && !isMythicMob(entity)) return;

        UUID entityId = entity.getUniqueId();
        UUID playerId = player.getUniqueId();

        Map<UUID, AggroEntry> targets = aggroTable.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());

        AggroEntry entry = targets.computeIfAbsent(playerId, k -> new AggroEntry());
        entry.aggro = amount;
        entry.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void removeAggro(LivingEntity entity, Player player) {
        if (entity == null || player == null) return;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets != null) {
            targets.remove(player.getUniqueId());
            if (targets.isEmpty()) {
                aggroTable.remove(entity.getUniqueId());
                decayRates.remove(entity.getUniqueId());
            }
        }
    }

    @Override
    public void clearAggro(LivingEntity entity) {
        if (entity == null) return;

        aggroTable.remove(entity.getUniqueId());
        decayRates.remove(entity.getUniqueId());
    }

    @Override
    public void clearAllAggro(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        for (Map<UUID, AggroEntry> targets : aggroTable.values()) {
            targets.remove(playerId);
        }
    }

    @Override
    public Player getTopAggroTarget(LivingEntity entity) {
        if (!enabled || entity == null) return null;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets == null || targets.isEmpty()) return null;

        UUID topPlayerId = null;
        double maxAggro = 0;

        for (Map.Entry<UUID, AggroEntry> entry : targets.entrySet()) {
            if (entry.getValue().aggro > maxAggro) {
                maxAggro = entry.getValue().aggro;
                topPlayerId = entry.getKey();
            }
        }

        return topPlayerId != null ? Bukkit.getPlayer(topPlayerId) : null;
    }

    @Override
    public Map<UUID, Double> getAllAggro(LivingEntity entity) {
        Map<UUID, Double> result = new HashMap<>();

        if (entity == null) return result;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets != null) {
            for (Map.Entry<UUID, AggroEntry> entry : targets.entrySet()) {
                result.put(entry.getKey(), entry.getValue().aggro);
            }
        }

        return result;
    }

    @Override
    public void transferAggro(LivingEntity entity, Player from, Player to, double percentage) {
        if (!enabled || entity == null || from == null || to == null) return;

        double currentAggro = getAggro(entity, from);
        if (currentAggro <= 0) return;

        double transferAmount = currentAggro * (percentage / 100.0);

        addAggro(entity, from, -transferAmount);
        addAggro(entity, to, transferAmount);

        plugin.getLogger().fine("仇恨转移: " + from.getName() + " -> " + to.getName() + " (" + percentage + "%)");
    }

    @Override
    public boolean hasAggro(LivingEntity entity, Player player) {
        return getAggro(entity, player) > 0;
    }

    @Override
    public int getAggroRank(LivingEntity entity, Player player) {
        if (!enabled || entity == null || player == null) return -1;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets == null) return -1;

        double playerAggro = getAggro(entity, player);
        if (playerAggro <= 0) return -1;

        int rank = 1;
        for (AggroEntry entry : targets.values()) {
            if (entry.aggro > playerAggro) {
                rank++;
            }
        }

        return rank;
    }

    @Override
    public double getTotalAggro(LivingEntity entity) {
        if (entity == null) return 0;

        Map<UUID, AggroEntry> targets = aggroTable.get(entity.getUniqueId());
        if (targets == null) return 0;

        double total = 0;
        for (AggroEntry entry : targets.values()) {
            total += entry.aggro;
        }

        return total;
    }

    @Override
    public void setAggroDecay(LivingEntity entity, double decayRate) {
        if (entity == null) return;

        if (decayRate <= 0) {
            decayRates.remove(entity.getUniqueId());
        } else {
            decayRates.put(entity.getUniqueId(), decayRate);
        }
    }

    @Override
    public void forceTarget(LivingEntity entity, Player target) {
        if (!enabled || entity == null || target == null) return;

        clearAggro(entity);
        setAggro(entity, target, 10000);

        plugin.getLogger().fine("强制目标: " + entity.getType() + " -> " + target.getName());
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    public void clearAll() {
        aggroTable.clear();
        decayRates.clear();
        plugin.getLogger().info("所有仇恨数据已清空");
    }

    public void onEntityDamageByPlayer(LivingEntity entity, Player player, double damage) {
        double aggro = damage * damageAggroMultiplier;
        addAggro(entity, player, aggro);
    }

    public void onPlayerHeal(Player healer, LivingEntity entity, double healAmount) {
        double aggro = healAmount * healAggroMultiplier;
        addAggro(entity, healer, aggro);
    }

    public void onPlayerDistance(Player player, LivingEntity entity, double distance) {
        if (distance <= 10) {
            double aggro = distanceAggroMultiplier * (10 - distance);
            addAggro(entity, player, aggro);
        }
    }

    private boolean isMythicMob(LivingEntity entity) {
        return mythicMobsHook != null && mythicMobsHook.isEnabled() && mythicMobsHook.isMythicMob(entity);
    }

    private void removeLowestAggro(Map<UUID, AggroEntry> targets) {
        UUID lowestId = null;
        double lowestAggro = Double.MAX_VALUE;

        for (Map.Entry<UUID, AggroEntry> entry : targets.entrySet()) {
            if (entry.getValue().aggro < lowestAggro) {
                lowestAggro = entry.getValue().aggro;
                lowestId = entry.getKey();
            }
        }

        if (lowestId != null) {
            targets.remove(lowestId);
        }
    }

    public void stopDecayTask() {
        if (decayTaskId != -1) {
            RPGCore rpgCore = plugin.getRPGCore();
            if (rpgCore != null && rpgCore.getScheduler() != null) {
                rpgCore.getScheduler().cancelTask(decayTaskId);
                plugin.getLogger().info("仇恨衰减任务已停止");
            }
            decayTaskId = -1;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getLogger().info("仇恨系统已" + (enabled ? "启用" : "禁用"));
    }

    private static class AggroEntry {
        double aggro = 0;
        long lastUpdate = System.currentTimeMillis();
    }
}
