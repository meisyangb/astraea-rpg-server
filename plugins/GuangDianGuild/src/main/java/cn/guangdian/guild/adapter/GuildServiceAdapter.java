package cn.guangdian.guild.adapter;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.points.event.PointsTransactionEvent;
import cn.guangdian.guild.event.GuildEvent;
import cn.guangdian.rpgcore.service.api.GuildService;
import cn.guangdian.rpgcore.util.OfflinePlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 公会服务适配器
 * 
 * <p>连接 GuangDianGuild 与 RPGCore 服务层，
 * 支持服务注册、事件发布/订阅、异步执行等功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class GuildServiceAdapter implements GuildService, Listener {

    private final GuangDianGuild plugin;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;

    public GuildServiceAdapter(GuangDianGuild plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();
                
                registry.registerService(GuildService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: GuildService");
                
                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                plugin.getLogger().info("已订阅 Bukkit Event: PointsTransactionEvent");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅点券交易事件 - 用于公会资金
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPointsTransaction(PointsTransactionEvent event) {
        // 当玩家进行点券交易时，可能需要给公会贡献
        UUID playerId = event.getPlayerId();
        long amount = event.getAmount();
        String reason = event.getReason();
        
        // 检查是否是公会贡献
        if ("guild_contribution".equals(reason) || "guild_deposit".equals(reason)) {
            // 异步处理公会资金
            if (asyncExecutor != null) {
                asyncExecutor.execute(() -> {
                    plugin.getLogger().fine("公会贡献: " + playerId + " 贡献了 " + amount + " 点券");
                });
            }
        }
    }

    @Override
    @Nullable
    public cn.guangdian.rpgcore.service.api.data.Guild getGuild(String name) {
        // 返回 null 表示此模块不直接提供 RPGCore Guild 实现
        // 实际公会数据通过其他方法提供
        return null;
    }

    @Override
    @Nullable
    public cn.guangdian.rpgcore.service.api.data.Guild getPlayerGuild(UUID playerId) {
        // 返回 null 表示此模块不直接提供 RPGCore Guild 实现
        // 实际公会数据通过其他方法提供
        return null;
    }

    @Override
    public boolean isInGuild(UUID playerId) {
        return false;
    }

    @Override
    public boolean createGuild(String name, UUID leaderId) {
        return false;
    }

    @Override
    public boolean disbandGuild(String name) {
        return false;
    }

    @Override
    public boolean invitePlayer(String guildName, UUID inviterId, UUID targetId) {
        return false;
    }

    @Override
    public boolean joinGuild(String guildName, UUID playerId) {
        return false;
    }

    @Override
    public boolean leaveGuild(UUID playerId) {
        return false;
    }

    @Override
    public boolean kickMember(String guildName, UUID kickerId, UUID targetId) {
        return false;
    }

    @Override
    public int getMemberCount(String guildName) {
        return 0;
    }

    @Override
    public int getGuildCount() {
        return 0;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(GuildService.class);
                plugin.getLogger().info("已从 RPGCore 注销: GuildService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}
