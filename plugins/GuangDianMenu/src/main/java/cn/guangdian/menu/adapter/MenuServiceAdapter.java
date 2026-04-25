package cn.guangdian.menu.adapter;

import cn.guangdian.menu.GuangDianMenu;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.armorstats.event.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.MenuService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 菜单服务适配器
 *
 * <p>连接 GuangDianMenu 实现与 MenuService 接口。</p>
 *
 * <p>集成 Bukkit 事件系统，订阅属性变化事件自动刷新动态菜单。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class MenuServiceAdapter implements MenuService, Listener {

    private final GuangDianMenu plugin;
    private final boolean useRPGCore;
    private Logger logger;

    public MenuServiceAdapter(GuangDianMenu plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();

                // 注册服务
                registry.registerService(MenuService.class, this);
                logger.info("已注册到 RPGCore: MenuService");

                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                logger.info("已订阅 Bukkit Event: PlayerStatsChangedEvent");

            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    // ==================== MenuService 实现 ====================

    @Override
    public boolean openMenu(Player player, String menuName) {
        return plugin.openMenuAPI(player, menuName);
    }

    @Override
    public void closeMenu(Player player) {
        player.closeInventory();
    }

    @Override
    public boolean hasMenu(String menuName) {
        return plugin.hasMenuAPI(menuName);
    }

    @Override
    public List<String> getMenuNames() {
        return plugin.getMenuNamesAPI();
    }

    @Override
    public void reloadMenus() {
        plugin.reloadMenusAPI();
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
                registry.unregisterService(MenuService.class);
                logger.info("已从 RPGCore 注销: MenuService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }
}
