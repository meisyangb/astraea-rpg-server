package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.manager.HealthManager;
import cn.guangdian.armorstats.manager.BossBarManager;
import cn.guangdian.armorstats.skill.SkillIntegration;
import cn.guangdian.armorstats.data.PlayerStats;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 事件监听器
 */
public class EventListeners implements Listener {

    private final StatsManager statsManager;
    private final HealthManager healthManager;
    private final BossBarManager bossBarManager;
    private final GuangDianArmorStats plugin;

    public EventListeners(GuangDianArmorStats plugin, StatsManager statsManager, HealthManager healthManager, SkillIntegration skillIntegration) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.healthManager = healthManager;
        this.bossBarManager = plugin.getBossBarManager();
    }

    // ==================== 禁用原生回血 ====================

    /**
     * 禁用原生自然回血
     * 
     * <p>原生的自然回血（饱食度满时）由 RegenTask 处理，
     * 这里取消原生回血事件，避免与 RPG 回血系统冲突。</p>
     */
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

    // ==================== 玩家生命周期 ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        statsManager.clearPlayerAttributes(player);

        if (bossBarManager != null && bossBarManager.isEnabled()) {
            bossBarManager.createBossBar(player);
        }

        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.loadPlayerData(player);
                    healthManager.syncPlayerHealth(player);
                    plugin.getLogger().info("[登录] " + player.getName() + " 属性加载完成");
                    
                    if (cn.guangdian.rpgcore.RPGCore.getInstance() != null) {
                        cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent statsEvent = 
                            new cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent(
                                player.getUniqueId(),
                                player.getName(),
                                0, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),
                                0, 0,
                                0, 0
                            );
                        cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(statsEvent);
                        
                        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                        double currentHealth = player.getHealth();
                        
                        if (currentHealth >= maxHealth) {
                            cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent fullHealthEvent = 
                                new cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent(
                                    player.getUniqueId(),
                                    player.getName(),
                                    maxHealth,
                                    cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent.FullHealthReason.LOGIN
                                );
                            cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(fullHealthEvent);
                        } else {
                            cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent healthEvent = 
                                new cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent(
                                    player.getUniqueId(),
                                    player.getName(),
                                    0,
                                    currentHealth,
                                    maxHealth,
                                    cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent.ChangeReason.OTHER
                                );
                            cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(healthEvent);
                        }
                    }
                }
            }, 40L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        statsManager.savePlayerData(player);
        statsManager.clearPlayerAttributes(player);
        statsManager.removePlayer(player.getUniqueId());

        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().removePlayer(player.getUniqueId());
        }
        if (bossBarManager != null) {
            bossBarManager.removeBossBar(player);
        }

        plugin.getLogger().info("[退出] " + player.getName() + " 数据已保存");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().clearCombat(player.getUniqueId());
        }

        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.refreshFullStats(player);
                    healthManager.restoreFullHealth(player);
                    if (bossBarManager != null) {
                        bossBarManager.updateBossBar(player);
                    }
                    
                    if (cn.guangdian.rpgcore.RPGCore.getInstance() != null) {
                        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                        cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent fullHealthEvent = 
                            new cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent(
                                player.getUniqueId(),
                                player.getName(),
                                maxHealth,
                                cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent.FullHealthReason.RESPAWN
                            );
                        cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(fullHealthEvent);
                    }
                }
            }, 10L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (cn.guangdian.rpgcore.RPGCore.getInstance() != null) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent healthEvent = 
                new cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent(
                    player.getUniqueId(),
                    player.getName(),
                    player.getHealth(),
                    0,
                    maxHealth,
                    cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent.ChangeReason.DEATH
                );
            cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(healthEvent);
        }
    }

    // ==================== Paper 防具事件 ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        
        // 忽略未完成登录加载的玩家（防止登录时重复刷新）
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) {
            plugin.getLogger().info("[Paper防具事件] " + player.getName() + " 未完成登录加载，忽略事件");
            return;
        }

        plugin.getLogger().info("[Paper防具事件] " + player.getName() + 
            " 槽位: " + event.getSlot().name() +
            " 旧: " + itemName(event.getOldItem()) + 
            " 新: " + itemName(event.getNewItem()));

        statsManager.refreshArmorOnly(player);
        if (bossBarManager != null) {
            bossBarManager.updateBossBar(player);
        }
    }

    // ==================== 主手/副手切换 ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int newSlot = event.getNewSlot();

        plugin.getLogger().info("[主手切换] " + player.getName() + " 切换到槽位: " + newSlot);

        // 延迟1tick，让物品栏先更新
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.refreshWeaponOnly(player);
                    if (bossBarManager != null) {
                        bossBarManager.updateBossBar(player);
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        plugin.getLogger().info("[副手切换] " + player.getName());

        cn.guangdian.rpgcore.RPGCore rpgCore2 = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore2 != null) {
            rpgCore2.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.refreshWeaponOnly(player);
                    if (bossBarManager != null) {
                        bossBarManager.updateBossBar(player);
                    }
                }
            }, 1L);
        }
    }

    // ==================== 伤害事件 ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getDamageManager() != null) {
            var damageManager = plugin.getDamageManager();

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
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            double oldHealth = player.getHealth();  // 伤害前的血量
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            
            if (!event.isCancelled() && plugin.getRegenTask() != null) {
                plugin.getRegenTask().markDamaged(player);
            }
            
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (bossBarManager != null && rpgCore != null) {
                rpgCore.getScheduler().runSyncLater(() -> {
                    if (player.isOnline()) {
                        bossBarManager.updateBossBar(player);
                    }
                }, 1L);
            }
            
            // 【新增】发布血量变化事件，让 GuangDianName 等插件可以监听
            if (rpgCore != null) {
                rpgCore.getScheduler().runSyncLater(() -> {
                    if (player.isOnline() && cn.guangdian.rpgcore.RPGCore.getInstance() != null) {
                        double currentHealth = player.getHealth();  // 伤害后的血量
                    
                        // 使用完整版构造函数，传入伤害前后的血量
                        cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent healthEvent = 
                            new cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent(
                                player.getUniqueId(),
                                player.getName(),
                                oldHealth,      // 伤害前的血量
                                currentHealth,  // 伤害后的血量
                                maxHealth,
                                cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent.ChangeReason.DAMAGE
                            );
                        cn.guangdian.rpgcore.RPGCore.getInstance().getEventBus().publish(healthEvent);
                    }
                }, 1L);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private String itemName(ItemStack item) {
        return item == null ? "AIR" : item.getType().name();
    }

    // ==================== 禁用原生护甲减伤 ====================

    /**
     * 禁用原生护甲减伤
     * 
     * <p>原生的护甲减伤系统由 Minecraft 自动计算，这会与 RPG 属性系统冲突。
     * 在 LOWEST 优先级重置伤害，让 RPG 系统完全控制伤害计算。</p>
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageRaw(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // 只处理实体攻击伤害（包含护甲减伤的伤害类型）
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            
            // 获取原始伤害（护甲减伤前的伤害）
            double rawDamage = event.getDamage();
            
            // 重置护甲减伤，让 RPG 系统完全控制
            // 注意：这里不修改伤害值，只是标记我们会在后续处理
            // 实际的 RPG 伤害计算在 DamageManager 中进行
        }
    }

    // ==================== 击退抗性 ====================

    /**
     * 处理击退抗性
     * 
     * <p>在伤害事件中根据玩家的击退抗性属性减少击退效果。
     * 使用 Paper 的 setKnockback 方法（如果可用）。</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockbackDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) {
            return;
        }

        double knockbackResist = stats.getKnockbackResistPercent();
        if (knockbackResist <= 0) {
            return;
        }

        // 100% 抗性完全免疫击退
        if (knockbackResist >= 100.0) {
            try {
                // Paper API: 设置击退为 0
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
                // 取消击退效果
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            } catch (Exception ignored) {
            }
            return;
        }

        // 部分抗性 - 在后续的 velocity 事件中处理会更准确
        // 这里我们通过设置玩家的击退抗性属性来处理
        try {
            org.bukkit.attribute.AttributeInstance knockbackAttr = 
                player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                double baseValue = knockbackResist / 100.0;
                knockbackAttr.setBaseValue(baseValue);
            }
        } catch (Exception ignored) {
        }
    }

    // ==================== 环境伤害抗性 ====================

    /**
     * 处理环境伤害抗性
     * 
     * <p>根据玩家的环境伤害抗性属性减少对应的伤害。</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 不处理实体攻击伤害（由 DamageManager 处理）
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) {
            return;
        }

        double resistPercent = getEnvironmentalResistPercent(cause, stats);
        if (resistPercent <= 0) {
            return;
        }

        double originalDamage = event.getDamage();
        double reduction = resistPercent / 100.0;
        double newDamage = originalDamage * (1.0 - Math.min(1.0, reduction));
        
        if (newDamage < 0.1) {
            newDamage = 0;
        }
        
        event.setDamage(newDamage);
    }

    /**
     * 根据伤害类型获取对应的环境伤害抗性百分比
     */
    private double getEnvironmentalResistPercent(EntityDamageEvent.DamageCause cause, PlayerStats stats) {
        switch (cause) {
            case FIRE:
            case FIRE_TICK:
                return stats.getFireResistPercent();
            case FALL:
                return stats.getFallResistPercent();
            case DROWNING:
                return stats.getDrowningResistPercent();
            case POISON:
                return stats.getPoisonResistPercent();
            case WITHER:
                return stats.getWitherResistPercent();
            case LAVA:
                return stats.getLavaResistPercent();
            case MAGIC:
                return stats.getMagicResistPercent();
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
                return stats.getExplosionResistPercent();
            case PROJECTILE:
                return stats.getProjectileResistPercent();
            case HOT_FLOOR:
                return Math.max(stats.getFireResistPercent(), stats.getLavaResistPercent());
            default:
                return 0;
        }
    }
    
    // ==================== 职业属性变更 ====================
    
    /**
     * 监听职业属性变更事件
     * 
     * <p>当玩家在职业系统中分配或回收属性点时，刷新玩家的属性。</p>
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClassAttributeChange(cn.guangdian.classsystem.event.PlayerAttributeChangeEvent event) {
        Player player = event.getPlayer();
        
        plugin.getLogger().info("[职业属性变更] " + player.getName() + 
            " 类型: " + event.getChangeType() +
            " 属性: " + (event.getAttributeType() != null ? event.getAttributeType().getDisplayName() : "全部") +
            " 变化: " + event.getDelta());
        
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.refreshFullStats(player);
                    healthManager.syncPlayerHealth(player);
                    if (bossBarManager != null) {
                        bossBarManager.updateBossBar(player);
                    }
                }
            }, 1L);
        }
    }
}