package cn.guangdian.collection.service;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.*;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.PointsService;
import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.api.RPGItemsAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionServiceImpl implements CollectionService {
    
    private final GuangDianCollection plugin;
    private final Map<String, CollectionSet> sets = new ConcurrentHashMap<>();
    private final Map<String, CollectionCategory> categories = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerCollectionData> playerDataCache = new ConcurrentHashMap<>();
    
    private File playerDataFolder;
    
    private static final NamespacedKey MYTHIC_TYPE_KEY = new NamespacedKey("mythicmobs", "type");
    private static final NamespacedKey MYTHIC_OLD_KEY = new NamespacedKey("mythicmobs", "item");
    
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
    }
    
    private void loadCollections() {
        sets.clear();
        categories.clear();
        
        File collectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        if (!collectionsFile.exists()) {
            plugin.saveResource("collections.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(collectionsFile);
        
        ConfigurationSection setsSection = config.getConfigurationSection("sets");
        if (setsSection != null) {
            for (String setId : setsSection.getKeys(false)) {
                ConfigurationSection setSection = setsSection.getConfigurationSection(setId);
                if (setSection == null) continue;
                
                CollectionSet set = new CollectionSet(setId);
                set.setName(setSection.getString("name", setId));
                set.setDescription(setSection.getString("description", ""));
                
                String iconStr = setSection.getString("icon", "CHEST");
                try {
                    set.setIcon(Material.valueOf(iconStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    set.setIcon(Material.CHEST);
                }
                
                set.setSlot(setSection.getInt("slot", 0));
                set.setCategoryIds(setSection.getStringList("categories"));
                
                sets.put(setId, set);
            }
        }
        
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                ConfigurationSection catSection = categoriesSection.getConfigurationSection(categoryId);
                if (catSection == null) continue;
                
                CollectionCategory category = new CollectionCategory(categoryId);
                category.setName(catSection.getString("name", categoryId));
                category.setDescription(catSection.getString("description", ""));
                
                String iconStr = catSection.getString("icon", "CHEST");
                try {
                    category.setIcon(Material.valueOf(iconStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    category.setIcon(Material.CHEST);
                }
                
                category.setSlot(catSection.getInt("slot", 0));
                
                String typeStr = catSection.getString("type", "ITEM_COLLECT");
                category.setType(CollectionCategory.CategoryType.valueOf(typeStr));
                
                loadEntries(category, catSection);
                
                categories.put(categoryId, category);
            }
        }
        
        for (CollectionSet set : sets.values()) {
            for (String categoryId : set.getCategoryIds()) {
                CollectionCategory category = categories.get(categoryId);
                if (category != null) {
                    category.setSetId(set.getId());
                }
            }
        }
    }
    
    private void loadEntries(CollectionCategory category, ConfigurationSection section) {
        ConfigurationSection entriesSection = section.getConfigurationSection("entries");
        if (entriesSection == null) return;
        
        for (String entryId : entriesSection.getKeys(false)) {
            ConfigurationSection entrySection = entriesSection.getConfigurationSection(entryId);
            if (entrySection == null) continue;
            
            String fullId = category.getId() + "." + entryId;
            String typeStr = entrySection.getString("type", "VANILLA");
            
            CollectionEntry.EntryType entryType;
            if (typeStr.equals("MYTHICMOBS")) {
                entryType = CollectionEntry.EntryType.MYTHICMOBS_ITEM;
            } else if (typeStr.equals("RPGITEMS")) {
                entryType = CollectionEntry.EntryType.RPGITEMS_ITEM;
            } else {
                entryType = CollectionEntry.EntryType.VANILLA_ITEM;
            }
            
            CollectionEntry entry = new CollectionEntry(
                fullId, 
                category.getId(),
                entrySection.getString("name", entryId),
                entryType,
                entrySection.getString("hint", "")
            );
            
            entry.setSlot(entrySection.getInt("slot", 0));
            
            if (entryType == CollectionEntry.EntryType.VANILLA_ITEM) {
                String materialStr = entrySection.getString("material", "STONE");
                try {
                    entry.setMaterial(Material.valueOf(materialStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    entry.setMaterial(Material.STONE);
                }
            } else if (entryType == CollectionEntry.EntryType.MYTHICMOBS_ITEM) {
                entry.setMythicId(entrySection.getString("mythic-id", ""));
            } else if (entryType == CollectionEntry.EntryType.RPGITEMS_ITEM) {
                entry.setRpgItemId(entrySection.getString("rpg-item-id", ""));
            }
            
            ConfigurationSection rewardSection = entrySection.getConfigurationSection("reward");
            if (rewardSection != null) {
                CollectionEntry.EntryReward reward = new CollectionEntry.EntryReward();
                reward.setMoney(rewardSection.getDouble("money", 0));
                reward.setPoints(rewardSection.getLong("points", 0));
                reward.setCommands(rewardSection.getStringList("commands"));
                reward.setMessages(rewardSection.getStringList("messages"));
                entry.setReward(reward);
            }
            
            category.addEntry(entry);
        }
    }
    
    @Override
    public Map<String, CollectionSet> getSets() {
        return Collections.unmodifiableMap(sets);
    }
    
    @Override
    public Optional<CollectionSet> getSet(String setId) {
        return Optional.ofNullable(sets.get(setId));
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
                    new CollectedEntry(parts[0], Long.parseLong(parts[1])));
            }
        }
        
        data.setDirty(false);
        
        return data;
    }
    
    @Override
    public boolean submitItem(Player player, CollectionEntry entry, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        PlayerCollectionData data = getPlayerData(player);
        
        if (data.hasCollected(entry.getId())) {
            player.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getPrefix() + "该物品已收集！", net.kyori.adventure.text.format.NamedTextColor.RED));
            return false;
        }
        
        if (!matchesEntry(entry, item)) {
            player.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getPrefix() + "物品不匹配！需要: " + entry.getName(), net.kyori.adventure.text.format.NamedTextColor.RED));
            return false;
        }
        
        item.setAmount(item.getAmount() - 1);
        
        data.collectItem(entry.getId());
        
        if (entry.getReward() != null) {
            giveReward(player, entry.getReward());
        }
        
        notifyPlayer(player, entry);
        
        return true;
    }
    
    @Override
    public boolean matchesEntry(CollectionEntry entry, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        switch (entry.getType()) {
            case VANILLA_ITEM:
                return matchesVanillaItem(entry, item);
            case MYTHICMOBS_ITEM:
                return matchesMythicMobsItem(entry, item);
            case RPGITEMS_ITEM:
                return matchesRPGItemsItem(entry, item);
            default:
                return false;
        }
    }
    
    private boolean matchesVanillaItem(CollectionEntry entry, ItemStack item) {
        if (entry.getMaterial() == null) return false;
        return entry.getMaterial() == item.getType();
    }
    
    private boolean matchesMythicMobsItem(CollectionEntry entry, ItemStack item) {
        if (entry.getMythicId() == null || entry.getMythicId().isEmpty()) return false;
        
        String mythicId = getMythicMobsId(item);
        if (mythicId == null) return false;
        
        return mythicId.equals(entry.getMythicId());
    }
    
    private boolean matchesRPGItemsItem(CollectionEntry entry, ItemStack item) {
        if (entry.getRpgItemId() == null || entry.getRpgItemId().isEmpty()) return false;
        
        String rpgItemId = getRPGItemsId(item);
        if (rpgItemId == null) return false;
        
        return rpgItemId.equals(entry.getRpgItemId());
    }
    
    private String getRPGItemsId(ItemStack item) {
        if (item == null) return null;
        
        RPGItems rpgItems = RPGItems.getInstance();
        if (rpgItems == null) return null;
        
        RPGItemsAPI api = rpgItems.getAPI();
        if (api == null) return null;
        
        return api.getItemId(item).orElse(null);
    }
    
    private String getMythicMobsId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        String typeId = pdc.get(MYTHIC_TYPE_KEY, PersistentDataType.STRING);
        if (typeId != null) return typeId;
        
        return pdc.get(MYTHIC_OLD_KEY, PersistentDataType.STRING);
    }
    
    private void giveReward(Player player, CollectionEntry.EntryReward reward) {
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
                pointsService.addBalance(player.getUniqueId(), reward.getPoints(), "图鉴收集奖励");
            }
        }
        
        for (String cmd : reward.getCommands()) {
            String parsedCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        for (String msg : reward.getMessages()) {
            player.sendMessage(Component.text(msg.replace("{player}", player.getName())));
        }
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
            NamespacedKey soundKey = NamespacedKey.minecraft(
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
    public int getTotalItemsCollected(UUID playerId) {
        return getPlayerData(playerId).getTotalItemsCollected();
    }
    
    @Override
    public void savePlayerData(UUID playerId) {
        PlayerCollectionData data = playerDataCache.get(playerId);
        if (data == null || !data.isDirty()) return;
        
        File file = new File(playerDataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        List<String> collectedItems = new ArrayList<>();
        for (CollectedEntry entry : data.getCollectedItems().values()) {
            collectedItems.add(entry.getEntryId() + ":" + entry.getCollectedAt());
        }
        config.set("collectedItems", collectedItems);
        
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
