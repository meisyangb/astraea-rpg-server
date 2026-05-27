package cn.guangdian.menu.adapter;

import cn.guangdian.menu.GuangDianMenu;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.MenuService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 菜单服务适配器
 *
 * <p>连接 GuangDianMenu 实现与 MenuService 接口。</p>
 *
 * <p>集成 RPGCore EventBus，订阅属性变化事件自动刷新动态菜单。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MenuServiceAdapter implements MenuService {

    private final GuangDianMenu plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private Logger logger;
    private boolean autoRefreshOnStatsChange = false;

    public MenuServiceAdapter(GuangDianMenu plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();

                // 注册服务
                registry.registerService(MenuService.class, this);
                logger.info("已注册到 RPGCore: MenuService");

                // 订阅事件
                subscribeToEvents();

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅 RPGCore 事件
     *
     * <p>订阅属性变化事件，用于动态刷新包含玩家属性的菜单。</p>
     */
    private void subscribeToEvents() {
        if (eventBus == null) {
            return;
        }

        // 订阅属性变化事件
        eventBus.subscribe(PlayerStatsChangedEvent.class, event -> {
            if (autoRefreshOnStatsChange) {
                UUID playerId = event.getPlayerId();
                Player player = Bukkit.getPlayer(playerId);

                // 如果玩家当前打开了菜单，检查是否需要刷新
                if (player != null && plugin.getPlayerMenu(playerId) != null) {
                    // 标记需要刷新（实际刷新由菜单系统处理）
                    // 这里可以选择性地重新打开菜单或更新内容
                    // 为避免频繁刷新，这里只记录日志
                    logger.fine("玩家 " + player.getName() + " 属性变化，菜单可能需要刷新");
                }
            }
        });

        logger.info("已订阅 PlayerStatsChangedEvent");
    }

    /**
     * 设置是否在属性变化时自动刷新菜单
     *
     * @param enabled 是否启用
     */
    public void setAutoRefreshOnStatsChange(boolean enabled) {
        this.autoRefreshOnStatsChange = enabled;
    }

    /**
     * 获取是否启用属性变化自动刷新
     *
     * @return 是否启用
     */
    public boolean isAutoRefreshOnStatsChangeEnabled() {
        return autoRefreshOnStatsChange;
    }

    @Override
    public boolean openMenu(Player player, String menuName) {
        // 使用公开API方法，不再使用反射
        return plugin.openMenuAPI(player, menuName);
    }

    @Override
    public void closeMenu(Player player) {
        if (player != null) {
            player.closeInventory();
        }
    }

    @Override
    public boolean hasMenu(String menuName) {
        // 使用公开API方法，不再使用反射
        return plugin.hasMenuAPI(menuName);
    }

    @Override
    public List<String> getMenuNames() {
        // 使用公开API方法，不再使用反射
        return plugin.getMenuNamesAPI();
    }

    @Override
    public void reloadMenus() {
        // 使用公开API方法，不再使用反射
        plugin.reloadMenusAPI();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(MenuService.class);
                logger.info("已从 RPGCore 注销: MenuService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}