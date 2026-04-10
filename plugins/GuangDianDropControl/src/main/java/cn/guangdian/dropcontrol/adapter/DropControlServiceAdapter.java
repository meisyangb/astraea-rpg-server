package cn.guangdian.dropcontrol.adapter;

import cn.guangdian.dropcontrol.GuangDianDropControl;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.DropControlService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DropControl 服务适配器
 * 
 * <p>连接 GuangDianDropControl 与 RPGCore 服务系统。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DropControlServiceAdapter implements DropControlService {

    private final GuangDianDropControl plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;

    public DropControlServiceAdapter(GuangDianDropControl plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();

                registry.registerService(DropControlService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: DropControlService");

            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isGlobalDropEnabled() {
        return plugin.isDropEnabled();
    }

    @Override
    public void setGlobalDropEnabled(boolean enabled) {
        plugin.setDropEnabled(enabled);
    }

    @Override
    public boolean canPlayerDrop(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;

        // 检查绕过权限
        if (hasBypassPermission(playerId)) {
            return true;
        }

        // 检查使用权限
        if (!hasUsePermission(playerId)) {
            return false;
        }

        // 检查玩家状态
        Boolean status = getPlayerDropStatus(playerId);
        if (status != null) {
            return status;
        }

        // 使用默认配置
        return plugin.isPlayerDefaultEnabled() || isGlobalDropEnabled();
    }

    @Override
    public void setPlayerDropEnabled(UUID playerId, boolean enabled) {
        plugin.getPlayerDropStatus().put(playerId, enabled);
    }

    @Override
    public boolean togglePlayerDrop(UUID playerId) {
        Boolean current = getPlayerDropStatus(playerId);
        boolean newStatus = (current == null) ? !isGlobalDropEnabled() : !current;
        setPlayerDropEnabled(playerId, newStatus);
        return newStatus;
    }

    @Override
    public Boolean getPlayerDropStatus(UUID playerId) {
        return plugin.getPlayerDropStatus().get(playerId);
    }

    @Override
    public void clearPlayerStatus(UUID playerId) {
        plugin.getPlayerDropStatus().remove(playerId);
    }

    @Override
    public boolean hasBypassPermission(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.hasPermission("gddrop.bypass");
    }

    @Override
    public boolean hasUsePermission(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.hasPermission("gddrop.use");
    }

    @Override
    public int getEnabledPlayerCount() {
        int count = 0;
        for (Boolean status : plugin.getPlayerDropStatus().values()) {
            if (status != null && status) {
                count++;
            }
        }
        return count;
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
                registry.unregisterService(DropControlService.class);
                plugin.getLogger().info("已从 RPGCore 注销: DropControlService");
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