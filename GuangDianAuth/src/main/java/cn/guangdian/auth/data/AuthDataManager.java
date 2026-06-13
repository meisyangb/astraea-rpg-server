package cn.guangdian.auth.data;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.security.PasswordHasher;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.database.CoreDatabase;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 认证数据管理器
 * 
 * <p>已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志 - 使用 RPGCore GameLogger（降级到 Bukkit Logger）</li>
 *   <li>异步 - 使用 RPGCore AsyncExecutor（降级到默认线程池）</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class AuthDataManager {

    private final GuangDianAuth plugin;
    private final PasswordHasher hasher = new PasswordHasher();
    private final String tableName;
    
    // RPGCore 服务
    private GameLogger logger;
    private AsyncExecutor asyncExecutor;

    public AuthDataManager(GuangDianAuth plugin) {
        this.plugin = plugin;
        this.tableName = "gd_auth_players";
        
        // 初始化 RPGCore 服务
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.logger = rpgCore.getGameLogger();
            this.asyncExecutor = rpgCore.getAsyncExecutor();
        }
    }

    public void initialize() {
        createTable();
    }

    private void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS `%s` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36) NOT NULL UNIQUE,
                `name` VARCHAR(16) NOT NULL UNIQUE,
                `password_hash` VARCHAR(64) NOT NULL,
                `salt` VARCHAR(64) NOT NULL,
                `register_date` BIGINT NOT NULL,
                `register_ip` VARCHAR(45) NOT NULL,
                `last_login` BIGINT NOT NULL,
                `last_ip` VARCHAR(45) NOT NULL,
                INDEX `idx_uuid` (`uuid`),
                INDEX `idx_name` (`name`),
                INDEX `idx_last_login` (`last_login`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """.formatted(tableName);

        try (Connection conn = CoreDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logInfo("数据表初始化成功: " + tableName);
        } catch (SQLException e) {
            logSevere("创建数据表失败", e);
        }
    }

    public boolean isRegistered(String playerName) {
        String sql = "SELECT 1 FROM `" + tableName + "` WHERE `name` = ? LIMIT 1";
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logWarning("检查注册状态失败: " + e.getMessage());
            return false;
        }
    }

    public Optional<PlayerAuthData> getByName(String playerName) {
        String sql = "SELECT * FROM `" + tableName + "` WHERE `name` = ? LIMIT 1";
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logWarning("查询玩家数据失败: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<PlayerAuthData> getByUuid(UUID uuid) {
        String sql = "SELECT * FROM `" + tableName + "` WHERE `uuid` = ? LIMIT 1";
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logWarning("查询玩家数据失败: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void register(String playerName, UUID uuid, String password, String ip) {
        String salt = hasher.generateSalt();
        String hash = hasher.hash(password, salt);

        String sql = "INSERT INTO `" + tableName + "` " +
            "(`uuid`, `name`, `password_hash`, `salt`, `register_date`, `register_ip`, `last_login`, `last_ip`) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        long now = System.currentTimeMillis();

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName.toLowerCase());
            stmt.setString(3, hash);
            stmt.setString(4, salt);
            stmt.setLong(5, now);
            stmt.setString(6, ip);
            stmt.setLong(7, now);
            stmt.setString(8, ip);
            stmt.executeUpdate();
            
            logInfo("玩家注册成功: " + playerName);
        } catch (SQLException e) {
            logSevere("注册玩家失败", e);
        }
    }

    public boolean checkPassword(String playerName, String password) {
        Optional<PlayerAuthData> data = getByName(playerName);
        if (data.isEmpty()) return false;
        
        PlayerAuthData authData = data.get();
        return hasher.verify(password, authData.getPasswordHash(), authData.getSalt());
    }

    public void updateLastLogin(String playerName, String ip) {
        String sql = "UPDATE `" + tableName + "` SET `last_login` = ?, `last_ip` = ? WHERE `name` = ?";
        
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, ip);
            stmt.setString(3, playerName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logWarning("更新登录时间失败: " + e.getMessage());
        }
    }

    public void changePassword(String playerName, String newPassword) {
        String salt = hasher.generateSalt();
        String hash = hasher.hash(newPassword, salt);

        String sql = "UPDATE `" + tableName + "` SET `password_hash` = ?, `salt` = ? WHERE `name` = ?";

        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.setString(3, playerName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logWarning("修改密码失败: " + e.getMessage());
        }
    }

    public void unregister(String playerName) {
        String sql = "DELETE FROM `" + tableName + "` WHERE `name` = ?";
        
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logWarning("注销账号失败: " + e.getMessage());
        }
    }

    public int getRegisteredCount() {
        String sql = "SELECT COUNT(*) FROM `" + tableName + "`";
        try (Connection conn = CoreDatabase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logWarning("统计玩家数量失败: " + e.getMessage());
        }
        return 0;
    }

    private PlayerAuthData mapResultSet(ResultSet rs) throws SQLException {
        PlayerAuthData data = new PlayerAuthData(
            rs.getString("name"),
            UUID.fromString(rs.getString("uuid"))
        );
        data.setPasswordHash(rs.getString("password_hash"));
        data.setSalt(rs.getString("salt"));
        data.setRegisterDate(rs.getLong("register_date"));
        data.setRegisterIp(rs.getString("register_ip"));
        data.setLastLogin(rs.getLong("last_login"));
        data.setLastIp(rs.getString("last_ip"));
        return data;
    }

    // ==================== 异步方法（使用 RPGCore AsyncExecutor）====================

    public CompletableFuture<Boolean> isRegisteredAsync(String playerName) {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(() -> isRegistered(playerName));
        }
        // 降级：使用默认线程池
        return CompletableFuture.supplyAsync(() -> isRegistered(playerName));
    }

    public CompletableFuture<Void> registerAsync(String playerName, UUID uuid, String password, String ip) {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(() -> {
                register(playerName, uuid, password, ip);
                return null;
            });
        }
        // 降级
        return CompletableFuture.runAsync(() -> register(playerName, uuid, password, ip));
    }

    public CompletableFuture<Boolean> checkPasswordAsync(String playerName, String password) {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(() -> checkPassword(playerName, password));
        }
        // 降级
        return CompletableFuture.supplyAsync(() -> checkPassword(playerName, password));
    }
    
    // ==================== 日志快捷方法 ====================
    
    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            plugin.getLogger().info(message);
        }
    }
    
    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        } else {
            plugin.getLogger().warning(message);
        }
    }
    
    private void logSevere(String message, Throwable throwable) {
        if (logger != null) {
            logger.severe(message, throwable);
        } else {
            plugin.getLogger().severe(message + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }
}
