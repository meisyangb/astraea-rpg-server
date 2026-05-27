package cn.guangdian.name.adapter;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.name.HealthDisplay;
import cn.guangdian.name.TitleDisplay;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.DisplayService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

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
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay == null) {
            return "";
        }
        return titleDisplay.getPrefix(player);
    }

    @Override
    public String getSuffix(Player player) {
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay == null) {
            return "";
        }
        return titleDisplay.getSuffix(player);
    }

    @Override
    public Object getDisplayData(UUID playerId) {
        // 返回玩家的显示配置数据
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay == null) {
            return null;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return null;
        }
        
        return new DisplayData(
            getPrefix(player),
            getSuffix(player),
            getRPGHealth(player),
            isDisplayEnabled(playerId)
        );
    }

    @Override
    public void updatePlayerDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // 更新血量显示
        HealthDisplay healthDisplay = plugin.getHealthDisplay();
        if (healthDisplay != null) {
            healthDisplay.updateHealth(player);
        }
        
        // 更新称号显示
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay != null) {
            titleDisplay.updateDisplay(player);
        }
        
        // 更新TextDisplay（工会等）
        plugin.getTextDisplayManager().updatePlayerTextDisplay(player);
    }

    @Override
    public void refreshAllDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }

    @Override
    public void clearDisplayCache(UUID playerId) {
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                titleDisplay.cleanupPlayer(player);
            }
        }
        
        HealthDisplay healthDisplay = plugin.getHealthDisplay();
        if (healthDisplay != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                healthDisplay.cleanupPlayer(player);
            }
        }
    }

    @Override
    public void setDisplayEnabled(Player player, boolean enabled) {
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay != null) {
            titleDisplay.setDisplayEnabled(player, enabled);
        }
    }

    @Override
    public boolean isDisplayEnabled(UUID playerId) {
        TitleDisplay titleDisplay = plugin.getTitleDisplay();
        if (titleDisplay == null) {
            return true; // 默认启用
        }
        return titleDisplay.isDisplayEnabled(playerId);
    }

    @Override
    public int getRPGHealth(Player player) {
        HealthDisplay healthDisplay = plugin.getHealthDisplay();
        if (healthDisplay == null) {
            // 返回原版血量作为后备
            return (int) player.getHealth();
        }
        return healthDisplay.getRPGHealth(player);
    }

    @Override
    public boolean isAvailable() {
        return plugin.isEnabled() && 
               plugin.getTitleDisplay() != null && 
               plugin.getHealthDisplay() != null;
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
