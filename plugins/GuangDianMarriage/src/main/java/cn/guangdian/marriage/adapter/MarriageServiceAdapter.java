package cn.guangdian.marriage.adapter;

import cn.guangdian.marriage.GuangDianMarriage;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.MarriageService;
import cn.guangdian.rpgcore.util.OfflinePlayerCache;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * 结婚服务适配器
 * 
 * <p>连接 GuangDianMarriage 实现与 MarriageService 接口。</p>
 * 
 * <p>改进：移除反射访问，使用公开API方法。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class MarriageServiceAdapter implements MarriageService {

    private final GuangDianMarriage plugin;
    private final boolean useRPGCore;

    public MarriageServiceAdapter(GuangDianMarriage plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.registerService(MarriageService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: MarriageService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isMarried(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.isMarriedAPI(playerId);
    }

    @Override
    public String getPartner(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.getPartnerAPI(playerId);
    }

    @Override
    public Object getMarriage(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.getMarriageAPI(playerId);
    }

    @Override
    public boolean marry(UUID player1, UUID player2) {
        // 使用公开API方法，不再使用反射
        return plugin.marryAPI(player1, player2);
    }

    @Override
    public boolean divorce(UUID playerId) {
        // 使用公开API方法，不再使用反射
        return plugin.divorceAPI(playerId);
    }

    @Override
    public long getMarriageDays(UUID playerId) {
        // 使用公开API方法，不再使用反射访问 marryDate 字段
        return plugin.getMarriageDaysAPI(playerId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(MarriageService.class);
                plugin.getLogger().info("已从 RPGCore 注销: MarriageService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}