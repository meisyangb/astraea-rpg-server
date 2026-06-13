package cn.guangdian.mobhealth;

import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class MobListener implements Listener {

    private final GuangDianMobHealth plugin;
    private final MobHealthDisplayManager displayManager;
    private final ActionBarHealthDisplay actionBarDisplay;

    public MobListener(GuangDianMobHealth plugin, MobHealthDisplayManager displayManager, ActionBarHealthDisplay actionBarDisplay) {
        this.plugin = plugin;
        this.displayManager = displayManager;
        this.actionBarDisplay = actionBarDisplay;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();

        // 处理全息图显示模式
        if (displayManager != null && (plugin.getDisplayMode() == GuangDianMobHealth.DisplayMode.HOLOGRAM || plugin.getDisplayMode() == GuangDianMobHealth.DisplayMode.BOTH)) {
            if (event instanceof EntityDamageByEntityEvent) {
                if (!displayManager.hasDisplay(entity.getUniqueId())) {
                    double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
                    plugin.debug("怪物被攻击: " + entity.getName() + " 血量: " + maxHealth);
                    displayManager.createDisplay(entity);
                }

                RPGCore rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    rpgCore.getScheduler().runSyncLater(() -> {
                        if (entity.isValid() && !entity.isDead()) {
                            displayManager.updateDisplay(entity);
                        }
                    }, 1L);
                }
            }
        }

        // 处理 ActionBar 显示模式
        if (actionBarDisplay != null && (plugin.getDisplayMode() == GuangDianMobHealth.DisplayMode.ACTIONBAR || plugin.getDisplayMode() == GuangDianMobHealth.DisplayMode.BOTH)) {
            if (event instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                if (damager instanceof Player) {
                    Player player = (Player) damager;
                    // 延迟1tick显示，确保伤害已经应用
                    RPGCore rpgCore = RPGCore.getInstance();
                    if (rpgCore != null) {
                        rpgCore.getScheduler().runSyncLater(() -> {
                            if (entity.isValid() && !entity.isDead()) {
                                actionBarDisplay.showHealth(player, entity);
                            }
                        }, 1L);
                    } else {
                        actionBarDisplay.showHealth(player, entity);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();

        // 更新全息图显示
        if (displayManager != null && displayManager.hasDisplay(entity.getUniqueId())) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSyncLater(() -> {
                    if (entity.isValid() && !entity.isDead()) {
                        displayManager.updateDisplay(entity);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 清理全息图显示
        if (displayManager != null) {
            displayManager.removeDisplay(entity);
        }

        // 清理 ActionBar 记录
        if (actionBarDisplay != null) {
            Player killer = entity.getKiller();
            if (killer != null) {
                actionBarDisplay.clearPlayer(killer);
            }
        }

        plugin.debug("怪物死亡: " + entity.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // 清理全息图显示
        if (displayManager != null) {
            for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof LivingEntity) {
                    displayManager.removeDisplay((LivingEntity) entity);
                }
            }
        }
    }
}
