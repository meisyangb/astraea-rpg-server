package cn.guangdian.particleblocker;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子事件监听器
 *
 * 这个类主要用于处理玩家相关事件和提供辅助功能
 * 实际粒子拦截由 ParticlePacketListener 通过 ProtocolLib 处理
 */
public class ParticleListener implements Listener {

    private final GuangDianParticleBlocker plugin;
    private final ParticleConfig config;
    private final ParticlePacketListener packetListener;

    // 玩家个人屏蔽设置（缓存）
    private final Map<UUID, Boolean> playerBlocked = new ConcurrentHashMap<>();

    public ParticleListener(GuangDianParticleBlocker plugin, ParticleConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.packetListener = new ParticlePacketListener(plugin, config);
    }

    /**
     * 玩家退出时清空数据
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerBlocked.remove(playerId);
        // ProtocolLib 侧也需要清理
        packetListener.setPlayerBlocked(playerId, false);
    }

    /**
     * 获取 ProtocolLib 监听器
     */
    public ParticlePacketListener getPacketListener() {
        return packetListener;
    }

    /**
     * 设置玩家个人屏蔽状态
     */
    public void setPlayerBlocked(UUID playerId, boolean blocked) {
        if (blocked) {
            playerBlocked.put(playerId, true);
        } else {
            playerBlocked.remove(playerId);
        }
        // 同步到 ProtocolLib 监听器
        packetListener.setPlayerBlocked(playerId, blocked);
    }

    /**
     * 获取玩家个人屏蔽状态
     */
    public boolean isPlayerBlocked(UUID playerId) {
        return playerBlocked.getOrDefault(playerId, false);
    }

    /**
     * 获取统计信息（从 ProtocolLib 监听器）
     */
    public long getTotalBlocked() {
        return packetListener.getTotalBlocked();
    }

    public long getTotalAllowed() {
        return packetListener.getTotalAllowed();
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        packetListener.resetStats();
    }

    /**
     * 取消注册
     */
    public void unregister() {
        packetListener.unregister();
    }
}