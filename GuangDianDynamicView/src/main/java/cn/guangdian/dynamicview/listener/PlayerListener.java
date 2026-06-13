package cn.guangdian.dynamicview.listener;

import cn.guangdian.dynamicview.DynamicViewPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 玩家事件监听器
 */
public class PlayerListener implements Listener {

    private final DynamicViewPlugin plugin;

    public PlayerListener(DynamicViewPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getViewDistanceManager().onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getViewDistanceManager().onPlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().equals(event.getTo())) {
            return;
        }
        // 更新活动时间
        plugin.getViewDistanceManager().onPlayerMove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 攻击者进入战斗
        if (event.getDamager() instanceof Player attacker) {
            plugin.getViewDistanceManager().onPlayerCombat(attacker);
        }
        // 被攻击者进入战斗
        if (event.getEntity() instanceof Player victim) {
            plugin.getViewDistanceManager().onPlayerCombat(victim);
        }
    }
}
