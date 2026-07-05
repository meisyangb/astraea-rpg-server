package cn.guangdian.cavefu.storage;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import org.bukkit.Location;

import java.util.Collection;
import java.util.UUID;

/**
 * 数据存储管理器 - SQLite 数据库存储
 * <p>委托给 CaveDatabaseStorage，提供统一 API</p>
 * <p>参考 GuangDianPoints 的架构：主类 → DatabaseStorage → SQLite</p>
 */
public class DataManager {

    private final CaveDatabaseStorage db;

    public DataManager(GuangDianCaveFu plugin) {
        this.db = new CaveDatabaseStorage(plugin);
    }

    public boolean initialize() { return db.initialize(); }
    public boolean isEnabled() { return db.isEnabled(); }
    public void load() { db.load(); }
    public void save() { /* 保留兼容，由 saveCave 替代 */ }

    /** 立即保存单个洞府（操作后调用） */
    public void saveCave(Cave cave) {
        if (cave != null) db.saveCaveSync(cave);
    }

    /** 异步全量保存（自动保存 + 退出保存） */
    public void saveAsync() { db.saveAsync(); }

    /** 同步全量保存（关闭时） */
    public void saveSync() { db.saveSync(); }

    public void shutdown() {
        db.saveSync();
        db.close();
    }

    // ==================== CRUD ====================

    public Cave createCave(UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        Cave cave = db.createCave(ownerUuid, ownerName, level, worldName, centerX, centerZ);
        db.saveCaveSync(cave); // 立即持久化
        return cave;
    }

    public void deleteCave(int id) {
        db.removeCave(id);
        db.deleteCaveSync(id);
    }

    // ==================== 查询 ====================

    public Cave getCaveById(int id) { return db.getCaveById(id); }
    public Cave getCaveByOwner(UUID uuid) { return db.getCaveByOwner(uuid); }
    public Cave getCaveByMember(UUID uuid) { return db.getCaveByMember(uuid); }
    public Cave getCaveAtLocation(Location loc) { return db.getCaveAtLocation(loc); }
    public Collection<Cave> getAllCaves() { return db.getAllCaves(); }
    public int getCaveCount() { return db.getCaveCount(); }
    public int getNextCaveId() { return db.getNextCaveId(); }
    public void updateMemberIndex(UUID uuid, Cave cave) { db.updateMemberIndex(uuid, cave); }

    // 兼容旧代码
    public java.util.Map<UUID, Cave> getCavesByOwner() {
        java.util.Map<UUID, Cave> map = new java.util.HashMap<>();
        for (Cave c : db.getAllCaves()) map.put(c.getOwnerUuid(), c);
        return map;
    }
}
