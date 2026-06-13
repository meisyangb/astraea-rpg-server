package cn.guangdian.rpgcore.module.armorstats;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.module.RPGModule;
import cn.guangdian.rpgcore.service.api.SkillService;
import cn.guangdian.rpgcore.service.api.StatsService;
import cn.guangdian.rpgcore.service.api.data.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * RPG属性模块
 * 
 * <p>提供玩家RPG属性和技能管理功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ArmorStatsModule extends RPGModule implements StatsService, SkillService {

    // 玩家属性缓存
    private final Map<UUID, Object> playerStatsCache;
    
    // 玩家技能缓存
    private final Map<UUID, Set<String>> playerSkillsCache;
    
    // 技能冷却
    private final Map<UUID, Map<String, Long>> skillCooldowns;
    
    // 技能等级缓存
    private final Map<UUID, Map<String, Integer>> skillLevelsCache;
    
    // 技能点缓存
    private final Map<UUID, Integer> skillPointsCache;

    /**
     * 创建RPG属性模块
     * 
     * @param plugin 插件实例
     */
    public ArmorStatsModule(JavaPlugin plugin) {
        super(plugin, "ArmorStats");
        this.playerStatsCache = new java.util.concurrent.ConcurrentHashMap<>();
        this.playerSkillsCache = new java.util.concurrent.ConcurrentHashMap<>();
        this.skillCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
        this.skillLevelsCache = new java.util.concurrent.ConcurrentHashMap<>();
        this.skillPointsCache = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    protected void registerServices() {
        getServices().registerService(StatsService.class, this);
        log("StatsService registered");
        
        getServices().registerService(SkillService.class, this);
        log("SkillService registered");
    }

    @Override
    protected void registerCommands() {
        // 命令注册将在后续迁移
    }

    @Override
    protected void registerListeners() {
        registerListener(this);
    }

    @Override
    protected void saveAllData() {
        AsyncExecutor executor = getAsyncExecutor();
        for (UUID playerId : playerStatsCache.keySet()) {
            executor.submitPlayerSave(playerId, () -> {
                // 保存逻辑
            });
        }
    }

    @Override
    protected void stopTasks() {
    }

    @Override
    protected void cleanupResources() {
        playerStatsCache.clear();
        playerSkillsCache.clear();
        skillCooldowns.clear();
        skillLevelsCache.clear();
        skillPointsCache.clear();
    }

    // ==================== 事件监听 ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        refreshPlayerStats(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        savePlayerData(playerId);
        clearPlayerCache(playerId);
    }

    // ==================== StatsService 实现 ====================

    @Override
    @Nullable
    public PlayerStats getPlayerStats(UUID playerId) {
        // 返回 null 表示此模块不直接提供 PlayerStats 实现
        // 实际属性通过 getTotalAttack, getTotalDefense 等方法提供
        return null;
    }

    @Override
    public void refreshPlayerStats(Player player) {
        playerStatsCache.put(player.getUniqueId(), new Object());
    }

    @Override
    public double calculateDamage(Player attacker, Player target, double baseDamage) {
        double attack = getTotalAttack(attacker);
        double defense = getTotalDefense(target);
        double damage = baseDamage + attack - defense;
        return Math.max(1, damage);
    }

    @Override
    public double getTotalAttack(Player player) {
        return 10.0;
    }

    @Override
    public double getTotalDefense(Player player) {
        return 5.0;
    }

    @Override
    public double getTotalHealth(Player player) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : player.getMaxHealth();
    }

    @Override
    public double getCritRate(Player player) {
        return 0.1;
    }

    @Override
    public double getCritDamage(Player player) {
        return 1.5;
    }

    @Override
    public void clearPlayerCache(UUID playerId) {
        playerStatsCache.remove(playerId);
        playerSkillsCache.remove(playerId);
        skillCooldowns.remove(playerId);
        skillLevelsCache.remove(playerId);
        skillPointsCache.remove(playerId);
    }

    @Override
    public CompletableFuture<Void> savePlayerData(UUID playerId) {
        return getAsyncExecutor().execute(() -> {});
    }

    // ==================== SkillService 实现 ====================

    @Override
    public boolean triggerActiveSkill(Player player, String skillId) {
        UUID playerId = player.getUniqueId();
        
        if (!isSkillAvailable(playerId, skillId)) return false;
        if (!hasSkill(playerId, skillId)) return false;
        
        boolean success = doTriggerSkill(player, skillId);
        
        if (success) {
            long cooldown = getSkillCooldown(skillId, getSkillLevel(playerId, skillId));
            setCooldown(playerId, skillId, cooldown > 0 ? cooldown : 5000);
        }
        
        return success;
    }

    @Override
    public boolean triggerSkill(Player player, String skillId, Map<String, Object> args) {
        return triggerActiveSkill(player, skillId);
    }

    @Override
    public List<String> getLearnedSkills(UUID playerId) {
        Set<String> skills = playerSkillsCache.get(playerId);
        return skills != null ? new ArrayList<>(skills) : new ArrayList<>();
    }

    @Override
    public int getLearnedSkillCount(UUID playerId) {
        Set<String> skills = playerSkillsCache.get(playerId);
        return skills != null ? skills.size() : 0;
    }

    @Override
    public boolean hasSkill(UUID playerId, String skillId) {
        Set<String> skills = playerSkillsCache.get(playerId);
        if (skills != null && skills.contains(skillId.toLowerCase())) {
            return true;
        }
        return skillExists(skillId);
    }

    @Override
    public boolean learnSkill(UUID playerId, String skillId) {
        Set<String> skills = playerSkillsCache.computeIfAbsent(playerId, k -> new HashSet<>());
        return skills.add(skillId.toLowerCase());
    }

    @Override
    public boolean learnSkill(UUID playerId, String skillId, int cost) {
        if (!hasEnoughSkillPoints(playerId, cost)) return false;
        if (!consumeSkillPoints(playerId, cost, "learn:" + skillId)) return false;
        return learnSkill(playerId, skillId);
    }

    @Override
    public boolean forgetSkill(UUID playerId, String skillId) {
        return forgetSkill(playerId, skillId, false);
    }

    @Override
    public boolean forgetSkill(UUID playerId, String skillId, boolean refund) {
        Set<String> skills = playerSkillsCache.get(playerId);
        if (skills == null || !skills.remove(skillId.toLowerCase())) return false;
        
        if (refund) {
            int level = getSkillLevel(playerId, skillId);
            int refundPoints = getUpgradeCost(skillId, 0) * level;
            addSkillPoints(playerId, refundPoints, "refund:" + skillId);
        }
        
        // 清除等级
        Map<String, Integer> levels = skillLevelsCache.get(playerId);
        if (levels != null) levels.remove(skillId.toLowerCase());
        
        return true;
    }

    @Override
    public int getSkillLevel(UUID playerId, String skillId) {
        if (!hasSkill(playerId, skillId)) return 0;
        Map<String, Integer> levels = skillLevelsCache.get(playerId);
        if (levels == null) return 1;
        return levels.getOrDefault(skillId.toLowerCase(), 1);
    }

    @Override
    public boolean setSkillLevel(UUID playerId, String skillId, int level) {
        if (!hasSkill(playerId, skillId)) return false;
        Map<String, Integer> levels = skillLevelsCache.computeIfAbsent(playerId, k -> new HashMap<>());
        levels.put(skillId.toLowerCase(), Math.max(1, level));
        return true;
    }

    @Override
    public boolean upgradeSkill(UUID playerId, String skillId) {
        int currentLevel = getSkillLevel(playerId, skillId);
        if (currentLevel <= 0) return false; // 未学习
        if (isSkillMaxLevel(playerId, skillId)) return false; // 已满级
        
        int cost = getUpgradeCost(skillId, currentLevel);
        if (!consumeSkillPoints(playerId, cost, "upgrade:" + skillId)) return false;
        
        return setSkillLevel(playerId, skillId, currentLevel + 1);
    }

    @Override
    public int getSkillMaxLevel(String skillId) {
        return 10; // 默认最大等级
    }

    @Override
    public boolean isSkillMaxLevel(UUID playerId, String skillId) {
        return getSkillLevel(playerId, skillId) >= getSkillMaxLevel(skillId);
    }

    @Override
    public int getUpgradeCost(String skillId, int currentLevel) {
        return (currentLevel + 1) * 100; // 简单公式
    }

    @Override
    public boolean isSkillAvailable(UUID playerId, String skillId) {
        return getCooldownRemaining(playerId, skillId) <= 0;
    }

    @Override
    public long getCooldownRemaining(UUID playerId, String skillId) {
        Map<String, Long> cooldowns = skillCooldowns.get(playerId);
        if (cooldowns == null) return 0;
        
        Long endTime = cooldowns.get(skillId.toLowerCase());
        if (endTime == null) return 0;
        
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    @Override
    public void setCooldown(UUID playerId, String skillId, long cooldownMs) {
        Map<String, Long> cooldowns = skillCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        cooldowns.put(skillId.toLowerCase(), System.currentTimeMillis() + cooldownMs);
    }

    @Override
    public void resetCooldown(UUID playerId, String skillId) {
        Map<String, Long> cooldowns = skillCooldowns.get(playerId);
        if (cooldowns != null) cooldowns.remove(skillId.toLowerCase());
    }

    @Override
    public void resetAllCooldowns(UUID playerId) {
        Map<String, Long> cooldowns = skillCooldowns.get(playerId);
        if (cooldowns != null) cooldowns.clear();
    }

    @Override
    public long getSkillCooldown(String skillId, int level) {
        var armorStatsPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (armorStatsPlugin == null || !armorStatsPlugin.isEnabled()) {
            return 5000;
        }
        
        try {
            var getInstanceMethod = armorStatsPlugin.getClass().getMethod("getInstance");
            var instance = getInstanceMethod.invoke(null);
            if (instance == null) return 5000;
            
            var getSkillManagerMethod = instance.getClass().getMethod("getSkillManager");
            var skillManager = getSkillManagerMethod.invoke(instance);
            if (skillManager == null) return 5000;
            
            var getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            var skill = getSkillMethod.invoke(skillManager, skillId);
            if (skill == null) return 5000;
            
            var getCooldownMethod = skill.getClass().getMethod("getCooldown");
            long cooldownSeconds = (Long) getCooldownMethod.invoke(skill);
            return cooldownSeconds * 1000;
        } catch (Exception e) {
            return 5000;
        }
    }

    @Override
    public int getSkillPoints(UUID playerId) {
        return skillPointsCache.getOrDefault(playerId, 0);
    }

    @Override
    public void setSkillPoints(UUID playerId, int points) {
        skillPointsCache.put(playerId, Math.max(0, points));
    }

    @Override
    public void addSkillPoints(UUID playerId, int amount, String reason) {
        setSkillPoints(playerId, getSkillPoints(playerId) + amount);
    }

    @Override
    public boolean consumeSkillPoints(UUID playerId, int amount, String reason) {
        if (!hasEnoughSkillPoints(playerId, amount)) return false;
        setSkillPoints(playerId, getSkillPoints(playerId) - amount);
        return true;
    }

    @Override
    public boolean hasEnoughSkillPoints(UUID playerId, int amount) {
        return getSkillPoints(playerId) >= amount;
    }

    @Override
    public void triggerPassiveSkills(Player player, String triggerType, Map<String, Object> context) {
        // 触发被动技能逻辑
        List<String> passives = getPassiveSkills(player.getUniqueId(), triggerType);
        for (String skillId : passives) {
            doTriggerSkill(player, skillId);
        }
    }

    @Override
    public List<String> getPassiveSkills(UUID playerId, String triggerType) {
        // 返回指定类型的被动技能
        return new ArrayList<>();
    }

    @Override
    public String getSkillName(String skillId) {
        return skillId; // 默认返回ID
    }

    @Override
    public String getSkillDescription(String skillId, int level) {
        return ""; // 默认无描述
    }

    @Override
    public String getSkillType(String skillId) {
        return "active"; // 默认主动技能
    }

    @Override
    public boolean skillExists(String skillId) {
        var armorStatsPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (armorStatsPlugin == null || !armorStatsPlugin.isEnabled()) {
            return false;
        }
        
        try {
            var getInstanceMethod = armorStatsPlugin.getClass().getMethod("getInstance");
            var instance = getInstanceMethod.invoke(null);
            if (instance == null) return false;
            
            var getSkillManagerMethod = instance.getClass().getMethod("getSkillManager");
            var skillManager = getSkillManagerMethod.invoke(instance);
            if (skillManager == null) return false;
            
            var getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            return getSkillMethod.invoke(skillManager, skillId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getAllSkills() {
        return new ArrayList<>();
    }

    @Override
    public CompletableFuture<Integer> getSkillLevelAsync(UUID playerId, String skillId) {
        return CompletableFuture.completedFuture(getSkillLevel(playerId, skillId));
    }

    @Override
    public CompletableFuture<Integer> getSkillPointsAsync(UUID playerId) {
        return CompletableFuture.completedFuture(getSkillPoints(playerId));
    }

    @Override
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        return getAsyncExecutor().execute(() -> {});
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void resetPlayerSkills(UUID playerId) {
        playerSkillsCache.remove(playerId);
        skillLevelsCache.remove(playerId);
        skillCooldowns.remove(playerId);
        setSkillPoints(playerId, 0);
    }

    // ==================== 辅助方法 ====================

    protected boolean doTriggerSkill(Player player, String skillName) {
        var armorStatsPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (armorStatsPlugin == null || !armorStatsPlugin.isEnabled()) {
            return false;
        }
        
        try {
            var getInstanceMethod = armorStatsPlugin.getClass().getMethod("getInstance");
            var instance = getInstanceMethod.invoke(null);
            if (instance == null) return false;
            
            var getSkillManagerMethod = instance.getClass().getMethod("getSkillManager");
            var skillManager = getSkillManagerMethod.invoke(instance);
            if (skillManager == null) return false;
            
            var triggerMethod = skillManager.getClass().getMethod("triggerActiveSkill", Player.class, String.class);
            return (Boolean) triggerMethod.invoke(skillManager, player, skillName);
        } catch (Exception e) {
            log("触发技能失败: " + skillName + " - " + e.getMessage());
            return false;
        }
    }
}