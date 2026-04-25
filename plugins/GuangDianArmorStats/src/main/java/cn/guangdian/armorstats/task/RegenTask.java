package cn.guangdian.armorstats.task;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.BossBarManager;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自然回血任务 - 重构版
 * 
 * <p>职责：处理自然回血逻辑，发布血量变化事件</p>
 * <p>不直接更新血量显示，由 GuangDianName 订阅事件处理</p>
 * 
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>单一职责：只负责回血逻辑和事件发布</li>
 *   <li>事件驱动：血量变化通过事件通知</li>
 *   <li>缓存机制：记录上次血量，防止异常值</li>
 * </ul>
 */
public class RegenTask implements Runnable {

    private final GuangDianArmorStats plugin;
    private final StatsManager statsManager;
    private final BossBarManager bossBarManager;
    private final Map<UUID, Long> lastDamageTimes = new ConcurrentHashMap<>();
    private final Set<UUID> playersWithRegen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> lastHealthValues = new ConcurrentHashMap<>();
    private long taskId = -1;
    private long intervalTicks;
    private boolean combatRegenEnabled;
    private double combatMultiplier;
    private long combatTimeoutMillis;

    public RegenTask(GuangDianArmorStats plugin, StatsManager statsManager, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.bossBarManager = bossBarManager;
        loadConfig();
    }

    public void loadConfig() {
        // 默认5秒（100 ticks）执行一次回血
        intervalTicks = Math.max(1L, plugin.getConfig().getLong("regen.interval", 100L));
        combatRegenEnabled = plugin.getConfig().getBoolean("regen.combat_regen", true);
        combatMultiplier = Math.max(0.0, plugin.getConfig().getDouble("regen.combat_multiplier", 0.5));
        combatTimeoutMillis = Math.max(0L, plugin.getConfig().getLong("regen.combat_timeout", 5000L));
    }

    public void start() {
        stop();
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            taskId = rpgCore.getScheduler().runSyncRepeating(this, intervalTicks, intervalTicks);
        } else {
            plugin.getLogger().warning("RPGCore 未启用，无法启动回血任务");
        }
    }

    public void stop() {
        if (taskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(taskId);
            }
            taskId = -1;
        }
    }

    public void updatePlayerRegen(UUID playerId, double regenPerSecond) {
        if (regenPerSecond > 0) {
            playersWithRegen.add(playerId);
        } else {
            playersWithRegen.remove(playerId);
        }
    }

    public void updatePlayerRegen(UUID playerId, double regenPerSecond, double regenPercent) {
        if (regenPerSecond > 0 || regenPercent > 0) {
            playersWithRegen.add(playerId);
        } else {
            playersWithRegen.remove(playerId);
        }
    }

    public void removePlayer(UUID playerId) {
        playersWithRegen.remove(playerId);
        lastDamageTimes.remove(playerId);
        lastHealthValues.remove(playerId);
    }

    public void markDamaged(Player player) {
        lastDamageTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clearCombat(UUID playerId) {
        lastDamageTimes.remove(playerId);
    }

    @Override
    public void run() {
        if (playersWithRegen.isEmpty()) {
            return;
        }

        double tickScale = intervalTicks / 20.0;
        for (UUID playerId : playersWithRegen) {
            Player player = plugin.getServer().getPlayer(playerId);
            
            if (player == null || !player.isOnline()) {
                playersWithRegen.remove(playerId);
                lastHealthValues.remove(playerId);
                continue;
            }

            if (player.isDead() || !player.isValid()) {
                continue;
            }

            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double currentHealth = player.getHealth();
            
            // 如果血量异常（<=0），尝试恢复
            if (currentHealth <= 0) {
                Double lastHealth = lastHealthValues.get(playerId);
                if (lastHealth != null && lastHealth > 0) {
                    currentHealth = lastHealth;
                } else {
                    currentHealth = maxHealth;
                }
                player.setHealth(currentHealth);
            }
            
            lastHealthValues.put(playerId, currentHealth);

            PlayerStats stats = statsManager.getPlayerStats(player);
            double regenPerSecond = stats.getHealthRegen();
            double regenPercent = stats.getHealthRegenPercent();

            if (regenPerSecond <= 0 && regenPercent <= 0) {
                playersWithRegen.remove(playerId);
                lastHealthValues.remove(playerId);
                continue;
            }

            boolean inCombat = isInCombat(player);
            if (inCombat && !combatRegenEnabled) {
                continue;
            }

            double regenMultiplier = inCombat ? combatMultiplier : 1.0;
            double regenAmount = (regenPerSecond + (maxHealth * regenPercent / 100.0)) * tickScale * Math.max(0.0, regenMultiplier);
            
            if (regenAmount <= 0) {
                continue;
            }

            // 满血时跳过回血，但发布满血事件确保显示正确
            if (currentHealth >= maxHealth) {
                publishFullHealthEvent(player, maxHealth);
                continue;
            }

            double oldHealth = currentHealth;
            double newHealth = Math.min(maxHealth, oldHealth + regenAmount);
            
            player.setHealth(newHealth);
            lastHealthValues.put(playerId, newHealth);

            if (bossBarManager != null && bossBarManager.isEnabled()) {
                bossBarManager.updateBossBar(player);
            }
            
            // 发布血量变化事件
            publishHealthChangedEvent(player, oldHealth, newHealth, maxHealth);
            
            // 如果达到满血，发布满血事件
            if (newHealth >= maxHealth) {
                publishFullHealthEvent(player, maxHealth);
            }
        }
    }

    private boolean isInCombat(Player player) {
        Long lastDamageTime = lastDamageTimes.get(player.getUniqueId());
        if (lastDamageTime == null) {
            return false;
        }
        return System.currentTimeMillis() - lastDamageTime < combatTimeoutMillis;
    }
    
    /**
     * 发布血量变化事件
     */
    private void publishHealthChangedEvent(Player player, double oldHealth, double newHealth, double maxHealth) {
        cn.guangdian.armorstats.event.PlayerHealthChangedEvent healthEvent = 
            new cn.guangdian.armorstats.event.PlayerHealthChangedEvent(
                player.getUniqueId(),
                player.getName(),
                oldHealth,
                newHealth,
                maxHealth,
                cn.guangdian.armorstats.event.PlayerHealthChangedEvent.ChangeReason.REGEN
            );
        org.bukkit.Bukkit.getPluginManager().callEvent(healthEvent);
    }
    
    /**
     * 发布满血事件
     */
    private void publishFullHealthEvent(Player player, double maxHealth) {
        cn.guangdian.armorstats.event.PlayerFullHealthEvent fullHealthEvent = 
            new cn.guangdian.armorstats.event.PlayerFullHealthEvent(
                player.getUniqueId(),
                player.getName(),
                maxHealth,
                cn.guangdian.armorstats.event.PlayerFullHealthEvent.FullHealthReason.REGEN
            );
        org.bukkit.Bukkit.getPluginManager().callEvent(fullHealthEvent);
    }
}
