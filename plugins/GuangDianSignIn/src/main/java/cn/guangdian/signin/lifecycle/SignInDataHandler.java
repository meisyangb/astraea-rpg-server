package cn.guangdian.signin.lifecycle;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.signin.GuangDianSignIn;
import cn.guangdian.signin.data.PlayerSignInData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SignInDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianSignIn plugin;
    private final Map<UUID, PlayerSignInData> dataCache;
    private boolean tableInitialized = false;
    
    public SignInDataHandler(GuangDianSignIn plugin) {
        super(plugin);
        this.plugin = plugin;
        this.dataCache = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        createTable();
    }
    
    private void createTable() {
        if (!CoreDatabase.isEnabled()) {
            plugin.getLogger().info("数据库未启用，签到数据将使用内存存储");
            return;
        }
        
        String sql = """
            CREATE TABLE IF NOT EXISTS `player_signin` (
                `player_id` VARCHAR(36) NOT NULL PRIMARY KEY,
                `last_signin` VARCHAR(10) NOT NULL DEFAULT '',
                `consecutive_days` INT NOT NULL DEFAULT 0,
                `total_days` INT NOT NULL DEFAULT 0,
                INDEX `idx_player_id` (`player_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;
        
        try (Connection conn = CoreDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            tableInitialized = true;
            plugin.getLogger().info("签到数据表初始化成功: player_signin");
        } catch (SQLException e) {
            plugin.getLogger().warning("创建签到数据表失败: " + e.getMessage());
        }
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        if (rpgCore != null && rpgCore.getScheduler() != null) {
            rpgCore.getScheduler().runAsync(() -> {
                PlayerSignInData data = loadFromDatabase(player.getUniqueId());
                rpgCore.getScheduler().runSyncLater(() -> {
                    dataCache.put(player.getUniqueId(), data);
                }, 0L);
            });
        } else {
            dataCache.put(player.getUniqueId(), new PlayerSignInData(player.getUniqueId()));
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        PlayerSignInData data = dataCache.remove(player.getUniqueId());
        if (data != null && rpgCore != null && rpgCore.getScheduler() != null) {
            rpgCore.getScheduler().runAsync(() -> {
                saveToDatabase(player.getUniqueId(), data);
            });
        }
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "SignInData";
    }
    
    public Map<UUID, PlayerSignInData> getDataCache() {
        return dataCache;
    }
    
    private PlayerSignInData loadFromDatabase(UUID playerId) {
        if (!CoreDatabase.isEnabled()) {
            return new PlayerSignInData(playerId);
        }
        
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT last_signin, consecutive_days, total_days FROM player_signin WHERE player_id = ?")) {
            
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlayerSignInData data = new PlayerSignInData(playerId);
                String lastSignInStr = rs.getString("last_signin");
                if (lastSignInStr != null && !lastSignInStr.isEmpty()) {
                    data.setLastSignInDate(LocalDate.parse(lastSignInStr));
                }
                data.setConsecutiveDays(rs.getInt("consecutive_days"));
                data.setTotalDays(rs.getInt("total_days"));
                return data;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载玩家签到数据失败: " + e.getMessage());
        }
        
        return new PlayerSignInData(playerId);
    }
    
    private void saveToDatabase(UUID playerId, PlayerSignInData data) {
        if (!CoreDatabase.isEnabled()) {
            return;
        }
        
        try (Connection conn = CoreDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO player_signin (player_id, last_signin, consecutive_days, total_days) VALUES (?, ?, ?, ?) " +
                 "ON DUPLICATE KEY UPDATE last_signin = VALUES(last_signin), consecutive_days = VALUES(consecutive_days), total_days = VALUES(total_days)")) {

            stmt.setString(1, playerId.toString());
            stmt.setString(2, data.getLastSignInDate() != null ? data.getLastSignInDate().toString() : "");
            stmt.setInt(3, data.getConsecutiveDays());
            stmt.setInt(4, data.getTotalDays());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            plugin.getLogger().warning("保存玩家签到数据失败: " + e.getMessage());
        }
    }
}
