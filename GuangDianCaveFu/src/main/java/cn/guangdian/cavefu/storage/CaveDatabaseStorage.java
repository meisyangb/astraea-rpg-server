package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.permission.PermissionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 洞府 SQLite 数据库存储管理器
 * <p>参考 GuangDianPoints.DatabaseStorage 的 SQLite + 异步保存模式</p>
 * <p>SQLite 线程安全，支持 CompletableFuture.runAsync 异步写入</p>
 */
public class CaveDatabaseStorage {

    private final GuangDianCaveFu plugin;
    private final File dbFile;
    private Connection connection;

    private final Map<Integer, Cave> cavesById = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByMember = new ConcurrentHashMap<>();
    private final AtomicInteger nextCaveId = new AtomicInteger(1);

    public CaveDatabaseStorage(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "caves.db");
    }

    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbFile.getParentFile().mkdirs();
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

    private void createTables() throws SQLException {
        String createCaves = """
            CREATE TABLE IF NOT EXISTS caves (
                id INTEGER PRIMARY KEY,
                owner_uuid TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                world_name TEXT NOT NULL,
                center_x INTEGER NOT NULL,
                center_z INTEGER NOT NULL,
                home_x REAL DEFAULT 0,
                home_y REAL DEFAULT 66,
                home_z REAL DEFAULT 0,
                home_yaw REAL DEFAULT 0,
                home_pitch REAL DEFAULT 0,
                create_time BIGINT NOT NULL
            )
            """;

        String createMembers = """
            CREATE TABLE IF NOT EXISTS cave_members (
                cave_id INTEGER NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                permission TEXT NOT NULL,
                join_time BIGINT NOT NULL,
                PRIMARY KEY (cave_id, uuid),
                FOREIGN KEY (cave_id) REFERENCES caves(id) ON DELETE CASCADE
            )
            """;

        String createMeta = """
            CREATE TABLE IF NOT EXISTS cave_meta (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(createCaves);
            stmt.execute(createMembers);
            stmt.execute(createMeta);
        }
    }

    public boolean isEnabled() {
        return connection != null;
    }

    /**
     * 从数据库加载所有洞府数据
     */
    public void load() {
        if (!isEnabled()) return;

        cavesById.clear();
        cavesByOwner.clear();
        cavesByMember.clear();

        // 读取 next-cave-id
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM cave_meta WHERE key = 'next_cave_id'")) {
            if (rs.next()) {
                nextCaveId.set(Integer.parseInt(rs.getString("value")));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("读取 next_cave_id 失败: " + e.getMessage());
        }

        // 读取洞府
        Map<Integer, Cave> tempCaves = new HashMap<>();
        String caveSql = "SELECT * FROM caves";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(caveSql)) {
            while (rs.next()) {
                Cave cave = readCaveFromResultSet(rs);
                tempCaves.put(cave.getId(), cave);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载洞府数据失败: " + e.getMessage());
            return;
        }

        // 读取成员
        String memberSql = "SELECT * FROM cave_members";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(memberSql)) {
            while (rs.next()) {
                int caveId = rs.getInt("cave_id");
                Cave cave = tempCaves.get(caveId);
                if (cave != null) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    PermissionType perm = PermissionType.fromString(rs.getString("permission"));
                    CaveMember member = new CaveMember(uuid, name, perm);
                    cave.getMembers().put(uuid, member);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载成员数据失败: " + e.getMessage());
        }

        // 构建索引
        for (Cave cave : tempCaves.values()) {
            cave.clearDirty();
            cavesById.put(cave.getId(), cave);
            cavesByOwner.put(cave.getOwnerUuid(), cave);
            for (UUID memberUuid : cave.getMembers().keySet()) {
                cavesByMember.put(memberUuid, cave);
            }
        }

        plugin.getLogger().info("已从数据库加载 " + cavesById.size() + " 个洞府，nextCaveId=" + nextCaveId.get());
    }

    private Cave readCaveFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");
        int level = rs.getInt("level");
        String worldName = rs.getString("world_name");
        int centerX = rs.getInt("center_x");
        int centerZ = rs.getInt("center_z");

        Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
        // 直接设置字段而不是通过构造函数默认值
        cave.setHomeLocationRaw(
            rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
            rs.getFloat("home_yaw"), rs.getFloat("home_pitch")
        );
        return cave;
    }

    // ==================== 保存方法 ====================

    /**
     * 异步全量保存（定时自动保存 + 玩家退出保存）
     */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::saveSync);
    }

    /**
     * 同步全量保存（关闭时调用）
     */
    public void saveSync() {
        if (!isEnabled()) return;

        try {
            connection.setAutoCommit(false);
            try {
                // 保存 meta
                String upsertMeta = "INSERT OR REPLACE INTO cave_meta (key, value) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(upsertMeta)) {
                    ps.setString(1, "next_cave_id");
                    ps.setString(2, String.valueOf(nextCaveId.get()));
                    ps.executeUpdate();
                }

                // 保存洞府
                String upsertCave = """
                    INSERT OR REPLACE INTO caves
                    (id, owner_uuid, owner_name, level, world_name, center_x, center_z,
                     home_x, home_y, home_z, home_yaw, home_pitch, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                String deleteMembers = "DELETE FROM cave_members WHERE cave_id = ?";
                String insertMember = "INSERT INTO cave_members (cave_id, uuid, name, permission, join_time) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement psCave = connection.prepareStatement(upsertCave);
                     PreparedStatement psDelMem = connection.prepareStatement(deleteMembers);
                     PreparedStatement psInsMem = connection.prepareStatement(insertMember)) {

                    for (Cave cave : cavesById.values()) {
                        psCave.setInt(1, cave.getId());
                        psCave.setString(2, cave.getOwnerUuid().toString());
                        psCave.setString(3, cave.getOwnerName());
                        psCave.setInt(4, cave.getLevel());
                        psCave.setString(5, cave.getWorldName());
                        psCave.setInt(6, cave.getCenterX());
                        psCave.setInt(7, cave.getCenterZ());
                        psCave.setDouble(8, cave.getHomeX());
                        psCave.setDouble(9, cave.getHomeY());
                        psCave.setDouble(10, cave.getHomeZ());
                        psCave.setFloat(11, cave.getHomeYaw());
                        psCave.setFloat(12, cave.getHomePitch());
                        psCave.setLong(13, cave.getCreateTime());
                        psCave.addBatch();

                        // 删除旧成员，重新插入
                        psDelMem.setInt(1, cave.getId());
                        psDelMem.addBatch();

                        for (CaveMember member : cave.getMembers().values()) {
                            psInsMem.setInt(1, cave.getId());
                            psInsMem.setString(2, member.getUuid().toString());
                            psInsMem.setString(3, member.getName());
                            psInsMem.setString(4, member.getPermission().name());
                            psInsMem.setLong(5, member.getJoinTime());
                            psInsMem.addBatch();
                        }
                    }
                    psCave.executeBatch();
                    psDelMem.executeBatch();
                    psInsMem.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存洞府数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 同步保存单个洞府（操作后立即调用，事务性）
     */
    public void saveCaveSync(Cave cave) {
        if (!isEnabled()) return;

        try {
            connection.setAutoCommit(false);
            try {
                String upsertCave = """
                    INSERT OR REPLACE INTO caves
                    (id, owner_uuid, owner_name, level, world_name, center_x, center_z,
                     home_x, home_y, home_z, home_yaw, home_pitch, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement ps = connection.prepareStatement(upsertCave)) {
                    ps.setInt(1, cave.getId());
                    ps.setString(2, cave.getOwnerUuid().toString());
                    ps.setString(3, cave.getOwnerName());
                    ps.setInt(4, cave.getLevel());
                    ps.setString(5, cave.getWorldName());
                    ps.setInt(6, cave.getCenterX());
                    ps.setInt(7, cave.getCenterZ());
                    ps.setDouble(8, cave.getHomeX());
                    ps.setDouble(9, cave.getHomeY());
                    ps.setDouble(10, cave.getHomeZ());
                    ps.setFloat(11, cave.getHomeYaw());
                    ps.setFloat(12, cave.getHomePitch());
                    ps.setLong(13, cave.getCreateTime());
                    ps.executeUpdate();
                }

                // 更新成员
                try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM cave_members WHERE cave_id = ?")) {
                    psDel.setInt(1, cave.getId());
                    psDel.executeUpdate();
                }

                String insertMember = "INSERT INTO cave_members (cave_id, uuid, name, permission, join_time) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertMember)) {
                    for (CaveMember member : cave.getMembers().values()) {
                        ps.setInt(1, cave.getId());
                        ps.setString(2, member.getUuid().toString());
                        ps.setString(3, member.getName());
                        ps.setString(4, member.getPermission().name());
                        ps.setLong(5, member.getJoinTime());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 保存 next_cave_id
                String upsertMeta = "INSERT OR REPLACE INTO cave_meta (key, value) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(upsertMeta)) {
                    ps.setString(1, "next_cave_id");
                    ps.setString(2, String.valueOf(nextCaveId.get()));
                    ps.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存洞府失败: caveId=" + cave.getId() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 同步删除洞府
     */
    public void deleteCaveSync(int caveId) {
        if (!isEnabled()) return;

        String sql = "DELETE FROM caves WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, caveId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除洞府失败: " + e.getMessage());
        }
    }

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

    // ==================== 内存 CRUD ====================

    public Cave createCave(UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        int id = nextCaveId.getAndIncrement();
        Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
        cavesById.put(id, cave);
        cavesByOwner.put(ownerUuid, cave);
        cavesByMember.put(ownerUuid, cave);
        return cave;
    }

    public void removeCave(int id) {
        Cave cave = cavesById.remove(id);
        if (cave != null) {
            cavesByOwner.remove(cave.getOwnerUuid());
            for (UUID memberUuid : cave.getMembers().keySet()) {
                cavesByMember.remove(memberUuid);
            }
        }
    }

    // ==================== 查询 ====================

    public Cave getCaveById(int id) { return cavesById.get(id); }
    public Cave getCaveByOwner(UUID uuid) { return cavesByOwner.get(uuid); }
    public Cave getCaveByMember(UUID uuid) { return cavesByMember.get(uuid); }

    public Cave getCaveAtLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Cave cave : cavesById.values()) {
            if (cave.isInside(loc)) return cave;
        }
        return null;
    }

    public Collection<Cave> getAllCaves() { return cavesById.values(); }
    public int getCaveCount() { return cavesById.size(); }
    public int getNextCaveId() { return nextCaveId.get(); }

    public void updateMemberIndex(UUID uuid, Cave cave) {
        if (cave != null) cavesByMember.put(uuid, cave);
        else cavesByMember.remove(uuid);
    }
}
