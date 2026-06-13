package cn.guangdian.holo.listener;

import cn.guangdian.holo.GuangDianHolo;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class HologramListener implements Listener {

    private final GuangDianHolo plugin;

    public HologramListener(GuangDianHolo plugin) {
        this.plugin = plugin;
    }

    /**
     * 当世界加载时，加载该世界的全息图
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (world == null) return;
        
        // 延迟一点加载，确保世界完全初始化
        plugin.getScheduler().runSyncLater(() -> {
            plugin.getHologramManager().loadHologramsForWorld(world);
        }, 20L); // 延迟1秒
    }

    /**
     * 当世界卸载时，清理该世界的全息图
     */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        if (world == null) return;
        
        plugin.getHologramManager().unloadHologramsForWorld(world);
    }
}
