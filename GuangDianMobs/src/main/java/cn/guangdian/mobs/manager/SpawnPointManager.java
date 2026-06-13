package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.mobs.model.SpawnPoint;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷新点管理器
 * 管理自定义刷新点的创建、加载和定时刷新
 */
public class SpawnPointManager {

    private final GuangDianMobs plugin;
    private final Map<String, SpawnPoint> spawnPoints = new ConcurrentHashMap<>();
    private final Map<UUID, String> spawnedMobs = new ConcurrentHashMap<>(); // entity UUID -> spawn point ID
    private File configFile;
    private YamlConfiguration config;
    private long updateTaskId = -1;

    public SpawnPointManager(GuangDianMobs plugin) {
        this.plugin = plugin;
        loadConfig();
        startUpdateTask();
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "spawnpoints.yml");
        if (!configFile.exists()) {
            plugin.saveResource("spawnpoints.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadSpawnPoints();
    }

    /**
     * 加载所有刷新点
     */
    public void loadSpawnPoints() {
        spawnPoints.clear();

        ConfigurationSection section = config.getConfigurationSection("spawnpoints");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            SpawnPoint point = loadSpawnPoint(section.getConfigurationSection(id));
            if (point != null) {
                spawnPoints.put(id, point);
            }
        }

        plugin.getLogger().info("已加载 " + spawnPoints.size() + " 个刷新点");
    }

    /**
     * 加载单个刷新点
     */
    private SpawnPoint loadSpawnPoint(ConfigurationSection section) {
        if (section == null) return null;

        String id = section.getName();
        SpawnPoint point = new SpawnPoint(id);

        // 加载位置
        String worldName = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        if (worldName == null) return null;

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;

        point.setLocation(new Location(world, x, y, z, yaw, pitch));

        // 加载配置
        point.setDisplayName(section.getString("display-name", id));
        point.setMobId(section.getString("mob"));
        point.setLevel(section.getInt("level", -1));
        point.setAmount(section.getInt("amount", 1));
        point.setMaxMobs(section.getInt("max-mobs", 1));
        point.setCooldown(section.getInt("cooldown", 200));
        point.setRadius(section.getDouble("radius", 5.0));
        point.setUseTimer(section.getBoolean("use-timer", true));
        point.setTimerInterval(section.getInt("timer-interval", 400));
        point.setRequirePlayer(section.getBoolean("require-player", true));
        point.setPlayerRange(section.getDouble("player-range", 50.0));
        point.setEnabled(section.getBoolean("enabled", true));
        point.setConditions(section.getStringList("conditions"));

        return point;
    }

    /**
     * 保存刷新点
     */
    public void saveSpawnPoint(SpawnPoint point) {
        String path = "spawnpoints." + point.getId();

        config.set(path + ".world", point.getLocation().getWorld().getName());
        config.set(path + ".x", point.getLocation().getX());
        config.set(path + ".y", point.getLocation().getY());
        config.set(path + ".z", point.getLocation().getZ());
        config.set(path + ".yaw", point.getLocation().getYaw());
        config.set(path + ".pitch", point.getLocation().getPitch());
        config.set(path + ".display-name", point.getDisplayName());
        config.set(path + ".mob", point.getMobId());
        config.set(path + ".level", point.getLevel());
        config.set(path + ".amount", point.getAmount());
        config.set(path + ".max-mobs", point.getMaxMobs());
        config.set(path + ".cooldown", point.getCooldown());
        config.set(path + ".radius", point.getRadius());
        config.set(path + ".use-timer", point.isUseTimer());
        config.set(path + ".timer-interval", point.getTimerInterval());
        config.set(path + ".require-player", point.isRequirePlayer());
        config.set(path + ".player-range", point.getPlayerRange());
        config.set(path + ".enabled", point.isEnabled());
        config.set(path + ".conditions", point.getConditions());

        saveConfig();
    }

