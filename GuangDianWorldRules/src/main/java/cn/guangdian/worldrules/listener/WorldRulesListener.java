package cn.guangdian.worldrules.listener;

import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.model.ProtectedRegion;
import cn.guangdian.worldrules.model.WorldRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.List;

public class WorldRulesListener implements Listener {

    private final GuangDianWorldRules plugin;

    public WorldRulesListener(GuangDianWorldRules plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理死亡不掉落物品
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();
        World world = player.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);
        
        boolean keepInventory = rules.isKeepInventory();
        boolean keepExp = rules.isKeepExp();

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(location);
        for (ProtectedRegion region : regions) {
            // 区域规则覆盖死亡不掉落物品
            if (region.getKeepInventory() != null) {
                keepInventory = region.getKeepInventory();
            }
            // 区域规则覆盖死亡不掉落经验
            if (region.getKeepExp() != null) {
                keepExp = region.getKeepExp();
            }
        }

        // 应用规则
        if (keepInventory) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }

        if (keepExp) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    /**
     * 处理生物自然刷新
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        World world = entity.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);
        
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        
        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(location);
        for (ProtectedRegion region : regions) {
            // 检查区域是否禁止自然刷新
            if (region.getDisableNaturalSpawn() != null && region.getDisableNaturalSpawn()) {
                if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                    reason == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            // 检查区域是否禁止怪物刷新
            if (entity instanceof Monster && region.getDisableMonsterSpawn() != null) {
                if (region.getDisableMonsterSpawn()) {
                    if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                        reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                        reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            
            // 检查区域是否禁止动物刷新
            if (entity instanceof Animals && region.getDisableAnimalSpawn() != null) {
                if (region.getDisableAnimalSpawn()) {
                    if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                        reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                        reason == CreatureSpawnEvent.SpawnReason.BREEDING) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // 使用世界规则
        // 检查是否禁止所有自然刷新
        if (rules.isDisableNaturalSpawn()) {
            // 禁止自然刷新和刷怪笼刷新
            if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                event.setCancelled(true);
                return;
            }
        }

        // 检查怪物刷新
        if (entity instanceof Monster) {
            if (rules.isDisableMonsterSpawn()) {
                // 禁止所有怪物刷新（包括自然、刷怪笼、插件）
                if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                    reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                    reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 检查动物刷新
        if (entity instanceof Animals) {
            if (rules.isDisableAnimalSpawn()) {
                // 禁止所有动物刷新
                if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                    reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                    reason == CreatureSpawnEvent.SpawnReason.BREEDING) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 检查特定生物类型
        String mobType = entity.getType().name();
        if (rules.isMobDisabled(mobType)) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理天气变化
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableWeatherChange() && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理时间变化
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTimeSkip(TimeSkipEvent event) {
        World world = event.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableTimeChange()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理饥饿度变化
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        World world = player.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableHunger()) {
            // 保持饥饿度满
            event.setFoodLevel(20);
            event.setCancelled(true);
        }
    }

    /**
     * 处理摔落伤害
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        World world = player.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        switch (event.getCause()) {
            case FALL:
                if (rules.isDisableFallDamage()) {
                    event.setCancelled(true);
                }
                break;
            case FIRE:
            case FIRE_TICK:
            case LAVA:
                if (rules.isDisableFireDamage()) {
                    event.setCancelled(true);
                }
                break;
            case DROWNING:
                if (rules.isDisableDrowningDamage()) {
                    event.setCancelled(true);
                }
                break;
            case STARVATION:
                if (rules.isDisableHunger()) {
                    event.setCancelled(true);
                }
                break;
        }
    }

    /**
     * 处理爆炸破坏方块
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableExplosionBlockDamage()) {
            event.blockList().clear();
        }
    }

    /**
     * 处理方块爆炸
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        World world = event.getBlock().getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableExplosionBlockDamage()) {
            event.blockList().clear();
        }
    }

    /**
     * 处理生物破坏 (苦力怕爆炸、末影人搬方块等)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        World world = event.getBlock().getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableMobGriefing()) {
            // 禁止生物破坏方块
            if (entity instanceof Monster || entity instanceof Enderman || entity instanceof Creeper) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 处理生物爆炸 (苦力怕、TNT等)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplodeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Explosive)) {
            return;
        }

        World world = event.getEntity().getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableExplosionBlockDamage()) {
            // 这里只处理对实体的伤害，方块破坏在 onEntityExplode 中处理
            // 如果需要完全禁用爆炸伤害，可以取消事件
        }
    }

    /**
     * 处理物品丢弃
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(player.getLocation());
        for (ProtectedRegion region : regions) {
            if (region.getAllowItemDrop() != null) {
                if (!region.getAllowItemDrop()) {
                    event.setCancelled(true);
                    return;
                }
                // 如果区域允许丢弃，直接返回（覆盖世界规则）
                return;
            }
        }

        if (rules.isDisableItemDrop()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理物品拾取
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(player.getLocation());
        for (ProtectedRegion region : regions) {
            if (region.getAllowItemPickup() != null) {
                if (!region.getAllowItemPickup()) {
                    event.setCancelled(true);
                    return;
                }
                // 如果区域允许拾取，直接返回（覆盖世界规则）
                return;
            }
        }

        if (rules.isDisableItemPickup()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理 PVP
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        World world = victim.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 检查是否是 PVP
        if (event.getDamager() instanceof Player) {
            // 先检查区域规则
            List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(victim.getLocation());
            for (ProtectedRegion region : regions) {
                if (region.getAllowPVP() != null) {
                    if (!region.getAllowPVP()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // 使用世界规则
            if (!rules.isPvp()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 处理方块破坏
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        World world = location.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(location);
        for (ProtectedRegion region : regions) {
            if (region.getAllowBreak() != null) {
                if (!region.getAllowBreak()) {
                    event.setCancelled(true);
                    return;
                }
                // 如果区域允许破坏，直接返回（覆盖世界规则）
                return;
            }
        }

        // 使用世界规则
        if (rules.isDisableBlockBreak()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理方块放置
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        World world = location.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(location);
        for (ProtectedRegion region : regions) {
            if (region.getAllowPlace() != null) {
                if (!region.getAllowPlace()) {
                    event.setCancelled(true);
                    return;
                }
                // 如果区域允许放置，直接返回（覆盖世界规则）
                return;
            }
        }

        // 使用世界规则
        if (rules.isDisableBlockPlace()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理方块交互
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        World world = location.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        // 先检查区域规则
        List<ProtectedRegion> regions = plugin.getRegionManager().getRegionsAt(location);
        for (ProtectedRegion region : regions) {
            if (region.getAllowInteract() != null) {
                if (!region.getAllowInteract()) {
                    event.setCancelled(true);
                    return;
                }
                // 如果区域允许交互，直接返回（覆盖世界规则）
                return;
            }
        }

        // 使用世界规则
        if (rules.isDisableBlockInteract()) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理液体流动 (水和岩浆)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();

        // 只处理水和岩浆
        if (type != Material.WATER && type != Material.LAVA) {
            return;
        }

        World world = block.getWorld();
        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(world);

        if (rules.isDisableLiquidFlow()) {
            event.setCancelled(true);
        }
    }
}
