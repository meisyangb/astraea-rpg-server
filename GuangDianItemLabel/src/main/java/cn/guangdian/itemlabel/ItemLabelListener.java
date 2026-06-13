package cn.guangdian.itemlabel;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

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
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
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
        Item sourceItem = event.getEntity();

        if (sourceItem != null) {
            labelManager.removeLabel(sourceItem);
        }

        // 合并目标需要重新创建标签（数量变化）
        Item targetItem = event.getTarget();
        if (targetItem != null) {
            labelManager.removeLabel(targetItem);
            labelManager.queueLabel(targetItem);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (!labelManager.hasLabel(item) && !labelManager.isPending(item)) {
                    labelManager.queueLabel(item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item) {
                labelManager.removeLabel((Item) entity);
            }
        }
    }

    /**
     * 监听所有实体移除事件，覆盖 item.remove() 调用
     * 这是清理标签的关键事件，确保 Cleaner 等插件直接 remove() 物品时标签也能被清除
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        if (event.getEntity() instanceof Item) {
            labelManager.removeLabel((Item) event.getEntity());
        }
    }
}
