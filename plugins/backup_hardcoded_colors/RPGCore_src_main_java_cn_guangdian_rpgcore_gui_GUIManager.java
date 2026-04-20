package cn.guangdian.rpgcore.gui;

import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * GUI 管理器 - RPGCore GUI 框架入口
 *
 * <p>管理所有 GUI 的注册、事件监听和生命周期。</p>
 *
 * <h2>使用示例:</h2>
 * <pre>{@code
 * // 在插件启动时初始化
 * GUIManager guiManager = GUIManager.getInstance();
 * guiManager.initialize(rpgCore);
 *
 * // 创建并打开 GUI
 * GUI gui = GUIBuilder.create("&6我的菜单", 6)
 *     .setItem(0, item, click -> {
 *         // 点击处理
 *     })
 *     .setFiller(Material.GRAY_STAINED_GLASS_PANE)
 *     .build();
 *
 * gui.open(player);
 * }</pre>
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public final class GUIManager {

    private static GUIManager instance;

    private GUIListener listener;
    private boolean initialized = false;
    private Logger logger;

    private GUIManager() {}

    public static synchronized GUIManager getInstance() {
        if (instance == null) {
            instance = new GUIManager();
        }
        return instance;
    }

    /**
     * 初始化 GUI 管理器
     *
     * @param plugin RPGCore 插件实例
     */
    public void initialize(@NotNull RPGCore plugin) {
        if (initialized) {
            logger.warning("[GUIManager] 已经初始化过了!");
            return;
        }

        this.logger = plugin.getLogger();

        // 注册事件监听器
        listener = new GUIListener();
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(listener, plugin);

        initialized = true;
        logger.info("[GUIManager] 已初始化");
    }

    /**
     * 关闭 GUI 管理器
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        // 关闭所有玩家打开的 GUI
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            GUI gui = listener.getPlayerGUI(player);
            if (gui != null) {
                player.closeInventory();
            }
        }

        listener = null;
        initialized = false;
        logger.info("[GUIManager] 已关闭");
    }

    /**
     * 获取 GUI 监听器
     */
    public @NotNull Listener getListener() {
        if (!initialized) {
            throw new IllegalStateException("GUIManager 未初始化!");
        }
        return listener;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
