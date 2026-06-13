package cn.guangdian.worldrules.listener;

import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.*;

/**
 * 区域进入监听器
 * 当玩家进入或离开区域时显示 Title 信息
 */
public class RegionEnterListener implements Listener {

    private final GuangDianWorldRules plugin;
    private final Map<UUID, Set<String>> playerRegions = new HashMap<>();

    // Title 显示时间
    private final Title.Times times = Title.Times.times(
            Duration.ofMillis(500),   // 淡入
            Duration.ofMillis(3000),  // 停留
            Duration.ofMillis(500)    // 淡出
    );

    public RegionEnterListener(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 只处理实际移动（坐标变化）
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 获取玩家当前所在的区域
        List<ProtectedRegion> currentRegions = plugin.getRegionManager().getRegionsAt(event.getTo());
        Set<String> currentRegionNames = new HashSet<>();
        for (ProtectedRegion region : currentRegions) {
            currentRegionNames.add(region.getName().toLowerCase());
        }

        // 获取玩家之前所在的区域
        Set<String> previousRegionNames = playerRegions.getOrDefault(playerId, Collections.emptySet());

        // 检查进入的区域
        for (ProtectedRegion region : currentRegions) {
            if (!previousRegionNames.contains(region.getName().toLowerCase())) {
                // 玩家进入了新区域
                showEnterTitle(player, region);
            }
        }

        // 检查离开的区域
        for (String previousRegionName : previousRegionNames) {
            if (!currentRegionNames.contains(previousRegionName)) {
                // 玩家离开了区域
                ProtectedRegion region = plugin.getRegionManager().getRegion(previousRegionName);
                if (region != null) {
                    showLeaveTitle(player, region);
                }
            }
        }

        // 更新玩家所在的区域
        playerRegions.put(playerId, currentRegionNames);
    }

    /**
     * 显示进入区域 Title
     */
    private void showEnterTitle(Player player, ProtectedRegion region) {
        String titleText = region.getEnterTitle();
        String subtitleText = region.getEnterSubtitle();

        if (titleText == null && subtitleText == null) {
            return;
        }

        Component titleComponent = titleText != null ? Component.text(titleText) : Component.empty();
        Component subtitleComponent = subtitleText != null ? Component.text(subtitleText) : Component.empty();

        Title title = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(title);
    }

    /**
     * 显示离开区域 Title
     */
    private void showLeaveTitle(Player player, ProtectedRegion region) {
        String titleText = region.getLeaveTitle();
        String subtitleText = region.getLeaveSubtitle();

        if (titleText == null && subtitleText == null) {
            return;
        }

        Component titleComponent = titleText != null ? Component.text(titleText) : Component.empty();
        Component subtitleComponent = subtitleText != null ? Component.text(subtitleText) : Component.empty();

        Title title = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(title);
    }

    /**
     * 清除玩家的区域缓存
     */
    public void clearPlayerCache(Player player) {
        playerRegions.remove(player.getUniqueId());
    }
}
