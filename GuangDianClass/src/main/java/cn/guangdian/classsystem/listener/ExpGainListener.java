package cn.guangdian.classsystem.listener;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.event.ExpGainEvent;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.PlayerClassData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 经验获取监听器
 * 实现等级差惩罚 + 每日经验上限
 */
public class ExpGainListener implements Listener {

    private final GuangDianClass plugin;
    private final ExpManager expManager;

    // 怪物等级配置
    private final Map<String, Integer> mobLevelMap = new HashMap<>();
    private final Map<String, Long> mobExpMap = new HashMap<>();
    private long baseMobExp = 15;
    private int baseMobLevel = 1;
    private boolean mobExpEnabled = true;

    // 每日经验上限
    private long dailyClassExpLimit = 1000;
    private long dailyVanillaExpLimit = 500;
    private boolean dailyLimitEnabled = true;

    // 玩家每日经验追踪 (UUID -> 当日已获取经验)
    private final ConcurrentHashMap<UUID, Long> dailyClassExpEarned = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> dailyVanillaExpEarned = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastResetDate = new ConcurrentHashMap<>();

    // MythicMobs 反射
    private Object mythicMobManager;
    private java.lang.reflect.Method isMythicMobMethod;
    private java.lang.reflect.Method getMythicMobInstanceMethod;
    private boolean mythicMobsEnabled = false;

    public ExpGainListener(GuangDianClass plugin, ExpManager expManager) {
        this.plugin = plugin;
        this.expManager = expManager;
        loadConfig();
        initMythicMobs();
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("exp-sources.monster-kill");
        if (section != null) {
            mobExpEnabled = section.getBoolean("enabled", true);
            baseMobExp = section.getLong("base-exp", 15);
            baseMobLevel = section.getInt("base-level", 1);

            // 怪物经验配置
            ConfigurationSection mobExpSection = section.getConfigurationSection("mob-exp");
            if (mobExpSection != null) {
                for (String mobId : mobExpSection.getKeys(false)) {
                    mobExpMap.put(mobId.toLowerCase(), mobExpSection.getLong(mobId));
                }
            }

            // 怪物等级配置
            ConfigurationSection mobLevelSection = section.getConfigurationSection("mob-level");
            if (mobLevelSection != null) {
                for (String mobId : mobLevelSection.getKeys(false)) {
                    mobLevelMap.put(mobId.toLowerCase(), mobLevelSection.getInt(mobId));
                }
            }
        }

        // 每日经验上限配置
        ConfigurationSection limitSection = plugin.getConfig().getConfigurationSection("exp-sources.daily-limit");
        if (limitSection != null) {
            dailyLimitEnabled = limitSection.getBoolean("enabled", true);
            dailyClassExpLimit = limitSection.getLong("class-exp", 1000);
            dailyVanillaExpLimit = limitSection.getLong("vanilla-exp", 500);
        }
    }

    private void initMythicMobs() {
        if (!Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) return;
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = mythicBukkitClass.getMethod("inst").invoke(null);
            mythicMobManager = mythicBukkitClass.getMethod("getMobManager").invoke(inst);

            Class<?> mobManagerClass = mythicMobManager.getClass();
            isMythicMobMethod = mobManagerClass.getMethod("isMythicMob", org.bukkit.entity.Entity.class);
            getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class);

