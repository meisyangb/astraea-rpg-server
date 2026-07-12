package cn.guangdian.dungeon.listener;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonTemplate;
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

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session != null) {
            plugin.getMapInstanceManager().teleportToExitWorld(player);
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
        if (session == null) return;

        // 增加死亡次数
        session.incrementTotalDeaths();

        // 检查是否达到死亡上限
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(session.getDungeonId());
        if (template == null) return;

        int maxDeaths = template.getSettings().getMaxDeaths();
        if (maxDeaths > 0 && session.getTotalDeaths() >= maxDeaths) {
            // 达到死亡上限，副本失败
            plugin.getLogger().info("副本 " + session.getDungeonId() + " 死亡次数达到上限 (" + maxDeaths + ")，挑战失败");
            plugin.getSessionManager().endSession(session, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String sessionId = plugin.getMobBridge().getSessionIdFromEntity(entity);

        if (sessionId == null) {
            Player killer = entity.getKiller();
            if (killer != null) {
                DungeonSession fallbackSession = plugin.getSessionManager().getPlayerSession(killer.getUniqueId());
                if (fallbackSession != null && fallbackSession.isSessionMob(entity.getUniqueId())) {
                    sessionId = fallbackSession.getSessionId();
                }
            }
        }

        if (sessionId == null) return;

        DungeonSession session = plugin.getSessionManager().getSession(sessionId);
        if (session == null) return;

        plugin.getSessionManager().onMobKill(session, entity);

        if (plugin.getMobBridge().isDungeonBoss(entity)) {
            plugin.getMobBridge().removeBoss(entity.getUniqueId());
        }
    }
}