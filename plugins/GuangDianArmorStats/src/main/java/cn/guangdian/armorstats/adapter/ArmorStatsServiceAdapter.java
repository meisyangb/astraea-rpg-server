package cn.guangdian.armorstats.adapter;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.skill.SkillManager;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.parser.LoreParser;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.monitor.OperationTimer;
import cn.guangdian.rpgcore.monitor.PerformanceMonitor;
import cn.guangdian.rpgcore.service.api.AttributeParseService;
import cn.guangdian.rpgcore.service.api.SkillService;
import cn.guangdian.rpgcore.service.api.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * ArmorStats 服务适配器
 * 
 * <p>连接旧的 GuangDianArmorStats 实现与新的服务接口。
 * 在属性刷新时发布PlayerStatsChangedEvent事件，实现事件驱动通信。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ArmorStatsServiceAdapter implements StatsService, SkillService, AttributeParseService {

    private final GuangDianArmorStats plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private PerformanceMonitor performanceMonitor;
    private Logger logger;

    public ArmorStatsServiceAdapter(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                this.performanceMonitor = rpgCore.getPerformanceMonitor();
                
                registry.registerService(StatsService.class, this);
                registry.registerService(SkillService.class, this);
                registry.registerService(AttributeParseService.class, this);
                logger.info("已注册到 RPGCore: StatsService, SkillService, AttributeParseService");
            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    // ==================== StatsService 实现 ====================

    @Override
    public Object getPlayerStats(UUID playerId) {
        StatsManager statsManager = plugin.getStatsManager();
        Player player = Bukkit.getPlayer(playerId);
        if (statsManager != null && player != null) {
            return statsManager.getPlayerStats(player);
        }
        return null;
    }

    @Override
    public void refreshPlayerStats(Player player) {
        // 性能监控
        OperationTimer timer = null;
        if (performanceMonitor != null) {
            timer = performanceMonitor.startOperation("refreshPlayerStats");
        }
        
        try {
            StatsManager statsManager = plugin.getStatsManager();
            if (statsManager != null) {
                // 获取旧属性值
                double oldHealth = getTotalHealth(player);
                double oldAttack = getTotalAttack(player);
                double oldDefense = getTotalDefense(player);
                
                // 刷新属性
                statsManager.refreshFullStats(player);
                
                // 获取新属性值
                double newHealth = getTotalHealth(player);
                double newAttack = getTotalAttack(player);
                double newDefense = getTotalDefense(player);
                
                // 发布属性变化事件
                publishStatsChangedEvent(player, oldHealth, newHealth, oldAttack, newAttack, oldDefense, newDefense);
            }
        } finally {
            if (timer != null) {
                timer.close();
            }
        }
    }
    
    /**
     * 发布属性变化事件
     */
    private void publishStatsChangedEvent(Player player, 
                                          double oldHealth, double newHealth,
                                          double oldAttack, double newAttack,
                                          double oldDefense, double newDefense) {
        if (eventBus == null) {
            return;
        }
        
        // 检查是否有属性变化
        if (oldHealth != newHealth || oldAttack != newAttack || oldDefense != newDefense) {
            PlayerStatsChangedEvent event = new PlayerStatsChangedEvent(
                player.getUniqueId(),
                player.getName(),
                oldHealth, newHealth,
                oldAttack, newAttack,
                oldDefense, newDefense
            );
            
            eventBus.publish(event);
            logger.fine("Published PlayerStatsChangedEvent for " + player.getName());
        }
    }

    @Override
    public double calculateDamage(Player attacker, Player target, double baseDamage) {
        if (plugin.getDamageManager() != null) {
            // 使用实际伤害计算
            double attack = getTotalAttack(attacker);
            double defense = getTotalDefense(target);
            return Math.max(1, baseDamage + attack - defense);
        }
        
        double attack = getTotalAttack(attacker);
        double defense = getTotalDefense(target);
        return Math.max(1, baseDamage + attack - defense);
    }

    @Override
    public double getTotalAttack(Player player) {
        if (plugin.getDamageManager() != null) {
            return plugin.getDamageManager().getTotalAttack(player);
        }
        return 10.0;
    }

    @Override
    public double getTotalDefense(Player player) {
        if (plugin.getDamageManager() != null) {
            return plugin.getDamageManager().getTotalDefense(player);
        }
        return 5.0;
    }

    @Override
    public double getTotalHealth(Player player) {
        // 返回玩家的实际最大血量（包含装备加成）
        // StatsManager 已经通过 AttributeModifier 应用了装备血量加成
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            return attr.getValue();
        }
        return player.getMaxHealth();
    }

    @Override
    public double getCritRate(Player player) {
        // 需要从 StatsManager 获取
        return 0.1;
    }

    @Override
    public double getCritDamage(Player player) {
        // 需要从 StatsManager 获取
        return 1.5;
    }

    @Override
    public void clearPlayerCache(UUID playerId) {
        StatsManager statsManager = plugin.getStatsManager();
        Player player = Bukkit.getPlayer(playerId);
        if (statsManager != null && player != null) {
            statsManager.clearPlayerAttributes(player);
        }
    }

    @Override
    public CompletableFuture<Void> savePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            // 保存数据逻辑
        });
    }

    // ==================== SkillService 实现 ====================

    @Override
    public boolean triggerActiveSkill(Player player, String skillName) {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            return skillManager.tryTriggerPassiveSkill(player, skillName, 0);
        }
        return false;
    }

    @Override
    public List<String> getLearnedSkills(UUID playerId) {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            return new ArrayList<>(skillManager.getSkills().keySet());
        }
        return new ArrayList<>();
    }
    
    @Override
    public int getLearnedSkillCount(UUID playerId) {
        return getLearnedSkills(playerId).size();
    }

    @Override
    public boolean hasSkill(UUID playerId, String skillName) {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            return skillManager.getSkill(skillName) != null;
        }
        return false;
    }

    @Override
    public boolean learnSkill(UUID playerId, String skillName) {
        // ArmorStats 不支持动态学习技能
        return false;
    }
    
    @Override
    public boolean learnSkill(UUID playerId, String skillName, int cost) {
        // ArmorStats 不支持动态学习技能
        return false;
    }

    @Override
    public boolean forgetSkill(UUID playerId, String skillName) {
        // ArmorStats 不支持遗忘技能
        return false;
    }
    
    @Override
    public boolean forgetSkill(UUID playerId, String skillName, boolean refund) {
        // ArmorStats 不支持遗忘技能
        return false;
    }

    @Override
    public long getCooldownRemaining(UUID playerId, String skillName) {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null && skillManager.isOnCooldown(playerId, skillName)) {
            return 1; // 还在冷却中
        }
        return 0;
    }

    @Override
    public boolean isSkillAvailable(UUID playerId, String skillName) {
        return getCooldownRemaining(playerId, skillName) == 0;
    }

    @Override
    public int getSkillLevel(UUID playerId, String skillName) {
        return hasSkill(playerId, skillName) ? 1 : 0;
    }
    
    @Override
    public boolean setSkillLevel(UUID playerId, String skillName, int level) {
        return false;
    }

    @Override
    public boolean upgradeSkill(UUID playerId, String skillName) {
        return false;
    }
    
    @Override
    public int getSkillMaxLevel(String skillId) {
        return 1;
    }
    
    @Override
    public boolean isSkillMaxLevel(UUID playerId, String skillId) {
        return true;
    }
    
    @Override
    public int getUpgradeCost(String skillId, int currentLevel) {
        return 0;
    }
    
    @Override
    public void setCooldown(UUID playerId, String skillId, long cooldownMs) {
        // ArmorStats 不支持设置冷却
    }
    
    @Override
    public void resetCooldown(UUID playerId, String skillId) {
        // ArmorStats 不支持重置冷却
    }
    
    @Override
    public void resetAllCooldowns(UUID playerId) {
        // ArmorStats 不支持重置冷却
    }
    
    @Override
    public long getSkillCooldown(String skillId, int level) {
        return 0;
    }
    
    @Override
    public int getSkillPoints(UUID playerId) {
        return 0;
    }
    
    @Override
    public void setSkillPoints(UUID playerId, int points) {
        // ArmorStats 不支持技能点
    }
    
    @Override
    public void addSkillPoints(UUID playerId, int amount, String reason) {
        // ArmorStats 不支持技能点
    }
    
    @Override
    public boolean consumeSkillPoints(UUID playerId, int amount, String reason) {
        return false;
    }
    
    @Override
    public boolean hasEnoughSkillPoints(UUID playerId, int amount) {
        return false;
    }
    
    @Override
    public void triggerPassiveSkills(Player player, String triggerType, Map<String, Object> context) {
        // ArmorStats 被动技能触发
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            skillManager.tryTriggerPassiveSkill(player, triggerType, 0);
        }
    }
    
    @Override
    public List<String> getPassiveSkills(UUID playerId, String triggerType) {
        return new ArrayList<>();
    }
    
    @Override
    public String getSkillName(String skillId) {
        return skillId;
    }
    
    @Override
    public String getSkillDescription(String skillId, int level) {
        return "";
    }
    
    @Override
    public String getSkillType(String skillId) {
        return "passive";
    }
    
    @Override
    public boolean skillExists(String skillId) {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            return skillManager.getSkill(skillId) != null;
        }
        return false;
    }
    
    @Override
    public List<String> getAllSkills() {
        SkillManager skillManager = plugin.getSkillManager();
        if (skillManager != null) {
            return new ArrayList<>(skillManager.getSkills().keySet());
        }
        return new ArrayList<>();
    }
    
    @Override
    public CompletableFuture<Integer> getSkillLevelAsync(UUID playerId, String skillId) {
        return CompletableFuture.completedFuture(getSkillLevel(playerId, skillId));
    }
    
    @Override
    public CompletableFuture<Integer> getSkillPointsAsync(UUID playerId) {
        return CompletableFuture.completedFuture(0);
    }
    
    @Override
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void resetPlayerSkills(UUID playerId) {
        // ArmorStats 不支持重置技能
    }
    
    @Override
    public boolean triggerSkill(Player player, String skillId, Map<String, Object> args) {
        return triggerActiveSkill(player, skillId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    // ==================== AttributeParseService 实现 ====================

    @Override
    public Map<String, Object> parseItemAttributes(ItemStack item) {
        Map<String, Object> result = new HashMap<>();
        Map<String, AttributeValue> attrs = LoreParser.parse(item);
        
        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        
        return result;
    }

    @Override
    public Object createEmptyAttributes() {
        StatsManager statsManager = plugin.getStatsManager();
        if (statsManager != null) {
            return statsManager.createEmptyStats();
        }
        return new PlayerStats();
    }

    @Override
    public void addAttribute(Object container, String attributeName, Object value) {
        if (container instanceof PlayerStats && value instanceof AttributeValue) {
            PlayerStats stats = (PlayerStats) container;
            AttributeValue attrValue = (AttributeValue) value;
            Map<String, AttributeValue> map = new HashMap<>();
            map.put(attributeName, attrValue);
            stats.addStats(map);
        }
    }

    @Override
    public void mergeAttributes(Object container, Map<String, Object> attributes) {
        if (container instanceof PlayerStats) {
            PlayerStats stats = (PlayerStats) container;
            Map<String, AttributeValue> attrs = new HashMap<>();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (entry.getValue() instanceof AttributeValue) {
                    attrs.put(entry.getKey(), (AttributeValue) entry.getValue());
                }
            }
            stats.addStats(attrs);
        }
    }

    @Override
    public void setExternalAccessoryStats(Player player, Object accessoryAttributes) {
        StatsManager statsManager = plugin.getStatsManager();
        if (statsManager != null && accessoryAttributes instanceof PlayerStats) {
            statsManager.setExternalAccessoryStats(player, (PlayerStats) accessoryAttributes);
        }
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(StatsService.class);
                registry.unregisterService(SkillService.class);
                registry.unregisterService(AttributeParseService.class);
                plugin.getLogger().info("已从 RPGCore 注销: StatsService, SkillService, AttributeParseService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}