package cn.guangdian.worldrules.listener;

import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.ProtectedRegion;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 区块加载监听器
 * 阻止裁剪区域外的区块加载
 * 支持从配置文件自动加载区块卸载设置
 */
public class ChunkLoadListener implements Listener {

    private final GuangDianWorldRules plugin;
    // 世界 -> 裁剪区域名称
    private final Map<String, String> trimRegions = new HashMap<>();
    // 是否启用裁剪限制
    private boolean enabled = false;
    // 卸载延迟(秒)
    private int unloadDelay = 5;
    // 是否在玩家离开世界时立即卸载
    private boolean unloadOnLeave = true;
    // 已卸载的区块缓存(避免重复卸载)
    private final Set<String> unloadedChunks = new HashSet<>();

    public ChunkLoadListener(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    /**
     * 从配置文件加载区块卸载设置
     */
    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection section = config.getConfigurationSection("chunk-unload");
        
        if (section == null) {
            plugin.getLogger().info("未找到区块卸载配置，使用默认设置");
            return;
        }

        enabled = section.getBoolean("enabled", false);
        unloadDelay = section.getInt("unload-delay", 5);
        unloadOnLeave = section.getBoolean("unload-on-leave", true);

        // 加载活跃区域配置
        ConfigurationSection activeRegions = section.getConfigurationSection("active-regions");
        if (activeRegions != null) {
            trimRegions.clear();
            for (String worldName : activeRegions.getKeys(false)) {
                String regionName = activeRegions.getString(worldName);
                if (regionName != null) {
                    trimRegions.put(worldName, regionName);
                    plugin.getLogger().info("配置世界 " + worldName + " 的活跃区域: " + regionName);
                }
            }
        }

        plugin.getLogger().info("区块卸载配置已加载: enabled=" + enabled + 
            ", unloadDelay=" + unloadDelay + "s, unloadOnLeave=" + unloadOnLeave);
    }

    /**
     * 设置世界的裁剪区域
     */
    public void setTrimRegion(String worldName, String regionName) {
        if (regionName == null) {
            trimRegions.remove(worldName);
        } else {
            trimRegions.put(worldName, regionName);
        }
    }

    /**
     * 获取世界的裁剪区域
     */
    public String getTrimRegion(String worldName) {
        return trimRegions.get(worldName);
    }

    /**
     * 移除世界的裁剪区域
     */
    public void removeTrimRegion(String worldName) {
        trimRegions.remove(worldName);
    }

    /**
     * 清除所有裁剪区域
     */
    public void clearTrimRegions() {
        trimRegions.clear();
    }

    /**
     * 获取所有裁剪区域
     */
    public Map<String, String> getAllTrimRegions() {
        return new HashMap<>(trimRegions);
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 生成区块的唯一标识
     */
    private String getChunkKey(String worldName, int chunkX, int chunkZ) {
        return worldName + ":" + chunkX + "," + chunkZ;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!enabled) {
            return;
        }

        World world = event.getWorld();
        String worldName = world.getName();
        String regionName = trimRegions.get(worldName);

        if (regionName == null) {
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("未找到区域: " + regionName + " (世界: " + worldName + ")");
            }
            return;
        }

        // 检查区块是否在区域内
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();

        // 计算区块的方块范围
        int minBlockX = chunkX << 4;
        int maxBlockX = minBlockX + 15;
        int minBlockZ = chunkZ << 4;
        int maxBlockZ = minBlockZ + 15;

        // 检查区块是否与区域有交集
        boolean intersects = !(maxBlockX < region.getMinX() || minBlockX > region.getMaxX() ||
                              maxBlockZ < region.getMinZ() || minBlockZ > region.getMaxZ());

        if (!intersects) {
            String chunkKey = getChunkKey(worldName, chunkX, chunkZ);
            
            // 检查是否已经卸载过
            if (unloadedChunks.contains(chunkKey)) {
                return;
            }

            // 区块在区域外，卸载它
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("卸载区域外区块: " + chunkX + "," + chunkZ + " @ " + worldName);
            }
            
