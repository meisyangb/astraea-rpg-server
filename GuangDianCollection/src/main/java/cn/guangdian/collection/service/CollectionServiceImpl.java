package cn.guangdian.collection.service;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.model.*;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.PointsService;
import net.kyori.adventure.text.Component;
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
import java.util.concurrent.atomic.AtomicLong;

public class CollectionServiceImpl implements CollectionService {
    
    private final GuangDianCollection plugin;
    private final Map<String, CollectionSet> sets = new ConcurrentHashMap<>();
    private final Map<String, CollectionCategory> categories = new ConcurrentHashMap<>();
    
    // ✅ 内存缓存：避免频繁文件IO
    private final Map<UUID, PlayerCollectionData> dataCache = new ConcurrentHashMap<>();
    private final File playerDataFolder;
    
    // 缓存统计
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
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
            
            CollectionEntry.EntryType entryType = typeStr.equals("MYTHICMOBS") 
                ? CollectionEntry.EntryType.MYTHICMOBS_ITEM 
                : CollectionEntry.EntryType.VANILLA_ITEM;
            
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
            } else {
                entry.setMythicId(entrySection.getString("mythic-id", ""));
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
        // ✅ 先从缓存获取
        PlayerCollectionData cached = dataCache.get(playerId);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        
        // 缓存未命中，加载并缓存
        cacheMisses.incrementAndGet();
        PlayerCollectionData data = loadPlayerDataFromFile(playerId);
        dataCache.put(playerId, data);
        return data;
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
        
        // 加载收集的物品
        List<String> collectedItems = config.getStringList("collectedItems");
        for (String entry : collectedItems) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                String entryId = parts[0];
                data.getCollectedItems().put(entryId, 
                    new CollectedEntry(entryId, Long.parseLong(parts[1])));
            }
        }
        
        // 加载枚举计数器
        ConfigurationSection progressSection = config.getConfigurationSection("categoryProgress");
        if (progressSection != null) {
            for (String categoryId : progressSection.getKeys(false)) {
                data.restoreCategoryProgress(categoryId, progressSection.getInt(categoryId));
            }
        }
        
        return data;
    }
    
    @Override
    public boolean submitItem(Player player, CollectionEntry entry, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        MiniMessageService mm = plugin.getMiniMessage();
        
        // ✅ 从缓存获取（缓存未命中会自动加载）
        PlayerCollectionData data = getPlayerData(player);
        
        if (data.hasCollected(entry.getId())) {
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<red>该物品已收集！");
            return false;
        }
        
        if (!matchesEntry(entry, item)) {
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + "<red>物品不匹配！需要: <yellow>" + entry.getName());
            return false;
        }
        
        item.setAmount(item.getAmount() - 1);
        
        // 收集物品（更新枚举计数器）
        data.collectItem(entry.getId(), entry.getCategoryId());
        
        // ✅ 异步保存到文件（不阻塞主线程）
        savePlayerDataToFileAsync(player.getUniqueId(), data);
        
        if (entry.getReward() != null) {
            giveReward(player, entry.getReward());
        }
        
        notifyPlayer(player, entry);
        
        return true;
    }
    
    private void sendMessage(Player player, MiniMessageService mm, String message) {
        if (mm != null) {
            player.sendMessage(mm.colorize(message));
        } else {
            player.sendMessage(Component.text(message));
        }
    }
    
    @Override
    public boolean matchesEntry(CollectionEntry entry, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        switch (entry.getType()) {
            case VANILLA_ITEM:
                return matchesVanillaItem(entry, item);
            case MYTHICMOBS_ITEM:
                return matchesMythicMobsItem(entry, item);
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
        MiniMessageService mm = plugin.getMiniMessage();
        
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
            sendMessage(player, mm, msg.replace("{player}", player.getName()));
        }
    }
    
    private void notifyPlayer(Player player, CollectionEntry entry) {
        MiniMessageService mm = plugin.getMiniMessage();
        CollectionCategory category = categories.get(entry.getCategoryId());
        int progress = getCategoryProgress(player, entry.getCategoryId());
        int total = category != null ? category.getTotalEntries() : 0;
        
        String message = plugin.getConfigManager().getMessage("collected")
            .replace("{item}", entry.getName())
            .replace("{current}", String.valueOf(progress))
            .replace("{total}", String.valueOf(total));
        
        sendMessage(player, mm, plugin.getConfigManager().getPrefix() + message);
        
        try {
            NamespacedKey soundKey = NamespacedKey.minecraft(
                plugin.getConfigManager().getNotifySound().toLowerCase().replace("minecraft:", ""));
            Sound sound = org.bukkit.Registry.SOUNDS.get(soundKey);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 
                    (float) plugin.getConfigManager().getNotifyVolume(),
                    (float) plugin.getConfigManager().getNotifyPitch());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("播放收藏通知声音失败: " + e.getMessage());
        }
        
        if (category != null && isCategoryComplete(player, entry.getCategoryId())) {
            String completeMsg = plugin.getConfigManager().getMessage("category-complete")
                .replace("{category}", category.getName());
            sendMessage(player, mm, plugin.getConfigManager().getPrefix() + completeMsg);
        }
    }
    
    @Override
    public int getCategoryProgress(Player player, String categoryId) {
        // ✅ 从缓存读取计数器（缓存未命中会自动加载）
        return getPlayerData(player).getCategoryProgress(categoryId);
    }
    
    @Override
    public boolean isCategoryComplete(Player player, String categoryId) {
        CollectionCategory category = categories.get(categoryId);
        if (category == null) return false;
        
        return getPlayerData(player).isCategoryComplete(categoryId, category.getTotalEntries());
    }
    
    @Override
    public int getTotalItemsCollected(UUID playerId) {
        return getPlayerData(playerId).getTotalItemsCollected();
    }
    
    @Override
    public void savePlayerData(UUID playerId) {
        // ✅ 保存缓存中的数据
        PlayerCollectionData data = dataCache.get(playerId);
        if (data != null) {
            savePlayerDataToFileSync(playerId, data);
        }
    }
    
    /**
     * 异步保存玩家数据到文件（不阻塞主线程）
     */
    private void savePlayerDataToFileAsync(UUID playerId, PlayerCollectionData data) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            savePlayerDataToFileSync(playerId, data);
        });
    }
    
    /**
     * 同步保存玩家数据到文件
     */
    private void savePlayerDataToFileSync(UUID playerId, PlayerCollectionData data) {
        File file = new File(playerDataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        // 保存收集的物品
        List<String> collectedItems = new ArrayList<>();
        for (CollectedEntry entry : data.getCollectedItems().values()) {
            collectedItems.add(entry.getEntryId() + ":" + entry.getCollectedAt());
        }
        config.set("collectedItems", collectedItems);
        
        // 保存枚举计数器
        for (Map.Entry<String, Integer> entry : data.getAllCategoryProgress().entrySet()) {
            config.set("categoryProgress." + entry.getKey(), entry.getValue());
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存玩家数据: " + playerId + " - " + e.getMessage());
        }
    }
    
    @Override
    public void saveAllPlayerData() {
        // ✅ 保存所有缓存中的数据
        for (Map.Entry<UUID, PlayerCollectionData> entry : dataCache.entrySet()) {
            savePlayerDataToFileSync(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 保存并清理玩家缓存（玩家退出时调用）
     */
    public void saveAndClearCache(UUID playerId) {
        PlayerCollectionData data = dataCache.remove(playerId);
        if (data != null) {
            // 同步保存（玩家退出时必须确保数据保存）
            savePlayerDataToFileSync(playerId, data);
        }
    }
    
    /**
     * 清理缓存但不保存（用于重载配置）
     */
    public void clearCache() {
        dataCache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return dataCache.size();
    }
    
    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("缓存统计: 命中=%d, 未命中=%d, 命中率=%.2f%%, 缓存大小=%d",
            cacheHits.get(), cacheMisses.get(), getCacheHitRate() * 100, getCacheSize());
    }
    
    @Override
    public void reloadData() {
        loadData();
    }
}
