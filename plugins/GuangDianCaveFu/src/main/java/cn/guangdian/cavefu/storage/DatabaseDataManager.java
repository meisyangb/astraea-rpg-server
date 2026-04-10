package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.permission.PermissionType;
import cn.guangdian.rpgcore.database.CoreDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 数据库存储管理器
 * 
 * <p>使用 MySQL 数据库存储洞府数据，支持异步操作。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DatabaseDataManager {

    private final GuangDianCaveFu plugin;
    private final Map<Integer, Cave> cavesById = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Cave> cavesByMember = new ConcurrentHashMap<>();
    private int nextCaveId = 1;

    public DatabaseDataManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
    }

    /**
     * 从数据库加载所有数据
     */
    public void load() {
        if (!CoreDatabase.isEnabled()) {
            plugin.getLogger().warning("数据库未启用，无法加载数据");
            return;
        }

        cavesById.clear();
        cavesByOwner.clear();
        cavesByMember.clear();

        try (Connection conn = CoreDatabase.getConnection()) {
            // 先创建表（如果不存在）
            createTablesIfNotExist(conn);
            
            loadCaves(conn);
            loadMembers(conn);

            plugin.getLogger().info("已从数据库加载 " + cavesById.size() + " 个洞府");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载数据库数据失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    /**
     * 创建数据库表（如果不存在）
     */
    private void createTablesIfNotExist(Connection conn) throws SQLException {
        // 创建洞府表
        String createCavesTable = """
            CREATE TABLE IF NOT EXISTS cavefu_caves (
                id INT AUTO_INCREMENT PRIMARY KEY,
                owner_uuid VARCHAR(36) NOT NULL UNIQUE,
                owner_name VARCHAR(16) NOT NULL,
                level INT DEFAULT 1,
                world_name VARCHAR(64) NOT NULL,
                center_x INT NOT NULL,
                center_z INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
        
        // 创建成员表
        String createMembersTable = """
            CREATE TABLE IF NOT EXISTS cavefu_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                cave_id INT NOT NULL,
                member_uuid VARCHAR(36) NOT NULL,
                member_name VARCHAR(16) NOT NULL,
                permission_level INT DEFAULT 0,
                added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (cave_id) REFERENCES cavefu_caves(id) ON DELETE CASCADE,
                UNIQUE KEY unique_member (member_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createCavesTable);
            stmt.execute(createMembersTable);
            plugin.getLogger().info("数据库表检查完成");
        }
    }

    private void loadCaves(Connection conn) throws SQLException {
        String sql = "SELECT id, owner_uuid, owner_name, level, world_name, center_x, center_z FROM cavefu_caves";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
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

    private void loadMembers(Connection conn) throws SQLException {
        String sql = "SELECT cave_id, member_uuid, member_name, permission_level FROM cavefu_members";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
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

    /**
     * 异步保存所有数据
     */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(() -> {
            saveSync();
        });
    }

    /**
     * 同步保存所有数据
     */
    public void saveSync() {
        if (!CoreDatabase.isEnabled()) {
            return;
        }

        try (Connection conn = CoreDatabase.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                for (Cave cave : cavesById.values()) {
                    saveCave(conn, cave);
                    saveMembers(conn, cave);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存数据到数据库失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    private void saveCave(Connection conn, Cave cave) throws SQLException {
        String sql = "INSERT INTO cavefu_caves (id, owner_uuid, owner_name, level, world_name, center_x, center_z) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE owner_name = VALUES(owner_name), level = VALUES(level)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    private void saveMembers(Connection conn, Cave cave) throws SQLException {
        String deleteSql = "DELETE FROM cavefu_members WHERE cave_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, cave.getId());
            ps.executeUpdate();
        }

        String insertSql = "INSERT INTO cavefu_members (cave_id, member_uuid, member_name, permission_level) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
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

    /**
     * 创建新洞府（同步方法，已废弃）
     * @deprecated 使用 createCaveAsync 替代
     */
    @Deprecated
    public Cave createCave(UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        try {
            return createCaveAsync(ownerUuid, ownerName, level, worldName, centerX, centerZ).get();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "创建洞府失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建新洞府（异步方法）
     * 
     * 工业级优化: 数据库操作异步化，避免阻塞主线程
     * 
     * @return CompletableFuture 包含创建的洞府
     */
    public CompletableFuture<Cave> createCaveAsync(UUID ownerUuid, String ownerName, int level, 
                                                    String worldName, int centerX, int centerZ) {
        return CompletableFuture.supplyAsync(() -> {
            if (!CoreDatabase.isEnabled()) {
                plugin.getLogger().warning("数据库未启用，无法创建洞府");
                return null;
            }

            try (Connection conn = CoreDatabase.getConnection()) {
                String sql = "INSERT INTO cavefu_caves (owner_uuid, owner_name, level, world_name, center_x, center_z) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ownerUuid.toString());
                    ps.setString(2, ownerName);
                    ps.setInt(3, level);
                    ps.setString(4, worldName);
                    ps.setInt(5, centerX);
                    ps.setInt(6, centerZ);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);
                            
                            // 同步更新缓存（在主线程）
                            cavesById.put(id, cave);
                            cavesByOwner.put(ownerUuid, cave);
                            cavesByMember.put(ownerUuid, cave);
                            
                            return cave;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建洞府数据库操作失败: " + e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 删除洞府（异步方法）
     * 
     * 工业级优化: 数据库操作异步化
     */
    public CompletableFuture<Void> deleteCaveAsync(int id) {
        return CompletableFuture.runAsync(() -> {
            if (!CoreDatabase.isEnabled()) {
                return;
            }

            Cave cave = cavesById.get(id);
            if (cave == null) return;

            try (Connection conn = CoreDatabase.getConnection()) {
                String sql = "DELETE FROM cavefu_caves WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 更新缓存
                cavesById.remove(id);
                cavesByOwner.remove(cave.getOwnerUuid());
                for (UUID memberUuid : cave.getMembers().keySet()) {
                    cavesByMember.remove(memberUuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除洞府数据库操作失败: " + e.getMessage(), e);
            }
        });
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
