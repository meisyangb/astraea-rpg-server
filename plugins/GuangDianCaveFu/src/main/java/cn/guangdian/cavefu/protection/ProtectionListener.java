package cn.guangdian.cavefu.protection;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.permission.PermissionType;
import cn.guangdian.cavefu.world.CaveWorldManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * 区域保护监听器
 * 洞府插件自带保护，无需 WorldGuard
 */
public class ProtectionListener implements Listener {
    private final GuangDianCaveFu plugin;
    private final CaveManager caveManager;
    private final ConfigManager configManager;
    private final CaveWorldManager worldManager;

    public ProtectionListener(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.caveManager = plugin.getCaveManager();
        this.configManager = plugin.getConfigManager();
        this.worldManager = plugin.getWorldManager();
    }

    /**
     * 破坏方块
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        if (!worldManager.isCaveWorld(loc.getWorld())) return;

        Cave cave = caveManager.getCaveAt(loc);

        if (cave == null) {
            event.setCancelled(true);
            player.sendMessage("§c这里不是你的洞府区域！");
            return;
        }

        PermissionType permission = caveManager.checkPermission(player.getUniqueId(), cave);
        if (!permission.isAtLeast(PermissionType.MEMBER)) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("no-permission"));
        }
    }

    /**
     * 放置方块
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        if (!worldManager.isCaveWorld(loc.getWorld())) return;

        Cave cave = caveManager.getCaveAt(loc);

        if (cave == null) {
            event.setCancelled(true);
            player.sendMessage("§c这里不是你的洞府区域！");
            return;
        }

        PermissionType permission = caveManager.checkPermission(player.getUniqueId(), cave);
        if (!permission.isAtLeast(PermissionType.MEMBER)) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("no-permission"));
        }
    }

    /**
     * 交互事件 - 只阻止对方块的交互，不阻止物品使用
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Location loc = clickedBlock.getLocation();

        if (!worldManager.isCaveWorld(loc.getWorld())) return;

        // 非交互方块允许物品使用（如菜单）
        if (!isInteractableBlock(clickedBlock.getType())) {
            return;
        }

        Cave cave = caveManager.getCaveAt(loc);

        if (cave == null) {
            event.setCancelled(true);
            player.sendMessage("§c这里不是你的洞府区域！");
            return;
        }

        PermissionType permission = caveManager.checkPermission(player.getUniqueId(), cave);
        if (!permission.isAtLeast(PermissionType.MEMBER)) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("no-permission"));
        }
    }

    private boolean isInteractableBlock(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST ||
               type == Material.ENDER_CHEST || type == Material.BARREL ||
               type == Material.FURNACE || type == Material.BLAST_FURNACE ||
               type == Material.SMOKER || type == Material.BREWING_STAND ||
               type == Material.ANVIL || type == Material.LEVER ||
               type.name().contains("BUTTON") || type.name().contains("DOOR") ||
               type.name().contains("TRAPDOOR") || type.name().contains("FENCE_GATE") ||
               type.name().contains("SHULKER_BOX") || type == Material.HOPPER ||
               type == Material.DISPENSER || type == Material.DROPPER ||
               type == Material.ENCHANTING_TABLE || type == Material.CRAFTING_TABLE ||
               type == Material.BEACON || type == Material.BELL ||
               type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE ||
               type == Material.GRINDSTONE || type == Material.STONECUTTER ||
               type == Material.BEDROCK;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (worldManager.isCaveWorld(player.getWorld())) {
            worldManager.onEnterCave(player);
        }
        if (worldManager.isCaveWorld(event.getFrom())) {
            worldManager.onLeaveCave(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() != null && worldManager.isCaveWorld(event.getTo().getWorld())) {
            worldManager.onEnterCave(player);
        }
        if (worldManager.isCaveWorld(event.getFrom().getWorld()) && event.getTo() != null && !worldManager.isCaveWorld(event.getTo().getWorld())) {
            worldManager.onLeaveCave(player);
        }
    }
}