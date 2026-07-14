package cn.guangdian.realm;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeType;
import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 境界属性集成类
 * 
 * 通过 GuangDianArmorStats API 给玩家增加境界属性加成
 */
public class RealmAttributeIntegration {
    private final GuangDianRealm plugin;
    private GuangDianArmorStats armorStatsPlugin;
    
    public RealmAttributeIntegration(GuangDianRealm plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化集成
     */
    public boolean init() {
        armorStatsPlugin = (GuangDianArmorStats) Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (armorStatsPlugin == null) {
            plugin.getLogger().warning("未找到 GuangDianArmorStats 插件，境界属性加成将不会生效!");
            return false;
        }
        plugin.getLogger().info("已连接 GuangDianArmorStats，境界属性加成已启用!");
        return true;
    }
    
    /**
     * 应用境界属性加成
     */
    public void applyRealmBonus(Player player, Realm realm) {
        if (armorStatsPlugin == null) return;
        
        Realm.RealmBonuses bonuses = realm.getBonuses();
        
        // 获取当前玩家属性
        PlayerStats stats = armorStatsPlugin.getStatsManager().getPlayerStats(player);
        
        // 添加境界加成
        stats.add(AttributeType.MAX_HEALTH, bonuses.getMaxHealth());
        stats.add(AttributeType.MIN_ATTACK, bonuses.getAttackDamage());
        stats.add(AttributeType.MAX_ATTACK, bonuses.getAttackDamage());
        stats.add(AttributeType.DEFENSE_MIN, bonuses.getDefense());
        stats.add(AttributeType.DEFENSE_MAX, bonuses.getDefense());
        
        // 刷新玩家属性
        armorStatsPlugin.getStatsManager().refreshFullStats(player);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("已为玩家 " + player.getName() + " 应用境界属性: " + realm.getName());
        }
    }
    
    /**
     * 移除境界属性加成
     */
    public void removeRealmBonus(Player player, Realm realm) {
        if (armorStatsPlugin == null) return;
        
        Realm.RealmBonuses bonuses = realm.getBonuses();
        
        PlayerStats stats = armorStatsPlugin.getStatsManager().getPlayerStats(player);
        
        // 移除境界加成（减去）
        stats.add(AttributeType.MAX_HEALTH, -bonuses.getMaxHealth());
        stats.add(AttributeType.MIN_ATTACK, -bonuses.getAttackDamage());
        stats.add(AttributeType.MAX_ATTACK, -bonuses.getAttackDamage());
        stats.add(AttributeType.DEFENSE_MIN, -bonuses.getDefense());
        stats.add(AttributeType.DEFENSE_MAX, -bonuses.getDefense());
        
        armorStatsPlugin.getStatsManager().refreshFullStats(player);
    }
    
    /**
     * 刷新玩家境界属性
     */
    public void refreshRealmAttributes(Player player) {
        if (armorStatsPlugin == null) return;
        
        Realm currentRealm = plugin.getCurrentRealm(player);
        if (currentRealm == null) return;
        
        applyRealmBonus(player, currentRealm);
    }
    
    /**
     * 更新境界属性（境界变化时调用）
     */
    public void updateRealmAttributes(Player player, Realm oldRealm, Realm newRealm) {
        if (armorStatsPlugin == null) return;
        
        // 先移除旧境界加成
        if (oldRealm != null) {
            removeRealmBonus(player, oldRealm);
        }
        
        // 再添加新境界加成
        if (newRealm != null) {
            applyRealmBonus(player, newRealm);
        }
    }
}