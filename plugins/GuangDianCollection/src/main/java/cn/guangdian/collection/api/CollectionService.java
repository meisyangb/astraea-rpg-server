package cn.guangdian.collection.api;

import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import cn.guangdian.collection.model.CollectionReward;
import cn.guangdian.collection.model.PlayerCollectionData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CollectionService {
    
    Map<String, CollectionCategory> getCategories();
    
    Optional<CollectionCategory> getCategory(String categoryId);
    
    Map<String, CollectionReward> getRewards();
    
    Optional<CollectionReward> getReward(String rewardId);
    
    PlayerCollectionData getPlayerData(UUID playerId);
    
    PlayerCollectionData getPlayerData(Player player);
    
    boolean collectItem(Player player, String categoryId, String entryId);
    
    boolean collectItem(Player player, CollectionEntry entry);
    
    int addKill(Player player, String categoryId, String entryId);
    
    int addKill(Player player, CollectionEntry entry);
    
    int getCategoryProgress(Player player, String categoryId);
    
    boolean isCategoryComplete(Player player, String categoryId);
    
    List<CollectionReward> getAvailableRewards(Player player);
    
    boolean claimReward(Player player, String rewardId);
    
    int getTotalItemsCollected(UUID playerId);
    
    int getTotalKills(UUID playerId);
    
    void savePlayerData(UUID playerId);
    
    void saveAllPlayerData();
    
    void reloadData();
}
