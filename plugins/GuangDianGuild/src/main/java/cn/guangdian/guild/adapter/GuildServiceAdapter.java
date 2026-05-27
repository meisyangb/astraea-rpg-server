package cn.guangdian.guild.adapter;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PointsTransactionEvent;
import cn.guangdian.rpgcore.event.events.RpgGuildEvent;
import cn.guangdian.rpgcore.service.api.GuildService;
import cn.guangdian.rpgcore.util.OfflinePlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
public class GuildServiceAdapter implements GuildService {

    private final GuangDianGuild plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private AsyncExecutor asyncExecutor;

    public GuildServiceAdapter(GuangDianGuild plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                this.asyncExecutor = rpgCore.getAsyncExecutor();
                
                registry.registerService(GuildService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: GuildService");
                
                // 订阅事件
                subscribeEvents();
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅事件
     */
    private void subscribeEvents() {
        if (eventBus == null) return;
        
        // 订阅点券交易事件 - 用于公会资金
        eventBus.subscribe(PointsTransactionEvent.class, event -> {
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
        });
        
        plugin.getLogger().info("已订阅 EventBus: PointsTransactionEvent");
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
        // 使用离线玩家缓存
        String name = OfflinePlayerCache.getPlayerName(playerId);
        if (name == null) return false;
        return plugin.isInGuild(name);
    }

    @Override
    public boolean createGuild(String name, UUID leaderId) {
        var player = Bukkit.getPlayer(leaderId);
        if (player == null) return false;
        boolean result = plugin.createGuild(name, player);
        
        // 公会创建事件通过 Bukkit 事件系统发布（RpgGuildEvent 是 Bukkit Event）
        
        return result;
    }

    @Override
    public boolean disbandGuild(String name) {
        boolean result = plugin.disbandGuild(name);
        
        // 公会解散事件通过 Bukkit 事件系统发布
        
        return result;
    }

    @Override
    public boolean invitePlayer(String guildName, UUID inviterId, UUID targetId) {
        var inviter = Bukkit.getPlayer(inviterId);
        var target = Bukkit.getPlayer(targetId);
        if (inviter == null || target == null) return false;
        plugin.invitePlayer(inviter.getName(), target.getName());
        return true;
    }

    @Override
    public boolean joinGuild(String guildName, UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        boolean result = plugin.joinGuild(guildName, player);
        
        // 公会加入事件通过 Bukkit 事件系统发布
        
        return result;
    }

    @Override
    public boolean leaveGuild(UUID playerId) {
        // 使用离线玩家缓存
        String name = OfflinePlayerCache.getPlayerName(playerId);
        if (name == null) return false;
        
        boolean result = plugin.leaveGuild(name);
        
        // 公会离开事件通过 Bukkit 事件系统发布
        
        return result;
    }

    @Override
    public boolean kickMember(String guildName, UUID kickerId, UUID targetId) {
        // 使用离线玩家缓存
        String kickerName = OfflinePlayerCache.getPlayerName(kickerId);
        String targetName = OfflinePlayerCache.getPlayerName(targetId);
        if (kickerName == null || targetName == null) return false;
        
        boolean result = plugin.kickMember(kickerName, targetName);
        
        // 公会踢出事件通过 Bukkit 事件系统发布
        
        return result;
    }

    @Override
    public int getMemberCount(String guildName) {
        Object guild = plugin.getGuild(guildName);
        if (guild != null) {
            // 使用反射获取 members Map 的大小
            try {
                var field = guild.getClass().getField("members");
                @SuppressWarnings("unchecked")
                var members = (java.util.Map<String, ?>) field.get(guild);
                return members != null ? members.size() : 0;
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public int getGuildCount() {
        return plugin.getGuildCount();
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

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}