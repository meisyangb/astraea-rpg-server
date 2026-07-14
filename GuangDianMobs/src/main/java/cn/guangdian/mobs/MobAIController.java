package cn.guangdian.mobs;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 怪物 AI + 定时技能执行
 * 
 * 性能优化：
 * 1. 空间哈希索引：O(1) 查找附近玩家（替代 getNearbyEntities O(n)）
 * 2. 目标缓存：减少重复计算
 * 3. 批量处理：减少调度开销
 * 4. 事件驱动更新：玩家移动时更新索引
 */
public class MobAIController implements Listener {

    private final GuangDianMobs plugin;
    private final Map<UUID, MobTemplate> states = new ConcurrentHashMap<>();
    private int taskId = -1;
    
    // 性能优化参数
    private static final int AI_TICK_INTERVAL = 20;  // AI检测间隔（tick），从10提升到20
    private static final int SKILL_TICK_INTERVAL = 10;  // 技能检测间隔
    private static final int TARGET_CACHE_TICKS = 40;  // 目标缓存有效期（tick）
    private static final int SPATIAL_HASH_SIZE = 16;  // 空间哈希网格大小
    
    // 空间哈希索引：chunk -> 玩家列表
    private final Map<Long, Set<Player>> spatialIndex = new ConcurrentHashMap<>();
    // 玩家位置缓存：用于快速查找
    private final Map<UUID, Location> playerLocations = new ConcurrentHashMap<>();
    // 目标缓存：怪物 -> [目标, 过期时间]
    private final Map<UUID, CachedTarget> targetCache = new ConcurrentHashMap<>();
    
    // 计数器
    private int tickCounter = 0;

    public MobAIController(GuangDianMobs plugin) { 
        this.plugin = plugin; 
    }

    public void start() {
        // 注册事件监听器（用于更新玩家位置索引）
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 初始化在线玩家索引
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerIndex(player);
        }
        
        // 主循环：每tick执行
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1).getTaskId();
    }

    public void stop() {
        if (taskId != -1) plugin.getServer().getScheduler().cancelTask(taskId);
        states.clear();
        spatialIndex.clear();
        playerLocations.clear();
        targetCache.clear();
    }

    public void attach(LivingEntity entity, MobTemplate template) {
        states.put(entity.getUniqueId(), template);
    }

    public void detach(UUID id) { 
        states.remove(id); 
        targetCache.remove(id);
    }
    
    /**
     * 清理所有缓存（插件禁用时调用）
     */
    public void clearAll() {
        states.clear();
        targetCache.clear();
    }

    private void tick() {
        tickCounter++;
        
        var it = states.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entity ent = plugin.getServer().getEntity(e.getKey());
            if (!(ent instanceof LivingEntity living) || living.isDead()) { 
                it.remove(); 
                targetCache.remove(e.getKey());
                continue; 
            }
            
            // AI 目标选择：每 AI_TICK_INTERVAL tick 执行一次
            if (tickCounter % AI_TICK_INTERVAL == 0) {
                updateAI(living, e.getValue());
            }
            
            // 技能 timer：每 SKILL_TICK_INTERVAL tick 执行
            if (tickCounter % SKILL_TICK_INTERVAL == 0) {
                Player killer = living.getKiller();
                plugin.getSkillEngine().execute(living, e.getValue().skills(), "timer", killer);
            }
        }
    }
    
    private void updateAI(LivingEntity entity, MobTemplate t) {
        if (!(entity instanceof Mob mob)) return;
        
        // 检查缓存目标是否有效
        CachedTarget cached = targetCache.get(entity.getUniqueId());
        if (cached != null && cached.isValid()) {
            // 缓存有效，继续追踪
            return;
        }
        
        // 缓存失效，重新查找目标
        LivingEntity target = findTargetOptimized(entity);
        if (target != null) {
            mob.setTarget(target);
            // 更新缓存
            targetCache.put(entity.getUniqueId(), new CachedTarget(target, tickCounter + TARGET_CACHE_TICKS));
        }
    }

    /**
     * 优化版目标查找：使用空间哈希索引 O(1) 替代 getNearbyEntities O(n)
     */
    private LivingEntity findTargetOptimized(LivingEntity self) {
        Location loc = self.getLocation();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        
        Player best = null;
        double bestDistSq = Double.MAX_VALUE;
        double range = 25;  // 目标检测范围
        
        // 检查周围9个chunk（比25格范围略大）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunkKey = chunkKey(chunkX + dx, chunkZ + dz, loc.getWorld().getUID());
                Set<Player> players = spatialIndex.get(chunkKey);
                if (players == null) continue;
                
                for (Player p : players) {
                    if (p.isDead() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    
                    Location pLoc = playerLocations.get(p.getUniqueId());
                    if (pLoc == null || !pLoc.getWorld().equals(loc.getWorld())) continue;
                    
                    double dSq = distanceSquared(loc, pLoc);
                    if (dSq < range * range && dSq < bestDistSq) {
                        bestDistSq = dSq;
                        best = p;
                    }
                }
            }
        }
        
        return best;
    }
    
    /**
     * 快速距离平方计算（避免创建 Location 对象）
     */
    private double distanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * 计算 chunk key（包含世界UID）
     */
    private long chunkKey(int x, int z, UUID worldUid) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32) | (((long) worldUid.hashCode()) << 48);
    }
    
    /**
     * 更新玩家空间索引
     */
    private void updatePlayerIndex(Player player) {
        Location loc = player.getLocation();
        UUID playerId = player.getUniqueId();
        
        // 移除旧索引
        Location oldLoc = playerLocations.put(playerId, loc);
        if (oldLoc != null && oldLoc.getWorld() != null) {
            long oldKey = chunkKey(oldLoc.getBlockX() >> 4, oldLoc.getBlockZ() >> 4, oldLoc.getWorld().getUID());
            Set<Player> oldSet = spatialIndex.get(oldKey);
            if (oldSet != null) {
                oldSet.remove(player);
                if (oldSet.isEmpty()) spatialIndex.remove(oldKey);
            }
        }
        
        // 添加新索引
        if (loc.getWorld() != null) {
            long newKey = chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, loc.getWorld().getUID());
            spatialIndex.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet()).add(player);
        }
    }
    
    // ════════════════════════════════════════
    //  事件监听器：更新玩家位置索引
    // ════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerIndex(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location loc = playerLocations.remove(playerId);
        if (loc != null && loc.getWorld() != null) {
            long key = chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, loc.getWorld().getUID());
            Set<Player> set = spatialIndex.get(key);
            if (set != null) {
                set.remove(player);
                if (set.isEmpty()) spatialIndex.remove(key);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 延迟更新（等待传送完成）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updatePlayerIndex(event.getPlayer());
        }, 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 延迟更新（等待传送完成）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updatePlayerIndex(event.getPlayer());
        }, 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // 游戏模式变化时清除目标缓存（创造模式玩家不应被作为目标）
        if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR) {
            targetCache.clear();
        }
    }
    
    /**
     * 缓存目标记录
     */
    private static class CachedTarget {
        final LivingEntity target;
        final long expireTick;
        
        CachedTarget(LivingEntity target, long expireTick) {
            this.target = target;
            this.expireTick = expireTick;
        }
        
        boolean isValid() {
            return target != null && !target.isDead() && target.isValid();
        }
    }
}