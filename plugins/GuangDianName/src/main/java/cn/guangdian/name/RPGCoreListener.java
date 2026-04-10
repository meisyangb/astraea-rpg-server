package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.event.EventHandler;
import cn.guangdian.rpgcore.event.events.PlayerHealthChangedEvent;
import cn.guangdian.rpgcore.event.events.PlayerFullHealthEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * RPGCore 事件监听器
 * 
 * 订阅 RPGCore 血量事件，实时更新显示
 */
public class RPGCoreListener {
    
    private final JavaPlugin plugin;
    private final HealthDisplay healthDisplay;
    
    private EventHandler<PlayerHealthChangedEvent> healthHandler;
    private EventHandler<PlayerFullHealthEvent> fullHealthHandler;
    
    public RPGCoreListener(JavaPlugin plugin, HealthDisplay healthDisplay) {
        this.plugin = plugin;
        this.healthDisplay = healthDisplay;
    }
    
    /**
     * 订阅 RPGCore 事件
     */
    public void subscribe() {
        if (RPGCore.getInstance() == null || RPGCore.getInstance().getEventBus() == null) {
            plugin.getLogger().warning("RPGCore 不可用，血量显示将不会实时更新");
            return;
        }
        
        healthHandler = new EventHandler<PlayerHealthChangedEvent>() {
            @Override
            public void handle(PlayerHealthChangedEvent event) {
                Player player = Bukkit.getPlayer(event.getPlayerId());
                if (player != null && player.isOnline()) {
                    healthDisplay.updateHealth(player, event.getNewHealth());
                }
            }
        };
        
        fullHealthHandler = new EventHandler<PlayerFullHealthEvent>() {
            @Override
            public void handle(PlayerFullHealthEvent event) {
                Player player = Bukkit.getPlayer(event.getPlayerId());
                if (player != null && player.isOnline()) {
                    healthDisplay.updateHealth(player, event.getMaxHealth());
                }
            }
        };
        
        RPGCore.getInstance().getEventBus().subscribe(PlayerHealthChangedEvent.class, healthHandler);
        RPGCore.getInstance().getEventBus().subscribe(PlayerFullHealthEvent.class, fullHealthHandler);
        
        plugin.getLogger().info("已订阅 RPGCore 血量事件");
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe() {
        if (RPGCore.getInstance() != null && RPGCore.getInstance().getEventBus() != null) {
            if (healthHandler != null) {
                RPGCore.getInstance().getEventBus().unsubscribe(healthHandler);
            }
            if (fullHealthHandler != null) {
                RPGCore.getInstance().getEventBus().unsubscribe(fullHealthHandler);
            }
        }
    }
}
