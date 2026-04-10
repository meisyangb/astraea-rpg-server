package cn.guangdian.aggro.listener;

import cn.guangdian.aggro.GuangDianAggro;
import cn.guangdian.aggro.manager.AggroManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class AggroListener implements Listener {

    private final GuangDianAggro plugin;
    private final AggroManager aggroManager;

    public AggroListener(GuangDianAggro plugin, AggroManager aggroManager) {
        this.plugin = plugin;
        this.aggroManager = aggroManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        double damage = event.getFinalDamage();
        aggroManager.onEntityDamageByPlayer(entity, player, damage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double healAmount = event.getAmount();

        for (LivingEntity entity : player.getWorld().getEntitiesByClass(LivingEntity.class)) {
            if (entity.hasLineOfSight(player) && entity.getLocation().distanceSquared(player.getLocation()) <= 100) {
                if (aggroManager.hasAggro(entity, player)) {
                    aggroManager.onPlayerHeal(player, entity, healAmount);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        aggroManager.clearAggro(entity);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        aggroManager.clearAllAggro(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        Player topTarget = aggroManager.getTopAggroTarget(entity);
        if (topTarget != null && topTarget.isOnline()) {
            if (event.getTarget() == null || !event.getTarget().equals(topTarget)) {
                event.setCancelled(true);
            }
        }
    }
}
