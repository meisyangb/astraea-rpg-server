package cn.guangdian.netopt.listener;

import cn.guangdian.netopt.GuangDianNetOpt;
import cn.guangdian.netopt.config.NetOptConfig;
import cn.guangdian.netopt.manager.EntityOptimizer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * 网络监听器
 * 
 * 职责：监听实体生成事件，立即应用优化
 */
public class NetworkListener implements Listener {

    private final GuangDianNetOpt plugin;
    private final NetOptConfig config;
    private final EntityOptimizer entityOptimizer;

    public NetworkListener(GuangDianNetOpt plugin, EntityOptimizer entityOptimizer) {
        this.plugin = plugin;
        this.config = plugin.getNetOptConfig();
        this.entityOptimizer = entityOptimizer;
    }

    /**
     * 实体生成时立即优化
     * 这样新生成的静态实体也不会被AI计算浪费资源
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!config.isEntityOptEnabled()) return;
        
        Entity entity = event.getEntity();
        if (entity instanceof Mob mob) {
            // 立即优化新生成的实体
            entityOptimizer.forceOptimize(mob);
        }
    }
}