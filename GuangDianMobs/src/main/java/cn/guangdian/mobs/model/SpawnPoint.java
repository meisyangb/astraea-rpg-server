package cn.guangdian.mobs.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * 刷新点数据模型
 * 参考 MythicMobs 的 RandomSpawns 和 Spawners 系统
 */
public class SpawnPoint {

    private String id;                    // 刷新点ID
    private String displayName;           // 显示名称
    private Location location;            // 刷新位置
    private String mobId;                 // 怪物ID
    private int level;                    // 怪物等级 (-1 表示使用模板等级)
    private int amount;                   // 每次刷新数量
    private int maxMobs;                  // 最大同时存在数量
    private int cooldown;                 // 刷新冷却时间 (tick)
    private double radius;                // 刷新半径
    private boolean useTimer;             // 是否使用定时刷新
    private int timerInterval;            // 定时刷新间隔 (tick)
    private boolean requirePlayer;        // 是否需要玩家在附近才刷新
    private double playerRange;           // 玩家检测范围
    private List<String> conditions;      // 刷新条件
    private boolean enabled;              // 是否启用
    private long lastSpawnTime;           // 上次刷新时间
    private int currentMobs;              // 当前存在的怪物数量

    public SpawnPoint(String id) {
        this.id = id;
        this.level = -1;
        this.amount = 1;
        this.maxMobs = 1;
        this.cooldown = 200;  // 默认10秒
        this.radius = 5.0;
        this.useTimer = true;
        this.timerInterval = 400;  // 默认20秒
        this.requirePlayer = true;
        this.playerRange = 50.0;
        this.conditions = new ArrayList<>();
        this.enabled = true;
        this.lastSpawnTime = 0;
        this.currentMobs = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName != null ? displayName : id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getMobId() { return mobId; }
    public void setMobId(String mobId) { this.mobId = mobId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getMaxMobs() { return maxMobs; }
    public void setMaxMobs(int maxMobs) { this.maxMobs = maxMobs; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public boolean isUseTimer() { return useTimer; }
    public void setUseTimer(boolean useTimer) { this.useTimer = useTimer; }

    public int getTimerInterval() { return timerInterval; }
    public void setTimerInterval(int timerInterval) { this.timerInterval = timerInterval; }

    public boolean isRequirePlayer() { return requirePlayer; }
    public void setRequirePlayer(boolean requirePlayer) { this.requirePlayer = requirePlayer; }

    public double getPlayerRange() { return playerRange; }
    public void setPlayerRange(double playerRange) { this.playerRange = playerRange; }

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getLastSpawnTime() { return lastSpawnTime; }
    public void setLastSpawnTime(long lastSpawnTime) { this.lastSpawnTime = lastSpawnTime; }

    public int getCurrentMobs() { return currentMobs; }
    public void setCurrentMobs(int currentMobs) { this.currentMobs = currentMobs; }
    public void incrementCurrentMobs() { this.currentMobs++; }
    public void decrementCurrentMobs() { this.currentMobs = Math.max(0, this.currentMobs - 1); }

    /**
     * 检查是否可以刷新
     */
    public boolean canSpawn() {
        if (!enabled) return false;
        if (currentMobs >= maxMobs) return false;

        long currentTime = System.currentTimeMillis();
        long cooldownMs = cooldown * 50L;  // tick to ms

        return currentTime - lastSpawnTime >= cooldownMs;
    }

    /**
     * 检查是否有玩家在范围内
     */
    public boolean hasPlayerNearby() {
        if (!requirePlayer) return true;
        if (location == null || location.getWorld() == null) return false;

        for (org.bukkit.entity.Player player : location.getWorld().getPlayers()) {
            // 检查玩家位置是否有效且与刷新点在同一世界
            if (player.getLocation().getWorld() == null) continue;
            if (!player.getLocation().getWorld().equals(location.getWorld())) continue;

            try {
                if (player.getLocation().distance(location) <= playerRange) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // 世界不一致时会抛出异常，已在上方检查，这里作为保险
            }
        }
        return false;
    }

    /**
     * 获取随机刷新位置
     */
    public Location getRandomSpawnLocation() {
        if (location == null) return null;

        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;

        double x = location.getX() + distance * Math.cos(angle);
        double z = location.getZ() + distance * Math.sin(angle);
        double y = location.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;

        return new Location(location.getWorld(), x, y, z);
    }

    /**
     * 序列化为配置
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(":");
        sb.append(location.getWorld().getName()).append(",");
        sb.append(location.getX()).append(",");
        sb.append(location.getY()).append(",");
        sb.append(location.getZ()).append(",");
        sb.append(location.getYaw()).append(",");
        sb.append(location.getPitch()).append("|");
        sb.append(mobId).append("|");
        sb.append(level).append("|");
        sb.append(amount).append("|");
        sb.append(maxMobs).append("|");
        sb.append(cooldown).append("|");
        sb.append(radius).append("|");
        sb.append(useTimer).append("|");
        sb.append(timerInterval).append("|");
        sb.append(requirePlayer).append("|");
        sb.append(playerRange).append("|");
        sb.append(enabled);
        return sb.toString();
    }

    /**
     * 从配置反序列化
     */
    public static SpawnPoint deserialize(String data) {
        try {
            String[] parts = data.split("\\|");
            String[] locParts = parts[0].split(":")[1].split(",");

            SpawnPoint point = new SpawnPoint(parts[0].split(":")[0]);

            org.bukkit.World world = org.bukkit.Bukkit.getWorld(locParts[0]);
            if (world == null) return null;

            double x = Double.parseDouble(locParts[1]);
            double y = Double.parseDouble(locParts[2]);
            double z = Double.parseDouble(locParts[3]);
            float yaw = Float.parseFloat(locParts[4]);
            float pitch = Float.parseFloat(locParts[5]);

            point.setLocation(new Location(world, x, y, z, yaw, pitch));
            point.setMobId(parts[1]);
            point.setLevel(Integer.parseInt(parts[2]));
            point.setAmount(Integer.parseInt(parts[3]));
            point.setMaxMobs(Integer.parseInt(parts[4]));
            point.setCooldown(Integer.parseInt(parts[5]));
            point.setRadius(Double.parseDouble(parts[6]));
            point.setUseTimer(Boolean.parseBoolean(parts[7]));
            point.setTimerInterval(Integer.parseInt(parts[8]));
            point.setRequirePlayer(Boolean.parseBoolean(parts[9]));
            point.setPlayerRange(Double.parseDouble(parts[10]));
            point.setEnabled(Boolean.parseBoolean(parts[11]));

            return point;
        } catch (Exception e) {
            return null;
        }
    }
}
