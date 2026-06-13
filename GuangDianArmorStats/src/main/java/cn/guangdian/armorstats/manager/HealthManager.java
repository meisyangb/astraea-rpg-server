package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * 生命值管理器
 * 
 * 支持功能：
 * - 生命值同步
 * - 生命值缩放（控制显示行数）
 */
public class HealthManager {

    private final StatsManager statsManager;
    private final GuangDianArmorStats plugin;
    
    // 配置
    private boolean enableScale;
    private int maxRows;
    private double healthPerRow; // 每行显示的生命值（默认 20 = 10颗心）
    
    // Minecraft 默认值
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double HEARTS_PER_ROW = 10.0; // 每行 10 颗心
    private static final double MAX_HEALTH_LIMIT = 2000000.0;

    public HealthManager(GuangDianArmorStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    public void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("health_display");
        if (section != null) {
            enableScale = section.getBoolean("enable_scale", true);
            maxRows = section.getInt("max_rows", 2);
        } else {
            enableScale = true;
            maxRows = 2;
        }
        
        // 计算每行显示的生命值
        healthPerRow = DEFAULT_MAX_HEALTH;
        
        plugin.getLogger().info("[HealthManager] 配置加载: enableScale=" + enableScale + ", maxRows=" + maxRows);
    }
    
    /**
     * 立即应用生命值缩放（玩家加入时调用）
     * 避免显示超多行
     */
    public void applyHealthScaleImmediately(Player player) {
        if (!enableScale) {
            player.setHealthScale(DEFAULT_MAX_HEALTH);
            return;
        }
        
        // 立即应用缩放到 maxRows 行
        double maxDisplayHealth = maxRows * healthPerRow; // 2 * 20 = 40
        player.setHealthScale(maxDisplayHealth);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[HealthManager] " + player.getName() + 
                " 立即应用缩放: 显示=" + maxDisplayHealth + 
                ", 行数=" + maxRows);
        }
    }
    
    /**
     * 应用生命值缩放
     * 
     * 原理：
     * - Minecraft 默认显示 10 颗心（20 生命值）
     * - 当玩家生命值超过 20 时，可以通过缩放让显示保持在 2 行内
     * - 例如：玩家有 100 生命值，缩放后显示为 20（每颗心代表 5 生命值）
     * 
     * @param player 玩家
     * @param actualMaxHealth 实际最大生命值
     */
    public void applyHealthScale(Player player, double actualMaxHealth) {
        if (!enableScale) {
            // 不缩放，使用默认显示
            player.setHealthScale(DEFAULT_MAX_HEALTH);
            return;
        }
        
        // 计算缩放后的显示值
        double maxDisplayHealth = maxRows * healthPerRow;
        
        if (actualMaxHealth <= maxDisplayHealth) {
            // 生命值在显示范围内，不需要缩放
            player.setHealthScale(actualMaxHealth);
        } else {
            // 生命值超过显示范围，缩放到 maxRows 行
            player.setHealthScale(maxDisplayHealth);
        }
        
        // 只在调试模式输出日志
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[HealthManager] " + player.getName() + 
                " 生命值缩放: 实际=" + actualMaxHealth + 
                ", 显示=" + player.getHealthScale() + 
                ", 行数=" + (player.getHealthScale() / healthPerRow));
        }
    }

    public void syncPlayerHealth(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) return;

        double bonusHealth = stats.getMaxHealth();  // 装备额外血量
        double currentHealth = player.getHealth();
        double currentMaxHealth = getPlayerMaxHealth(player);  // 实际最大血量
        double expectedMaxHealth = Math.min(20.0 + bonusHealth, MAX_HEALTH_LIMIT);

        // 如果实际最大血量与期望不符，需要重新应用
        if (Math.abs(currentMaxHealth - expectedMaxHealth) > 0.1) {
            statsManager.applyMaxHealth(player, stats);
        }

        // 如果当前血量超过实际最大血量，调整到最大血量
        // 注意：使用 currentMaxHealth，不是 bonusHealth
        if (currentHealth > currentMaxHealth) {
            player.setHealth(currentMaxHealth);
        }
        
        // 应用生命值缩放
        applyHealthScale(player, currentMaxHealth);
    }

    public void setPlayerHealth(Player player, double health) {
        double maxHealth = getPlayerMaxHealth(player);
        player.setHealth(Math.min(health, maxHealth));
    }

    public double getPlayerMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            return attribute.getValue();
        }
        return 20.0;
    }

    public void restoreFullHealth(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats != null) {
            // stats.getMaxHealth() 是装备提供的额外血量，玩家基础血量是 20
            double totalMaxHealth = Math.min(20.0 + stats.getMaxHealth(), MAX_HEALTH_LIMIT);
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                // 使用实际的当前最大血量（可能已经被 applyMaxHealth 设置）
                double currentMaxHealth = attribute.getValue();
                player.setHealth(currentMaxHealth);
                
                // 应用生命值缩放
                applyHealthScale(player, currentMaxHealth);
            } else {
                player.setHealth(Math.min(totalMaxHealth, 20.0));
            }
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
        
        // 更新所有在线玩家的生命值缩放
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            double maxHealth = getPlayerMaxHealth(player);
            applyHealthScale(player, maxHealth);
        }
    }
}
