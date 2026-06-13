package cn.guangdian.armorstats.lifecycle;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.HealthManager;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.manager.BossBarManager;
import cn.guangdian.armorstats.storage.PlayerDataStorage;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent;
import cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        
        // 1. 先读取缓存数据
        PlayerDataStorage.PlayerData cachedData = null;
        if (plugin.getPlayerDataStorage() != null) {
            cachedData = plugin.getPlayerDataStorage().loadPlayerData(player.getUniqueId());
        }
        
        // 2. 立即应用生命值缩放（使用缓存的最大血量，避免显示跳动）
        if (cachedData != null && cachedData.maxHealth > 20) {
            healthManager.applyHealthScale(player, cachedData.maxHealth);
            plugin.getLogger().info("[登录] " + player.getName() + " 使用缓存最大血量: " + cachedData.maxHealth);
        } else {
            healthManager.applyHealthScale(player, 20);
        }
        
        // 3. 延迟加载完整属性
        final PlayerDataStorage.PlayerData finalCachedData = cachedData;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    statsManager.loadPlayerData(player);
                    healthManager.syncPlayerHealth(player);
                    
                    // 4. 恢复保存的血量
                    if (finalCachedData != null && finalCachedData.health > 0) {
                        double maxHp = healthManager.getPlayerMaxHealth(player);
                        double savedHealth = Math.min(finalCachedData.health, maxHp);
                        player.setHealth(savedHealth);
                        plugin.getLogger().info("[登录] " + player.getName() + 
                            " 恢复血量: " + savedHealth + "/" + maxHp);
                    }
                    
                    plugin.getLogger().info("[登录] " + player.getName() + " 属性加载完成");
                    publishHealthEvents(player);
                }
            }, 40L);
        }
    }
    
    private void publishHealthEvents(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;
        
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        
        PlayerStatsChangedEvent statsEvent = new PlayerStatsChangedEvent(
            player.getUniqueId(),
            player.getName(),
            0, maxHealth,
            0, 0,
            0, 0
        );
        rpgCore.getEventBus().publish(statsEvent);
        
        if (currentHealth >= maxHealth) {
            PlayerFullHealthEvent fullHealthEvent = new PlayerFullHealthEvent(
                player.getUniqueId(),
                player.getName(),
                maxHealth,
                PlayerFullHealthEvent.FullHealthReason.LOGIN
            );
            rpgCore.getEventBus().publish(fullHealthEvent);
        } else {
            PlayerHealthChangedEvent healthEvent = new PlayerHealthChangedEvent(
                player.getUniqueId(),
                player.getName(),
                0,
                currentHealth,
                maxHealth,
                PlayerHealthChangedEvent.ChangeReason.OTHER
            );
            rpgCore.getEventBus().publish(healthEvent);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        // 保存玩家数据
        UUID uuid = player.getUniqueId();
        double currentHealth = player.getHealth();
        double maxHealth = healthManager.getPlayerMaxHealth(player);
        PlayerStats armorStats = statsManager.getPlayerStats(uuid);
        
        if (plugin.getPlayerDataStorage() != null) {
            plugin.getPlayerDataStorage().savePlayerData(
                uuid, 
                currentHealth, 
                maxHealth, 
                armorStats,
                statsManager.getPlayerSkills(player),
                new ArrayList<>()
            );
            plugin.getLogger().info("[退出] " + player.getName() + 
                " 数据已保存: 血量=" + currentHealth + "/" + maxHealth);
        }
        
        statsManager.clearPlayerAttributes(player);
        statsManager.removePlayer(player.getUniqueId());
        
        if (plugin.getRegenTask() != null) {
            plugin.getRegenTask().removePlayer(player.getUniqueId());
        }
        if (bossBarManager != null) {
            bossBarManager.removeBossBar(player);
        }
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
