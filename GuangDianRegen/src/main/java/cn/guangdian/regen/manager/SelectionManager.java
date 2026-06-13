package cn.guangdian.regen.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 选区管理器
 */
public class SelectionManager {

    private final Map<UUID, Selection> selections = new HashMap<>();

    /**
     * 设置第一个点
     */
    public void setPos1(Player player, Location location) {
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        selection.pos1 = location.clone();
    }

    /**
     * 设置第二个点
     */
    public void setPos2(Player player, Location location) {
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        selection.pos2 = location.clone();
    }

    /**
     * 获取选区
     */
    public Selection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    /**
     * 检查选区是否有效
     */
    public boolean isValidSelection(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null && selection.isValid();
    }

    /**
     * 清除选区
     */
    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    /**
     * 选区数据
     */
    public static class Selection {
        private Location pos1;
        private Location pos2;

        public boolean isValid() {
            return pos1 != null && pos2 != null &&
                   pos1.getWorld() != null && pos2.getWorld() != null &&
                   pos1.getWorld().equals(pos2.getWorld());
        }

        public Location getPos1() {
            return pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public String getWorldName() {
            return pos1 != null && pos1.getWorld() != null ? pos1.getWorld().getName() : null;
        }

        public int getMinX() {
            return Math.min(pos1.getBlockX(), pos2.getBlockX());
        }

        public int getMinY() {
            return Math.min(pos1.getBlockY(), pos2.getBlockY());
        }

        public int getMinZ() {
            return Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        }

        public int getMaxX() {
            return Math.max(pos1.getBlockX(), pos2.getBlockX());
        }

        public int getMaxY() {
            return Math.max(pos1.getBlockY(), pos2.getBlockY());
        }

        public int getMaxZ() {
            return Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        }
    }
}
