package cn.guangdian.netopt;

import cn.guangdian.netopt.command.NetOptCommand;
import cn.guangdian.netopt.config.NetOptConfig;
import cn.guangdian.netopt.listener.NetworkListener;
import cn.guangdian.netopt.manager.EntityOptimizer;
import cn.guangdian.netopt.manager.PacketCacheManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GuangDianNetOpt - 静态世界网络优化器
 * 
 * 职责分工（与GuangDianDynamicView协作）：
 * - 本插件：实体AI禁用 + 数据包缓存
 * - DynamicView：动态视距调整 + 区块加载优化
 * 
 * 适用场景：静态世界的RPG服务器
 */
public class GuangDianNetOpt extends JavaPlugin {

    private static GuangDianNetOpt instance;
    private NetOptConfig config;
    private EntityOptimizer entityOptimizer;
    private PacketCacheManager packetCache;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        config = new NetOptConfig(this);
        
        // 实体AI优化 - 禁用静态NPC/动物的AI计算
        entityOptimizer = new EntityOptimizer(this, config);
        
        // 数据包缓存 - 缓存静态区块数据包，避免重复发送
        packetCache = new PacketCacheManager(this, config);
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new NetworkListener(this, entityOptimizer), this);
        
        // 注册命令
        if (getCommand("netopt") != null) {
            getCommand("netopt").setExecutor(new NetOptCommand(this));
        }
        
        // 启动优化
        entityOptimizer.disableEntityAI();
        packetCache.start();
        
        getLogger().info("=== GuangDianNetOpt 职责分工 ===");
        getLogger().info("本插件负责:");
        getLogger().info("  - 实体AI禁用（减少CPU计算）");
        getLogger().info("  - 数据包缓存（减少网络发送）");
        getLogger().info("与GuangDianDynamicView协作:");
        getLogger().info("  - DynamicView负责动态视距调整");
        getLogger().info("  - DynamicView负责区块加载优化");
    }

    @Override
    public void onDisable() {
        if (entityOptimizer != null) entityOptimizer.stop();
        if (packetCache != null) packetCache.stop();
        
        getLogger().info("GuangDianNetOpt disabled");
    }

    public static GuangDianNetOpt getInstance() {
        return instance;
    }

    public NetOptConfig getNetOptConfig() {
        return config;
    }

    public EntityOptimizer getEntityOptimizer() {
        return entityOptimizer;
    }

    public PacketCacheManager getPacketCache() {
        return packetCache;
    }
}