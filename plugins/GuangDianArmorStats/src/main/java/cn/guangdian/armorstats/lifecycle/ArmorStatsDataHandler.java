package cn.guangdian.armorstats.lifecycle;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.manager.HealthManager;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.manager.BossBarManager;
import cn.guangdian.armorstats.event.PlayerFullHealthEvent;
import cn.guangdian.armorstats.event.PlayerHealthChangedEvent;
import cn.guangdian.armorstats.event.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ArmorStatsDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianArmorStats plugin;
    private final StatsManager statsManager;
    private final HealthManager healthManager;
    private final BossBarManager bossBarManager;
    
    public ArmorStatsDataHandler(GuangDianArmorStats plugin, StatsManager statsManager, 
                                  HealthManager healthManager, BossBarManager bossBarManager) {
        super(plugin);
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.healthManager = healthManager;
        this.bossBarManager = bossBarManager;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        statsManager.clearPlayerAttributes(player);
        
        if (bossBarManager != null && bossBarManager.isEnabled()) {
            bossBarManager.createBossBar(player);
        }
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.loadPlayerData(player);
                    healthManager.syncPlayerHealth(player);
                    plugin.getLogger().info("[登录] " + player.getName() + " 属性加载完成");
                    
                    publishHealthEvents(player);
                }
            }, 40L);
        }
    }
    
    private void publishHealthEvents(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        
        PlayerStatsChangedEvent statsEvent = new PlayerStatsChangedEvent(
            player.getUniqueId(),
            player.getName(),
            0, maxHealth,
            0, 0,
            0, 0
        );
        Bukkit.getPluginManager().callEvent(statsEvent);
        
        if (currentHealth >= maxHealth) {
            PlayerFullHealthEvent fullHealthEvent = new PlayerFullHealthEvent(
                player.getUniqueId(),
                player.getName(),
                maxHealth,
                PlayerFullHealthEvent.FullHealthReason.LOGIN
            );
            Bukkit.getPluginManager().callEvent(fullHealthEvent);
        } else {
            PlayerHealthChangedEvent healthEvent = new PlayerHealthChangedEvent(
                player.getUniqueId(),
                player.getName(),
                0,
                currentHealth,
                maxHealth,
                PlayerHealthChangedEvent.ChangeReason.OTHER
            );
            Bukkit.getPluginManager().callEvent(healthEvent);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
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
    
    @Override
    public int getPriority() {
        return 50;
    }
    
    @Override
    public String getHandlerName() {
        return "ArmorStats";
    }
}
