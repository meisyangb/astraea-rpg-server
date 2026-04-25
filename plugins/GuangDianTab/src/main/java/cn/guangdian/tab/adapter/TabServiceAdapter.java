package cn.guangdian.tab.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.armorstats.event.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.TabService;
import cn.guangdian.tab.GuangDianTab;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
public class TabServiceAdapter implements TabService, Listener {

    private final GuangDianTab plugin;
    private final boolean useRPGCore;
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

                // 注册服务
                registry.registerService(TabService.class, this);
                logger.info("已注册到 RPGCore: TabService");

                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                logger.info("已订阅 Bukkit Event: PlayerStatsChangedEvent");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
        UUID playerId = event.getPlayerId();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            refreshTabName(player);
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
        String tabName = getTabName(player);
        player.playerListName(net.kyori.adventure.text.Component.text(tabName));
    }

    @Override
    public void refreshAllTabNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshTabName(player);
        }
    }

    @Override
    public void setCustomName(UUID playerId, String customName) {
        if (customName == null || customName.isEmpty()) {
            customNames.remove(playerId);
        } else {
            customNames.put(playerId, customName);
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
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
    }

    @Override
    public void setAutoUpdateOnGuildChange(boolean enabled) {
        this.autoUpdateOnGuildChange = enabled;
    }

    @Override
    public void clearCache(UUID playerId) {
        customNames.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
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
    }
}
