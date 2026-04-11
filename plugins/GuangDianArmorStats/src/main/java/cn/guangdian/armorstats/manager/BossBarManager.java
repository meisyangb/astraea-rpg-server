package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBar管理器
 * 
 * 优化特性:
 * - 使用BossBarOptimizer按需更新，减少网络开销
 * - 支持战斗状态检测，动态调整更新频率
 * - 血量满时隐藏BossBar，受伤时显示
 */
public class BossBarManager {

    private final StatsManager statsManager;
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private BossBarOptimizer bossBarOptimizer;
    private boolean enabled;
    private int updateInterval;
    private String format;
    private boolean hideWhenFull;
    private final GuangDianArmorStats plugin;

    public BossBarManager(GuangDianArmorStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.bossBarOptimizer = null; // 将在配置加载后初始化
        loadConfig();
    }
    
    /**
     * 设置BossBar优化器（由主插件类调用）
     */
    public void setBossBarOptimizer(BossBarOptimizer optimizer) {
        this.bossBarOptimizer = optimizer;
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        var bossBarSection = config.getConfigurationSection("bossbar");
        if (bossBarSection != null) {
            enabled = bossBarSection.getBoolean("enabled", true);
            updateInterval = bossBarSection.getInt("update_interval", 20);
            format = bossBarSection.getString("format", "&c❤ &f%current%&7/&f%max%");
            hideWhenFull = bossBarSection.getBoolean("hide_when_full", true);
        } else {
            enabled = true;
            updateInterval = 20;
            format = "&c❤ &f%current%&7/&f%max%";
            hideWhenFull = true;
        }
    }

    public void startUpdateTask() {
        if (!enabled) return;

        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncRepeating(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateBossBar(player);
                }
            }, 20L, updateInterval);
        }
    }

    public void createBossBar(Player player) {
        if (!enabled) return;

        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
            playerBossBars.put(player.getUniqueId(), bossBar);
        }
        updateBossBar(player);
    }

    public void updateBossBar(Player player) {
        if (!enabled) return;

        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        UUID uuid = player.getUniqueId();
        
        // 使用BossBarOptimizer检查是否需要更新
        if (bossBarOptimizer != null && !bossBarOptimizer.shouldUpdate(uuid, currentHealth)) {
            return; // 跳过更新
        }

        BossBar bossBar = playerBossBars.get(uuid);
        if (bossBar == null) {
            createBossBar(player);
            return;
        }
        
        // 血量满时隐藏BossBar
        if (hideWhenFull && currentHealth >= maxHealth) {
            bossBar.setVisible(false);
            return;
        } else {
            bossBar.setVisible(true);
        }

        String title = format
            .replace("%current%", String.format("%.0f", currentHealth))
            .replace("%max%", String.format("%.0f", maxHealth));
        bossBar.setTitle(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(title)));

        double progress = maxHealth > 0 ? currentHealth / maxHealth : 0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        bossBar.setProgress(progress);

        if (progress > 0.5) {
            bossBar.setColor(BarColor.GREEN);
        } else if (progress > 0.25) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.RED);
        }
        
        // 记录更新
        if (bossBarOptimizer != null) {
            bossBarOptimizer.recordUpdate(uuid, currentHealth);
        }
    }
    
    /**
     * 玩家受伤时调用，进入战斗状态
     */
    public void onPlayerDamaged(Player player) {
        if (bossBarOptimizer != null) {
            bossBarOptimizer.enterCombat(player.getUniqueId());
        }
        // 立即更新BossBar显示
        updateBossBar(player);
    }

    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        // 清理优化器数据
        if (bossBarOptimizer != null) {
            bossBarOptimizer.cleanup(uuid);
        }
    }

    public void removeAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reloadConfig() {
        loadConfig();
        if (enabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                createBossBar(player);
            }
        } else {
            removeAllBossBars();
        }
    }
}
