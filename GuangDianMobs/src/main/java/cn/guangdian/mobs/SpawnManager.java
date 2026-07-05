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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class SpawnManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final GuangDianMobs mobPlugin;
    private final List<Fixed> fixed = new ArrayList<>();
    private final List<RandomRule> random = new ArrayList<>();
    private int taskF, taskR;

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
        taskF = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickFixed, 40, 20).getTaskId();
        taskR = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickRandom, 60, 60).getTaskId();
    }

    public void stop() {
        plugin.getServer().getScheduler().cancelTask(taskF);
        plugin.getServer().getScheduler().cancelTask(taskR);
    }

    // ══════════════════════════════ fixed ══════════════

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
            if (now - f.last < f.interval * 50L) continue;
            World w = plugin.getServer().getWorld(f.world);
            if (w == null) continue;
            MobTemplate t = mobPlugin.getMobTemplates().get(f.mob);
            if (t == null) continue;
            int nearby = countNearby(w, f.x, f.y, f.z, f.maxNearby + f.radius);
            if (nearby >= f.maxNearby) continue;
            int n = Math.min(f.amount, f.maxNearby - nearby);
            for (int i = 0; i < n; i++) {
                Location loc = new Location(w, f.x + rng(-f.radius, f.radius), f.y, f.z + rng(-f.radius, f.radius));
                loc.setY(w.getHighestBlockYAt(loc));
                LivingEntity e = mobPlugin.getMobSpawner().spawn(t, loc);
                if (e != null) mobPlugin.getAIController().attach(e, t);
            }
            f.last = now;
        }
    }

    // ══════════════════════════════ random ══════════════

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
            if (now - r.last < r.interval * 50L) continue;
            if (Math.random() > r.chance) { r.last = now; continue; }
            MobTemplate t = mobPlugin.getMobTemplates().get(r.mob);
            if (t == null) continue;
            for (World w : plugin.getServer().getWorlds()) {
                if (!r.worlds.contains(w.getName())) continue;
                var pl = w.getPlayers();
                if (pl.isEmpty()) continue;
                var p = pl.get(ThreadLocalRandom.current().nextInt(pl.size()));
                int rx = ThreadLocalRandom.current().nextInt(20, 61) * (Math.random() > 0.5 ? 1 : -1);
                int rz = ThreadLocalRandom.current().nextInt(20, 61) * (Math.random() > 0.5 ? 1 : -1);
                Location loc = p.getLocation().clone().add(rx, 0, rz);
                loc.setY(r.surface ? w.getHighestBlockYAt(loc) : (r.ht != null ? r.ht[0] + ThreadLocalRandom.current().nextInt(r.ht[1] - r.ht[0]) : loc.getBlockY()));
                if (loc.getBlock().getLightLevel() > r.maxLight || loc.getBlock().getLightLevel() < r.minLight) continue;
                if (countNearby(w, loc.getX(), loc.getY(), loc.getZ(), 30) >= r.maxNearby) continue;
                int cnt = r.amt[0] == r.amt[1] ? r.amt[0] : r.amt[0] + ThreadLocalRandom.current().nextInt(r.amt[1] - r.amt[0] + 1);
                for (int i = 0; i < cnt; i++) {
                    Location sp = loc.clone().add(rng(-3, 3), 0, rng(-3, 3));
                    sp.setY(w.getHighestBlockYAt(sp));
                    LivingEntity e = mobPlugin.getMobSpawner().spawn(t, sp);
                    if (e != null) mobPlugin.getAIController().attach(e, t);
                }
                break;
            }
            r.last = now;
        }
    }

    // ══════════════════════════════ util ══════════════

    private int countNearby(World w, double x, double y, double z, double r) {
        int c = 0;
        for (var e : w.getNearbyEntities(new Location(w, x, y, z), r, r, r))
            if (e instanceof Mob) c++;
        return c;
    }

    private int[] parseRange(ConfigurationSection s, String key, int d1, int d2) {
        if (s.isList(key)) { var l = s.getIntegerList(key); return new int[]{l.get(0), l.size() > 1 ? l.get(1) : l.get(0)}; }
        if (s.isInt(key)) { int v = s.getInt(key); return new int[]{v, v}; }
        return new int[]{d1, d2};
    }

    private double rng(double min, double max) { return min + Math.random() * (max - min); }

    // ══════════════════════════════ data ══════════════

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
