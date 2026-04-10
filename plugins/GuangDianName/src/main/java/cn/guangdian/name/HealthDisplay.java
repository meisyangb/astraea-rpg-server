package cn.guangdian.name;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 血量显示管理器
 * 
 * 在每个玩家的 Scoreboard 中设置 BELOW_NAME 血量显示
 * 这样每个玩家都能看到其他玩家头顶的血量
 */
public class HealthDisplay {
    
    private static final String OBJECTIVE_NAME = "gdnhealth";
    
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> lastHealth = new HashMap<>();
    private String displayName;
    private boolean debug = false;
    
    public HealthDisplay(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        displayName = plugin.getConfig().getString("display-name", "&c❤");
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * 初始化玩家的血量显示
     * 在玩家的 Scoreboard 中创建 BELOW_NAME Objective
     */
    public void initPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        log("[初始化] " + player.getName());
        
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        
        ensureObjectiveExists(board);
        
        for (Player target : Bukkit.getOnlinePlayers()) {
            int health = getHealth(target);
            setHealthScore(board, target.getName(), health);
        }
        
        updateAllPlayersScoreboards(player);
    }
    
    /**
     * 确保 Scoreboard 中有血量 Objective
     */
    private void ensureObjectiveExists(Scoreboard board) {
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            try {
                obj = board.registerNewObjective(OBJECTIVE_NAME, "dummy", displayName);
                obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
                log("[创建] Objective: " + OBJECTIVE_NAME);
            } catch (Exception e) {
                plugin.getLogger().warning("[HealthDisplay] 创建 Objective 失败: " + e.getMessage());
            }
        } else {
            if (obj.getDisplaySlot() != DisplaySlot.BELOW_NAME) {
                obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
        }
    }
    
    /**
     * 更新所有玩家的 Scoreboard 中的新玩家血量
     */
    private void updateAllPlayersScoreboards(Player newPlayer) {
        int health = getHealth(newPlayer);
        String name = newPlayer.getName();
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(newPlayer)) continue;
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                setHealthScore(board, name, health);
            }
        }
    }
    
    /**
     * 设置 Scoreboard 中的血量分数
     */
    private void setHealthScore(Scoreboard board, String playerName, int health) {
        Objective obj = board.getObjective(OBJECTIVE_NAME);
        if (obj != null) {
            obj.getScore(playerName).setScore(health);
        }
    }
    
    /**
     * 更新玩家的血量显示（更新所有玩家的 Scoreboard）
     */
    public void updateHealth(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        int health = getHealth(player);
        UUID playerId = player.getUniqueId();
        
        Integer last = lastHealth.get(playerId);
        if (last != null && last == health) {
            return;
        }
        
        log("[更新] " + player.getName() + " 血量: " + health);
        
        String name = player.getName();
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                setHealthScore(board, name, health);
            }
        }
        
        lastHealth.put(playerId, health);
    }
    
    /**
     * 更新指定玩家的血量（带新血量值）
     */
    public void updateHealth(Player player, double newHealth) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        int health = (int) Math.ceil(newHealth);
        if (health <= 0 && !player.isDead()) {
            health = (int) Math.ceil(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
        health = Math.max(0, health);
        
        UUID playerId = player.getUniqueId();
        Integer last = lastHealth.get(playerId);
        if (last != null && last == health) {
            return;
        }
        
        log("[更新事件] " + player.getName() + " 血量: " + health);
        
        String name = player.getName();
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                setHealthScore(board, name, health);
            }
        }
        
        lastHealth.put(playerId, health);
    }
    
    /**
     * 获取玩家的血量
     */
    private int getHealth(Player player) {
        if (player == null || !player.isOnline()) {
            return 0;
        }
        
        if (player.isDead()) {
            return 0;
        }
        
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        
        if (health <= 0) {
            return (int) Math.ceil(maxHealth);
        }
        
        return Math.max(1, (int) Math.ceil(health));
    }
    
    /**
     * 清理玩家的血量显示
     */
    public void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String name = player.getName();
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board != null) {
                try {
                    board.resetScores(name);
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
        
        lastHealth.remove(playerId);
    }
    
    /**
     * 清理所有缓存
     */
    public void clear() {
        lastHealth.clear();
    }
    
    /**
     * 获取缓存数量
     */
    public int getCacheSize() {
        return lastHealth.size();
    }
    
    /**
     * 获取缓存的血量
     */
    public Integer getCachedHealth(UUID playerId) {
        return lastHealth.get(playerId);
    }
    
    private void log(String message) {
        if (debug) {
            plugin.getLogger().info("[HealthDisplay] " + message);
        }
    }
    
    /**
     * 获取玩家的RPG血量（公开方法）
     */
    public int getRPGHealth(Player player) {
        return getHealth(player);
    }
}
