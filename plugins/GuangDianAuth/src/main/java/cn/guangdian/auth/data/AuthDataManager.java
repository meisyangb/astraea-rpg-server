package cn.guangdian.auth.data;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.security.PasswordHasher;
import cn.guangdian.rpgcore.database.CoreDatabase;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthDataManager {

    private final GuangDianAuth plugin;
    private final PasswordHasher hasher = new PasswordHasher();
    private final String tableName;

    public AuthDataManager(GuangDianAuth plugin) {
        this.plugin = plugin;
        this.tableName = "gd_auth_players";
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
            plugin.getLogger().info("数据表初始化成功: " + tableName);
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据表失败: " + e.getMessage());
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
            plugin.getLogger().warning("检查注册状态失败: " + e.getMessage());
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
            plugin.getLogger().warning("查询玩家数据失败: " + e.getMessage());
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
            plugin.getLogger().warning("查询玩家数据失败: " + e.getMessage());
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
            
            plugin.getLogger().info("玩家注册成功: " + playerName);
        } catch (SQLException e) {
            plugin.getLogger().severe("注册玩家失败: " + e.getMessage());
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
            plugin.getLogger().warning("更新登录时间失败: " + e.getMessage());
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
            plugin.getLogger().warning("修改密码失败: " + e.getMessage());
        }
    }

    public void unregister(String playerName) {
        String sql = "DELETE FROM `" + tableName + "` WHERE `name` = ?";
        
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("注销账号失败: " + e.getMessage());
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
            plugin.getLogger().warning("统计玩家数量失败: " + e.getMessage());
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

    public CompletableFuture<Boolean> isRegisteredAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> isRegistered(playerName));
    }

    public CompletableFuture<Void> registerAsync(String playerName, UUID uuid, String password, String ip) {
        return CompletableFuture.runAsync(() -> register(playerName, uuid, password, ip));
    }

    public CompletableFuture<Boolean> checkPasswordAsync(String playerName, String password) {
        return CompletableFuture.supplyAsync(() -> checkPassword(playerName, password));
    }
}