            unloadedChunks.add(chunkKey);
            
            // 在主线程延迟卸载区块（区块操作必须在主线程）
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    world.unloadChunk(chunkX, chunkZ, false);
                }
            }, unloadDelay * 20L); // 转换为 tick (1秒 = 20 tick)
        }
    }

    /**
     * 玩家加入服务器时，检查是否需要清理非活跃区块
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }

        World world = event.getPlayer().getWorld();
        String worldName = world.getName();
        String regionName = trimRegions.get(worldName);

        if (regionName == null) {
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            return;
        }

        // 玩家进入副本世界，清理非活跃区块（在主线程执行）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            unloadInactiveChunks(world, region);
        }, 20 * 20L); // 20秒后执行 (20 tick/秒)
    }

    /**
     * 玩家切换世界时：
     * 1. 卸载原世界的所有区块
     * 2. 清理新世界的非活跃区块
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        World fromWorld = event.getFrom();
        World toWorld = event.getPlayer().getWorld();
        String fromWorldName = fromWorld.getName();
        String toWorldName = toWorld.getName();

        // 1. 卸载原世界的所有区块
        if (enabled && unloadOnLeave && trimRegions.containsKey(fromWorldName)) {
            if (fromWorld.getPlayers().isEmpty()) {
                unloadAllChunksInWorld(fromWorld);
            }
        }

        // 2. 清理新世界的非活跃区块（在主线程执行）
        if (enabled && trimRegions.containsKey(toWorldName)) {
            String regionName = trimRegions.get(toWorldName);
            ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
            if (region != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    unloadInactiveChunks(toWorld, region);
                }, 5 * 20L); // 5秒后执行
            }
        }
    }

    /**
     * 玩家退出时检查是否需要卸载区块
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled || !unloadOnLeave) {
            return;
        }

        World world = event.getPlayer().getWorld();
        String worldName = world.getName();
        
        // 检查是否是配置了区块卸载的世界
        if (!trimRegions.containsKey(worldName)) {
            return;
        }

        // 延迟检查，确保玩家已经完全离开（在主线程执行）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (world.getPlayers().isEmpty()) {
                unloadAllChunksInWorld(world);
            }
        }, 20L); // 1秒后执行
    }

    /**
     * 卸载世界的所有区块（玩家离开时调用）
     */
    private void unloadAllChunksInWorld(World world) {
        String worldName = world.getName();
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("玩家已离开，卸载世界 " + worldName + " 的所有区块...");
        }

        int unloadedCount = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            int cx = chunk.getX();
            int cz = chunk.getZ();
            
            if (world.unloadChunk(cx, cz, false)) {
                unloadedCount++;
            }
        }

        // 清除该世界的卸载缓存
        unloadedChunks.removeIf(key -> key.startsWith(worldName + ":"));

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("已卸载 " + unloadedCount + " 个区块 @ " + worldName);
        }
    }

    /**
     * 卸载世界的非活跃区块（区块加载时调用）
     */
    private void unloadInactiveChunks(World world, ProtectedRegion region) {
        String worldName = world.getName();

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("卸载世界 " + worldName + " 的非活跃区块...");
        }

        // 计算活跃区域的区块范围
        int minChunkX = region.getMinX() >> 4;
        int maxChunkX = region.getMaxX() >> 4;
        int minChunkZ = region.getMinZ() >> 4;
        int maxChunkZ = region.getMaxZ() >> 4;

        int unloadedCount = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            int cx = chunk.getX();
            int cz = chunk.getZ();
            
            // 检查区块是否在活跃区域内
            if (cx < minChunkX || cx > maxChunkX || cz < minChunkZ || cz > maxChunkZ) {
                if (world.unloadChunk(cx, cz, false)) {
                    unloadedCount++;
                }
            }
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("已卸载 " + unloadedCount + " 个非活跃区块 @ " + worldName);
        }
    }

    /**
     * 清除卸载缓存
     */
    public void clearCache() {
        unloadedChunks.clear();
    }
}
