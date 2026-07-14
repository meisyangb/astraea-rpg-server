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
            // 使用玩家进入副本前的原始位置传送
            org.bukkit.Location originalLoc = session.getOriginalLocation(player.getUniqueId());
            plugin.getMapInstanceManager().teleportToOriginalLocation(player, originalLoc);

            // 检查是否所有玩家都已离开副本
            checkAndDestroyEmptyInstance(session);
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

    /**
     * 检查副本是否所有玩家都已离开，如果为空则销毁实例
     */
    private void checkAndDestroyEmptyInstance(DungeonSession session) {
        if (session == null || session.getParty() == null) return;

        // 检查是否还有在线玩家在副本中
        boolean hasOnlinePlayer = false;
        for (var member : session.getParty().getMembers()) {
            Player p = plugin.getServer().getPlayer(member.getPlayerId());
            if (p != null && p.isOnline()) {
                // 检查玩家是否还在副本世界中
                if (session.getInstanceWorld() != null &&
                    p.getWorld().equals(session.getInstanceWorld())) {
                    hasOnlinePlayer = true;
                    break;
                }
            }
        }

        // 如果没有在线玩家在副本中，销毁实例
        if (!hasOnlinePlayer) {
            plugin.getLogger().info("副本 " + session.getDungeonId() + " 所有玩家已离开，销毁实例");
            plugin.getSessionManager().endSession(session, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
        if (session == null) return;

        // 增加死亡次数
        session.incrementTotalDeaths();
        int currentDeaths = session.getTotalDeaths();

        // 检查是否达到死亡上限
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(session.getDungeonId());
        if (template == null) return;

        int maxDeaths = template.getSettings().getMaxDeaths();

        // 默认不掉落物品（副本内死亡保护）
        boolean keepInventory = template.getSettings().isKeepInventoryOnDeath();
        event.setKeepInventory(keepInventory);
        event.setKeepLevel(keepInventory);
        if (keepInventory) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        if (maxDeaths > 0 && currentDeaths >= maxDeaths) {
            // 达到死亡上限 - T出副本
            plugin.getLogger().info("副本 " + session.getDungeonId() + " 死亡次数达到上限 (" + maxDeaths + ")，挑战失败");

            // 延迟1秒后复活并传送
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    // 强制复活
                    if (player.isDead()) {
                        player.spigot().respawn();
                    }

                    // 传送到原位置
                    org.bukkit.Location originalLoc = session.getOriginalLocation(player.getUniqueId());
                    plugin.getMapInstanceManager().teleportToOriginalLocation(player, originalLoc);

                    player.sendMessage(plugin.color("<red>死亡次数达到上限，已退出副本！"));
                }

                // 结束会话并销毁实例
                plugin.getSessionManager().endSession(session, false);
            }, 20L); // 1秒后执行
        } else {
            // 未达到死亡上限 - 在副本内复活
            // 读取副本出生点
            org.bukkit.Location spawnLoc = getDungeonSpawnLocation(session);

            // 延迟复活后传送到副本出生点
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.isDead()) {
                    player.spigot().respawn();
                    // 传送到副本出生点
                    player.teleport(spawnLoc);
                    player.sendMessage(plugin.color("<yellow>你在副本内复活！剩余死亡次数: " + (maxDeaths - currentDeaths)));
                }
            }, 20L);
        }
    }

    /**
     * 获取副本出生点位置
     */
    private org.bukkit.Location getDungeonSpawnLocation(DungeonSession session) {
        org.bukkit.configuration.file.YamlConfiguration dungeonConfig =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "dungeons/" + session.getDungeonId() + ".yml"));

        double x = dungeonConfig.getDouble("teleports.entrance.x", 0);
        double y = dungeonConfig.getDouble("teleports.entrance.y", 64);
        double z = dungeonConfig.getDouble("teleports.entrance.z", 0);

        return new org.bukkit.Location(session.getInstanceWorld(), x, y, z);
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