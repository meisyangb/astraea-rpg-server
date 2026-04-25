package cn.guangdian.name.adapter;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.armorstats.event.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.DisplayService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * DisplayService 服务适配器
 * 
 * <p>将 GuangDianName 的显示功能注册到 RPGCore ServiceRegistry。</p>
 * 
 * <h3>提供的功能：</h3>
 * <ul>
 *   <li>玩家前缀/后缀显示</li>
 *   <li>血量显示</li>
 *   <li>头顶标签显示</li>
 *   <li>显示缓存管理</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DisplayServiceAdapter implements DisplayService, Listener {

    private final GuangDianName plugin;
    private final ServiceRegistry serviceRegistry;
    
    public DisplayServiceAdapter(GuangDianName plugin, ServiceRegistry serviceRegistry) {
        this.plugin = plugin;
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * 注册服务到 ServiceRegistry
     */
    public void register() {
        serviceRegistry.registerService(DisplayService.class, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[DisplayServiceAdapter] 已注册 DisplayService 到 RPGCore");
    }
    
    /**
     * 订阅属性变化事件，实现事件驱动更新
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
        UUID playerId = event.getPlayerId();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            updatePlayerDisplay(player);
        }
    }
    
    /**
     * 从 ServiceRegistry 注销服务
     */
    public void unregister() {
        serviceRegistry.unregisterService(DisplayService.class);
        plugin.getLogger().info("[DisplayServiceAdapter] 已注销 DisplayService");
    }

    @Override
    public String getPrefix(Player player) {
        return "";
    }

    @Override
    public String getSuffix(Player player) {
        return "";
    }

    @Override
    public Object getDisplayData(UUID playerId) {
        return null;
    }

    @Override
    public void updatePlayerDisplay(Player player) {
        // 更新玩家头顶显示
        plugin.getNameDisplayManager().updateDisplays(player);
    }

    @Override
    public void refreshAllDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }

    @Override
    public void clearDisplayCache(UUID playerId) {
        // 清理缓存
    }

    @Override
    public void setDisplayEnabled(Player player, boolean enabled) {
        // 切换显示状态
    }

    @Override
    public boolean isDisplayEnabled(UUID playerId) {
        return true;
    }

    @Override
    public int getRPGHealth(Player player) {
        return (int) player.getHealth();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
