package cn.guangdian.tab.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.TabService;
import cn.guangdian.tab.GuangDianTab;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tab 服务适配器
 *
 * <p>连接 GuangDianTab 与 RPGCore 服务系统。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class TabServiceAdapter implements TabService {

    private final GuangDianTab plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private Logger logger;

    private boolean autoUpdateOnTitleChange = true;
    private boolean autoUpdateOnGuildChange = true;
    private final ConcurrentHashMap<UUID, String> customNames = new ConcurrentHashMap<>();

    public TabServiceAdapter(GuangDianTab plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();

                // 注册服务
                registry.registerService(TabService.class, this);
                logger.info("已注册到 RPGCore: TabService");

                // 订阅称号变化事件
                subscribeToEvents();

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    private void subscribeToEvents() {
        if (eventBus != null) {
            eventBus.subscribe(PlayerStatsChangedEvent.class, event -> {
                UUID playerId = event.getPlayerId();
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    refreshTabName(player);
                }
            });
            logger.info("已订阅 PlayerStatsChangedEvent (事件驱动模式)");
        }
    }

    // ==================== TabService 实现 ====================

    @Override
    public String getTabName(Player player) {
        String custom = customNames.get(player.getUniqueId());
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        return plugin.getPrefixForPlayer(player) + player.getName() + plugin.getSuffixForPlayer(player);
    }

    @Override
    public String getPrefix(Player player) {
        return plugin.getPrefixForPlayer(player);
    }

    @Override
    public String getSuffix(Player player) {
        return plugin.getSuffixForPlayer(player);
    }

    @Override
    public void refreshTabName(Player player) {
        // 强制更新该玩家的Tab显示
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.updatePlayerTab(player);
            plugin.updateHeaderFooter(player);
        }, 1L);
    }

    @Override
    public void refreshAllTabNames() {
        // 强制更新所有玩家的Tab显示
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.refreshAll();
            plugin.updateAllHeadersAndFooters();
        }, 1L);
    }

    @Override
    public void setCustomName(UUID playerId, String customName) {
        if (customName == null || customName.isEmpty()) {
            customNames.remove(playerId);
        } else {
            customNames.put(playerId, customName);
        }

        // 立即更新显示
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            refreshTabName(player);
        }
    }

    @Override
    public String getCustomName(UUID playerId) {
        return customNames.get(playerId);
    }

    @Override
    public void setAutoUpdateOnTitleChange(boolean enabled) {
        this.autoUpdateOnTitleChange = enabled;
        if (enabled && eventBus != null) {
            subscribeToEvents();
        }
    }

    @Override
    public void setAutoUpdateOnGuildChange(boolean enabled) {
        this.autoUpdateOnGuildChange = enabled;
        // 公会变化监听可以在这里添加
    }

    @Override
    public void clearCache(UUID playerId) {
        customNames.remove(playerId);
        // 触发重新计算
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            refreshTabName(player);
        }
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
                registry.unregisterService(TabService.class);
                logger.info("已从 RPGCore 注销: TabService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
        customNames.clear();
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    /**
     * 检查是否启用称号自动更新
     */
    public boolean isAutoUpdateOnTitleChange() {
        return autoUpdateOnTitleChange;
    }

    /**
     * 检查是否启用公会自动更新
     */
    public boolean isAutoUpdateOnGuildChange() {
        return autoUpdateOnGuildChange;
    }
}