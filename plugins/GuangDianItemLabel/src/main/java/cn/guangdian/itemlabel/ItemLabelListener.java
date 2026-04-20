package cn.guangdian.itemlabel;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public class ItemLabelListener implements Listener {

    private final GuangDianItemLabel plugin;
    private final ItemLabelManager labelManager;

    public ItemLabelListener(GuangDianItemLabel plugin) {
        this.plugin = plugin;
        this.labelManager = plugin.getItemLabelManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (item == null) return;

        labelManager.queueLabel(item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (item != null) {
            labelManager.removeLabel(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        if (item != null) {
            labelManager.removeLabel(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        Item targetItem = event.getTarget();
        Item sourceItem = event.getEntity();

        // 移除源物品的标签
        if (sourceItem != null) {
            labelManager.removeLabel(sourceItem);
        }

        // 重新为目标物品创建标签（数量可能变化）
        if (targetItem != null) {
            labelManager.removeLabel(targetItem);
            labelManager.queueLabel(targetItem);
        }
    }

    /**
     * 区块加载时检查已有物品
     * Paper 1.21+ 推荐使用 EntitiesLoadEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Item item) {
                if (!labelManager.hasLabel(item) && !labelManager.isPending(item)) {
                    labelManager.queueLabel(item);
                }
            }
        }
    }

    /**
     * 区块卸载时清理标签
     * Paper 1.21+ 推荐使用 EntitiesUnloadEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Item item) {
                labelManager.removeLabel(item);
            }
        }
    }

    /**
     * 兼容旧版本的区块加载事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // 如果 EntitiesLoadEvent 可用，优先使用它
        // 这里作为后备处理
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                if (!labelManager.hasLabel(item) && !labelManager.isPending(item)) {
                    labelManager.queueLabel(item);
                }
            }
        }
    }

    /**
     * 兼容旧版本的区块卸载事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                labelManager.removeLabel(item);
            }
        }
    }
}
