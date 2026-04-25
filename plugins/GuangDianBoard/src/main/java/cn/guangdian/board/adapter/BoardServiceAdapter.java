package cn.guangdian.board.adapter;

import cn.guangdian.board.GuangDianBoard;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.armorstats.event.PlayerStatsChangedEvent;
import cn.guangdian.rpgcore.service.api.BoardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Board 服务适配器
 * 
 * <p>连接 GuangDianBoard 与 RPGCore 服务系统。</p>
 * <p>订阅属性变化事件自动更新记分板。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class BoardServiceAdapter implements BoardService, Listener {

    private final GuangDianBoard plugin;
    private final boolean useRPGCore;
    private Logger logger;
    private boolean autoUpdateOnStatsChange = true;

    public BoardServiceAdapter(GuangDianBoard plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        this.logger = plugin.getLogger();
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                
                // 注册服务
                registry.registerService(BoardService.class, this);
                logger.info("已注册到 RPGCore: BoardService");
                
                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(this, plugin);
                logger.info("已订阅 PlayerStatsChangedEvent (脏标记模式)");
                
            } catch (Exception e) {
                logger.warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 订阅属性变化事件 - 高性能优化版
     * 
     * 优化特性:
     * 1. 使用脏标记替代立即刷新
     * 2. 批量处理事件，减少刷新频率
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStatsChanged(PlayerStatsChangedEvent event) {
        if (autoUpdateOnStatsChange) {
            // 高性能优化: 只标记脏，不立即刷新
            // 定时任务会检查脏标记并刷新
            plugin.markDirty(event.getPlayerId());
        }
    }

    // ==================== BoardService 实现 ====================

    @Override
    public List<String> getBoardLines(Player player) {
        // 使用PlaceholderAPI解析后的行
        List<String> lines = new ArrayList<>();
        // 这里返回模板行，实际解析在插件内部完成
        return plugin.getConfig().getStringList("lines");
    }

    @Override
    public void refreshBoard(Player player) {
        plugin.requestSmartRefresh(player);
    }

    @Override
    public void refreshAllBoards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.shouldShowBoardPublic(player)) {
                plugin.requestSmartRefresh(player);
            }
        }
    }

    @Override
    public void enableBoard(Player player) {
        plugin.toggleBoard(player);
    }

    @Override
    public void disableBoard(Player player) {
        plugin.toggleBoard(player);
    }

    @Override
    public boolean isBoardEnabled(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return plugin.shouldShowBoardPublic(player);
        }
        return false;
    }

    @Override
    public void setAutoUpdateOnStatsChange(boolean enabled) {
        this.autoUpdateOnStatsChange = enabled;
    }

    @Override
    public boolean isAutoUpdateOnStatsChangeEnabled() {
        return autoUpdateOnStatsChange;
    }

    @Override
    public void clearCache(UUID playerId) {
        // 清理缓存 - 通过内部方法
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.removeBoard(player);
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
                registry.unregisterService(BoardService.class);
                logger.info("已从 RPGCore 注销: BoardService");
            } catch (Exception e) {
                logger.warning("从 RPGCore 注销失败: " + e.getMessage());
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
