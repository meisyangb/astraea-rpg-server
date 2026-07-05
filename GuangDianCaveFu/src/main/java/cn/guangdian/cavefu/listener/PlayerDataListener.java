package cn.guangdian.cavefu.listener;

import cn.guangdian.cavefu.GuangDianCaveFu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家数据生命周期监听器
 * 
 * <p>负责在玩家退出时触发洞府数据保存，确保数据不丢失。</p>
 * <p>参考 GuangDianPoints 和 GuangDianQuest 的玩家退出保存模式。</p>
 *
 * @author GuangDian
 * @since 1.2.0
 */
public class PlayerDataListener implements Listener {

    private final GuangDianCaveFu plugin;

    public PlayerDataListener(GuangDianCaveFu plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家退出时保存洞府数据
     * 使用 MONITOR 优先级确保在其他插件处理完后执行
     * ignoreCancelled = true 防止重复处理
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.saveOnPlayerQuit();
    }
}
