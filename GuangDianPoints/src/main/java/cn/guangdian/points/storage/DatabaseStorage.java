package cn.guangdian.points.storage;

import cn.guangdian.points.GuangDianPoints;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/**
 * 点券 SQLite 数据库存储管理器
 *
 * <p>使用 SQLite 数据库存储点券数据，支持异步操作。</p>
 *
 * @author GuangDian
 * @since 1.2.0
 */
public class DatabaseStorage {

    private final GuangDianPoints plugin;
    private final File dbFile;
    private Connection connection;

    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> totalEarned = new ConcurrentHashMap<>();
    private final Map<UUID, Long> totalSpent = new ConcurrentHashMap<>();

    public DatabaseStorage(GuangDianPoints plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "points.db");
    }

    /**
     * 初始化数据库连接和表结构
     */
    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);

            createTables();
            plugin.getLogger().info("SQLite 数据库已初始化: " + dbFile.getAbsolutePath());
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC 驱动未找到: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("初始化 SQLite 数据库失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS points_player_data (" +
            "uuid TEXT PRIMARY KEY," +
            "points BIGINT DEFAULT 0," +
            "total_earned BIGINT DEFAULT 0," +
            "total_spent BIGINT DEFAULT 0," +
            "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS points_transactions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "amount BIGINT NOT NULL," +
            "balance_before BIGINT NOT NULL," +
            "balance_after BIGINT NOT NULL," +
            "transaction_type TEXT NOT NULL," +
            "reason TEXT," +
            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (uuid) REFERENCES points_player_data(uuid)" +
            ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerDataTable);
            stmt.execute(createTransactionsTable);
        }
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("SQLite 数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 检查数据库是否可用
     */
    public boolean isEnabled() {
        return connection != null;
    }

    /**
     * 从数据库加载所有数据
     */
    public void load() {
        if (!isEnabled()) {
            plugin.getLogger().warning("数据库未初始化，无法加载点券数据");
            return;
        }

        balances.clear();
        totalEarned.clear();
        totalSpent.clear();

        String sql = "SELECT uuid, points, total_earned, total_spent FROM points_player_data";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long points = rs.getLong("points");
                long earned = rs.getLong("total_earned");
                long spent = rs.getLong("total_spent");

                balances.put(uuid, points);
                totalEarned.put(uuid, earned);
                totalSpent.put(uuid, spent);
            }

            plugin.getLogger().info("已从数据库加载 " + balances.size() + " 个玩家点券数据");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载点券数据库数据失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    /**
     * 异步保存所有数据
     */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::saveSync);
    }

    /**
     * 同步保存所有数据
     */
    public void saveSync() {
        if (!isEnabled()) {
            return;
        }

        String sql = "INSERT INTO points_player_data (uuid, points, total_earned, total_spent) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "points = excluded.points, " +
                     "total_earned = excluded.total_earned, " +
                     "total_spent = excluded.total_spent, " +
                     "last_update = CURRENT_TIMESTAMP";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
                UUID uuid = entry.getKey();
                ps.setString(1, uuid.toString());
                ps.setLong(2, entry.getValue());
                ps.setLong(3, totalEarned.getOrDefault(uuid, 0L));
                ps.setLong(4, totalSpent.getOrDefault(uuid, 0L));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存点券数据到数据库失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    /**
     * 异步保存单个玩家数据
     */
    public CompletableFuture<Void> savePlayerAsync(UUID uuid) {
        return CompletableFuture.runAsync(() -> savePlayerSync(uuid));
    }

    /**
     * 同步保存单个玩家数据
     */
    public void savePlayerSync(UUID uuid) {
        if (!isEnabled()) {
            return;
        }

        String sql = "INSERT INTO points_player_data (uuid, points, total_earned, total_spent) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "points = excluded.points, " +
                     "total_earned = excluded.total_earned, " +
                     "total_spent = excluded.total_spent, " +
                     "last_update = CURRENT_TIMESTAMP";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, balances.getOrDefault(uuid, 0L));
            ps.setLong(3, totalEarned.getOrDefault(uuid, 0L));
            ps.setLong(4, totalSpent.getOrDefault(uuid, 0L));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家点券数据失败: " + e.getMessage());
        }
    }

    /**
     * 记录交易到数据库
     */
    public void logTransaction(UUID uuid, long amount, long balanceBefore, long balanceAfter,
                                String transactionType, String reason) {
        if (!isEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO points_transactions " +
                         "(uuid, amount, balance_before, balance_after, transaction_type, reason) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, amount);
                ps.setLong(3, balanceBefore);
                ps.setLong(4, balanceAfter);
                ps.setString(5, transactionType);
                ps.setString(6, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("记录交易日志失败: " + e.getMessage());
            }
        });
    }

    public Map<UUID, Long> getBalances() {
        return balances;
    }

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getDefaultBalance());
    }

    /**
     * 设置余额（原子操作）
     */
    public void setBalance(UUID uuid, long amount) {
        balances.compute(uuid, (key, oldBalance) -> {
            long old = oldBalance != null ? oldBalance : plugin.getDefaultBalance();
            long diff = amount - old;

            if (diff > 0) {
                totalEarned.merge(uuid, diff, Long::sum);
            } else if (diff < 0) {
                totalSpent.merge(uuid, -diff, Long::sum);
            }

            return amount;
        });
    }

    /**
     * 增加余额（原子操作）
     */
    public void addBalance(UUID uuid, long amount) {
        balances.compute(uuid, (key, oldBalance) -> {
            long newBalance = (oldBalance != null ? oldBalance : plugin.getDefaultBalance()) + amount;
            totalEarned.merge(uuid, amount, Long::sum);
            return newBalance;
        });
    }

    /**
     * 扣除余额（原子操作）
     *
     * @return 是否成功扣除
     */
    public boolean removeBalance(UUID uuid, long amount) {
        AtomicLong newBalance = new AtomicLong(-1);

        balances.computeIfPresent(uuid, (key, current) -> {
            if (current >= amount) {
                newBalance.set(current - amount);
                totalSpent.merge(uuid, amount, Long::sum);
                return current - amount;
            }
            return current;
        });

        if (!balances.containsKey(uuid)) {
            long defaultBalance = plugin.getDefaultBalance();
            if (defaultBalance >= amount) {
                balances.put(uuid, defaultBalance - amount);
                totalSpent.merge(uuid, amount, Long::sum);
                return true;
            }
            return false;
        }

        return newBalance.get() >= 0;
    }

    /**
     * 原子性地修改余额
     *
     * @param updateFunction 修改函数，参数为当前余额，返回新余额
     * @return 修改后的余额
     */
    public long updateBalanceAtomically(UUID uuid, BiFunction<UUID, Long, Long> updateFunction) {
        AtomicLong result = new AtomicLong();
        balances.compute(uuid, (key, oldBalance) -> {
            long old = oldBalance != null ? oldBalance : plugin.getDefaultBalance();
            long newBalance = updateFunction.apply(key, old);
            result.set(newBalance);

            long diff = newBalance - old;
            if (diff > 0) {
                totalEarned.merge(uuid, diff, Long::sum);
            } else if (diff < 0) {
                totalSpent.merge(uuid, -diff, Long::sum);
            }

            return newBalance;
        });
        return result.get();
    }

    public int getPlayerCount() {
        return balances.size();
    }

    public long getTotalEarned(UUID uuid) {
        return totalEarned.getOrDefault(uuid, 0L);
    }

    public long getTotalSpent(UUID uuid) {
        return totalSpent.getOrDefault(uuid, 0L);
    }
}
