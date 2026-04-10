package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 血量监控器 - 实时输出玩家血量状态到日志
 */
public class HealthMonitor implements Runnable {
    
    private final JavaPlugin plugin;
    private SyncScheduler scheduler;
    private long taskId = -1;
    private boolean enabled = false;
    
    public HealthMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
        hookScheduler();
    }
    
    private void hookScheduler() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            scheduler = rpgCore.getScheduler();
        }
    }
    
    public void start() {
        if (scheduler != null && taskId < 0) {
            taskId = scheduler.runSyncRepeating(this, 20L, 20L);
        }
    }
    
    public void stop() {
        if (scheduler != null && taskId >= 0) {
            scheduler.cancelTask(taskId);
            taskId = -1;
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            plugin.getLogger().info("[血量监控] 已启用 - 每秒输出所有玩家血量");
        } else {
            plugin.getLogger().info("[血量监控] 已禁用");
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void run() {
        if (!enabled) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[血量监控] 当前血量: ");
        
        boolean first = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!first) {
                sb.append(" | ");
            }
            first = false;
            
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            int displayHealth = (int) Math.ceil(currentHealth);
            
            sb.append(String.format("%s: %.0f/%.0f(显示:%d)", 
                player.getName(), currentHealth, maxHealth, displayHealth));
            
            if (currentHealth <= 0 && !player.isDead()) {
                plugin.getLogger().warning(String.format(
                    "[血量监控] %s 血量异常: %.1f (但未死亡!)",
                    player.getName(),
                    currentHealth
                ));
            }
        }
        
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            sb.append("(无在线玩家)");
        }
        
        plugin.getLogger().info(sb.toString());
    }
}
