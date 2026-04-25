package cn.guangdian.name.adapter;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.DisplayService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
public class DisplayServiceAdapter implements DisplayService {

    private final GuangDianName plugin;
    private final ServiceRegistry serviceRegistry;
    private final EventBus eventBus;
    
    public DisplayServiceAdapter(GuangDianName plugin, ServiceRegistry serviceRegistry, EventBus eventBus) {
        this.plugin = plugin;
        this.serviceRegistry = serviceRegistry;
        this.eventBus = eventBus;
    }
    
    /**
     * 注册服务到 ServiceRegistry
     */
    public void register() {
        serviceRegistry.registerService(DisplayService.class, this);
        subscribeToEvents();
        plugin.getLogger().info("[DisplayServiceAdapter] 已注册 DisplayService 到 RPGCore");
    }
    
    /**
     * 订阅 RPGCore 事件，实现事件驱动更新
     */
    private void subscribeToEvents() {
        if (eventBus == null) {
            return;
        }
        
        eventBus.subscribe(PlayerStatsChangedEvent.class, event -> {
            UUID playerId = event.getPlayerId();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                updatePlayerDisplay(player);
            }
        });
        
        plugin.getLogger().info("[DisplayServiceAdapter] 已订阅 PlayerStatsChangedEvent (事件驱动模式)");
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
        // 全TextDisplay版本，前缀通过TextDisplay显示
        return "";
    }

    @Override
    public String getSuffix(Player player) {
        // 全TextDisplay版本，后缀通过TextDisplay显示
        return "";
    }

    @Override
    public Object getDisplayData(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return null;
        }
        
        return new DisplayData(
            "",
            "",
            getRPGHealth(player),
            isDisplayEnabled(playerId)
        );
    }

    @Override
    public void updatePlayerDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // 更新所有TextDisplay显示
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
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.getNameDisplayManager().removeAllDisplays(player);
        }
    }

    @Override
    public void setDisplayEnabled(Player player, boolean enabled) {
        plugin.getNameDisplayManager().setDisplayEnabled(player, enabled);
    }

    @Override
    public boolean isDisplayEnabled(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return true;
        }
        return plugin.getNameDisplayManager().isDisplayEnabled(playerId);
    }

    @Override
    public int getRPGHealth(Player player) {
        if (player == null) {
            return 0;
        }
        // 返回原版血量
        return (int) Math.ceil(player.getHealth());
    }

    @Override
    public boolean isAvailable() {
        return plugin.isEnabled() && plugin.getNameDisplayManager() != null;
    }
    
    /**
     * 显示数据封装类
     */
    public static class DisplayData {
        public final String prefix;
        public final String suffix;
        public final int rpgHealth;
        public final boolean displayEnabled;
        
        public DisplayData(String prefix, String suffix, int rpgHealth, boolean displayEnabled) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.rpgHealth = rpgHealth;
            this.displayEnabled = displayEnabled;
        }
    }
}