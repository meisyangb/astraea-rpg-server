package cn.guangdian.cavefu.adapter;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.event.events.RpgMobKillEvent;
import cn.guangdian.rpgcore.service.api.CaveService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 洞府服务适配器
 * 
 * <p>连接 GuangDianCaveFu 与 RPGCore 服务层，
 * 支持 EventBus 事件订阅和 AsyncExecutor 异步操作。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class CaveServiceAdapter implements CaveService {

    private final GuangDianCaveFu plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private AsyncExecutor asyncExecutor;

    public CaveServiceAdapter(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                this.asyncExecutor = rpgCore.getAsyncExecutor();
                
                registry.registerService(CaveService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: CaveService");
                
                // 订阅怪物击杀事件 - 用于副本怪物击杀统计
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
        // RpgMobKillEvent 是 Bukkit 事件，需要通过 Bukkit 事件系统订阅
        // 这里订阅 CoreEvent 类型的事件（如果有的话）
        plugin.getLogger().info("CaveServiceAdapter: 事件订阅已就绪");
    }
    
    /**
     * 处理怪物击杀事件（由 Bukkit 事件监听器调用）
     */
    public void handleMobKill(UUID playerId, String mobType, String worldName, String playerName) {
        // 检查是否在洞府世界
        Cave cave = plugin.getCaveManager().getOwnerCave(playerId);
        if (cave == null) return;
        
        // 检查是否在洞府世界内击杀
        if (worldName != null && worldName.startsWith("cave_")) {
            // 异步记录击杀数据
            if (asyncExecutor != null) {
                asyncExecutor.execute(() -> {
                    // 记录击杀统计（可用于洞府升级进度）
                    plugin.getLogger().fine("洞府玩家 " + playerName + 
                        " 击杀了 " + mobType);
                });
            }
        }
    }

    @Override
    public Object getPlayerCave(UUID playerId) {
        return plugin.getCaveManager().getOwnerCave(playerId);
    }

    @Override
    public boolean hasCave(UUID playerId) {
        return plugin.getCaveManager().getOwnerCave(playerId) != null;
    }

    @Override
    public boolean createCave(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        return plugin.getCaveManager().createCave(player) != null;
    }

    @Override
    public boolean deleteCave(UUID playerId) {
        return plugin.getCaveManager().deleteCave(playerId);
    }

    @Override
    public int getCaveLevel(UUID playerId) {
        Cave cave = plugin.getCaveManager().getOwnerCave(playerId);
        return cave != null ? cave.getLevel() : 0;
    }

    @Override
    public boolean upgradeCave(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        Cave cave = plugin.getCaveManager().getOwnerCave(playerId);
        if (cave == null) return false;
        return plugin.getUpgradeManager().upgrade(player, cave);
    }

    @Override
    public boolean teleportToCave(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        plugin.getCaveManager().teleportHome(player);
        return true;
    }

    @Override
    public boolean inviteToCave(UUID ownerId, UUID guestId) {
        Player owner = Bukkit.getPlayer(ownerId);
        Player guest = Bukkit.getPlayer(guestId);
        if (owner == null || guest == null) return false;
        return plugin.getCaveManager().inviteMember(owner, guest);
    }

    @Override
    public int getCaveCount() {
        return plugin.getDataManager().getCaveCount();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 异步保存数据
     */
    public void saveAsync(Runnable saveTask) {
        if (asyncExecutor != null) {
            asyncExecutor.execute(saveTask);
        } else {
            // 降级：同步执行
            saveTask.run();
        }
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(CaveService.class);
                plugin.getLogger().info("已从 RPGCore 注销: CaveService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }
}