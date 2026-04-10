package cn.guangdian.cavefu.cave;

import cn.guangdian.cavefu.permission.PermissionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 洞府成员数据
 */
public class CaveMember {
    private final UUID uuid;
    private final String name;
    private PermissionType permission;
    private final long joinTime;

    public CaveMember(UUID uuid, String name, PermissionType permission) {
        this.uuid = uuid;
        this.name = name;
        this.permission = permission;
        this.joinTime = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid.toString());
        map.put("name", name);
        map.put("permission", permission.name());
        map.put("joinTime", joinTime);
        return map;
    }

    public static CaveMember deserialize(Map<String, Object> data) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        String name = (String) data.get("name");
        PermissionType permission = PermissionType.fromString((String) data.get("permission"));
        CaveMember member = new CaveMember(uuid, name, permission);
        return member;
    }
}