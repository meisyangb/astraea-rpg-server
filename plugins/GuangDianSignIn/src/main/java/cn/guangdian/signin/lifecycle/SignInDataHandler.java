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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SignInDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianSignIn plugin;
    private final Map<UUID, PlayerSignInData> dataCache;
    
    public SignInDataHandler(GuangDianSignIn plugin) {
        super(plugin);
        this.plugin = plugin;
        this.dataCache = new ConcurrentHashMap<>();
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
                 "INSERT OR REPLACE INTO player_signin (player_id, last_signin, consecutive_days, total_days) VALUES (?, ?, ?, ?)")) {
            
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
