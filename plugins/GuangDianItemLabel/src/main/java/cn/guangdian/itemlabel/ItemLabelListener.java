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

        if (sourceItem != null) {
            labelManager.removeLabel(sourceItem);
        }

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
}
