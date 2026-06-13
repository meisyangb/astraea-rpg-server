package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.manager.IncrementalStatsManager;
import cn.guangdian.armorstats.manager.DamageManager;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

/**
 * 战斗事件监听器
 * 
 * 处理：
 * - 伤害计算
 * - 回血禁用原生
 * - 死亡/重生
 * - 击退抗性
 * - BossBar 更新
 */
public class CombatListener implements Listener {

    private final GuangDianArmorStats plugin;
    private final IncrementalStatsManager statsManager;
    private final DamageManager damageManager;
    
    public CombatListener(GuangDianArmorStats plugin, IncrementalStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.damageManager = plugin.getDamageManager();
    }
    
    // ==================== 禁用原生回血 ====================
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onNativeRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // 只取消原生自然回血（饱食度满时的回血）
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }
    
    // ==================== 伤害事件 ====================
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 跳过 Citizens NPC
        if (isCitizensNPC(event.getEntity()) || isCitizensNPC(event.getDamager())) {
            return;
        }
        
        if (damageManager != null) {
            if (event.getEntity() instanceof Player) {
                damageManager.handlePlayerDamage(event);
            }
            if (event.getDamager() instanceof Player) {
                damageManager.handlePlayerAttack(event);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            double oldHealth = player.getHealth();
            
            // 标记玩家受到伤害（用于回血系统）
            if (!event.isCancelled() && plugin.getRegenTask() != null) {
                plugin.getRegenTask().markDamaged(player);
            }
            
            // 更新 BossBar
            if (plugin.getBossBarManager() != null) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getBossBarManager().updateBossBar(player);
                    }
                }, 1L);
            }
            
            // 发布血量变化事件
            if (!event.isCancelled()) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        double currentHealth = player.getHealth();
                        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        
                        publishHealthChangedEvent(player, oldHealth, currentHealth, maxHealth);
                    }
                }, 1L);
            }
        }
    }
    
    // ==================== 击退抗性 ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockbackDamage(EntityDamageByEntityEvent event) {
        if (isCitizensNPC(event.getEntity()) || isCitizensNPC(event.getDamager())) {
            return;
        }
        
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // 获取玩家属性
        PlayerStats stats = statsManager.getPlayerStats(player.getUniqueId());
        if (stats == null) {
            return;
        }
        
        double knockbackResist = stats.getKnockbackResistPercent();
        if (knockbackResist <= 0) {
            return;
        }
        
        // 减少击退效果
        // 注意：Paper 1.20+ 有 setKnockback 方法，这里使用传统的速度修改方式
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                Vector velocity = player.getVelocity();
                double reduction = 1.0 - (knockbackResist / 100.0);
                reduction = Math.max(0.0, Math.min(1.0, reduction));
                player.setVelocity(velocity.multiply(reduction));
            }
        }, 1L);
    }
    
    // ==================== 死亡/重生 ====================
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 清理回血系统
        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().clearCombat(player.getUniqueId());
        }
        
        // 发布死亡事件
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        publishHealthChangedEvent(player, player.getHealth(), 0, maxHealth);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // 清理回血系统
        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().clearCombat(player.getUniqueId());
        }
        
        // 延迟刷新属性
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                statsManager.onPlayerJoin(player);
                
                // 恢复满血
                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                player.setHealth(maxHealth);
                
                // 更新 BossBar
                if (plugin.getBossBarManager() != null) {
                    plugin.getBossBarManager().updateBossBar(player);
                }
                
                // 发布满血事件
                publishFullHealthEvent(player, maxHealth);
            }
        }, 10L);
    }
    
    // ==================== 辅助方法 ====================
    
    private boolean isCitizensNPC(org.bukkit.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.hasMetadata("NPC");
    }
    
    /**
     * 发布血量变化事件
     */
    private void publishHealthChangedEvent(Player player, double oldHealth, double newHealth, double maxHealth) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent healthEvent = 
                    new cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent(
                        player.getUniqueId(),
                        player.getName(),
                        oldHealth,
                        newHealth,
                        maxHealth,
                        cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent.ChangeReason.DAMAGE
                    );
                rpgCore.getEventBus().publish(healthEvent);
            }
        } catch (Exception e) {
            // RPGCore 不可用，忽略
        }
    }
    
    /**
     * 发布满血事件
     */
    private void publishFullHealthEvent(Player player, double maxHealth) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent fullHealthEvent = 
                    new cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent(
                        player.getUniqueId(),
                        player.getName(),
                        maxHealth,
                        cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent.FullHealthReason.RESPAWN
                    );
                rpgCore.getEventBus().publish(fullHealthEvent);
            }
        } catch (Exception e) {
            // RPGCore 不可用，忽略
        }
    }
}
