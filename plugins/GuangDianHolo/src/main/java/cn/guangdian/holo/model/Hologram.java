package cn.guangdian.holo.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Hologram {

    private final String name;
    private Location location;
    private final List<String> lines = new ArrayList<>();
    private double lineHeight = 0.3;
    private int viewDistance = 20;
    private boolean persistent = true;
    private List<Integer> entityIds = new ArrayList<>();
    private boolean visible = true;

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
        if (lines != null) {
            this.lines.addAll(lines);
        }
    }

    public void addLine(String line) {
        lines.add(line);
    }

    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
        }
    }

    public void setLine(int index, String line) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, line);
        }
    }

    public void clearLines() {
        lines.clear();
    }

    public int getLineCount() {
        return lines.size();
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Location getLineLocation(int lineIndex) {
        if (location == null) return null;
        return location.clone().add(0, -(lineIndex * lineHeight), 0);
    }
}
