package cn.guangdian.monthlycard.database;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLite 数据库管理器 - 使用 HikariCP 连接池
 * 
 * 表结构:
 * - monthly_card_data: 玩家月卡数据主表
 * - monthly_card_claims: 领取记录表
 */
public class DatabaseManager {

    private final GuangDianMonthlyCard plugin;
    private HikariDataSource dataSource;
    private final String dbPath;

    // SQL 语句缓存
    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS monthly_card_data (
            player_uuid TEXT PRIMARY KEY,
            card_type TEXT NOT NULL DEFAULT 'none',
            activate_time BIGINT DEFAULT 0,
            expire_time BIGINT DEFAULT 0,
            total_claimed_days INTEGER DEFAULT 0,
            last_claim_time BIGINT DEFAULT 0,
            consecutive_days INTEGER DEFAULT 0,
            makeup_count INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;

    private static final String CREATE_CLAIMS_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS monthly_card_claims (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            claim_date TEXT NOT NULL,
            day_number INTEGER NOT NULL,
            is_makeup BOOLEAN DEFAULT 0,
            reward_points BIGINT DEFAULT 0,
            reward_money DOUBLE DEFAULT 0,
            claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (player_uuid) REFERENCES monthly_card_data(player_uuid),
            UNIQUE(player_uuid, claim_date)
        )
        """;
    
    private static final String CREATE_STATS_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS monthly_card_stats (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            stat_date TEXT UNIQUE NOT NULL,
            new_cards INTEGER DEFAULT 0,
            renewed_cards INTEGER DEFAULT 0,
            total_revenue_points BIGINT DEFAULT 0,
            total_revenue_money DOUBLE DEFAULT 0,
            daily_claims INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;
    
    private static final String CREATE_MILESTONE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS monthly_card_milestones (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            milestone_day INTEGER NOT NULL,
            claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (player_uuid) REFERENCES monthly_card_data(player_uuid),
            UNIQUE(player_uuid, milestone_day)
        )
        """;

    private static final String CREATE_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_claims_player ON monthly_card_claims(player_uuid);
        CREATE INDEX IF NOT EXISTS idx_claims_date ON monthly_card_claims(claim_date);
        """;

    public DatabaseManager(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.dbPath = new File(plugin.getDataFolder(), "monthlycard.db").getAbsolutePath();
    }

    /**
     * 初始化数据库连接和表结构
     */
    public void init() throws SQLException {
        connect();
        createTables();
        plugin.getLogger().info("[Database] SQLite 数据库已初始化");
    }

    /**
     * 建立数据库连接池
     */
    private void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("MonthlyCard-HikariPool");
            
            config.setConnectionInitSql("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL;");
            
            dataSource = new HikariDataSource(config);
            
            plugin.getLogger().info("[Database] HikariCP 连接池已初始化 (maxPoolSize: 5)");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC 驱动未找到", e);
        }
    }

    /**
     * 创建数据表
     */
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            stmt.execute(CREATE_CLAIMS_TABLE_SQL);
            stmt.execute(CREATE_STATS_TABLE_SQL);
            stmt.execute(CREATE_MILESTONE_TABLE_SQL);
            stmt.execute(CREATE_INDEX_SQL);
            
            addColumnIfNotExists(conn, "monthly_card_claims", "is_makeup", "BOOLEAN DEFAULT 0");
        }
    }
    
    /**
     * 如果列不存在则添加
     */
    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDef) {
        String sql = "PRAGMA table_info(" + tableName + ")";
        boolean exists = false;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (columnName.equals(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] 检查列失败: " + e.getMessage());
        }
        
        if (!exists) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
                plugin.getLogger().info("[Database] 添加列 " + columnName + " 到表 " + tableName);
            } catch (SQLException e) {
                plugin.getLogger().warning("[Database] 添加列失败: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭数据库连接池
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[Database] HikariCP 连接池已关闭");
        }
    }

    // ==================== 玩家数据操作 ====================

    /**
     * 加载玩家月卡数据
     */
    public MonthlyCardData loadPlayerData(UUID playerId) {
        String sql = """
            SELECT card_type, activate_time, expire_time, total_claimed_days, 
                   last_claim_time, consecutive_days, makeup_count
            FROM monthly_card_data 
            WHERE player_uuid = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Set<String> claimedDays = loadClaimedDays(playerId);
                
                MonthlyCardData data = MonthlyCardData.fromStorage(
                    playerId,
                    rs.getString("card_type"),
                    rs.getLong("activate_time"),
                    rs.getLong("expire_time"),
                    claimedDays,
                    rs.getInt("total_claimed_days"),
                    rs.getLong("last_claim_time")
                );
                data.setConsecutiveDays(rs.getInt("consecutive_days"));
                data.setMakeupCount(rs.getInt("makeup_count"));
                
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 加载玩家数据失败: " + e.getMessage());
        }

