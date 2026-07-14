package cn.guangdian.cavefu.cave;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.permission.PermissionType;
import cn.guangdian.cavefu.storage.DataManager;
import cn.guangdian.cavefu.world.CaveWorldManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 洞府管理器
 * 完全独立，不依赖 RPGCore
 */
public class CaveManager {
    private final GuangDianCaveFu plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final CaveWorldManager worldManager;

    public CaveManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.worldManager = plugin.getWorldManager();
    }

    /**
     * 创建洞府
     */
    public Cave createCave(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        // 检查是否已有洞府
        if (dataManager.getCaveByOwner(uuid) != null) {
            return null;
        }

        // 计算洞府位置
        int caveId = dataManager.getNextCaveId();
        Location center = worldManager.calculateCaveCenter(caveId);

        // 创建洞府
        int level = configManager.getDefaultLevel();
        Cave cave = dataManager.createCave(uuid, name, level,
            configManager.getWorldName(), center.getBlockX(), center.getBlockZ());

        // 生成平台
        worldManager.generatePlatform(cave);

        return cave;
    }

    /**
     * 删除洞府
     */
    public boolean deleteCave(UUID ownerUuid) {
        Cave cave = dataManager.getCaveByOwner(ownerUuid);
        if (cave == null) {
            return false;
        }

        // 通知所有成员
        for (UUID memberUuid : cave.getMembers().keySet()) {
            Player member = plugin.getServer().getPlayer(memberUuid);
            if (member != null) {
                plugin.sendMiniMessage(member, configManager.getMessage("cave-deleted"));
            }
        }

        dataManager.deleteCave(cave.getId());
        return true;
    }

    /**
     * 获取玩家的洞府
     */
    public Cave getPlayerCave(UUID uuid) {
        return dataManager.getCaveByMember(uuid);
    }

    /**
     * 获取玩家作为洞主的洞府
     */
    public Cave getOwnerCave(UUID uuid) {
        return dataManager.getCaveByOwner(uuid);
    }

    /**
     * 获取位置所在的洞府
     */
    public Cave getCaveAt(Location loc) {
        return dataManager.getCaveAtLocation(loc);
    }

    /**
     * 邀请成员
     */
    public boolean inviteMember(Player owner, Player target) {
        Cave cave = dataManager.getCaveByOwner(owner.getUniqueId());
        if (cave == null) {
            return false;
        }

        // 检查目标是否已有洞府
        if (dataManager.getCaveByMember(target.getUniqueId()) != null) {
            return false;
        }

        // 检查成员数量
        if (cave.getMembers().size() >= configManager.getMaxMembers()) {
            return false;
        }

        // 添加成员
        cave.addMember(target.getUniqueId(), target.getName(), PermissionType.MEMBER);
        dataManager.updateMemberIndex(target.getUniqueId(), cave);
        dataManager.saveCave(cave);

        return true;
    }

    /**
     * 移除成员
     */
    public boolean kickMember(Player owner, UUID targetUuid) {
        Cave cave = dataManager.getCaveByOwner(owner.getUniqueId());
        if (cave == null) {
            return false;
        }

        CaveMember member = cave.getMember(targetUuid);
        if (member == null || member.getPermission() == PermissionType.OWNER) {
            return false;
        }

        cave.removeMember(targetUuid);
        dataManager.updateMemberIndex(targetUuid, null);
        dataManager.saveCave(cave);

        return true;
    }

    /**
     * 成员离开洞府
     */
    public boolean leaveCave(UUID uuid) {
        Cave cave = dataManager.getCaveByMember(uuid);
        if (cave == null) {
            return false;
        }

        // 洞主不能直接离开，需要先转让
        if (cave.getOwnerUuid().equals(uuid)) {
            return false;
        }

        cave.removeMember(uuid);
        dataManager.updateMemberIndex(uuid, null);
        dataManager.saveCave(cave);

        return true;
    }

    /**
     * 转让洞主
     */
    public boolean transferOwner(Player owner, Player target) {
        Cave cave = dataManager.getCaveByOwner(owner.getUniqueId());
        if (cave == null) {
            return false;
        }

        if (!cave.isMember(target.getUniqueId())) {
            return false;
        }

        cave.transferOwner(target.getUniqueId(), target.getName());
        dataManager.updateMemberIndex(owner.getUniqueId(), cave);
        dataManager.saveCave(cave);

        return true;
    }

    /**
     * 设置传送点
     */
    public boolean setHome(Player player) {
        Cave cave = dataManager.getCaveByMember(player.getUniqueId());
        if (cave == null) {
            return false;
        }

        // 检查是否在洞府内
        if (!cave.isInside(player.getLocation())) {
            return false;
        }

        cave.setHomeLocation(player.getLocation());
        dataManager.saveCave(cave);

        return true;
    }

    /**
     * 传送回家
     */
    public void teleportHome(Player player) {
        Cave cave = dataManager.getCaveByMember(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(plugin.color("<red>你还没有洞府！"));
            return;
        }

        // 获取洞府位置
        org.bukkit.Location homeLoc = cave.getHomeLocation();
        if (homeLoc == null || homeLoc.getWorld() == null) {
            player.sendMessage(plugin.color("<red>洞府世界未加载！请尝试重新进入服务器。"));
            plugin.getLogger().warning("玩家 " + player.getName() + " 洞府世界未加载: " + cave.getWorldName());
            return;
        }

        // 检查世界是否已加载
        if (!org.bukkit.Bukkit.getWorlds().contains(homeLoc.getWorld())) {
            player.sendMessage(plugin.color("<red>洞府世界正在加载，请稍后再试..."));
            plugin.getLogger().warning("玩家 " + player.getName() + " 洞府世界不在已加载列表中: " + cave.getWorldName());
            return;
        }

        // 执行传送
        boolean success = player.teleport(homeLoc);
        if (success) {
            worldManager.onEnterCave(player);
            player.sendMessage(plugin.color("<green>已传送回洞府！"));
        } else {
            player.sendMessage(plugin.color("<red>传送失败！请稍后再试。"));
            plugin.getLogger().warning("玩家 " + player.getName() + " 传送到洞府失败");
        }
    }

    /**
     * 检查权限
     */
    public PermissionType checkPermission(UUID uuid, Cave cave) {
        return cave.getPermission(uuid);
    }

    /**
     * 是否可以操作（建造/破坏）
     */
    public boolean canBuild(UUID uuid, Cave cave) {
        PermissionType permission = cave.getPermission(uuid);
        return permission.isAtLeast(PermissionType.MEMBER);
    }

    /**
     * 是否是洞主
     */
    public boolean isOwner(UUID uuid, Cave cave) {
        return cave.getOwnerUuid().equals(uuid);
    }
}
