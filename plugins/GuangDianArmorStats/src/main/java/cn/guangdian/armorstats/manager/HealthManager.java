package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealthManager {

    private final StatsManager statsManager;

    public HealthManager(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public void syncPlayerHealth(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) return;

        double bonusHealth = stats.getMaxHealth();  // 装备额外血量
        double currentHealth = player.getHealth();
        double currentMaxHealth = getPlayerMaxHealth(player);  // 实际最大血量
        double expectedMaxHealth = 20.0 + bonusHealth;  // 期望的最大血量

        // 如果实际最大血量与期望不符，需要重新应用
        if (Math.abs(currentMaxHealth - expectedMaxHealth) > 0.1) {
            statsManager.applyMaxHealth(player, stats);
        }

        // 如果当前血量超过实际最大血量，调整到最大血量
        // 注意：使用 currentMaxHealth，不是 bonusHealth
        if (currentHealth > currentMaxHealth) {
            player.setHealth(currentMaxHealth);
        }
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
            double totalMaxHealth = 20.0 + stats.getMaxHealth();
            AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) {
                // 使用实际的当前最大血量（可能已经被 applyMaxHealth 设置）
                double currentMaxHealth = attribute.getValue();
                player.setHealth(currentMaxHealth);
            } else {
                player.setHealth(Math.min(totalMaxHealth, 20.0));
            }
        }
    }
}
