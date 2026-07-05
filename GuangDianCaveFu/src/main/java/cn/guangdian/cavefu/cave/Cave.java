package cn.guangdian.cavefu.cave;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.permission.PermissionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 洞府数据模型
 */
public class Cave {
    private final int id;                     // 洞府ID（用于计算位置）
    private UUID ownerUuid;                   // 洞主UUID
    private String ownerName;                 // 洞主名称
    private int level;                        // 当前等级
    private String worldName;                 // 世界名称
    private int centerX;                      // 中心X坐标
    private int centerZ;                      // 中心Z坐标
    private double homeX;                     // 传送点X
    private double homeY;                     // 传送点Y
    private double homeZ;                     // 传送点Z
    private float homeYaw;                    // 传送点Yaw
    private float homePitch;                  // 传送点Pitch
    private final long createTime;            // 创建时间
    private final Map<UUID, CaveMember> members; // 成员列表
    private int cachedSize;                   // 缓存的大小（从配置获取）
    private boolean dirty = true;            // 脏标记，用于增量保存

    public Cave(int id, UUID ownerUuid, String ownerName, int level, String worldName, int centerX, int centerZ) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.level = level;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.createTime = System.currentTimeMillis();
        this.members = new ConcurrentHashMap<>();

        // 默认传送点在中心
        this.homeX = centerX + 0.5;
        this.homeY = 66;
        this.homeZ = centerZ + 0.5;
        this.homeYaw = 0;
        this.homePitch = 0;

        // 洞主自动加入成员列表
        members.put(ownerUuid, new CaveMember(ownerUuid, ownerName, PermissionType.OWNER));
    }

    public int getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        markDirty();  // 标记数据已修改
    }

    public String getWorldName() {
        return worldName;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public Location getHomeLocation() {
        return new Location(Bukkit.getWorld(worldName), homeX, homeY, homeZ, homeYaw, homePitch);
    }

    // SQLite 存储用的 raw getter
    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public double getHomeZ() { return homeZ; }
    public float getHomeYaw() { return homeYaw; }
    public float getHomePitch() { return homePitch; }

    public void setHomeLocationRaw(double x, double y, double z, float yaw, float pitch) {
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }

    public void setHomeLocation(Location loc) {
        this.homeX = loc.getX();
        this.homeY = loc.getY();
        this.homeZ = loc.getZ();
        this.homeYaw = loc.getYaw();
        this.homePitch = loc.getPitch();
        markDirty();  // 标记数据已修改
    }

    public long getCreateTime() {
        return createTime;
    }

    public Map<UUID, CaveMember> getMembers() {
        return members;
    }

    public void addMember(UUID uuid, String name, PermissionType permission) {
        members.put(uuid, new CaveMember(uuid, name, permission));
        markDirty();  // 标记数据已修改
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        markDirty();  // 标记数据已修改
    }

    public CaveMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public PermissionType getPermission(UUID uuid) {
        CaveMember member = members.get(uuid);
        return member != null ? member.getPermission() : PermissionType.VISITOR;
    }

    public void transferOwner(UUID newOwnerUuid, String newOwnerName) {
        // 原洞主降为成员
        CaveMember oldOwner = members.get(ownerUuid);
        if (oldOwner != null) {
            oldOwner.setPermission(PermissionType.MEMBER);
        }

        // 新洞主
        CaveMember newOwner = members.get(newOwnerUuid);
        if (newOwner != null) {
            newOwner.setPermission(PermissionType.OWNER);
        } else {
            members.put(newOwnerUuid, new CaveMember(newOwnerUuid, newOwnerName, PermissionType.OWNER));
        }

        this.ownerUuid = newOwnerUuid;
        this.ownerName = newOwnerName;
        markDirty();  // 标记数据已修改
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }

        int halfSize = getSize() / 2;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return x >= centerX - halfSize && x <= centerX + halfSize - 1
            && z >= centerZ - halfSize && z <= centerZ + halfSize - 1;
    }

    public int getSize() {
        // 从配置获取实际大小
        if (cachedSize > 0) {
            return cachedSize;
        }
        
        // 尝试从插件配置获取
        var plugin = GuangDianCaveFu.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            CaveLevel caveLevel = plugin.getConfigManager().getLevel(level);
            if (caveLevel != null) {
                cachedSize = caveLevel.getSize();
                return cachedSize;
            }
        }
        
        // 后备值：基础大小
        return 16;
    }
    
    // ==================== 脏标记相关方法 ====================
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markDirty() {
        this.dirty = true;
    }
    
    public void clearDirty() {
        this.dirty = false;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("owner-uuid", ownerUuid.toString());
        map.put("owner-name", ownerName);
        map.put("level", level);
        map.put("world", worldName);
        map.put("center-x", centerX);
        map.put("center-z", centerZ);
        map.put("home-x", homeX);
        map.put("home-y", homeY);
        map.put("home-z", homeZ);
        map.put("home-yaw", homeYaw);
        map.put("home-pitch", homePitch);
        map.put("create-time", createTime);

        Map<String, Map<String, Object>> membersData = new HashMap<>();
        for (Map.Entry<UUID, CaveMember> entry : members.entrySet()) {
            membersData.put(entry.getKey().toString(), entry.getValue().serialize());
        }
        map.put("members", membersData);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static Cave deserialize(Map<String, Object> data) {
        int id = ((Number) data.get("id")).intValue();
        UUID ownerUuid = UUID.fromString((String) data.get("owner-uuid"));
        String ownerName = (String) data.get("owner-name");
        int level = ((Number) data.getOrDefault("level", 1)).intValue();
        String worldName = (String) data.get("world");
        int centerX = ((Number) data.get("center-x")).intValue();
        int centerZ = ((Number) data.get("center-z")).intValue();

        Cave cave = new Cave(id, ownerUuid, ownerName, level, worldName, centerX, centerZ);

        // 加载传送点
        if (data.containsKey("home-x")) {
            cave.homeX = ((Number) data.get("home-x")).doubleValue();
            cave.homeY = ((Number) data.get("home-y")).doubleValue();
            cave.homeZ = ((Number) data.get("home-z")).doubleValue();
            cave.homeYaw = ((Number) data.getOrDefault("home-yaw", 0)).floatValue();
            cave.homePitch = ((Number) data.getOrDefault("home-pitch", 0)).floatValue();
        }

        // 加载成员
        Object membersObj = data.get("members");
        if (membersObj instanceof Map) {
            Map<?, ?> membersMap = (Map<?, ?>) membersObj;
            for (Map.Entry<?, ?> entry : membersMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Map) {
                    UUID memberUuid = UUID.fromString((String) entry.getKey());
                    Map<String, Object> memberData = (Map<String, Object>) entry.getValue();
                    CaveMember member = CaveMember.deserialize(memberData);
                    cave.members.put(memberUuid, member);
                }
            }
        }

        return cave;
    }
}