package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成管理器
 */
public class SpawnManager {

    private final GuangDianMobs plugin;
    private final List<SpawnRule> spawnRules = new ArrayList<>();
    private final Map<String, Long> taskIds = new HashMap<>();
    private SyncScheduler scheduler;

    public SpawnManager(GuangDianMobs plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.scheduler = rpgCore.getScheduler();
        }
    }

    /**
     * 加载生成规则
     */
    public void loadSpawns() {
        spawnRules.clear();

        File file = new File(plugin.getDataFolder(), "spawns.yml");
        if (!file.exists()) {
            plugin.saveResource("spawns.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("spawns");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection spawnSection = section.getConfigurationSection(id);
            if (spawnSection == null) continue;

            try {
                SpawnRule rule = parseSpawnRule(id, spawnSection);
                spawnRules.add(rule);
                plugin.getLogger().info("加载生成规则: " + id + " - 怪物: " + rule.getMobId());
            } catch (Exception e) {
                plugin.getLogger().warning("加载生成规则失败: " + id + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("共加载 " + spawnRules.size() + " 个生成规则");

        // 启动生成任务
        startSpawnTasks();
    }

    /**
     * 解析生成规则
     */
    private SpawnRule parseSpawnRule(String id, ConfigurationSection section) {
        SpawnRule rule = new SpawnRule(id);

        rule.setMobId(section.getString("mob"));
        rule.setWorld(section.getString("worlds", "world"));

        // 解析条件配置
        ConfigurationSection conditions = section.getConfigurationSection("conditions");
        if (conditions != null) {
            rule.setTimeCondition(conditions.getString("time", "any"));
            rule.setWeatherCondition(conditions.getString("weather", "any"));
            rule.setLightCondition(conditions.getString("light", "0-15"));
            rule.setHeightCondition(conditions.getString("height", "0-320"));
        }

        rule.setChance(section.getDouble("chance", 0.5));
        rule.setMaxMobs(section.getInt("max-mobs", 10));

        return rule;
    }

    /**
     * 启动生成任务
     */
    private void startSpawnTasks() {
        // 取消旧任务
        for (long taskId : taskIds.values()) {
            if (scheduler != null) {
                scheduler.cancelTask(taskId);
            }
        }
        taskIds.clear();

        // 启动新任务 (使用 RPGCore SyncScheduler)
        for (SpawnRule rule : spawnRules) {
            if (scheduler != null) {
                long taskId = scheduler.runSyncRepeating(
                    () -> trySpawn(rule),
                    300L,  // 默认15秒间隔
                    300L
                );
                taskIds.put(rule.getId(), taskId);
            }
        }
    }

    /**
     * 尝试生成怪物
     */
    private void trySpawn(SpawnRule rule) {
        // 检查几率
        if (ThreadLocalRandom.current().nextDouble() > rule.getChance()) {
            return;
        }

        World world = Bukkit.getWorld(rule.getWorld());
        if (world == null) return;

        // 检查生成条件
        if (!checkSpawnConditions(rule, world)) {
            return;
        }

        // 检查当前怪物数量
        int currentMobs = countMobsInWorld(world, rule.getMobId());
        if (currentMobs >= rule.getMaxMobs()) {
            return;
        }

        // 生成怪物 - 在随机玩家附近生成
        Player targetPlayer = getRandomPlayerInWorld(world);
        if (targetPlayer == null) return;

        Location spawnLoc = getRandomLocationNearPlayer(targetPlayer);
        if (spawnLoc != null) {
            plugin.getMobManager().spawnMob(rule.getMobId(), spawnLoc);
        }
    }

    /**
     * 检查生成条件
     */
    private boolean checkSpawnConditions(SpawnRule rule, World world) {
        // 检查时间条件
        String timeCondition = rule.getTimeCondition();
        if (!"any".equalsIgnoreCase(timeCondition)) {
            long time = world.getTime();
            boolean isDay = time >= 0 && time < 13000;
            if ("day".equalsIgnoreCase(timeCondition) && !isDay) return false;
            if ("night".equalsIgnoreCase(timeCondition) && isDay) return false;
        }

        // 检查天气条件
        String weatherCondition = rule.getWeatherCondition();
        if (!"any".equalsIgnoreCase(weatherCondition)) {
            boolean hasStorm = world.hasStorm();
            boolean isThundering = world.isThundering();
            if ("clear".equalsIgnoreCase(weatherCondition) && (hasStorm || isThundering)) return false;
            if ("rain".equalsIgnoreCase(weatherCondition) && !hasStorm) return false;
            if ("storm".equalsIgnoreCase(weatherCondition) && !isThundering) return false;
        }

        return true;
    }

    /**
     * 获取世界中的随机玩家
     */
    private Player getRandomPlayerInWorld(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return null;
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }

    /**
     * 获取玩家附近的随机位置
     */
    private Location getRandomLocationNearPlayer(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // 在玩家周围随机位置生成（20-50格范围）
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble(20, 50);

        int x = (int) (center.getX() + distance * Math.cos(angle));
        int z = (int) (center.getZ() + distance * Math.sin(angle));

        // 找到合适的高度
        int y = world.getHighestBlockYAt(x, z);
        if (y < 0 || y > 320) return null;

        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    /**
     * 计算世界中的怪物数量
     */
    private int countMobsInWorld(World world, String mobId) {
        int count = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                String entityMobId = plugin.getMobManager().getMobIdFromEntity(living);
                if (mobId.equals(entityMobId)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 停止所有生成任务
     */
    public void stopSpawnTasks() {
        for (long taskId : taskIds.values()) {
            if (scheduler != null) {
                scheduler.cancelTask(taskId);
            }
        }
        taskIds.clear();
    }

    /**
     * 生成规则类
     */
    public static class SpawnRule {
        private String id;
        private String mobId;
        private String world;
        private int maxMobs;
        private double chance;

        // 条件配置
        private String timeCondition = "any";
        private String weatherCondition = "any";
        private String lightCondition = "0-15";
        private String heightCondition = "0-320";

        public SpawnRule(String id) {
            this.id = id;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getMobId() { return mobId; }
        public void setMobId(String mobId) { this.mobId = mobId; }

        public String getWorld() { return world; }
        public void setWorld(String world) { this.world = world; }

        public int getMaxMobs() { return maxMobs; }
        public void setMaxMobs(int maxMobs) { this.maxMobs = maxMobs; }

        public double getChance() { return chance; }
        public void setChance(double chance) { this.chance = chance; }

        public String getTimeCondition() { return timeCondition; }
        public void setTimeCondition(String timeCondition) { this.timeCondition = timeCondition; }

        public String getWeatherCondition() { return weatherCondition; }
        public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

        public String getLightCondition() { return lightCondition; }
        public void setLightCondition(String lightCondition) { this.lightCondition = lightCondition; }

        public String getHeightCondition() { return heightCondition; }
        public void setHeightCondition(String heightCondition) { this.heightCondition = heightCondition; }
    }
}
