package cn.guangdian.netopt.manager;

import cn.guangdian.netopt.GuangDianNetOpt;
import cn.guangdian.netopt.config.NetOptConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches chunk packet data for static worlds
 * Instead of rebuilding chunk packets every time, cache them
 */
public class PacketCacheManager {

    private final GuangDianNetOpt plugin;
    private final NetOptConfig config;
    private BukkitTask cacheTask;
    private volatile boolean running;
    
    // Cache statistics
    private long cacheHits;
    private long cacheMisses;
    private long packetsSaved;
    
    // Track player chunk viewing
    private final Map<Player, Set<String>> playerViewedChunks;

    public PacketCacheManager(GuangDianNetOpt plugin, NetOptConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.running = false;
        this.cacheHits = 0;
        this.cacheMisses = 0;
        this.packetsSaved = 0;
        this.playerViewedChunks = new ConcurrentHashMap<>();
    }

    public void start() {
        if (!config.isPacketCacheEnabled()) {
            plugin.getLogger().info("Packet caching is disabled");
            return;
        }

        running = true;
        
        // Clean up cache periodically
        cacheTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!running) return;
            
            // Log cache statistics
            long total = cacheHits + cacheMisses;
            if (total > 0) {
                double hitRate = (double) cacheHits / total * 100;
                plugin.getLogger().info("Packet cache stats: " + 
                    String.format("%.1f%% hit rate (%d/%d), %d packets saved", 
                        hitRate, cacheHits, total, packetsSaved));
            }
        }, 6000L, 6000L); // Every 5 minutes
        
        plugin.getLogger().info("Packet cache started (expire: " + config.getCacheExpireSeconds() + "s)");
    }

    public void stop() {
        running = false;
        if (cacheTask != null) {
            cacheTask.cancel();
        }
        playerViewedChunks.clear();
    }

    /**
     * Record that a player has viewed a chunk
     * Used to optimize re-sending of chunks
     */
    public void recordChunkView(Player player, String chunkKey) {
        Set<String> viewed = playerViewedChunks.computeIfAbsent(player, p -> ConcurrentHashMap.newKeySet());
        viewed.add(chunkKey);
    }

    /**
     * Check if player has already viewed this chunk
     * If so, we can use cached data instead of rebuilding
     */
    public boolean hasViewedChunk(Player player, String chunkKey) {
        Set<String> viewed = playerViewedChunks.get(player);
        if (viewed != null && viewed.contains(chunkKey)) {
            cacheHits++;
            packetsSaved++;
            return true;
        }
        cacheMisses++;
        return false;
    }

    /**
     * Clear cached chunks for a player (e.g., on teleport)
     */
    public void clearPlayerCache(Player player) {
        playerViewedChunks.remove(player);
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public long getPacketsSaved() {
        return packetsSaved;
    }
}