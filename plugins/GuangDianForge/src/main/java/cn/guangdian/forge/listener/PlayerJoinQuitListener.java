package cn.guangdian.forge.listener;

import cn.guangdian.forge.GuangDianForge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家加入/退出监听器
 */
public class PlayerJoinQuitListener implements Listener {
    private final GuangDianForge plugin;

    public PlayerJoinQuitListener(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 加载玩家数据
        plugin.getPlayerDataManager().get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 保存并卸载玩家数据
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}