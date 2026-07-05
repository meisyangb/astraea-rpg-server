package cn.guangdian.lottery.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** 抽奖数据 SQLite 存储 - 冷却时间 + 抽奖历史 */
public class LotteryStorage {
    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection conn;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> history = new ConcurrentHashMap<>();

    public LotteryStorage(JavaPlugin p) { this.plugin = p; this.dbFile = new File(p.getDataFolder(), "lottery.db"); }

    public boolean init() {
        try { Class.forName("org.sqlite.JDBC"); dbFile.getParentFile().mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement s = conn.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS cooldowns(uuid TEXT, pool_id TEXT, last_time BIGINT, PRIMARY KEY(uuid,pool_id))");
                s.execute("CREATE TABLE IF NOT EXISTS history(uuid TEXT, idx INTEGER, prize TEXT, PRIMARY KEY(uuid,idx))");
            }
            plugin.getLogger().info("SQLite 抽奖数据已初始化");
            return true;
        } catch (Exception e) { plugin.getLogger().severe("SQLite 抽奖初始化失败: "+e.getMessage()); return false; }
    }

    public void load() {
        if (conn == null) return;
        cooldowns.clear(); history.clear();
        try (Statement s = conn.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM cooldowns");
            while (rs.next()) {
                UUID u = UUID.fromString(rs.getString("uuid"));
                cooldowns.computeIfAbsent(u, k -> new ConcurrentHashMap<>()).put(rs.getString("pool_id"), rs.getLong("last_time"));
            }
            rs = s.executeQuery("SELECT * FROM history ORDER BY uuid, idx");
            while (rs.next()) {
                UUID u = UUID.fromString(rs.getString("uuid"));
                history.computeIfAbsent(u, k -> Collections.synchronizedList(new ArrayList<>())).add(rs.getString("prize"));
            }
        } catch (SQLException e) { plugin.getLogger().warning("加载抽奖数据失败: "+e.getMessage()); }
        plugin.getLogger().info("已加载抽奖数据: " + cooldowns.size() + " 玩家冷却, " + history.size() + " 玩家历史");
    }

    public CompletableFuture<Void> saveAsync() { return CompletableFuture.runAsync(this::save); }

    public void save() {
        if (conn == null) return;
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement pd = conn.prepareStatement("DELETE FROM cooldowns");
                 PreparedStatement pi = conn.prepareStatement("INSERT OR REPLACE INTO cooldowns VALUES(?,?,?)");
                 PreparedStatement hd = conn.prepareStatement("DELETE FROM history");
                 PreparedStatement hi = conn.prepareStatement("INSERT INTO history VALUES(?,?,?)")) {
                pd.executeUpdate(); hd.executeUpdate();
                for (var e : cooldowns.entrySet())
                    for (var e2 : e.getValue().entrySet()) {
                        pi.setString(1, e.getKey().toString()); pi.setString(2, e2.getKey()); pi.setLong(3, e2.getValue()); pi.addBatch();
                    }
                pi.executeBatch();
                for (var e : history.entrySet()) {
                    int idx = 0;
                    for (String prize : e.getValue()) { hi.setString(1, e.getKey().toString()); hi.setInt(2, idx++); hi.setString(3, prize); hi.addBatch(); }
                }
                hi.executeBatch();
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { plugin.getLogger().severe("保存抽奖数据失败: "+e.getMessage()); }
    }

    public void close() { if (conn != null) try { conn.close(); } catch (SQLException ignored) {} }

    public Map<UUID, Map<String, Long>> cooldowns() { return cooldowns; }
    public Map<UUID, List<String>> history() { return history; }
}