            mythicMobsEnabled = true;
            plugin.getLogger().info("MythicMobs 经验支持已启用");
        } catch (Exception e) {
            plugin.getLogger().warning("MythicMobs 经验集成初始化失败: " + e.getMessage());
        }
    }

    // ============================================
    // 击杀怪物给职业经验
    // ============================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!mobExpEnabled) return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // 检查每日职业经验上限
        if (dailyLimitEnabled && !checkClassExpLimit(killer)) {
            return;
        }

        // 获取怪物等级和经验
        int mobLevel = getMobLevel(entity);
        long baseExp = getMobExp(entity);

        // 应用等级差惩罚
        int playerLevel = getPlayerTier(killer);
        long finalExp = applyLevelPenalty(baseExp, mobLevel, playerLevel);

        if (finalExp <= 0) return;

        // 检查是否超过每日上限
        long currentEarned = dailyClassExpEarned.getOrDefault(killer.getUniqueId(), 0L);
        long remaining = dailyClassExpLimit - currentEarned;
        if (remaining <= 0) return;

        // 限制在剩余额度内
        finalExp = Math.min(finalExp, remaining);

        // 触发经验获取事件
        ExpGainEvent expEvent = new ExpGainEvent(killer, finalExp, ExpGainEvent.ExpSource.MOB_KILL);
        Bukkit.getPluginManager().callEvent(expEvent);

        if (expEvent.isCancelled()) return;

        // 给予经验
        expManager.addExp(killer.getUniqueId(), expEvent.getAmount());

        // 更新每日经验追踪
        dailyClassExpEarned.merge(killer.getUniqueId(), expEvent.getAmount(), Long::sum);
    }

    // ============================================
    // 拦截原版经验（每日上限）
    // ============================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        if (!dailyLimitEnabled) return;

        Player player = event.getPlayer();
        int amount = event.getAmount();

        if (amount <= 0) return;

        // 检查每日原版经验上限
        long currentEarned = dailyVanillaExpEarned.getOrDefault(player.getUniqueId(), 0L);
        long remaining = dailyVanillaExpLimit - currentEarned;

        if (remaining <= 0) {
            // 已达上限，完全拦截
            event.setAmount(0);
            return;
        }

        // 限制在剩余额度内
        int allowed = (int) Math.min(amount, remaining);
        event.setAmount(allowed);

        // 更新追踪
        dailyVanillaExpEarned.merge(player.getUniqueId(), (long) allowed, Long::sum);
    }

    // ============================================
    // 每日重置检查
    // ============================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkAndResetDaily(player.getUniqueId());
    }

    private boolean checkClassExpLimit(Player player) {
        checkAndResetDaily(player.getUniqueId());
        long current = dailyClassExpEarned.getOrDefault(player.getUniqueId(), 0L);
        return current < dailyClassExpLimit;
    }

    private void checkAndResetDaily(UUID playerId) {
        String today = java.time.LocalDate.now().toString();
        String lastReset = lastResetDate.get(playerId);

        if (!today.equals(lastReset)) {
            // 新的一天，重置
            dailyClassExpEarned.put(playerId, 0L);
            dailyVanillaExpEarned.put(playerId, 0L);
            lastResetDate.put(playerId, today);
        }
    }

    // ============================================
    // 等级差惩罚计算
    // ============================================
    private long applyLevelPenalty(long baseExp, int mobLevel, int playerLevel) {
        int levelDiff = playerLevel - mobLevel;

        if (levelDiff <= 2) {
            // 怪物等级 ≥ 玩家等级-2: 100% 经验
            return baseExp;
        } else if (levelDiff == 3) {
            // 怪物等级 = 玩家等级-3: 50% 经验
            return baseExp / 2;
        } else if (levelDiff >= 5) {
            // 怪物等级 ≤ 玩家等级-5: 10% 经验
            return baseExp / 10;
        } else {
            // levelDiff == 4: 25% 经验
            return baseExp / 4;
        }
    }

    // ============================================
    // 怪物数据获取
    // ============================================
    private int getMobLevel(LivingEntity entity) {
        String mobId = getMobId(entity);

        // 检查是否有特定等级配置
        if (mobId != null && mobLevelMap.containsKey(mobId.toLowerCase())) {
            return mobLevelMap.get(mobId.toLowerCase());
        }

        // 使用基础等级
        return baseMobLevel;
    }

    private long getMobExp(LivingEntity entity) {
        String mobId = getMobId(entity);

        // 检查是否有特定经验配置
        if (mobId != null && mobExpMap.containsKey(mobId.toLowerCase())) {
            return mobExpMap.get(mobId.toLowerCase());
        }

        // 使用基础经验
        return baseMobExp;
    }

    private String getMobId(LivingEntity entity) {
        // 先检查是否是 MythicMob
        if (mythicMobsEnabled && isMythicMob(entity)) {
            return getMythicMobId(entity);
        }

        // 返回原版实体类型
        return entity.getType().name();
    }

    private int getPlayerTier(Player player) {
        PlayerClassData data = plugin.getDataHandler().getPlayerData(player.getUniqueId());
        return data != null ? data.getTier() : 1;
    }

    private boolean isMythicMob(LivingEntity entity) {
        if (!mythicMobsEnabled || mythicMobManager == null || isMythicMobMethod == null) {
            return false;
        }
        try {
            return (boolean) isMythicMobMethod.invoke(mythicMobManager, entity);
        } catch (Exception e) {
            return false;
        }
    }

    private String getMythicMobId(LivingEntity entity) {
        if (!mythicMobsEnabled || mythicMobManager == null || getMythicMobInstanceMethod == null) {
            return null;
        }
        try {
            Object mythicMobInstance = getMythicMobInstanceMethod.invoke(mythicMobManager, entity);
            if (mythicMobInstance != null) {
                String[] methodNames = {"getInternalName", "getMobType", "getType"};
                for (String methodName : methodNames) {
                    try {
                        java.lang.reflect.Method method = mythicMobInstance.getClass().getMethod(methodName);
                        Object result = method.invoke(mythicMobInstance);
                        if (result instanceof String) {
                            return (String) result;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 获取玩家当日已获取职业经验
     */
    public long getDailyClassExpEarned(UUID playerId) {
        checkAndResetDaily(playerId);
        return dailyClassExpEarned.getOrDefault(playerId, 0L);
    }

    /**
     * 获取玩家当日已获取原版经验
     */
    public long getDailyVanillaExpEarned(UUID playerId) {
        checkAndResetDaily(playerId);
        return dailyVanillaExpEarned.getOrDefault(playerId, 0L);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        mobExpMap.clear();
        mobLevelMap.clear();
        loadConfig();
    }
}
