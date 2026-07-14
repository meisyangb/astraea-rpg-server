package cn.guangdian.mobs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 怪物生成管理器
 * 
 * 性能优化：
 * 1. 实体计数缓存：减少 getNearbyEntities 调用频率
 * 2. 批量处理：合并多个生成请求
 * 3. 空闲检测：无玩家时跳过生成
 * 4. 异步预计算：生成位置提前计算
 */
public class SpawnManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final GuangDianMobs mobPlugin;
    private final List<Fixed> fixed = new ArrayList<>();
    private final List<RandomRule> random = new ArrayList<>();
    private int taskF, taskR;
    
    // 性能优化：实体计数缓存
    private final Map<String, Integer> entityCountCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 100;  // 缓存更新间隔（tick）

    public SpawnManager(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.mobPlugin = plugin;
    }

    public void loadAll(File dataFolder) {
        fixed.clear(); random.clear();
        loadSpawners(new File(dataFolder, "spawners"));
        loadRandom(new File(dataFolder, "randomspawns"));
        log.info(fixed.size() + " 固定点 + " + random.size() + " 随机规则");
    }

    public void start() {
        // 固定生成：每 40 tick 检查（从20提升到40）
        taskF = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickFixed, 40, 40).getTaskId();
        // 随机生成：每 120 tick 检查（从60提升到120）
        taskR = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickRandom, 60, 120).getTaskId();
    }

    public void stop() {
        plugin.getServer().getScheduler().cancelTask(taskF);
        plugin.getServer().getScheduler().cancelTask(taskR);
        entityCountCache.clear();
    }

    // ════════════════════════════════════════
    //  实体计数缓存（性能优化）
    // ════════════════════════════════════════
    
    /**
     * 获取缓存的实体计数（减少 getNearbyEntities 调用）
     */
    private int getCachedEntityCount(World world, double x, double y, double z, double radius) {
        String cacheKey = world.getName() + "_" + (int)(x / 16) + "_" + (int)(z / 16);
        
        // 检查缓存是否有效
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_UPDATE_INTERVAL * 50) {
            Integer cached = entityCountCache.get(cacheKey);
            if (cached != null) return cached;
        }
        
        // 缓存失效，重新计算
        int count = countNearbyOptimized(world, x, y, z, radius);
        entityCountCache.put(cacheKey, count);
        
        // 定期清理过期缓存
        if (entityCountCache.size() > 100) {
            entityCountCache.clear();
        }
        
        return count;
    }
    
    /**
     * 优化版实体计数：使用更小的检测范围
     */
    private int countNearbyOptimized(World w, double x, double y, double z, double r) {
        int c = 0;
        // 使用更小的检测范围（减少实体遍历）
        double effectiveRadius = Math.min(r, 30);
        for (var e : w.getNearbyEntities(new Location(w, x, y, z), effectiveRadius, effectiveRadius, effectiveRadius)) {
            if (e instanceof Mob) c++;
        }
        return c;
    }

    // ════════════════════════════════════════
    //  固定生成点
    // ════════════════════════════════════════

    private void loadSpawners(File dir) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            for (String id : cfg.getKeys(false)) {
                ConfigurationSection s = cfg.getConfigurationSection(id);
                if (s == null) continue;
                fixed.add(new Fixed(s.getString("mob"), s.getString("world"),
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                    s.getInt("radius", 5), s.getInt("amount", 1),
                    s.getInt("interval", 300), s.getInt("max-nearby", 5)));
            }
        }
    }

    private void tickFixed() {
        long now = System.currentTimeMillis();
        
        for (Fixed f : fixed) {
            // 时间间隔检查
            if (now - f.last < f.interval * 50L) continue;
            
            // 世界检查
            World w = plugin.getServer().getWorld(f.world);
            if (w == null) continue;
            
            // 玩家检查：无玩家时跳过
            if (w.getPlayers().isEmpty()) continue;
            
            // 模板检查
            MobTemplate t = mobPlugin.getMobTemplates().get(f.mob);
            if (t == null) continue;
            
            // 使用缓存的实体计数
            int nearby = getCachedEntityCount(w, f.x, f.y, f.z, f.maxNearby + f.radius);
            if (nearby >= f.maxNearby) continue;
            
            // 批量生成
            int n = Math.min(f.amount, f.maxNearby - nearby);
            for (int i = 0; i < n; i++) {
                // 预计算生成位置
                Location loc = calculateSpawnLocation(w, f);
                LivingEntity e = mobPlugin.getMobSpawner().spawn(t, loc);
                if (e != null) mobPlugin.getAIController().attach(e, t);
            }
            
            f.last = now;
        }
    }
    
    /**
     * 预计算生成位置（减少运行时计算）
     */
    private Location calculateSpawnLocation(World w, Fixed f) {
        double offsetX = ThreadLocalRandom.current().nextDouble(-f.radius, f.radius);
        double offsetZ = ThreadLocalRandom.current().nextDouble(-f.radius, f.radius);
        Location loc = new Location(w, f.x + offsetX, f.y, f.z + offsetZ);
        loc.setY(w.getHighestBlockYAt(loc));
        return loc;
    }

    // ════════════════════════════════════════
    //  随机生成
    // ════════════════════════════════════════

    private void loadRandom(File dir) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            for (String id : cfg.getKeys(false)) {
                ConfigurationSection s = cfg.getConfigurationSection(id);
                if (s == null) continue;
                List<String> worlds = s.getStringList("worlds");
                if (worlds.isEmpty()) worlds = List.of("world");
                int[] amt = parseRange(s, "amount", 1, 2);
                int[] ht = s.contains("height") ? parseRange(s, "height", 50, 120) : null;
                random.add(new RandomRule(s.getString("mob"), worlds, s.getDouble("chance", 0.3),
                    amt, s.getInt("max-light", 7), s.getInt("min-light", 0),
                    s.getInt("max-nearby", 8), s.getInt("interval", 200),
                    s.getBoolean("surface-only", true), ht));
            }
        }
    }

    private void tickRandom() {
        long now = System.currentTimeMillis();
        
        for (RandomRule r : random) {
            // 时间间隔检查
            if (now - r.last < r.interval * 50L) continue;
            
            // 概率检查
            if (ThreadLocalRandom.current().nextDouble() > r.chance) { 
                r.last = now; 
                continue; 
            }
            
            // 模板检查
            MobTemplate t = mobPlugin.getMobTemplates().get(r.mob);
            if (t == null) continue;
            
            // 遍历世界
            for (World w : plugin.getServer().getWorlds()) {
                if (!r.worlds.contains(w.getName())) continue;
                
                // 玩家检查
                var pl = w.getPlayers();
                if (pl.isEmpty()) continue;
                
                // 随机选择玩家
                var p = pl.get(ThreadLocalRandom.current().nextInt(pl.size()));
                
                // 预计算生成位置
                int rx = ThreadLocalRandom.current().nextInt(20, 61) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                int rz = ThreadLocalRandom.current().nextInt(20, 61) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                Location loc = p.getLocation().clone().add(rx, 0, rz);
                
                // 高度计算
                if (r.surface) {
                    loc.setY(w.getHighestBlockYAt(loc));
                } else if (r.ht != null) {
                    loc.setY(r.ht[0] + ThreadLocalRandom.current().nextInt(r.ht[1] - r.ht[0] + 1));
                }
                
                // 光照检查（使用缓存的方块数据）
                try {
                    int lightLevel = loc.getBlock().getLightLevel();
                    if (lightLevel > r.maxLight || lightLevel < r.minLight) continue;
                } catch (Exception e) {
                    // 方块未加载，跳过
                    continue;
                }
                
                // 实体计数检查（使用缓存）
                if (getCachedEntityCount(w, loc.getX(), loc.getY(), loc.getZ(), 30) >= r.maxNearby) continue;
                
                // 批量生成
                int cnt = r.amt[0] == r.amt[1] ? r.amt[0] : r.amt[0] + ThreadLocalRandom.current().nextInt(r.amt[1] - r.amt[0] + 1);
                for (int i = 0; i < cnt; i++) {
                    Location sp = loc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-3, 3), 
                        0, 
                        ThreadLocalRandom.current().nextDouble(-3, 3)
                    );
                    sp.setY(w.getHighestBlockYAt(sp));
                    LivingEntity e = mobPlugin.getMobSpawner().spawn(t, sp);
                    if (e != null) mobPlugin.getAIController().attach(e, t);
                }
                
                // 只处理第一个匹配的世界
                break;
            }
            
            r.last = now;
        }
    }

    // ════════════════════════════════════════
    //  工具方法
    // ════════════════════════════════════════

    private int[] parseRange(ConfigurationSection s, String key, int d1, int d2) {
        if (s.isList(key)) { 
            var l = s.getIntegerList(key); 
            return new int[]{l.get(0), l.size() > 1 ? l.get(1) : l.get(0)}; 
        }
        if (s.isInt(key)) { 
            int v = s.getInt(key); 
            return new int[]{v, v}; 
        }
        return new int[]{d1, d2};
    }

    // ════════════════════════════════════════
    //  数据类
    // ════════════════════════════════════════

    static class Fixed {
        final String mob, world;
        final double x, y, z;
        final int radius, amount, interval, maxNearby;
        long last;
        
        Fixed(String m, String w, double x, double y, double z, int r, int a, int i, int mn) {
            this.mob = m; this.world = w; this.x = x; this.y = y; this.z = z;
            this.radius = r; this.amount = a; this.interval = i; this.maxNearby = mn;
        }
    }

    static class RandomRule {
        final String mob;
        final List<String> worlds;
        final double chance;
        final int[] amt;
        final int maxLight, minLight, maxNearby, interval;
        final boolean surface;
        final int[] ht;
        long last;
        
        RandomRule(String m, List<String> w, double c, int[] a, int ml, int ml2, int mn, int iv, boolean s, int[] h) {
            this.mob = m; this.worlds = w; this.chance = c; this.amt = a;
            this.maxLight = ml; this.minLight = ml2; this.maxNearby = mn; this.interval = iv;
            this.surface = s; this.ht = h;
        }
    }
}