        return new MonthlyCardData(playerId);
    }

    /**
     * 保存玩家月卡数据
     */
    public void savePlayerData(MonthlyCardData data) {
        String sql = """
            INSERT INTO monthly_card_data 
            (player_uuid, card_type, activate_time, expire_time, total_claimed_days, 
             last_claim_time, consecutive_days, makeup_count, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(player_uuid) DO UPDATE SET
            card_type = excluded.card_type,
            activate_time = excluded.activate_time,
            expire_time = excluded.expire_time,
            total_claimed_days = excluded.total_claimed_days,
            last_claim_time = excluded.last_claim_time,
            consecutive_days = excluded.consecutive_days,
            makeup_count = excluded.makeup_count,
            updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getPlayerId().toString());
            stmt.setString(2, data.getCardType());
            stmt.setLong(3, data.getActivateTime());
            stmt.setLong(4, data.getExpireTime());
            stmt.setInt(5, data.getTotalClaimedDays());
            stmt.setLong(6, data.getLastClaimTime());
            stmt.setInt(7, data.getConsecutiveDays());
            stmt.setInt(8, data.getMakeupCount());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 保存玩家数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载已领取天数
     */
    private Set<String> loadClaimedDays(UUID playerId) {
        Set<String> claimedDays = new HashSet<>();
        String sql = "SELECT claim_date FROM monthly_card_claims WHERE player_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                claimedDays.add(rs.getString("claim_date"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 加载领取记录失败: " + e.getMessage());
        }

        return claimedDays;
    }

    /**
     * 记录领取
     */
    public void recordClaim(UUID playerId, String claimDate, int dayNumber, long points, double money) {
        recordClaim(playerId, claimDate, dayNumber, points, money, false);
    }
    
    /**
     * 记录领取（支持补签）
     */
    public void recordClaim(UUID playerId, String claimDate, int dayNumber, long points, double money, boolean isMakeup) {
        String sql = """
            INSERT INTO monthly_card_claims 
            (player_uuid, claim_date, day_number, is_makeup, reward_points, reward_money)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, claim_date) DO UPDATE SET
            day_number = excluded.day_number,
            is_makeup = excluded.is_makeup,
            reward_points = excluded.reward_points,
            reward_money = excluded.reward_money,
            claimed_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, claimDate);
            stmt.setInt(3, dayNumber);
            stmt.setBoolean(4, isMakeup);
            stmt.setLong(5, points);
            stmt.setDouble(6, money);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 记录领取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取玩家补签次数
     */
    public int getMakeupCount(UUID playerId) {
        String sql = """
            SELECT COUNT(*) FROM monthly_card_claims 
            WHERE player_uuid = ? AND is_makeup = 1
            AND claimed_at >= datetime('now', 'start of month')
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询补签次数失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 检查里程碑奖励是否已领取
     */
    public boolean hasClaimedMilestone(UUID playerId, int milestoneDay) {
        String sql = "SELECT 1 FROM monthly_card_milestones WHERE player_uuid = ? AND milestone_day = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, milestoneDay);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询里程碑奖励失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 记录里程碑奖励领取
     */
    public void recordMilestoneClaim(UUID playerId, int milestoneDay) {
        String sql = """
            INSERT INTO monthly_card_milestones (player_uuid, milestone_day)
            VALUES (?, ?)
            ON CONFLICT(player_uuid, milestone_day) DO NOTHING
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, milestoneDay);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 记录里程碑奖励失败: " + e.getMessage());
        }
    }

    /**
     * 删除玩家数据
     */
    public void deletePlayerData(UUID playerId) {
        String deleteClaimsSql = "DELETE FROM monthly_card_claims WHERE player_uuid = ?";
        String deleteDataSql = "DELETE FROM monthly_card_data WHERE player_uuid = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt1 = conn.prepareStatement(deleteClaimsSql);
                 PreparedStatement stmt2 = conn.prepareStatement(deleteDataSql)) {
                stmt1.setString(1, playerId.toString());
                stmt1.executeUpdate();
                
                stmt2.setString(1, playerId.toString());
                stmt2.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 删除玩家数据失败: " + e.getMessage());
        }
    }

    // ==================== 统计查询 ====================

    /**
     * 获取活跃月卡数量
     */
    public int getActiveCardCount() {
        String sql = "SELECT COUNT(*) FROM monthly_card_data WHERE expire_time > ?";
        long now = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, now);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询活跃月卡数量失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 获取今日领取人数
     */
    public int getTodayClaimCount() {
        String sql = "SELECT COUNT(DISTINCT player_uuid) FROM monthly_card_claims WHERE claim_date = ?";
        String today = java.time.LocalDate.now().toString();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询今日领取人数失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 获取所有玩家UUID列表
     */
    public List<UUID> getAllPlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        String sql = "SELECT player_uuid FROM monthly_card_data WHERE card_type != 'none'";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    uuids.add(UUID.fromString(rs.getString("player_uuid")));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Database] 无效的UUID: " + rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询所有玩家失败: " + e.getMessage());
        }

        return uuids;
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 记录每日统计
     */
    public void recordDailyStats(int newCards, int renewedCards, long revenuePoints, double revenueMoney, int dailyClaims) {
        String sql = """
            INSERT INTO monthly_card_stats 
            (stat_date, new_cards, renewed_cards, total_revenue_points, total_revenue_money, daily_claims)
            VALUES (date('now'), ?, ?, ?, ?, ?)
            ON CONFLICT(stat_date) DO UPDATE SET
            new_cards = new_cards + excluded.new_cards,
            renewed_cards = renewed_cards + excluded.renewed_cards,
            total_revenue_points = total_revenue_points + excluded.total_revenue_points,
            total_revenue_money = total_revenue_money + excluded.total_revenue_money,
            daily_claims = daily_claims + excluded.daily_claims
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newCards);
            stmt.setInt(2, renewedCards);
            stmt.setLong(3, revenuePoints);
            stmt.setDouble(4, revenueMoney);
            stmt.setInt(5, dailyClaims);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 记录统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取总销售额（点券）
     */
    public long getTotalRevenuePoints() {
        String sql = "SELECT SUM(total_revenue_points) FROM monthly_card_stats";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询总销售额失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 获取总销售额（游戏币）
     */
    public double getTotalRevenueMoney() {
        String sql = "SELECT SUM(total_revenue_money) FROM monthly_card_stats";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询总销售额失败: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * 获取本月新购月卡数量
     */
    public int getMonthlyNewCards() {
        String sql = """
            SELECT SUM(new_cards) FROM monthly_card_stats 
            WHERE stat_date >= date('now', 'start of month')
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Database] 查询月新购数量失败: " + e.getMessage());
        }
        return 0;
    }
}