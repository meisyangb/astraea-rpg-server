package cn.guangdian.chunktrim.config;

import cn.guangdian.chunktrim.GuangDianChunkTrim;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 裁剪配置
 */
public class TrimConfig {

    private final GuangDianChunkTrim plugin;
    private int defaultRadius;
    private int chunksPerSecond;
    private int bufferSize;
    private boolean confirmRequired;

    public TrimConfig(GuangDianChunkTrim plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        
        defaultRadius = config.getInt("defaults.radius", 100);
        chunksPerSecond = config.getInt("performance.chunks-per-second", 20);
        bufferSize = config.getInt("performance.buffer-size", 16);
        confirmRequired = config.getBoolean("safety.confirm-required", true);
    }

    // Getters
    public int getDefaultRadius() { return defaultRadius; }
    public int getChunksPerSecond() { return chunksPerSecond; }
    public int getBufferSize() { return bufferSize; }
    public boolean isConfirmRequired() { return confirmRequired; }
}