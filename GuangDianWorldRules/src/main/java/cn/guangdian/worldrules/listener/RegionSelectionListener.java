package cn.guangdian.worldrules.listener;

import cn.guangdian.worldrules.GuangDianWorldRules;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 区域选择工具监听器
 * OP 玩家用木斧选择两个点来创建区域
 */
public class RegionSelectionListener implements Listener {

    private final GuangDianWorldRules plugin;
    private final Map<UUID, Location> firstPoints = new HashMap<>();
    private final Map<UUID, Location> secondPoints = new HashMap<>();

    public RegionSelectionListener(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 只允许 OP 使用选择工具
        if (!player.isOp()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return;
        }

        // 只处理点击方块
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        Location clickedLoc = event.getClickedBlock().getLocation();
        UUID playerId = player.getUniqueId();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // 左键设置第一个点
            firstPoints.put(playerId, clickedLoc);
            player.sendMessage(Component.text()
                    .color(NamedTextColor.GREEN)
                    .content("已设置第一个点: ")
                    .append(Component.text()
                            .color(NamedTextColor.YELLOW)
                            .content(String.format("(%d, %d, %d) @ %s",
                                    clickedLoc.getBlockX(),
                                    clickedLoc.getBlockY(),
                                    clickedLoc.getBlockZ(),
                                    clickedLoc.getWorld().getName()))));

            // 如果已经设置了第二个点，显示区域大小
            Location second = secondPoints.get(playerId);
            if (second != null && second.getWorld().equals(clickedLoc.getWorld())) {
                showRegionSize(player, clickedLoc, second);
            }
        } else {
            // 右键设置第二个点
            secondPoints.put(playerId, clickedLoc);
            player.sendMessage(Component.text()
                    .color(NamedTextColor.GREEN)
                    .content("已设置第二个点: ")
                    .append(Component.text()
                            .color(NamedTextColor.YELLOW)
                            .content(String.format("(%d, %d, %d) @ %s",
                                    clickedLoc.getBlockX(),
                                    clickedLoc.getBlockY(),
                                    clickedLoc.getBlockZ(),
                                    clickedLoc.getWorld().getName()))));

            // 如果已经设置了第一个点，显示区域大小
            Location first = firstPoints.get(playerId);
            if (first != null && first.getWorld().equals(clickedLoc.getWorld())) {
                showRegionSize(player, first, clickedLoc);
            }
        }
    }

    /**
     * 显示区域大小信息
     */
    private void showRegionSize(Player player, Location loc1, Location loc2) {
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        int volume = width * height * depth;

        player.sendMessage(Component.text()
                .color(NamedTextColor.AQUA)
                .content(String.format("区域大小: %d x %d x %d (共 %d 个方块)", width, height, depth, volume)));

        player.sendMessage(Component.text()
                .color(NamedTextColor.GRAY)
                .content("使用 /gwr create <区域名> 来创建区域"));
    }

    /**
     * 获取玩家的第一个选择点
     */
    public Location getFirstPoint(Player player) {
        return firstPoints.get(player.getUniqueId());
    }

    /**
     * 获取玩家的第二个选择点
     */
    public Location getSecondPoint(Player player) {
        return secondPoints.get(player.getUniqueId());
    }

    /**
     * 清除玩家的选择点
     */
    public void clearSelection(Player player) {
        firstPoints.remove(player.getUniqueId());
        secondPoints.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否已选择两个点
     */
    public boolean hasSelection(Player player) {
        Location first = firstPoints.get(player.getUniqueId());
        Location second = secondPoints.get(player.getUniqueId());
        return first != null && second != null && first.getWorld().equals(second.getWorld());
    }
}
