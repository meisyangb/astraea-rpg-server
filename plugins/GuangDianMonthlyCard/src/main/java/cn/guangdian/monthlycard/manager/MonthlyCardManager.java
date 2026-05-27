package cn.guangdian.monthlycard.manager;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.DailyReward;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.service.api.PointsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MonthlyCardManager {
    
    private final GuangDianMonthlyCard plugin;
    private final Map<String, MonthlyCardType> cardTypes;
    private final Map<UUID, MonthlyCardData> playerDataCache;
    private final File dataFile;
    
    public MonthlyCardManager(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.cardTypes = new ConcurrentHashMap<>();
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }
    
    public void loadCardTypes() {
        cardTypes.clear();
        FileConfiguration config = plugin.getConfig();
        
        ConfigurationSection cardsSection = config.getConfigurationSection("card-types");
        if (cardsSection == null) {
            plugin.getLogger().warning("未找到 card-types 配置节");
            return;
        }
        
        for (String cardId : cardsSection.getKeys(false)) {
            ConfigurationSection cardSection = cardsSection.getConfigurationSection(cardId);
            if (cardSection != null) {
                MonthlyCardType type = MonthlyCardType.fromConfig(cardId, cardSection);
                cardTypes.put(cardId, type);
                plugin.getLogger().info("加载月卡类型: " + cardId + " - " + type.getDisplayName());
            }
        }
    }
    
    public void loadPlayerData(UUID playerId) {
        if (playerDataCache.containsKey(playerId)) {
            return;
        }
        
        org.bukkit.configuration.file.YamlConfiguration dataConfig = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        
        String path = "players." + playerId.toString();
        if (dataConfig.contains(path)) {
            String cardType = dataConfig.getString(path + ".card-type", "none");
            long activateTime = dataConfig.getLong(path + ".activate-time", 0);
            long expireTime = dataConfig.getLong(path + ".expire-time", 0);
            int totalClaimedDays = dataConfig.getInt(path + ".total-claimed-days", 0);
            long lastClaimTime = dataConfig.getLong(path + ".last-claim-time", 0);
            
            Set<String> claimedDays = new HashSet<>();
            List<String> claimedList = dataConfig.getStringList(path + ".claimed-days");
            claimedDays.addAll(claimedList);
            
            MonthlyCardData data = MonthlyCardData.fromStorage(
                playerId, cardType, activateTime, expireTime, 
                claimedDays, totalClaimedDays, lastClaimTime
            );
            playerDataCache.put(playerId, data);
        } else {
            MonthlyCardData data = new MonthlyCardData(playerId);
            playerDataCache.put(playerId, data);
        }
    }
    
    public void savePlayerData(UUID playerId) {
        MonthlyCardData data = playerDataCache.get(playerId);
        if (data == null) return;
        
        org.bukkit.configuration.file.YamlConfiguration dataConfig = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        
        String path = "players." + playerId.toString();
        dataConfig.set(path + ".card-type", data.getCardType());
        dataConfig.set(path + ".activate-time", data.getActivateTime());
        dataConfig.set(path + ".expire-time", data.getExpireTime());
        dataConfig.set(path + ".total-claimed-days", data.getTotalClaimedDays());
        dataConfig.set(path + ".last-claim-time", data.getLastClaimTime());
        dataConfig.set(path + ".claimed-days", new ArrayList<>(data.getClaimedDays()));
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }
    
    public void saveAllData() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
    }
    
    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }
    
    public Optional<MonthlyCardType> getCardType(String typeId) {
        return Optional.ofNullable(cardTypes.get(typeId));
    }
    
    public List<MonthlyCardType> getAllCardTypes() {
        return new ArrayList<>(cardTypes.values());
    }
    
    public MonthlyCardData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, MonthlyCardData::new);
    }
    
    public boolean hasActiveCard(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.hasActiveCard();
    }
    
    public boolean activateCard(UUID playerId, String cardTypeId) {
        MonthlyCardType type = cardTypes.get(cardTypeId);
        if (type == null) {
            return false;
        }
        
        MonthlyCardData data = getPlayerData(playerId);
        
        if (data.hasActiveCard() && data.getCardType().equals(cardTypeId)) {
            extendCard(playerId, type.getDurationDays());
            return true;
        }
        
        long now = System.currentTimeMillis();
        long expireTime = now + (type.getDurationDays() * 24L * 60 * 60 * 1000);
        
        data.setCardType(cardTypeId);
        data.setActivateTime(now);
        data.setExpireTime(expireTime);
        data.getClaimedDays().clear();
        data.setTotalClaimedDays(0);
        
        savePlayerData(playerId);
        
        return true;
    }
    
    public boolean canClaimToday(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.canClaimToday();
    }
    
    public boolean claimDailyReward(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        
        if (!data.hasActiveCard()) {
            return false;
        }
        
        if (!data.canClaimToday()) {
            return false;
        }
        
        MonthlyCardType type = cardTypes.get(data.getCardType());
        if (type == null) {
            return false;
        }
        
        int day = data.getDaysSinceActivation();
        DailyReward reward = type.getRewardForDay(day);
        
        if (reward == null || !reward.hasAnyReward()) {
            data.markClaimedToday();
            savePlayerData(playerId);
            return true;
        }
        
        giveReward(playerId, reward);
        
        data.markClaimedToday();
        savePlayerData(playerId);
        
        return true;
    }
    
    private void giveReward(UUID playerId, DailyReward reward) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        
        if (reward.getPoints() > 0) {
            givePoints(playerId, reward.getPoints());
            player.sendMessage(Component.text("获得 " + reward.getPoints() + " 点券")
                .color(NamedTextColor.GOLD));
        }
        
        if (reward.getMoney() > 0) {
            giveMoney(playerId, reward.getMoney());
            player.sendMessage(Component.text("获得 " + reward.getMoney() + " 游戏币")
                .color(NamedTextColor.GREEN));
        }
        
        if (!reward.getItems().isEmpty()) {
            for (ItemStack item : reward.getItems()) {
                player.getInventory().addItem(item.clone());
            }
            player.sendMessage(Component.text("获得物品奖励")
                .color(NamedTextColor.YELLOW));
        }
        
        if (!reward.getCommands().isEmpty()) {
            for (String cmd : reward.getCommands()) {
                String parsedCmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
            }
        }
        
        for (String msg : reward.getMessages()) {
            player.sendMessage(Component.text(msg.replace("%player%", player.getName()))
                .color(NamedTextColor.AQUA));
        }
    }
    
    private void givePoints(UUID playerId, long amount) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            PointsService pointsService = rpgCore.getServiceRegistry().getService(PointsService.class);
            if (pointsService != null) {
                pointsService.addBalance(playerId, amount, "月卡每日奖励");
            }
        }
    }
    
    private void giveMoney(UUID playerId, double amount) {
        if (plugin.getExternalServices() != null && plugin.getExternalServices().isVaultEnabled()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getExternalServices().deposit(player, amount);
            }
        }
    }
    
    public long getRemainingDays(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.getRemainingDays();
    }
    
    public int getTotalClaimedDays(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.getTotalClaimedDays();
    }
    
    public void extendCard(UUID playerId, int additionalDays) {
        MonthlyCardData data = getPlayerData(playerId);
        
        if (!data.hasActiveCard() && "none".equals(data.getCardType())) {
            return;
        }
        
        long newExpireTime = data.getExpireTime() + (additionalDays * 24L * 60 * 60 * 1000);
        data.setExpireTime(newExpireTime);
        
        savePlayerData(playerId);
    }
    
    public void setCard(UUID playerId, String cardTypeId, int durationDays) {
        MonthlyCardData data = getPlayerData(playerId);
        
        long now = System.currentTimeMillis();
        long expireTime = now + (durationDays * 24L * 60 * 60 * 1000);
        
        data.setCardType(cardTypeId);
        data.setActivateTime(now);
        data.setExpireTime(expireTime);
        data.getClaimedDays().clear();
        data.setTotalClaimedDays(0);
        
        savePlayerData(playerId);
    }
    
    public void removeCard(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        data.clear();
        savePlayerData(playerId);
    }
    
    public void checkExpiredCards() {
        long now = System.currentTimeMillis();
        for (MonthlyCardData data : playerDataCache.values()) {
            if (data.isExpired()) {
                Player player = Bukkit.getPlayer(data.getPlayerId());
                if (player != null) {
                    player.sendMessage(Component.text("你的月卡已过期")
                        .color(NamedTextColor.RED));
                }
                data.clear();
                savePlayerData(data.getPlayerId());
            }
        }
    }
}
