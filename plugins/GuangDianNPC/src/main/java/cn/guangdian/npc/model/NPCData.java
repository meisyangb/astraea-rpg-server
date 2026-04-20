package cn.guangdian.npc.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCData {

    private final String id;
    private String displayName;
    private NPCType type;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String menuId;
    private String skinName;
    private List<String> commands;
    private Map<String, Object> metadata;
    private int entityId;
    private UUID entityUUID;
    private boolean enabled;
    private long createdTime;
    private long lastModified;

    public NPCData(String id) {
        this.id = id;
        this.displayName = "<yellow>" + id;
        this.type = NPCType.GENERAL;
        this.menuId = "main";
        this.commands = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.enabled = true;
        this.createdTime = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }

    public NPCData(String id, String displayName, Location location, String menuId) {
        this(id);
        this.displayName = displayName;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "world";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.menuId = menuId;
    }

    public NPCType getType() {
        return type;
    }

    public void setType(NPCType type) {
        this.type = type != null ? type : NPCType.GENERAL;
        this.lastModified = System.currentTimeMillis();
    }

    public String getFullDisplayName() {
        return type.getPrefix() + " " + displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.lastModified = System.currentTimeMillis();
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
        this.lastModified = System.currentTimeMillis();
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.lastModified = System.currentTimeMillis();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.lastModified = System.currentTimeMillis();
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
        this.lastModified = System.currentTimeMillis();
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.lastModified = System.currentTimeMillis();
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.lastModified = System.currentTimeMillis();
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
        this.lastModified = System.currentTimeMillis();
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
        this.lastModified = System.currentTimeMillis();
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        this.lastModified = System.currentTimeMillis();
    }

    public void addCommand(String command) {
        if (this.commands == null) {
            this.commands = new ArrayList<>();
        }
        this.commands.add(command);
        this.lastModified = System.currentTimeMillis();
    }

    public void removeCommand(int index) {
        if (this.commands != null && index >= 0 && index < this.commands.size()) {
            this.commands.remove(index);
            this.lastModified = System.currentTimeMillis();
        }
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
        this.lastModified = System.currentTimeMillis();
    }

    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public UUID getEntityUUID() {
        return entityUUID;
    }

    public void setEntityUUID(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lastModified = System.currentTimeMillis();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLocation(Location location) {
        if (location != null && location.getWorld() != null) {
            this.worldName = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
            this.lastModified = System.currentTimeMillis();
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", displayName);
        map.put("type", type.name());
        map.put("world", worldName);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        map.put("menuId", menuId);
        if (skinName != null) {
            map.put("skinName", skinName);
        }
        if (commands != null && !commands.isEmpty()) {
            map.put("commands", new ArrayList<>(commands));
        }
        if (!metadata.isEmpty()) {
            map.put("metadata", new HashMap<>(metadata));
        }
        map.put("enabled", enabled);
        map.put("createdTime", createdTime);
        map.put("lastModified", lastModified);
        return map;
    }

    public static NPCData deserialize(String id, ConfigurationSection section) {
        NPCData npc = new NPCData(id);
        npc.displayName = section.getString("displayName", "<yellow>" + id);
        npc.type = NPCType.fromString(section.getString("type", "GENERAL"));
        npc.worldName = section.getString("world", "world");
        npc.x = section.getDouble("x");
        npc.y = section.getDouble("y");
        npc.z = section.getDouble("z");
        npc.yaw = (float) section.getDouble("yaw");
        npc.pitch = (float) section.getDouble("pitch");
        npc.menuId = section.getString("menuId", "main");
        npc.skinName = section.getString("skinName");
        npc.commands = new ArrayList<>(section.getStringList("commands"));
        npc.enabled = section.getBoolean("enabled", true);
        npc.createdTime = section.getLong("createdTime", System.currentTimeMillis());
        npc.lastModified = section.getLong("lastModified", System.currentTimeMillis());

        ConfigurationSection metaSection = section.getConfigurationSection("metadata");
        if (metaSection != null) {
            for (String key : metaSection.getKeys(false)) {
                npc.metadata.put(key, metaSection.get(key));
            }
        }
        return npc;
    }
}