    /**
     * 删除刷新点
     */
    public boolean deleteSpawnPoint(String id) {
        SpawnPoint point = spawnPoints.remove(id);
        if (point == null) return false;

        config.set("spawnpoints." + id, null);
        saveConfig();

        return true;
    }

    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存刷新点配置失败: " + e.getMessage());
        }
    }

    /**
     * 创建刷新点
     */
    public SpawnPoint createSpawnPoint(String id, Location location, String mobId) {
        if (spawnPoints.containsKey(id)) {
            return null;
        }

        SpawnPoint point = new SpawnPoint(id);
        point.setLocation(location);
        point.setMobId(mobId);

        spawnPoints.put(id, point);
        saveSpawnPoint(point);

        return point;
    }

    /**
     * 获取刷新点
     */
    public SpawnPoint getSpawnPoint(String id) {
        return spawnPoints.get(id);
    }

    /**
     * 获取所有刷新点
     */
    public Collection<SpawnPoint> getAllSpawnPoints() {
        return spawnPoints.values();
    }

    /**
     * 启动定时任务
     */
    private void startUpdateTask() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        updateTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            for (SpawnPoint point : spawnPoints.values()) {
                if (point.isUseTimer() && point.canSpawn() && point.hasPlayerNearby()) {
                    spawnFromPoint(point);
                }
            }
        }, 20L, 20L); // 每秒检查一次
    }

    /**
     * 从刷新点生成怪物
     */
    public boolean spawnFromPoint(SpawnPoint point) {
        if (!point.canSpawn()) return false;

        CustomMob template = plugin.getMobManager().getMobTemplate(point.getMobId());
        if (template == null) return false;

        MiniMessageService mm = MiniMessageService.getInstance();

        // 生成指定数量的怪物
        for (int i = 0; i < point.getAmount(); i++) {
            if (point.getCurrentMobs() >= point.getMaxMobs()) break;

            Location spawnLoc = point.getRandomSpawnLocation();
            if (spawnLoc == null) continue;

            // 计算等级
            int level = point.getLevel() > 0 ? point.getLevel() : template.getLevel();

            LivingEntity entity = plugin.getMobManager().spawnMob(point.getMobId(), spawnLoc, level);
            if (entity != null) {
                point.incrementCurrentMobs();
                spawnedMobs.put(entity.getUniqueId(), point.getId());

                // 发送消息给附近玩家
                for (org.bukkit.entity.Player player : spawnLoc.getWorld().getPlayers()) {
                    if (player.getLocation().distance(spawnLoc) <= 30) {
                        player.sendMessage(mm.colorize("<red>" + template.getDisplayName() + " <gray>在附近出现了！"));
                    }
                }
            }
        }

        point.setLastSpawnTime(System.currentTimeMillis());
        return true;
    }

    /**
     * 手动触发刷新点
     */
    public boolean forceSpawn(String id) {
        SpawnPoint point = spawnPoints.get(id);
        if (point == null) return false;

        return spawnFromPoint(point);
    }

    /**
     * 怪物死亡时调用
     */
    public void onMobDeath(UUID entityId) {
        String pointId = spawnedMobs.remove(entityId);
        if (pointId == null) return;

        SpawnPoint point = spawnPoints.get(pointId);
        if (point != null) {
            point.decrementCurrentMobs();
        }
    }

    /**
     * 获取最近的刷新点
     */
    public SpawnPoint getNearestSpawnPoint(Location location, double maxDistance) {
        SpawnPoint nearest = null;
        double minDistance = maxDistance;

        for (SpawnPoint point : spawnPoints.values()) {
            if (point.getLocation() == null) continue;
            if (!point.getLocation().getWorld().equals(location.getWorld())) continue;

            double distance = point.getLocation().distance(location);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = point;
            }
        }

        return nearest;
    }

    /**
     * 清理
     */
    public void cleanup() {
        if (updateTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(updateTaskId);
            }
        }
        spawnPoints.clear();
        spawnedMobs.clear();
    }
}
