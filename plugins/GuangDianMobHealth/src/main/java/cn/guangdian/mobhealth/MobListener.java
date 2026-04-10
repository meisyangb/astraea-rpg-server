package cn.guangdian.mobhealth;

import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
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

    public MobListener(GuangDianMobHealth plugin, MobHealthDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (displayManager.hasDisplay(entity.getUniqueId())) {
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
        displayManager.removeDisplay(entity);
        plugin.debug("怪物死亡: " + entity.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                displayManager.removeDisplay((LivingEntity) entity);
            }
        }
    }
}
