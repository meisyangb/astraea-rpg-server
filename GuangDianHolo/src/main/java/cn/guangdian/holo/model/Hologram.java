package cn.guangdian.holo.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hologram {

    private final String name;
    private Location location;
    private final List<String> lines = new ArrayList<>();
    private final List<LineType> lineTypes = new ArrayList<>();
    private final List<ItemStack> iconItems = new ArrayList<>();
    private double lineHeight = 0.3;
    private int viewDistance = 20;
    private boolean persistent = true;
    private List<Integer> entityIds = new ArrayList<>();
    private List<Integer> iconEntityIds = new ArrayList<>();
    private boolean visible = true;
    private final Map<String, Object> metadata = new HashMap<>();

    public enum LineType {
        TEXT,       // 普通文本
        ICON,       // 物品图标
        DYNAMIC     // 动态内容(占位符)
    }

    public Hologram(String name, Location location) {
        this.name = name;
        this.location = location != null ? location.clone() : null;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location != null ? location.clone() : null;
    }

    public World getWorld() {
        return location != null ? location.getWorld() : null;
    }

    public String getWorldName() {
        return location != null && location.getWorld() != null 
            ? location.getWorld().getName() : "";
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lineTypes.clear();
        this.iconItems.clear();
        if (lines != null) {
            for (String line : lines) {
                addLine(line);
            }
        }
    }

    public void addLine(String line) {
        lines.add(line);
        lineTypes.add(detectLineType(line));
        iconItems.add(parseIconItem(line));
    }

    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            lineTypes.remove(index);
            iconItems.remove(index);
        }
    }

    public void setLine(int index, String line) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, line);
            lineTypes.set(index, detectLineType(line));
            iconItems.set(index, parseIconItem(line));
        }
    }

    public void clearLines() {
        lines.clear();
        lineTypes.clear();
        iconItems.clear();
    }

    public int getLineCount() {
        return lines.size();
    }

    public LineType getLineType(int index) {
        if (index >= 0 && index < lineTypes.size()) {
            return lineTypes.get(index);
        }
        return LineType.TEXT;
    }

    public List<LineType> getLineTypes() {
        return lineTypes;
    }

    public ItemStack getIconItem(int index) {
        if (index >= 0 && index < iconItems.size()) {
            return iconItems.get(index);
        }
        return null;
    }

    public boolean hasDynamicContent() {
        for (LineType type : lineTypes) {
            if (type == LineType.DYNAMIC) {
                return true;
            }
        }
        return false;
    }

    public boolean isIconLine(int index) {
        return getLineType(index) == LineType.ICON;
    }

    public boolean isDynamicLine(int index) {
        return getLineType(index) == LineType.DYNAMIC;
    }

    private LineType detectLineType(String line) {
        if (line == null || line.isEmpty()) {
            return LineType.TEXT;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("ICON:")) {
            return LineType.ICON;
        }
        if (trimmed.contains("{") && trimmed.contains("}")) {
            return LineType.DYNAMIC;
        }
        return LineType.TEXT;
    }

    private ItemStack parseIconItem(String line) {
        if (line == null || !line.trim().startsWith("ICON:")) {
            return null;
        }
        String itemName = line.substring(line.indexOf(":") + 1).trim().toUpperCase();
        itemName = itemName.replace(" ", "_").replace("-", "_");
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(itemName);
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getDisplayText(int index) {
        if (index < 0 || index >= lines.size()) {
            return "";
        }
        String line = lines.get(index);
        LineType type = getLineType(index);
        
        if (type == LineType.ICON) {
            return null; // ICON行不显示文本
        }
        
        return line;
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(double lineHeight) {
        this.lineHeight = lineHeight;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(List<Integer> entityIds) {
        this.entityIds = entityIds != null ? entityIds : new ArrayList<>();
    }

    public List<Integer> getIconEntityIds() {
        return iconEntityIds;
    }

    public void setIconEntityIds(List<Integer> iconEntityIds) {
        this.iconEntityIds = iconEntityIds != null ? iconEntityIds : new ArrayList<>();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Location getLineLocation(int lineIndex) {
        if (location == null) return null;
        return location.clone().add(0, -(lineIndex * lineHeight), 0);
    }

    public Location getIconLocation(int lineIndex) {
        if (location == null) return null;
        // ICON显示在文本行的左侧
        return location.clone().add(-0.5, -(lineIndex * lineHeight), 0);
    }
}
