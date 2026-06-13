package cn.guangdian.dungeon.listener;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.manager.WorldInstanceManager;
import cn.guangdian.dungeon.model.session.DungeonSession;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DungeonEventListener implements Listener {

    private final GuangDianDungeon plugin;

    public DungeonEventListener(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        WorldInstanceManager.InstanceInfo instanceInfo =
            plugin.getWorldInstanceManager().getInstanceByPlayer(player.getUniqueId());

        if (instanceInfo != null) {
            plugin.getWorldInstanceManager().removePlayerFromInstance(
                instanceInfo.getInstanceWorldName(), player.getUniqueId());
            plugin.getWorldInstanceManager().teleportToExitWorld(player);
        }

        var partyOpt = plugin.getPartyManager().getPlayerParty(player);
        if (partyOpt.isPresent()) {
            if (partyOpt.get().isLeader(player)) {
                plugin.getPartyManager().disbandParty(partyOpt.get());
            } else {
                plugin.getPartyManager().leaveParty(player);
            }
        }

        plugin.getPlayerRepository().savePlayerData(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session != null) {
            session.incrementTotalDeaths();

            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " died in dungeon. Total deaths: " + session.getTotalDeaths());
        }
    }

    /**
     * P0 修复: 从被击杀实体的 PDC 读取 sessionId，而非通过 killer 查找
     * 这样即使怪物被环境伤害/非玩家击杀，也能正确追踪进度
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // P0 FIX: 从实体 PDC 中读取副本会话 ID
        String sessionId = plugin.getMobBridge().getSessionIdFromEntity(entity);

        if (sessionId == null) {
            plugin.getLogger().info("[DEBUG] Entity " + entity.getUniqueId() + " has no dungeon session tag");
            // 回退到通过 killer 查找（向后兼容）
            Player killer = entity.getKiller();
            if (killer != null) {
                DungeonSession fallbackSession = plugin.getSessionManager().getPlayerSession(killer.getUniqueId());
                if (fallbackSession != null && fallbackSession.isSessionMob(entity.getUniqueId())) {
                    plugin.getLogger().info("[DEBUG] Fallback: using killer session for entity tracking");
                    sessionId = fallbackSession.getSessionId();
                }
            }
        }

        if (sessionId == null) {
            return;
        }

        DungeonSession session = plugin.getSessionManager().getSession(sessionId);
        if (session == null) {
            plugin.getLogger().warning("[DEBUG] Session not found for sessionId: " + sessionId);
            return;
        }

        plugin.getLogger().info("[DEBUG] Entity killed in dungeon session " + sessionId + ": " + entity.getName());

        plugin.getSessionManager().onMobKill(session, entity);

        // 如果是 Boss，清理 Boss 追踪
        if (plugin.getMobBridge().isDungeonBoss(entity)) {
            plugin.getMobBridge().removeBoss(entity.getUniqueId());
        }
    }
}
