package cn.guangdian.killaura.listener;

import cn.guangdian.killaura.GuangDianKillAura;
import cn.guangdian.killaura.manager.AttackManager;
import cn.guangdian.killaura.model.KillAuraProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class KillAuraListener implements Listener {

    private final GuangDianKillAura plugin;
    private final AttackManager attackManager;

    public KillAuraListener(GuangDianKillAura plugin, AttackManager attackManager) {
        this.plugin = plugin;
        this.attackManager = attackManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        attackManager.removeProfile(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            KillAuraProfile profile = attackManager.getProfile(player.getUniqueId());
            if (profile != null) {
                profile.setEnabled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
    }
}
