package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.permission.PermissionType;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SQLiteDataManager {

    private final GuangDianCaveFu plugin;
    private final File databaseFile;
    private Connection connection;

    private final Map<Integer, Cave> cavesById = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByMember = new ConcurrentHashMap<>();
    private int nextCaveId = 1;

    public SQLiteDataManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "data.db");
    }

    public void load() {
        try {
            initializeDatabase();
            loadCaves();
            loadMembers();
            plugin.getLogger().info("[SQLite] 已加载 " + cavesById.size() + " 个洞府");
        } catch (SQLException e) {
            plugin.getLogger().severe("[SQLite] 加载数据失败: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "详细异常信息", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        if (!databaseFile.exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS caves (
                    id INTEGER PRIMARY KEY,
                    owner_uuid TEXT NOT NULL UNIQUE,
                    owner_name TEXT NOT NULL,
                    level INTEGER DEFAULT 1,
                    world_name TEXT NOT NULL,
                    center_x INTEGER NOT NULL,
                    center_z INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cave_id INTEGER NOT NULL,
                    member_uuid TEXT NOT NULL UNIQUE,
                    member_name TEXT NOT NULL,
                    permission_level INTEGER DEFAULT 0,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (cave_id) REFERENCES caves(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_caves_owner ON caves(owner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_members_cave ON members(cave_id)");
        }

        plugin.getLogger().info("[SQLite] 数据库初始化完成: " + databaseFile.getName());
    }

    private void loadCaves() throws SQLException {
        String sql = "SELECT id, owner_uuid, owner_name, level, world_name, center_x, center_z FROM caves";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                String ownerName = rs.getString("owner_name");
                int level = rs.getInt("level");
                String worldName = rs.getString("world_name");
                int centerX = rs.getInt("center_x");
                int centerZ = rs.getInt("center_z");

                Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
                cavesById.put(id, cave);
                cavesByOwner.put(ownerUuid, cave);
                cavesByMember.put(ownerUuid, cave);

                if (id >= nextCaveId) {
                    nextCaveId = id + 1;
                }
            }
        }
    }

    private void loadMembers() throws SQLException {
        String sql = "SELECT cave_id, member_uuid, member_name, permission_level FROM members";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int caveId = rs.getInt("cave_id");
                UUID memberUuid = UUID.fromString(rs.getString("member_uuid"));
                String memberName = rs.getString("member_name");
                int permissionLevel = rs.getInt("permission_level");

                Cave cave = cavesById.get(caveId);
                if (cave != null) {
                    PermissionType permission = PermissionType.values()[Math.min(permissionLevel, PermissionType.values().length - 1)];
                    cave.addMember(memberUuid, memberName, permission);
                    cavesByMember.put(memberUuid, cave);
                }
            }
        }
    }

    public void saveSync() {
        if (connection == null) {
            return;
        }

        try {
            connection.setAutoCommit(false);

            int savedCount = 0;
            int totalCount = cavesById.size();
            
            for (Cave cave : cavesById.values()) {
                // 只保存脏数据
                if (cave.isDirty()) {
                    saveCave(cave);
                    saveMembers(cave);
                    cave.clearDirty();  // 保存成功后清除脏标记
                    savedCount++;
                }
            }

            connection.commit();
            
            if (savedCount > 0) {
                plugin.getLogger().fine("[SQLite] 增量保存完成: " + savedCount + "/" + totalCount + " 个洞府");
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                plugin.getLogger().severe("[SQLite] 回滚失败: " + rollbackEx.getMessage());
            }
            plugin.getLogger().severe("[SQLite] 保存数据失败: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "详细异常信息", e);
        }
    }

    private void saveCave(Cave cave) throws SQLException {
        String sql = "INSERT OR REPLACE INTO caves (id, owner_uuid, owner_name, level, world_name, center_x, center_z) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cave.getId());
            ps.setString(2, cave.getOwnerUuid().toString());
            ps.setString(3, cave.getOwnerName());
            ps.setInt(4, cave.getLevel());
            ps.setString(5, cave.getWorldName());
            ps.setInt(6, cave.getCenterX());
            ps.setInt(7, cave.getCenterZ());
            ps.executeUpdate();
        }
    }

    private void saveMembers(Cave cave) throws SQLException {
        // 使用临时表保存现有数据，如果插入失败可以恢复
        String tempTableName = "members_temp_" + cave.getId();
        
        try {
            // 1. 创建临时表并复制现有数据
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TEMP TABLE IF NOT EXISTS " + tempTableName + 
                           " AS SELECT * FROM members WHERE cave_id = " + cave.getId());
            }
            
            // 2. 删除现有成员数据
            String deleteSql = "DELETE FROM members WHERE cave_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                ps.setInt(1, cave.getId());
                ps.executeUpdate();
            }
            
            // 3. 插入新成员数据
            if (!cave.getMembers().isEmpty()) {
                String insertSql = "INSERT INTO members (cave_id, member_uuid, member_name, permission_level) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<UUID, CaveMember> entry : cave.getMembers().entrySet()) {
                        CaveMember member = entry.getValue();
                        ps.setInt(1, cave.getId());
                        ps.setString(2, member.getUuid().toString());
                        ps.setString(3, member.getName());
                        ps.setInt(4, member.getPermission().ordinal());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            
            // 4. 成功后删除临时表
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS " + tempTableName);
            }
            
        } catch (SQLException e) {
            // 5. 失败后恢复数据
            plugin.getLogger().severe("[SQLite] 保存成员数据失败，尝试恢复: " + e.getMessage());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("INSERT OR IGNORE INTO members SELECT * FROM " + tempTableName);
                stmt.execute("DROP TABLE IF EXISTS " + tempTableName);
            } catch (SQLException restoreEx) {
                plugin.getLogger().severe("[SQLite] 恢复成员数据失败！数据可能已丢失: " + restoreEx.getMessage());
            }
            throw e;  // 重新抛出异常，触发回滚
        }
    }

    public Cave createCave(int caveId, UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        try {
            String sql = "INSERT INTO caves (id, owner_uuid, owner_name, level, world_name, center_x, center_z) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, caveId);
                ps.setString(2, ownerUuid.toString());
                ps.setString(3, ownerName);
                ps.setInt(4, level);
                ps.setString(5, worldName);
                ps.setInt(6, centerX);
                ps.setInt(7, centerZ);
                ps.executeUpdate();

                Cave cave = new Cave(caveId, ownerUuid, ownerName, level, worldName, centerX, centerZ);

                cavesById.put(caveId, cave);
                cavesByOwner.put(ownerUuid, cave);
                cavesByMember.put(ownerUuid, cave);

                nextCaveId = caveId + 1;

                return cave;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[SQLite] 创建洞府失败: " + e.getMessage(), e);
        }
        return null;
    }

    public CompletableFuture<Void> deleteCaveAsync(int id) {
        return CompletableFuture.runAsync(() -> {
            Cave cave = cavesById.get(id);
            if (cave == null) return;

            try {
                String sql = "DELETE FROM caves WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                cavesById.remove(id);
                cavesByOwner.remove(cave.getOwnerUuid());
                for (UUID memberUuid : cave.getMembers().keySet()) {
                    cavesByMember.remove(memberUuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[SQLite] 删除洞府失败: " + e.getMessage(), e);
            }
        });
    }

    public void close() {
        if (connection != null) {
            try {
                saveSync();
                connection.close();
                plugin.getLogger().info("[SQLite] 数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().severe("[SQLite] 关闭数据库失败: " + e.getMessage());
            }
        }
    }

    public Map<Integer, Cave> getCavesById() {
        return cavesById;
    }

    public Map<UUID, Cave> getCavesByOwner() {
        return cavesByOwner;
    }

    public Map<UUID, Cave> getCavesByMember() {
        return cavesByMember;
    }

    public int getNextCaveId() {
        return nextCaveId;
    }

    public int getCaveCount() {
        return cavesById.size();
    }
}