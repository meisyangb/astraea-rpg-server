package cn.guangdian.cleaner.listener;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.manager.CleanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 玩家事件监听器
 * 用于追踪玩家掉落物品，保护刚掉落的物品
 */
public class DropListener implements Listener {

    private final GuangDianCleaner plugin;
    private final CleanManager cleanManager;

    public DropListener(GuangDianCleaner plugin, CleanManager cleanManager) {
        this.plugin = plugin;
        this.cleanManager = cleanManager;
    }

    /**
     * 监听玩家丢弃物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // 记录掉落时间
        cleanManager.recordPlayerDrop(playerUuid);
    }

    /**
     * 玩家退出时清理记录
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家退出后，其掉落物保护记录会被自动清理
    }
}