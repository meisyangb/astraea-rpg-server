package cn.guangdian.collection.service;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.CollectionCategory;
import cn.guangdian.collection.model.CollectionEntry;
import cn.guangdian.collection.model.CollectionReward;
import cn.guangdian.collection.model.PlayerCollectionData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.PointsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionServiceImpl implements CollectionService {
    
    private final GuangDianCollection plugin;
    private final Map<String, CollectionCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, CollectionReward> rewards = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerCollectionData> playerDataCache = new ConcurrentHashMap<>();
    
    private File playerDataFolder;
    
    public CollectionServiceImpl(GuangDianCollection plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
        loadData();
    }
    
    private void loadData() {
        loadCollections();
        loadRewards();
    }
    
    private void loadCollections() {
        categories.clear();
        File collectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        if (!collectionsFile.exists()) {
            plugin.saveResource("collections.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(collectionsFile);
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;
        
        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            if (categorySection == null) continue;
            
            CollectionCategory category = new CollectionCategory(categoryId);
            category.setName(categorySection.getString("name", categoryId));
            category.setDescription(categorySection.getString("description", ""));
            
            String iconStr = categorySection.getString("icon", "CHEST");
            try {
                category.setIcon(org.bukkit.Material.valueOf(iconStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                category.setIcon(org.bukkit.Material.CHEST);
            }
            
            category.setSlot(categorySection.getInt("slot", 0));
            
            String typeStr = categorySection.getString("type", "ITEM_COLLECT");
            category.setType(CollectionCategory.CategoryType.valueOf(typeStr));
            
            if (category.getType() == CollectionCategory.CategoryType.MOB_KILL) {
                loadMobs(category, categorySection);
            } else {
                loadItems(category, categorySection);
            }
            
            categories.put(categoryId, category);
        }
    }
    
    private void loadItems(CollectionCategory category, ConfigurationSection section) {
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        for (String entryId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(entryId);
            if (itemSection == null) continue;
            
            String fullId = category.getId() + "." + entryId;
            String typeStr = itemSection.getString("type", "VANILLA");
            
            CollectionEntry.EntryType entryType = typeStr.equals("MYTHICMOBS") 
                ? CollectionEntry.EntryType.MYTHICMOBS_ITEM 
                : CollectionEntry.EntryType.VANILLA_ITEM;
            
            CollectionEntry entry = new CollectionEntry(
                fullId, 
                category.getId(),
                itemSection.getString("name", entryId),
                entryType,
                itemSection.getString("hint", "")
            );
            
            if (entryType == CollectionEntry.EntryType.VANILLA_ITEM) {
                String materialStr = itemSection.getString("material", "STONE");
                try {
                    entry.setMaterial(org.bukkit.Material.valueOf(materialStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    entry.setMaterial(org.bukkit.Material.STONE);
                }
            } else {
                entry.setMythicId(itemSection.getString("mythic-id", ""));
            }
            
            category.addEntry(entry);
        }
    }
    
    private void loadMobs(CollectionCategory category, ConfigurationSection section) {
        ConfigurationSection mobsSection = section.getConfigurationSection("mobs");
        if (mobsSection == null) return;
        
        for (String entryId : mobsSection.getKeys(false)) {
            ConfigurationSection mobSection = mobsSection.getConfigurationSection(entryId);
            if (mobSection == null) continue;
            
            String fullId = category.getId() + "." + entryId;
            String typeStr = mobSection.getString("type", "VANILLA");
            
            CollectionEntry.EntryType entryType = typeStr.equals("MYTHICMOBS") 
                ? CollectionEntry.EntryType.MYTHICMOBS_MOB 
                : CollectionEntry.EntryType.VANILLA_MOB;
            
            CollectionEntry entry = new CollectionEntry(
                fullId,
                category.getId(),
                mobSection.getString("name", entryId),
                entryType,
                mobSection.getString("hint", "")
            );
            
            entry.setTargetCount(mobSection.getInt("target-count", 1));
            
            if (entryType == CollectionEntry.EntryType.VANILLA_MOB) {
                String entityStr = mobSection.getString("entity", "ZOMBIE");
                try {
                    entry.setEntityType(org.bukkit.entity.EntityType.valueOf(entityStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    entry.setEntityType(org.bukkit.entity.EntityType.ZOMBIE);
                }
            } else {
                entry.setMythicId(mobSection.getString("mythic-id", ""));
            }
            
            category.addEntry(entry);
        }
    }
    
    private void loadRewards() {
        rewards.clear();
        File rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection == null) return;
        
        for (String rewardId : rewardsSection.getKeys(false)) {
            ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(rewardId);
            if (rewardSection == null) continue;
            
            CollectionReward reward = new CollectionReward(rewardId);
            reward.setName(rewardSection.getString("name", rewardId));
            reward.setDescription(rewardSection.getString("description", ""));
            reward.setMoney(rewardSection.getDouble("rewards.money", 0));
            reward.setPoints(rewardSection.getLong("rewards.points", 0));
            
            List<String> commands = rewardSection.getStringList("rewards.commands");
            for (String cmd : commands) {
                reward.addCommand(cmd);
            }
            
            List<String> messages = rewardSection.getStringList("rewards.messages");
            for (String msg : messages) {
                reward.addMessage(msg);
            }
            
            ConfigurationSection conditionSection = rewardSection.getConfigurationSection("condition");
            if (conditionSection != null) {
                CollectionReward.RewardCondition condition = new CollectionReward.RewardCondition();
                String typeStr = conditionSection.getString("type", "CATEGORY_COMPLETE");
                condition.setType(CollectionReward.RewardCondition.ConditionType.valueOf(typeStr));
                condition.setCategory(conditionSection.getString("category", ""));
                condition.setEntryId(conditionSection.getString("entry-id", ""));
                condition.setCount(conditionSection.getInt("count", 1));
                reward.setCondition(condition);
            }
            
            rewards.put(rewardId, reward);
        }
    }
    
    @Override
    public Map<String, CollectionCategory> getCategories() {
        return Collections.unmodifiableMap(categories);
    }
    
    @Override
    public Optional<CollectionCategory> getCategory(String categoryId) {
        return Optional.ofNullable(categories.get(categoryId));
    }
    
    @Override
    public Map<String, CollectionReward> getRewards() {
        return Collections.unmodifiableMap(rewards);
    }
    
    @Override
    public Optional<CollectionReward> getReward(String rewardId) {
        return Optional.ofNullable(rewards.get(rewardId));
    }
    
    @Override
    public PlayerCollectionData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, this::loadPlayerDataFromFile);
    }
    
    @Override
    public PlayerCollectionData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    private PlayerCollectionData loadPlayerDataFromFile(UUID playerId) {
        File file = new File(playerDataFolder, playerId.toString() + ".yml");
        PlayerCollectionData data = new PlayerCollectionData(playerId);
        
        if (!file.exists()) {
            return data;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        List<String> collectedItems = config.getStringList("collectedItems");
        for (String entry : collectedItems) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                data.getCollectedItems().put(parts[0], 
                    new cn.guangdian.collection.model.CollectedEntry(parts[0], Long.parseLong(parts[1])));
            }
        }
        
        ConfigurationSection killsSection = config.getConfigurationSection("killRecords");
        if (killsSection != null) {
            for (String entryId : killsSection.getKeys(false)) {
                cn.guangdian.collection.model.KillRecord record = 
                    new cn.guangdian.collection.model.KillRecord(entryId);
                record.setKillCount(killsSection.getInt(entryId, 0));
                data.getKillRecords().put(entryId, record);
            }
        }
        
        data.getClaimedRewards().addAll(config.getStringList("claimedRewards"));
        data.setDirty(false);
        
        return data;
    }
    
    @Override
    public boolean collectItem(Player player, String categoryId, String entryId) {
        CollectionCategory category = categories.get(categoryId);
        if (category == null) return false;
        
        CollectionEntry entry = category.getEntry(entryId);
        if (entry == null) return false;
        
        return collectItem(player, entry);
    }
    
    @Override
    public boolean collectItem(Player player, CollectionEntry entry) {
        PlayerCollectionData data = getPlayerData(player);
        
        if (data.hasCollected(entry.getId())) {
            return false;
        }
        
        boolean collected = data.collectItem(entry.getId());
        
        if (collected && plugin.getConfigManager().isNotifyPlayer()) {
            notifyPlayer(player, entry);
            checkAndNotifyRewards(player);
        }
        
        return collected;
    }
    
    @Override
    public int addKill(Player player, String categoryId, String entryId) {
        CollectionCategory category = categories.get(categoryId);
        if (category == null) return 0;
        
        CollectionEntry entry = category.getEntry(entryId);
        if (entry == null) return 0;
        
        return addKill(player, entry);
    }
    
    @Override
    public int addKill(Player player, CollectionEntry entry) {
        PlayerCollectionData data = getPlayerData(player);
        int newCount = data.addKill(entry.getId());
        
        if (newCount == entry.getTargetCount() && plugin.getConfigManager().isNotifyPlayer()) {
            notifyKillTargetMet(player, entry, newCount);
            checkAndNotifyRewards(player);
        }
        
        return newCount;
    }
    
    private void notifyPlayer(Player player, CollectionEntry entry) {
        CollectionCategory category = categories.get(entry.getCategoryId());
        int progress = getCategoryProgress(player, entry.getCategoryId());
        int total = category != null ? category.getTotalEntries() : 0;
        
        String message = plugin.getConfigManager().getMessage("collected")
            .replace("{item}", entry.getName())
            .replace("{current}", String.valueOf(progress))
            .replace("{total}", String.valueOf(total));
        
        player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + message));
        
        try {
            org.bukkit.NamespacedKey soundKey = org.bukkit.NamespacedKey.minecraft(
                plugin.getConfigManager().getNotifySound().toLowerCase().replace("minecraft:", ""));
            Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 
                    (float) plugin.getConfigManager().getNotifyVolume(),
                    (float) plugin.getConfigManager().getNotifyPitch());
            }
        } catch (Exception ignored) {}
        
        if (category != null && isCategoryComplete(player, entry.getCategoryId())) {
            String completeMsg = plugin.getConfigManager().getMessage("category-complete")
                .replace("{category}", category.getName());
            player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + completeMsg)
                .color(NamedTextColor.GOLD));
        }
    }
    
    private void notifyKillTargetMet(Player player, CollectionEntry entry, int count) {
        String message = plugin.getConfigManager().getMessage("mob-killed")
            .replace("{mob}", entry.getName())
            .replace("{current}", String.valueOf(count))
            .replace("{total}", String.valueOf(entry.getTargetCount()));
        
        player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + message));
    }
    
    private void checkAndNotifyRewards(Player player) {
        List<CollectionReward> available = getAvailableRewards(player);
        if (!available.isEmpty()) {
            String message = plugin.getConfigManager().getMessage("reward-available");
            player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + message));
        }
    }
    
    @Override
    public int getCategoryProgress(Player player, String categoryId) {
        CollectionCategory category = categories.get(categoryId);
        if (category == null) return 0;
        
        return getPlayerData(player).getCategoryProgress(categoryId, category);
    }
    
    @Override
    public boolean isCategoryComplete(Player player, String categoryId) {
        CollectionCategory category = categories.get(categoryId);
        if (category == null) return false;
        
        return getPlayerData(player).isCategoryComplete(categoryId, category);
    }
    
    @Override
    public List<CollectionReward> getAvailableRewards(Player player) {
        List<CollectionReward> available = new ArrayList<>();
        PlayerCollectionData data = getPlayerData(player);
        
        for (CollectionReward reward : rewards.values()) {
            if (data.hasClaimedReward(reward.getId())) continue;
            if (checkRewardCondition(player, reward)) {
                available.add(reward);
            }
        }
        
        return available;
    }
    
    private boolean checkRewardCondition(Player player, CollectionReward reward) {
        CollectionReward.RewardCondition condition = reward.getCondition();
        if (condition == null) return false;
        
        PlayerCollectionData data = getPlayerData(player);
        
        switch (condition.getType()) {
            case CATEGORY_COMPLETE:
                return isCategoryComplete(player, condition.getCategory());
            case ITEM_COUNT:
                return data.getTotalItemsCollected() >= condition.getCount();
            case KILL_COUNT:
                return data.getTotalKills() >= condition.getCount();
            case ITEM_COLLECT:
                return data.hasCollected(condition.getEntryId());
            case KILL_TARGET:
                CollectionCategory category = categories.get(condition.getCategory());
                if (category != null) {
                    CollectionEntry entry = category.getEntry(condition.getEntryId());
                    if (entry != null) {
                        return data.isKillTargetMet(entry.getId(), entry.getTargetCount());
                    }
                }
                return false;
            default:
                return false;
        }
    }
    
    @Override
    public boolean claimReward(Player player, String rewardId) {
        CollectionReward reward = rewards.get(rewardId);
        if (reward == null) return false;
        
        PlayerCollectionData data = getPlayerData(player);
        if (data.hasClaimedReward(rewardId)) return false;
        if (!checkRewardCondition(player, reward)) return false;
        
        data.claimReward(rewardId);
        giveReward(player, reward);
        
        return true;
    }
    
    private void giveReward(Player player, CollectionReward reward) {
        RPGCore rpgCore = RPGCore.getInstance();
        
        if (reward.getMoney() > 0 && rpgCore != null) {
            ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
            if (externalServices != null && externalServices.isVaultEnabled()) {
                externalServices.deposit(player, reward.getMoney());
            }
        }
        
        if (reward.getPoints() > 0 && rpgCore != null) {
            cn.guangdian.rpgcore.api.ServiceRegistry registry = rpgCore.getServiceRegistry();
            PointsService pointsService = registry.getService(PointsService.class);
            if (pointsService != null) {
                pointsService.addBalance(player.getUniqueId(), reward.getPoints(), "图鉴奖励: " + reward.getName());
            }
        }
        
        for (String cmd : reward.getCommands()) {
            String parsedCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        for (String msg : reward.getMessages()) {
            player.sendMessage(Component.text(msg.replace("{player}", player.getName())));
        }
        
        String message = plugin.getConfigManager().getMessage("reward-claimed")
            .replace("{reward}", reward.getName());
        player.sendMessage(Component.text(plugin.getConfigManager().getPrefix() + message));
    }
    
    @Override
    public int getTotalItemsCollected(UUID playerId) {
        return getPlayerData(playerId).getTotalItemsCollected();
    }
    
    @Override
    public int getTotalKills(UUID playerId) {
        return getPlayerData(playerId).getTotalKills();
    }
    
    @Override
    public void savePlayerData(UUID playerId) {
        PlayerCollectionData data = playerDataCache.get(playerId);
        if (data == null || !data.isDirty()) return;
        
        File file = new File(playerDataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        List<String> collectedItems = new ArrayList<>();
        for (cn.guangdian.collection.model.CollectedEntry entry : data.getCollectedItems().values()) {
            collectedItems.add(entry.getEntryId() + ":" + entry.getCollectedAt());
        }
        config.set("collectedItems", collectedItems);
        
        for (cn.guangdian.collection.model.KillRecord record : data.getKillRecords().values()) {
            config.set("killRecords." + record.getEntryId(), record.getKillCount());
        }
        
        config.set("claimedRewards", data.getClaimedRewards());
        
        try {
            config.save(file);
            data.setDirty(false);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据: " + playerId + " - " + e.getMessage());
        }
    }
    
    @Override
    public void saveAllPlayerData() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
    }
    
    @Override
    public void reloadData() {
        loadData();
    }
    
    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }
}
