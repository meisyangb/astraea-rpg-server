package cn.guangdian.cavefu.adapter;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 洞府服务适配器
 * 
 * <p>提供统一的服务接口供其他插件调用。</p>
 * <p>完全独立，不依赖 RPGCore 服务层。</p>
 * 
 * @author GuangDian
 * @since 1.1.0
 */
public class CaveServiceAdapter {

    private final GuangDianCaveFu plugin;

    public CaveServiceAdapter(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("CaveServiceAdapter 已就绪（独立模式）");
    }

    public Object getPlayerCave(UUID playerId) {
        return plugin.getCaveManager().getOwnerCave(playerId);
    }

    public boolean hasCave(UUID playerId) {
        return plugin.getCaveManager().getOwnerCave(playerId) != null;
    }

    public boolean createCave(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return false;
        return plugin.getCaveManager().createCave(player) != null;
    }

    public boolean deleteCave(UUID playerId) {
        return plugin.getCaveManager().deleteCave(playerId);
    }

    public int getCaveLevel(UUID playerId) {
        Cave cave = plugin.getCaveManager().getOwnerCave(playerId);
        return cave != null ? cave.getLevel() : 0;
    }

    public boolean upgradeCave(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return false;
        Cave cave = plugin.getCaveManager().getOwnerCave(playerId);
        if (cave == null) return false;
        return plugin.getUpgradeManager().upgrade(player, cave);
    }

    public boolean teleportToCave(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return false;
        plugin.getCaveManager().teleportHome(player);
        return true;
    }

    public boolean inviteToCave(UUID ownerId, UUID guestId) {
        Player owner = plugin.getServer().getPlayer(ownerId);
        Player guest = plugin.getServer().getPlayer(guestId);
        if (owner == null || guest == null) return false;
        return plugin.getCaveManager().inviteMember(owner, guest);
    }

    public int getCaveCount() {
        return plugin.getDataManager().getCaveCount();
    }

    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        plugin.getLogger().info("CaveServiceAdapter 已注销");
    }
}
