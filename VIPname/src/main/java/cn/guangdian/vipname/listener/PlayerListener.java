package cn.guangdian.vipname.listener;

import cn.guangdian.vipname.VIPname;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 玩家监听器
 */
public class PlayerListener implements Listener {

    private final VIPname plugin;

    public PlayerListener(VIPname plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 加载玩家数据
        plugin.getTitleManager().getPlayerData(playerId);
        
        // 更新显示名
        String displayName = plugin.getTitleManager().getPlayerDisplayName(player);
        player.displayName(VIPname.color(displayName));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 保存玩家数据
        plugin.getTitleManager().save();
    }
}