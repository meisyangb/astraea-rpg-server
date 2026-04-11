package cn.guangdian.collection.api;

import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import cn.guangdian.collection.model.CollectionSet;
import cn.guangdian.collection.model.PlayerCollectionData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CollectionService {
    
    Map<String, CollectionSet> getSets();
    
    Optional<CollectionSet> getSet(String setId);
    
    Map<String, CollectionCategory> getCategories();
    
    Optional<CollectionCategory> getCategory(String categoryId);
    
    PlayerCollectionData getPlayerData(UUID playerId);
    
    PlayerCollectionData getPlayerData(Player player);
    
    boolean submitItem(Player player, CollectionEntry entry, ItemStack item);
    
    boolean matchesEntry(CollectionEntry entry, ItemStack item);
    
    int getCategoryProgress(Player player, String categoryId);
    
    boolean isCategoryComplete(Player player, String categoryId);
    
    int getTotalItemsCollected(UUID playerId);
    
    void savePlayerData(UUID playerId);
    
    void saveAllPlayerData();
    
    void reloadData();
}
