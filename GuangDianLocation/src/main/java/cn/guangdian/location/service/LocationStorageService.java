package cn.guangdian.location.service;

import cn.guangdian.location.GuangDianLocation;
import cn.guangdian.rpgcore.service.api.LocationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
 * <p>支持三种存储方式：YML、MySQL、SQLite，默认使用 YML。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LocationStorageService {

    private final GuangDianLocation plugin;
    private final StorageType storageType;
    private final YmlStorage ymlStorage;
    private final DatabaseStorage databaseStorage;
    private boolean initialized = false;

    /**
     * 存储类型枚举
     */
    public enum StorageType {
        YML,
        MYSQL,
        SQLITE
    }

    public LocationStorageService(GuangDianLocation plugin) {
        this.plugin = plugin;
        
        // 读取存储类型配置
        String typeStr = plugin.getConfig().getString("storage.type", "yml").toUpperCase();
        StorageType type;
        try {
            type = StorageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("未知的存储类型 '" + typeStr + "'，将使用 YML 存储");
            type = StorageType.YML;
        }
        this.storageType = type;
        
        // 根据类型初始化存储
        if (type == StorageType.YML) {
            this.ymlStorage = new YmlStorage(plugin);
            this.databaseStorage = null;
            this.initialized = ymlStorage.init();
            if (initialized) {
                plugin.getLogger().info("使用 YML 文件存储坐标点数据");
            }
        } else {
            this.ymlStorage = null;
            this.databaseStorage = new DatabaseStorage(plugin, type);
            this.initialized = databaseStorage.init();
            if (initialized) {
                plugin.getLogger().info("使用 " + type + " 数据库存储坐标点数据");
            }
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

        if (storageType == StorageType.YML) {
            return ymlStorage.saveLocation(playerId, name, location);
        } else {
            return databaseStorage.saveLocation(playerId, name, location);
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

        if (storageType == StorageType.YML) {
            return ymlStorage.getLocation(playerId, name);
        } else {
            return databaseStorage.getLocation(playerId, name);
        }
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

        if (storageType == StorageType.YML) {
            return ymlStorage.deleteLocation(playerId, name);
        } else {
            return databaseStorage.deleteLocation(playerId, name);
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

        if (storageType == StorageType.YML) {
            return ymlStorage.listLocations(playerId);
        } else {
            return databaseStorage.listLocations(playerId);
        }
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

        if (storageType == StorageType.YML) {
            return ymlStorage.hasLocation(playerId, name);
        } else {
            return databaseStorage.hasLocation(playerId, name);
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

        if (storageType == StorageType.YML) {
            return ymlStorage.getLocationCount(playerId);
        } else {
            return databaseStorage.getLocationCount(playerId);
        }
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

        if (storageType == StorageType.YML) {
            return ymlStorage.clearLocations(playerId);
        } else {
            return databaseStorage.clearLocations(playerId);
        }
    }

    /**
     * 获取总坐标点数量
     */
    public int getTotalLocationCount() {
        if (!initialized) {
            return 0;
        }

        if (storageType == StorageType.YML) {
            return ymlStorage.getTotalLocationCount();
        } else {
            return databaseStorage.getTotalLocationCount();
        }
    }

    /**
     * 检查是否初始化完成
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取当前存储类型
     */
    public StorageType getStorageType() {
        return storageType;
    }

    // ==================== YML 存储实现 ====================

    /**
     * YML 文件存储实现
     */
    private static class YmlStorage {
        private final GuangDianLocation plugin;
        private final File dataFile;
        private FileConfiguration dataConfig;

        YmlStorage(GuangDianLocation plugin) {
            this.plugin = plugin;
            this.dataFile = new File(plugin.getDataFolder(), "locations.yml");
        }

        boolean init() {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "无法创建 locations.yml 文件: " + e.getMessage(), e);
                    return false;
                }
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            return true;
        }

        private void saveConfig() {
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "保存 locations.yml 失败: " + e.getMessage(), e);
            }
        }

        private String getPlayerPath(UUID playerId) {
            return "players." + playerId.toString();
        }

        private String getLocationPath(UUID playerId, String name) {
            return getPlayerPath(playerId) + "." + name;
        }

        boolean saveLocation(UUID playerId, String name, Location location) {
            World world = location.getWorld();
            if (world == null) return false;

            String path = getLocationPath(playerId, name);
            dataConfig.set(path + ".world", world.getName());
            dataConfig.set(path + ".x", location.getX());
            dataConfig.set(path + ".y", location.getY());
            dataConfig.set(path + ".z", location.getZ());
            dataConfig.set(path + ".pitch", location.getPitch());
            dataConfig.set(path + ".yaw", location.getYaw());
            dataConfig.set(path + ".updated", System.currentTimeMillis());
            saveConfig();
            return true;
        }

        Optional<Location> getLocation(UUID playerId, String name) {
            String path = getLocationPath(playerId, name);
            if (!dataConfig.contains(path)) {
                return Optional.empty();
            }

            String worldName = dataConfig.getString(path + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return Optional.empty();
            }

            double x = dataConfig.getDouble(path + ".x");
            double y = dataConfig.getDouble(path + ".y");
            double z = dataConfig.getDouble(path + ".z");
            float pitch = (float) dataConfig.getDouble(path + ".pitch", 0);
            float yaw = (float) dataConfig.getDouble(path + ".yaw", 0);

            return Optional.of(new Location(world, x, y, z, yaw, pitch));
        }

        boolean deleteLocation(UUID playerId, String name) {
            String path = getLocationPath(playerId, name);
            if (!dataConfig.contains(path)) {
                return false;
            }
            dataConfig.set(path, null);
            saveConfig();
            return true;
        }

        List<LocationService.SavedLocationInfo> listLocations(UUID playerId) {
            List<LocationService.SavedLocationInfo> locations = new ArrayList<>();
            String playerPath = getPlayerPath(playerId);
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(playerPath);
            
            if (playerSection == null) {
                return locations;
            }

            for (String name : playerSection.getKeys(false)) {
                String locPath = playerPath + "." + name;
                String worldName = dataConfig.getString(locPath + ".world");
                double x = dataConfig.getDouble(locPath + ".x");
                double y = dataConfig.getDouble(locPath + ".y");
                double z = dataConfig.getDouble(locPath + ".z");
                float pitch = (float) dataConfig.getDouble(locPath + ".pitch", 0);
                float yaw = (float) dataConfig.getDouble(locPath + ".yaw", 0);
                long updated = dataConfig.getLong(locPath + ".updated", 0);

                locations.add(new LocationService.SavedLocationInfo(name, worldName, x, y, z, pitch, yaw, updated));
            }

            return locations;
        }

        boolean hasLocation(UUID playerId, String name) {
            return dataConfig.contains(getLocationPath(playerId, name));
        }

        int getLocationCount(UUID playerId) {
            String playerPath = getPlayerPath(playerId);
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(playerPath);
            return playerSection != null ? playerSection.getKeys(false).size() : 0;
        }

        int clearLocations(UUID playerId) {
            String playerPath = getPlayerPath(playerId);
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(playerPath);
            if (playerSection == null) {
                return 0;
            }
            int count = playerSection.getKeys(false).size();
            dataConfig.set(playerPath, null);
            saveConfig();
            return count;
        }

        int getTotalLocationCount() {
            int total = 0;
            ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
            if (playersSection != null) {
                for (String playerId : playersSection.getKeys(false)) {
                    ConfigurationSection playerSection = playersSection.getConfigurationSection(playerId);
                    if (playerSection != null) {
                        total += playerSection.getKeys(false).size();
                    }
                }
            }
            return total;
        }
    }

    // ==================== 数据库存储实现（MySQL/SQLite） ====================

    /**
     * 数据库存储实现（支持 MySQL 和 SQLite）
     */
    private static class DatabaseStorage {
        private final GuangDianLocation plugin;
        private final StorageType storageType;
        private final String jdbcUrl;
        private final String username;
        private final String password;

        // 表名
        private static final String TABLE_NAME = "gd_saved_locations";

        // MySQL SQL 语句
        private static final String CREATE_TABLE_MYSQL = 
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        // SQLite SQL 语句
        private static final String CREATE_TABLE_SQLITE = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
            "UNIQUE KEY unique_player_location (player_uuid, location_name)" +
            ")";

        private static final String INSERT_SQL = 
            "INSERT INTO " + TABLE_NAME + " " +
            "(player_uuid, location_name, world_name, x, y, z, pitch, yaw, created_time, updated_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(player_uuid, location_name) DO UPDATE SET " +
            "world_name = excluded.world_name, x = excluded.x, y = excluded.y, z = excluded.z, " +
            "pitch = excluded.pitch, yaw = excluded.yaw, updated_time = excluded.updated_time";

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

        DatabaseStorage(GuangDianLocation plugin, StorageType type) {
            this.plugin = plugin;
            this.storageType = type;
            
            if (type == StorageType.MYSQL) {
                String host = plugin.getConfig().getString("mysql.host", "localhost");
                int port = plugin.getConfig().getInt("mysql.port", 3306);
                String database = plugin.getConfig().getString("mysql.database", "guangdian_location");
                boolean useSSL = plugin.getConfig().getBoolean("mysql.useSSL", false);
                String timezone = plugin.getConfig().getString("mysql.serverTimezone", "Asia/Shanghai");
                String encoding = plugin.getConfig().getString("mysql.characterEncoding", "utf8");
                this.username = plugin.getConfig().getString("mysql.username", "root");
                this.password = plugin.getConfig().getString("mysql.password", "");
                
                this.jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=%b&serverTimezone=%s&characterEncoding=%s",
                    host, port, database, useSSL, timezone, encoding
                );
            } else {
                // SQLite
                String filename = plugin.getConfig().getString("sqlite.filename", "locations.db");
                File dbFile = new File(plugin.getDataFolder(), filename);
                this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                this.username = null;
                this.password = null;
            }
        }

        Connection getConnection() throws SQLException {
            if (storageType == StorageType.MYSQL) {
                return DriverManager.getConnection(jdbcUrl, username, password);
            } else {
                return DriverManager.getConnection(jdbcUrl);
            }
        }

        boolean init() {
            try {
                Class.forName("org.sqlite.JDBC");
                if (storageType == StorageType.MYSQL) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("缺少数据库驱动: " + e.getMessage());
                return false;
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                String createSql = storageType == StorageType.MYSQL ? CREATE_TABLE_MYSQL : CREATE_TABLE_SQLITE;
                stmt.execute(createSql);
                plugin.getLogger().info("数据库表 " + TABLE_NAME + " 已初始化");
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "初始化数据库表失败: " + e.getMessage(), e);
                return false;
            }
        }

        boolean saveLocation(UUID playerId, String name, Location location) {
            World world = location.getWorld();
            if (world == null) {
                return false;
            }

            long now = System.currentTimeMillis();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
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

                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "保存坐标点失败: " + e.getMessage(), e);
                return false;
            }
        }

        Optional<Location> getLocation(UUID playerId, String name) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SELECT_SQL)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, name);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String worldName = rs.getString("world_name");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        return Optional.empty();
                    }

                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float pitch = rs.getFloat("pitch");
                    float yaw = rs.getFloat("yaw");

                    return Optional.of(new Location(world, x, y, z, yaw, pitch));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "获取坐标点失败: " + e.getMessage(), e);
            }

            return Optional.empty();
        }

        boolean deleteLocation(UUID playerId, String name) {
            try (Connection conn = getConnection();
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

        List<LocationService.SavedLocationInfo> listLocations(UUID playerId) {
            List<LocationService.SavedLocationInfo> locations = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL)) {
                stmt.setString(1, playerId.toString());

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String locName = rs.getString("location_name");
                    String worldName = rs.getString("world_name");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float pitch = rs.getFloat("pitch");
                    float yaw = rs.getFloat("yaw");
                    long createdTime = rs.getLong("created_time");

                    locations.add(new LocationService.SavedLocationInfo(
                        locName, worldName, x, y, z, pitch, yaw, createdTime
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "列出坐标点失败: " + e.getMessage(), e);
            }

            return locations;
        }

        boolean hasLocation(UUID playerId, String name) {
            try (Connection conn = getConnection();
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

        int getLocationCount(UUID playerId) {
            try (Connection conn = getConnection();
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

        int clearLocations(UUID playerId) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(DELETE_ALL_SQL)) {
                stmt.setString(1, playerId.toString());

                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "清空坐标点失败: " + e.getMessage(), e);
                return 0;
            }
        }

        int getTotalLocationCount() {
            try (Connection conn = getConnection();
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
    }
}