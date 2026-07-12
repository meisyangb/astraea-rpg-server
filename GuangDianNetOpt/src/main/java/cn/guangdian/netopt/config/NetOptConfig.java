package cn.guangdian.netopt.config;

import cn.guangdian.netopt.GuangDianNetOpt;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 职责：实体优化 + 数据包缓存
 * 区块优化交给GuangDianDynamicView
 */
public class NetOptConfig {

    private final GuangDianNetOpt plugin;

    // 实体优化（本插件核心职责）
    private boolean entityOptEnabled;
    private boolean disableMobAI;
    private boolean disableMobMovement;
    private boolean disableMobLook;

    // 数据包缓存（减少网络发送）
    private boolean packetCacheEnabled;
    private int cacheExpireSeconds;

    public NetOptConfig(GuangDianNetOpt plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        entityOptEnabled = config.getBoolean("entity.optimization-enabled", true);
        disableMobAI = config.getBoolean("entity.disable-mob-ai", true);
        disableMobMovement = config.getBoolean("entity.disable-mob-movement", true);
        disableMobLook = config.getBoolean("entity.disable-mob-look", true);

        packetCacheEnabled = config.getBoolean("packet.cache-enabled", true);
        cacheExpireSeconds = config.getInt("packet.cache-expire-seconds", 300);
    }

    public void reload() {
        load();
    }

    // Getters
    public boolean isEntityOptEnabled() { return entityOptEnabled; }
    public boolean isDisableMobAI() { return disableMobAI; }
    public boolean isDisableMobMovement() { return disableMobMovement; }
    public boolean isDisableMobLook() { return disableMobLook; }

    public boolean isPacketCacheEnabled() { return packetCacheEnabled; }
    public int getCacheExpireSeconds() { return cacheExpireSeconds; }
}