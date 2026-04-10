package cn.guangdian.armorstats.boss;

import cn.guangdian.armorstats.GuangDianArmorStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BOSS 公告系统
 * 处理 BOSS 死亡公告、血量阶段公告等
 */
public class BossAnnouncer implements Listener {

    private final GuangDianArmorStats plugin;
    private final BossStatsManager bossStatsManager;

    // BOSS 血量阶段跟踪 (用于阶段公告)
    private final Map<UUID, Integer> bossPhaseTracker = new HashMap<>();
    
    // 公告配置
    private boolean enabled = true;
    private int announceRadius = 50;

    public BossAnnouncer(GuangDianArmorStats plugin, BossStatsManager bossStatsManager) {
        this.plugin = plugin;
        this.bossStatsManager = bossStatsManager;
        loadConfig();
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        enabled = config.getBoolean("boss_announce.enabled", true);
        announceRadius = config.getInt("boss_announce.radius", 50);
    }

    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 监听 BOSS 死亡事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent event) {
        if (!enabled) return;
        
        LivingEntity entity = event.getEntity();
        BossStats stats = bossStatsManager.getBossStats(entity);
        
        if (stats == null) return;
        
        // 清除血量阶段跟踪
        bossPhaseTracker.remove(entity.getUniqueId());
        
        // 获取击杀者
        Player killer = entity.getKiller();
        String killerName = killer != null ? killer.getName() : "未知";
        
        // 发送死亡公告
        announceBossDeath(entity, stats, killerName);
    }

    /**
     * 检查 BOSS 血量阶段（在伤害事件中调用）
     */
    public void checkBossHealthPhase(LivingEntity boss, BossStats stats, double healthPercent) {
        if (!enabled) return;
        
        UUID bossId = boss.getUniqueId();
        int currentPhase = bossPhaseTracker.getOrDefault(bossId, 0);
        
        // 检查血量阶段
        if (healthPercent <= 30 && currentPhase < 3) {
            bossPhaseTracker.put(bossId, 3);
            announceBossPhase(boss, stats, 30);
        } else if (healthPercent <= 50 && currentPhase < 2) {
            bossPhaseTracker.put(bossId, 2);
            announceBossPhase(boss, stats, 50);
        } else if (healthPercent <= 70 && currentPhase < 1) {
            bossPhaseTracker.put(bossId, 1);
            announceBossPhase(boss, stats, 70);
        }
    }

    /**
     * 发送 BOSS 死亡公告
     */
    @SuppressWarnings("deprecation") // getCustomName() 在某些版本中过时，但无替代方案
    private void announceBossDeath(LivingEntity boss, BossStats stats, String killerName) {
        String bossName = stats.getDisplayName() != null ? stats.getDisplayName() : boss.getCustomName();
        if (bossName == null) bossName = boss.getType().name();

        // 移除颜色代码获取纯文本名称
        String plainName = bossName.replaceAll("[&§][0-9a-fk-or]", "");

        // 发送 Title (Paper 1.21.4: 使用 showTitle)
        Component title = LegacyComponentSerializer.legacySection().deserialize("§a§l胜利!");
        Component subtitleText = LegacyComponentSerializer.legacySection().deserialize(
            "§f" + plainName + " §e已被 " + killerName + " 击败!");
        Title titleObj = Title.title(title, subtitleText,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000)));

        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(boss.getLocation()) <= announceRadius * announceRadius) {
                player.showTitle(titleObj);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // 发送聊天消息
        Component deathMessage = LegacyComponentSerializer.legacySection().deserialize(
            "§c§l[BOSS] §f" + plainName + " §e已陨落！击杀者: §b" + killerName);
        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(boss.getLocation()) <= announceRadius * announceRadius) {
                player.sendMessage(deathMessage);
            }
        }

        plugin.getLogger().info("[BOSS公告] " + plainName + " 被 " + killerName + " 击杀");
    }

    /**
     * 发送 BOSS 血量阶段公告
     */
    @SuppressWarnings("deprecation") // getCustomName() 在某些版本中过时，但无替代方案
    private void announceBossPhase(LivingEntity boss, BossStats stats, int healthPercent) {
        String bossName = stats.getDisplayName() != null ? stats.getDisplayName() : boss.getCustomName();
        if (bossName == null) bossName = boss.getType().name();

        String plainName = bossName.replaceAll("[&§][0-9a-fk-or]", "");

        String titleText;
        String subtitleText;
        Sound sound;

        if (healthPercent <= 30) {
            titleText = "§c§l狂暴!";
            subtitleText = "§f" + plainName + " §c进入狂暴状态!";
            sound = Sound.ENTITY_WITHER_SPAWN;
        } else if (healthPercent <= 50) {
            titleText = "§e§l危险!";
            subtitleText = "§f" + plainName + " §e血量过低!";
            sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
        } else {
            titleText = "§6§l警告!";
            subtitleText = "§f" + plainName + " §6进入战斗状态!";
            sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
        }

        // Paper 1.21.4: 使用 showTitle
        Component title = LegacyComponentSerializer.legacySection().deserialize(titleText);
        Component subtitle = LegacyComponentSerializer.legacySection().deserialize(subtitleText);
        Title titleObj = Title.title(title, subtitle,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500)));

        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(boss.getLocation()) <= announceRadius * announceRadius) {
                player.showTitle(titleObj);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }
    }

    /**
     * 发送 BOSS 刷新公告（可在外部调用）
     */
    @SuppressWarnings("deprecation") // getCustomName() 在某些版本中过时，但无替代方案
    public void announceBossSpawn(LivingEntity boss, BossStats stats) {
        if (!enabled) return;

        String bossName = stats.getDisplayName() != null ? stats.getDisplayName() : boss.getCustomName();
        if (bossName == null) bossName = boss.getType().name();

        String plainName = bossName.replaceAll("[&§][0-9a-fk-or]", "");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!boss.isValid() || boss.isDead()) return;

            Component title = LegacyComponentSerializer.legacySection().deserialize("§c§lBOSS战");
            Component subtitle = LegacyComponentSerializer.legacySection().deserialize(
                "§f" + plainName + " §e已刷新!");
            Title titleObj = Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000)));

            for (Player player : boss.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(boss.getLocation()) <= announceRadius * announceRadius) {
                    player.showTitle(titleObj);
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        "§c§l[BOSS] §f" + plainName + ": §e愚蠢的入侵者，你们将成为我的祭品！"));
                }
            }
        }, 20L);
    }
}