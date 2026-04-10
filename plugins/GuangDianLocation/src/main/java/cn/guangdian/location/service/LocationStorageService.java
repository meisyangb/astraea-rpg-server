package cn.guangdian.location.service;

import cn.guangdian.location.GuangDianLocation;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.service.api.LocationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 坐标点存储服务
 * 
 * <p>负责将坐标点数据存储到MySQL数据库。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LocationStorageService {

    private final GuangDianLocation plugin;

    // 表名
    private static final String TABLE_NAME = "gd_saved_locations";

    // SQL 语句
    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
        "id INT AUTO_INCREMENT PRIMARY KEY, " +
        "player_uuid VARCHAR(36) NOT NULL, " +
        "location_name VARCHAR(64) NOT NULL, " +
        "world_name VARCHAR(64) NOT NULL, " +
        "x DOUBLE NOT NULL, " +
        "y DOUBLE NOT NULL, " +
        "z DOUBLE NOT NULL, " +
        "pitch FLOAT NOT NULL DEFAULT 0, " +
        "yaw FLOAT NOT NULL DEFAULT 0, " +
        "created_time BIGINT NOT NULL, " +
        "updated_time BIGINT NOT NULL, " +
        "UNIQUE KEY unique_player_location (player_uuid, location_name), " +
        "INDEX idx_player_uuid (player_uuid), " +
        "INDEX idx_world_name (world_name)" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

    private static final String INSERT_SQL = 
        "INSERT INTO " + TABLE_NAME + " " +
        "(player_uuid, location_name, world_name, x, y, z, pitch, yaw, created_time, updated_time) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "world_name = ?, x = ?, y = ?, z = ?, pitch = ?, yaw = ?, updated_time = ?";

    private static final String SELECT_SQL = 
        "SELECT world_name, x, y, z, pitch, yaw FROM " + TABLE_NAME + " " +
        "WHERE player_uuid = ? AND location_name = ?";

    private static final String SELECT_ALL_SQL = 
        "SELECT location_name, world_name, x, y, z, pitch, yaw, created_time FROM " + TABLE_NAME + " " +
        "WHERE player_uuid = ? ORDER BY created_time DESC";

    private static final String DELETE_SQL = 
        "DELETE FROM " + TABLE_NAME + " WHERE player_uuid = ? AND location_name = ?";

    private static final String DELETE_ALL_SQL = 
        "DELETE FROM " + TABLE_NAME + " WHERE player_uuid = ?";

    private static final String COUNT_SQL = 
        "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE player_uuid = ?";

    private static final String TOTAL_COUNT_SQL = 
        "SELECT COUNT(*) FROM " + TABLE_NAME;

    private static final String EXISTS_SQL = 
        "SELECT 1 FROM " + TABLE_NAME + " WHERE player_uuid = ? AND location_name = ? LIMIT 1";

    private boolean initialized = false;

    public LocationStorageService(GuangDianLocation plugin) {
        this.plugin = plugin;
        initTable();
    }

    /**
     * 初始化数据库表
     */
    private void initTable() {
        if (!CoreDatabase.isEnabled()) {
            plugin.getLogger().severe("数据库连接池未启用，无法初始化坐标点表!");
            return;
        }

        try (Connection conn = CoreDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            initialized = true;
            plugin.getLogger().info("数据库表 " + TABLE_NAME + " 已初始化");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "初始化数据库表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存坐标点
     */
    public boolean saveLocation(UUID playerId, String name, Location location) {
        if (!initialized) {
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            // INSERT 参数
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name);
            stmt.setString(3, world.getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setFloat(7, location.getPitch());
            stmt.setFloat(8, location.getYaw());
            stmt.setLong(9, now);
            stmt.setLong(10, now);

            // UPDATE 参数
            stmt.setString(11, world.getName());
            stmt.setDouble(12, location.getX());
            stmt.setDouble(13, location.getY());
            stmt.setDouble(14, location.getZ());
            stmt.setFloat(15, location.getPitch());
            stmt.setFloat(16, location.getYaw());
            stmt.setLong(17, now);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "保存坐标点失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步保存坐标点
     */
    public CompletableFuture<Boolean> saveLocationAsync(UUID playerId, String name, Location location) {
        return CompletableFuture.supplyAsync(() -> saveLocation(playerId, name, location));
    }

    /**
     * 获取坐标点
     */
    public Optional<Location> getLocation(UUID playerId, String name) {
        if (!initialized) {
            return Optional.empty();
        }

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_SQL)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world_name");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    // 世界未加载，返回空
                    return Optional.empty();
                }

                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");

                Location location = new Location(world, x, y, z, yaw, pitch);
                return Optional.of(location);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "获取坐标点失败: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * 异步获取坐标点
     */
    public CompletableFuture<Optional<Location>> getLocationAsync(UUID playerId, String name) {
        return CompletableFuture.supplyAsync(() -> getLocation(playerId, name));
    }

    /**
     * 删除坐标点
     */
    public boolean deleteLocation(UUID playerId, String name) {
        if (!initialized) {
            return false;
        }

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "删除坐标点失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步删除坐标点
     */
    public CompletableFuture<Boolean> deleteLocationAsync(UUID playerId, String name) {
        return CompletableFuture.supplyAsync(() -> deleteLocation(playerId, name));
    }

    /**
     * 列出所有坐标点
     */
    public List<LocationService.SavedLocationInfo> listLocations(UUID playerId) {
        if (!initialized) {
            return new ArrayList<>();
        }

        List<LocationService.SavedLocationInfo> locations = new ArrayList<>();

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL)) {
            stmt.setString(1, playerId.toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("location_name");
                String worldName = rs.getString("world_name");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");
                long createdTime = rs.getLong("created_time");

                locations.add(new LocationService.SavedLocationInfo(
                    name, worldName, x, y, z, pitch, yaw, createdTime
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "列出坐标点失败: " + e.getMessage(), e);
        }

        return locations;
    }

    /**
     * 异步列出所有坐标点
     */
    public CompletableFuture<List<LocationService.SavedLocationInfo>> listLocationsAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> listLocations(playerId));
    }

    /**
     * 检查坐标点是否存在
     */
    public boolean hasLocation(UUID playerId, String name) {
        if (!initialized) {
            return false;
        }

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS_SQL)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "检查坐标点存在失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步检查坐标点是否存在
     */
    public CompletableFuture<Boolean> hasLocationAsync(UUID playerId, String name) {
        return CompletableFuture.supplyAsync(() -> hasLocation(playerId, name));
    }

    /**
     * 获取坐标点数量
     */
    public int getLocationCount(UUID playerId) {
        if (!initialized) {
            return 0;
        }

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_SQL)) {
            stmt.setString(1, playerId.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "获取坐标点数量失败: " + e.getMessage(), e);
        }

        return 0;
    }

    /**
     * 异步获取坐标点数量
     */
    public CompletableFuture<Integer> getLocationCountAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> getLocationCount(playerId));
    }

    /**
     * 清空玩家的所有坐标点
     */
    public int clearLocations(UUID playerId) {
        if (!initialized) {
            return 0;
        }

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_ALL_SQL)) {
            stmt.setString(1, playerId.toString());

            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "清空坐标点失败: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取总坐标点数量
     */
    public int getTotalLocationCount() {
        if (!initialized) {
            return 0;
        }

        try (Connection conn = CoreDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(TOTAL_COUNT_SQL);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "获取总坐标点数量失败: " + e.getMessage(), e);
        }

        return 0;
    }

    /**
     * 检查是否初始化完成
     */
    public boolean isInitialized() {
        return initialized;
    }
}