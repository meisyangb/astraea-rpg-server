package cn.guangdian.socket.manager;

import cn.guangdian.socket.GuangDianSocket;
import org.bukkit.entity.Player;

/**
 * 宝石镶嵌服务
 * 供其他插件调用
 */
public class SocketService {

    private final GuangDianSocket plugin;

    public SocketService(GuangDianSocket plugin) {
        this.plugin = plugin;
    }

    /**
     * 刷新玩家属性（供其他插件调用）
     */
    public void refreshPlayerStats(Player player) {
        // 这里可以触发属性刷新事件或调用其他插件的服务
        // 暂时为空实现，由监听 SocketInlayEvent 的插件处理
    }
}
