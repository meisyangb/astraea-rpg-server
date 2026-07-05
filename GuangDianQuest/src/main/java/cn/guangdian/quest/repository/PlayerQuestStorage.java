package cn.guangdian.quest.repository;

import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.QuestType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家任务数据 SQLite 存储管理器
 * <p>参考 GuangDianPoints.DatabaseStorage 的 SQLite + 异步保存模式</p>
 */
public class PlayerQuestStorage {

    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    private final Map<UUID, PlayerQuestData> cache = new ConcurrentHashMap<>();

    public PlayerQuestStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "playerdata.db");
    }

    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbFile.getParentFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
            createTables();
            plugin.getLogger().info("SQLite 任务数据库已初始化: " + dbFile.getAbsolutePath());
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC 驱动未找到: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("初始化 SQLite 任务数据库失败: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_player_data (
                    uuid TEXT PRIMARY KEY,
                    total_completed INTEGER DEFAULT 0,
                    achievement_points INTEGER DEFAULT 0,
                    daily_completed_count INTEGER DEFAULT 0,
                    daily_reset_time BIGINT DEFAULT 0
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_active (
                    uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    objective_index INTEGER NOT NULL,
                    progress INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, quest_id, objective_index)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_completed (
                    uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    completion_time BIGINT NOT NULL,
                    PRIMARY KEY (uuid, quest_id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_line_progress (
                    uuid TEXT NOT NULL,
                    line_id TEXT NOT NULL,
                    progress INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, line_id)
                )
                """);
        }
    }

    public boolean isEnabled() { return connection != null; }

    // ==================== 加载 ====================

    public PlayerQuestData loadPlayer(UUID uuid) {
        PlayerQuestData data = new PlayerQuestData(uuid);

        if (!isEnabled()) return data;

        try {
            // 加载基本信息
            String sql = "SELECT * FROM quest_player_data WHERE uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.setTotalCompletedCount(rs.getInt("total_completed"));
                        data.setAchievementPoints(rs.getInt("achievement_points"));
                        data.setDailyCompletedCount(rs.getInt("daily_completed_count"));
                        data.setDailyResetTime(rs.getLong("daily_reset_time"));
                    }
                }
            }

            // 加载活跃任务
            String activeSql = "SELECT quest_id, objective_index, progress FROM quest_active WHERE uuid = ? ORDER BY quest_id, objective_index";
            try (PreparedStatement ps = connection.prepareStatement(activeSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, List<int[]>> tempActive = new LinkedHashMap<>();
                    while (rs.next()) {
                        String questId = rs.getString("quest_id");
                        int objIdx = rs.getInt("objective_index");
                        int prog = rs.getInt("progress");
                        tempActive.computeIfAbsent(questId, k -> new ArrayList<>()).add(new int[]{objIdx, prog});
                    }
                    for (var entry : tempActive.entrySet()) {
                        int maxIdx = entry.getValue().stream().mapToInt(a -> a[0]).max().orElse(0);
                        int[] progress = new int[maxIdx + 1];
                        for (int[] pair : entry.getValue()) {
                            if (pair[0] < progress.length) progress[pair[0]] = pair[1];
                        }
                        data.loadActiveQuest(entry.getKey(), progress);
                    }
                }
            }

            // 加载已完成任务
            String completedSql = "SELECT quest_id, completion_time FROM quest_completed WHERE uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(completedSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.loadCompletedQuest(rs.getString("quest_id"), rs.getLong("completion_time"));
                    }
                }
            }

            // 加载任务线进度
            String lineSql = "SELECT line_id, progress FROM quest_line_progress WHERE uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(lineSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.loadQuestLineProgress(rs.getString("line_id"), rs.getInt("progress"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载玩家任务数据失败: " + uuid + " - " + e.getMessage());
        }

        return data;
    }

    // ==================== 保存 ====================

    /** 同步保存单个玩家（操作后立即调用） */
    public void savePlayerSync(UUID uuid, PlayerQuestData data) {
        if (!isEnabled()) return;

        try {
            connection.setAutoCommit(false);
            try {
                // 基本信息
                String upsertData = """
                    INSERT OR REPLACE INTO quest_player_data
                    (uuid, total_completed, achievement_points, daily_completed_count, daily_reset_time)
                    VALUES (?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement ps = connection.prepareStatement(upsertData)) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, data.getTotalCompletedCount());
                    ps.setInt(3, data.getAchievementPoints());
                    ps.setInt(4, data.getDailyCompletedCount());
                    ps.setLong(5, data.getDailyResetTime());
                    ps.executeUpdate();
                }

                // 活跃任务
                try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM quest_active WHERE uuid = ?")) {
                    psDel.setString(1, uuid.toString());
                    psDel.executeUpdate();
                }
                String insertActive = "INSERT INTO quest_active (uuid, quest_id, objective_index, progress) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertActive)) {
                    for (String questId : data.getActiveQuestIds()) {
                        int[] progress = data.getProgress(questId);
                        if (progress == null) continue;
                        for (int i = 0; i < progress.length; i++) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, questId);
                            ps.setInt(3, i);
                            ps.setInt(4, progress[i]);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                // 已完成任务
                try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM quest_completed WHERE uuid = ?")) {
                    psDel.setString(1, uuid.toString());
                    psDel.executeUpdate();
                }
                String insertCompleted = "INSERT INTO quest_completed (uuid, quest_id, completion_time) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertCompleted)) {
                    for (var entry : data.getCompletedQuests().entrySet()) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, entry.getKey());
                        ps.setLong(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 任务线进度
                try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM quest_line_progress WHERE uuid = ?")) {
                    psDel.setString(1, uuid.toString());
                    psDel.executeUpdate();
                }
                String insertLine = "INSERT INTO quest_line_progress (uuid, line_id, progress) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertLine)) {
                    for (Map.Entry<String, Integer> entry : data.getQuestLineProgressMap().entrySet()) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家任务数据失败: " + uuid + " - " + e.getMessage());
        }
    }

    /** 异步保存单个玩家（定时/退出） */
    public CompletableFuture<Void> savePlayerAsync(UUID uuid, PlayerQuestData data) {
        return CompletableFuture.runAsync(() -> savePlayerSync(uuid, data));
    }

    /** 异步全量保存 */
    public CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            for (var entry : cache.entrySet()) {
                savePlayerSync(entry.getKey(), entry.getValue());
            }
        });
    }

    /** 同步全量保存（关闭时） */
    public void saveAll() {
        for (var entry : cache.entrySet()) {
            savePlayerSync(entry.getKey(), entry.getValue());
        }
    }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    // ==================== 缓存管理 ====================

    public PlayerQuestData getCached(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayer);
    }

    public void removeCached(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }
}
