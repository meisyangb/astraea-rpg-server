package cn.guangdian.realm;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 修为监听器
 * 
 * 监听怪物击杀事件，给予玩家修为
 */
public class CultivationListener implements Listener {
    private final GuangDianRealm plugin;
    
    public CultivationListener(GuangDianRealm plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        
        LivingEntity dead = e.getEntity();
        
        // 获取怪物信息
        String mobName = getMobName(dead);
        int mobLevel = getMobLevel(dead);
        
        // 计算修为
        long cultivation = plugin.getMobCultivation(mobName, mobLevel);
        
        if (cultivation > 0) {
            plugin.addCultivation(killer, cultivation);
        }
    }
    
    /**
     * 获取怪物名称
     */
    private String getMobName(LivingEntity entity) {
        // 优先使用自定义名称
        if (entity.getCustomName() != null) {
            return stripColor(entity.getCustomName());
        }
        
        // 使用实体类型名称
        return entity.getType().name();
    }
    
    /**
     * 获取怪物等级
     */
    private int getMobLevel(LivingEntity entity) {
        // 简化版：通过血量估算等级
        // 实际应该从GuangDianMobs或其他插件获取
        
        double maxHealth = entity.getMaxHealth();
        
        // 按血量估算等级
        if (maxHealth <= 20) return 1;
        if (maxHealth <= 40) return 3;
        if (maxHealth <= 80) return 5;
        if (maxHealth <= 150) return 8;
        if (maxHealth <= 300) return 10;
        if (maxHealth <= 500) return 15;
        if (maxHealth <= 1000) return 20;
        if (maxHealth <= 2000) return 30;
        if (maxHealth <= 5000) return 40;
        if (maxHealth <= 10000) return 50;
        
        return 60;
    }
    
    /**
     * 移除颜色代码
     */
    private String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("<[^>]+>", "");
    }
}