package cn.guangdian.collection.listener;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class MobKillListener implements Listener {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    private boolean mythicMobsEnabled = false;
    private Object mythicMobsPlugin = null;
    
    public MobKillListener(GuangDianCollection plugin, CollectionService collectionService) {
        this.plugin = plugin;
        this.collectionService = collectionService;
        
        if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.mythicMobsEnabled = true;
            this.mythicMobsPlugin = plugin.getServer().getPluginManager().getPlugin("MythicMobs");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null) return;
        
        String mythicId = null;
        if (mythicMobsEnabled) {
            mythicId = getMythicMobsType(entity);
        }
        
        for (CollectionCategory category : collectionService.getCategories().values()) {
            if (category.getType() != CollectionCategory.CategoryType.MOB_KILL) continue;
            
            for (CollectionEntry entry : category.getEntries().values()) {
                if (matchesEntry(entry, entity, mythicId)) {
                    collectionService.addKill(killer, entry);
                }
            }
        }
    }
    
    private boolean matchesEntry(CollectionEntry entry, Entity entity, String mythicId) {
        switch (entry.getType()) {
            case VANILLA_MOB:
                return entry.getEntityType() == entity.getType();
            case MYTHICMOBS_MOB:
                return mythicId != null && mythicId.equals(entry.getMythicId());
            default:
                return false;
        }
    }
    
    private String getMythicMobsType(Entity entity) {
        try {
            if (entity.hasMetadata("mythicmob")) {
                List<MetadataValue> values = entity.getMetadata("mythicmob");
                if (!values.isEmpty()) {
                    return values.get(0).asString();
                }
            }
            
            if (entity.hasMetadata("mythicmob_type")) {
                List<MetadataValue> values = entity.getMetadata("mythicmob_type");
                if (!values.isEmpty()) {
                    return values.get(0).asString();
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
