package cn.guangdian.monthlycard.manager;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.DailyReward;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.monthlycard.database.DatabaseManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.PointsService;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 月卡管理器 - 完整功能版本
 * 
 * 功能:
 * - SQLite数据存储
 * - 补签机制
 * - 累计奖励(7/14/21/30天)
 * - 特权系统(经验/掉落加成)
 * - 自动续期
 * - 数据统计
 */
public class MonthlyCardManager {

    private final GuangDianMonthlyCard plugin;
    private final Map<String, MonthlyCardType> cardTypes;
    private DatabaseManager databaseManager;
    private final File legacyDataFile;
    private MiniMessageService miniMessage;
    
    private static final int[] MILESTONE_DAYS = {7, 14, 21, 30};
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    private int dailyNewCards = 0;
    private int dailyRenewedCards = 0;
    private long dailyRevenuePoints = 0;
    private double dailyRevenueMoney = 0;
    private int dailyClaims = 0;

    public MonthlyCardManager(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.cardTypes = new ConcurrentHashMap<>();
        this.legacyDataFile = new File(plugin.getDataFolder(), "data.yml");
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.miniMessage = rpgCore.getMiniMessageService();
        } else {
            this.miniMessage = MiniMessageService.getInstance();
        }
    }

    public void init() {
        databaseManager = new DatabaseManager(plugin);
        try {
            databaseManager.init();
            migrateFromYaml();
            initializeCache();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MonthlyCardManager] 数据库初始化失败: " + e.getMessage());
        }
    }
    
    private void initializeCache() {
        plugin.getLogger().info("[MonthlyCardManager] 缓存将通过 CacheProvider 管理");
    }

    private void migrateFromYaml() {
        if (!legacyDataFile.exists()) return;
        
        if (databaseManager.tableExists("monthly_card_data")) {
            int count = databaseManager.getActiveCardCount();
            if (count > 0) {
                plugin.getLogger().info("[MonthlyCardManager] 数据已迁移，跳过迁移步骤");
                return;
            }
        }

        plugin.getLogger().info("[MonthlyCardManager] 开始从YAML迁移数据到SQLite...");

        try {
            YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(legacyDataFile);
            ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");

            if (playersSection == null) {
                plugin.getLogger().info("[MonthlyCardManager] 没有旧数据需要迁移");
                return;
            }

            int migratedCount = 0;
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    String path = "players." + uuidStr;

                    String cardType = dataConfig.getString(path + ".card-type", "none");
                    if ("none".equals(cardType)) continue;

                    long activateTime = dataConfig.getLong(path + ".activate-time", 0);
                    long expireTime = dataConfig.getLong(path + ".expire-time", 0);
                    int totalClaimedDays = dataConfig.getInt(path + ".total-claimed-days", 0);
                    long lastClaimTime = dataConfig.getLong(path + ".last-claim-time", 0);

                    Set<String> claimedDays = new HashSet<>(dataConfig.getStringList(path + ".claimed-days"));

                    MonthlyCardData data = MonthlyCardData.fromStorage(
                        playerId, cardType, activateTime, expireTime,
                        claimedDays, totalClaimedDays, lastClaimTime
                    );

                    databaseManager.savePlayerData(data);

                    for (String claimDate : claimedDays) {
                        databaseManager.recordClaim(playerId, claimDate, 0, 0, 0);
                    }

                    migratedCount++;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[MonthlyCardManager] 无效的UUID: " + uuidStr);
                }
            }

            plugin.getLogger().info("[MonthlyCardManager] 成功迁移 " + migratedCount + " 条玩家数据");

            File backupFile = new File(plugin.getDataFolder(), "data.yml.backup");
            if (legacyDataFile.renameTo(backupFile)) {
                plugin.getLogger().info("[MonthlyCardManager] 旧数据文件已备份为 data.yml.backup");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[MonthlyCardManager] 数据迁移失败: " + e.getMessage());
        }
    }

    public void loadCardTypes() {
        cardTypes.clear();
        
        // 从独立的cards.yml加载
        org.bukkit.configuration.file.YamlConfiguration cardsConfig = plugin.getConfigManager().getCardsConfig();
        
        if (cardsConfig == null) {
            plugin.getLogger().warning("无法加载 cards.yml 配置文件");
            return;
        }

        for (String cardId : cardsConfig.getKeys(false)) {
            ConfigurationSection cardSection = cardsConfig.getConfigurationSection(cardId);
            if (cardSection != null) {
                MonthlyCardType type = MonthlyCardType.fromConfig(cardId, cardSection);
                cardTypes.put(cardId, type);
                plugin.getLogger().info("加载月卡类型: " + cardId + " - " + type.getDisplayName());
            }
        }
        
        plugin.getLogger().info("共加载 " + cardTypes.size() + " 种月卡类型");
    }

    public void loadPlayerData(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        int actualMakeupCount = databaseManager.getMakeupCount(playerId);
        data.setMakeupCount(actualMakeupCount);
    }

    public void savePlayerData(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        if (data == null) return;
        databaseManager.savePlayerData(data);
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CacheProvider cacheProvider = rpgCore.getCacheProvider();
            String cacheKey = "monthlycard:playerdata:" + playerId.toString();
            cacheProvider.invalidate(cacheKey);
        }
    }

    public void saveAllData() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CacheProvider cacheProvider = rpgCore.getCacheProvider();
            cacheProvider.invalidatePattern("monthlycard:playerdata:*");
        }
        databaseManager.recordDailyStats(dailyNewCards, dailyRenewedCards, dailyRevenuePoints, dailyRevenueMoney, dailyClaims);
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CacheProvider cacheProvider = rpgCore.getCacheProvider();
            String cacheKey = "monthlycard:playerdata:" + playerId.toString();
            cacheProvider.invalidate(cacheKey);
        }
    }

    public void shutdown() {
        saveAllData();
        databaseManager.close();
    }

    public Optional<MonthlyCardType> getCardType(String typeId) {
        return Optional.ofNullable(cardTypes.get(typeId));
    }

    public List<MonthlyCardType> getAllCardTypes() {
        return new ArrayList<>(cardTypes.values());
    }

    public MonthlyCardData getPlayerData(UUID playerId) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            plugin.getLogger().warning("[MonthlyCardManager] RPGCore 未启用，直接从数据库加载");
            return databaseManager.loadPlayerData(playerId);
        }
        
        CacheProvider cacheProvider = rpgCore.getCacheProvider();
        String cacheKey = "monthlycard:playerdata:" + playerId.toString();
        
        return cacheProvider.getOrLoad(cacheKey, MonthlyCardData.class, 
            () -> databaseManager.loadPlayerData(playerId), CACHE_TTL);
    }

    public boolean hasActiveCard(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.hasActiveCard();
    }

    public boolean activateCard(UUID playerId, String cardTypeId) {
        MonthlyCardType type = cardTypes.get(cardTypeId);
        if (type == null) return false;

        MonthlyCardData data = getPlayerData(playerId);

        if (data.hasActiveCard() && data.getCardType().equals(cardTypeId)) {
            extendCard(playerId, type.getDurationDays());
            dailyRenewedCards++;
            return true;
        }

        long now = System.currentTimeMillis();
        long expireTime = now + (type.getDurationDays() * 24L * 60 * 60 * 1000);

        data.setCardType(cardTypeId);
        data.setActivateTime(now);
        data.setExpireTime(expireTime);
        data.getClaimedDays().clear();
        data.setTotalClaimedDays(0);
        data.setConsecutiveDays(0);
        data.setMakeupCount(0);

        databaseManager.savePlayerData(data);
        dailyNewCards++;

        return true;
    }

    public boolean canClaimToday(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        return data.canClaimToday();
    }

    public boolean claimDailyReward(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);

        if (!data.hasActiveCard()) return false;
        if (!data.canClaimToday()) return false;

        MonthlyCardType type = cardTypes.get(data.getCardType());
        if (type == null) return false;

        int day = data.getDaysSinceActivation();
        DailyReward reward = type.getRewardForDay(day);

        String today = LocalDate.now().toString();
        long points = reward != null ? reward.getPoints() : 0;
        double money = reward != null ? reward.getMoney() : 0;

        // 先记录领取
        databaseManager.recordClaim(playerId, today, day, points, money, false);

        data.markClaimedToday();
        updateConsecutiveDays(data);
        databaseManager.savePlayerData(data);

        // 发放奖励
        if (reward != null && reward.hasAnyReward()) {
            giveReward(playerId, reward);
        }

        // 检查累计奖励
        checkMilestoneRewards(playerId, data.getTotalClaimedDays());

        dailyClaims++;
        return true;
    }

    /**
     * 补签功能
     */
    public boolean makeupClaim(UUID playerId, String targetDate) {
        MonthlyCardData data = getPlayerData(playerId);
        
        if (!data.hasActiveCard()) {
            return false;
        }

        // 检查补签次数
        int maxMakeup = plugin.getConfigManager().getMakeupConfig().getInt("limits.max-per-month", 3);
        if (data.getMakeupCount() >= maxMakeup) {
            return false;
        }

        // 检查日期是否有效（必须是过去的日期且未领取）
        LocalDate target = LocalDate.parse(targetDate);
        LocalDate today = LocalDate.now();
        
        if (!target.isBefore(today)) {
            return false; // 只能补签过去的日期
        }
        
        if (data.getClaimedDays().contains(targetDate)) {
            return false; // 已经领取过
        }

        // 检查是否在月卡有效期内
        LocalDate activationDate = LocalDate.ofEpochDay(data.getActivateTime() / (24 * 60 * 60 * 1000));
        LocalDate expireDate = LocalDate.ofEpochDay(data.getExpireTime() / (24 * 60 * 60 * 1000));
        
        if (target.isBefore(activationDate) || target.isAfter(expireDate)) {
            return false; // 不在有效期内
        }

        // 扣费 - 从makeup.yml读取
        org.bukkit.configuration.file.YamlConfiguration makeupConfig = plugin.getConfigManager().getMakeupConfig();
        String costType = makeupConfig.getString("cost.type", "points");
        long costAmount = makeupConfig.getLong("cost.amount", 500);
        
        // 根据月卡类型应用倍率
        double multiplier = makeupConfig.getDouble("cost.multipliers." + data.getCardType(), 1.0);
        long costPoints = "points".equals(costType) ? (long) (costAmount * multiplier) : 0;
        double costMoney = "money".equals(costType) ? (costAmount * multiplier) : 0;
        
        boolean paid = false;
        if (costPoints > 0) {
            paid = deductPoints(playerId, costPoints);
        } else if (costMoney > 0) {
            paid = deductMoney(playerId, costMoney);
        }
        
        if (!paid) {
            return false;
        }

        // 计算是第几天
        int day = (int) ChronoUnit.DAYS.between(activationDate, target) + 1;
        MonthlyCardType type = cardTypes.get(data.getCardType());
        DailyReward reward = type != null ? type.getRewardForDay(day) : null;
        
        long points = reward != null ? reward.getPoints() : 0;
        double money = reward != null ? reward.getMoney() : 0;

        // 记录补签
        databaseManager.recordClaim(playerId, targetDate, day, points, money, true);
        
        data.getClaimedDays().add(targetDate);
        data.setTotalClaimedDays(data.getTotalClaimedDays() + 1);
        data.setMakeupCount(data.getMakeupCount() + 1);
        databaseManager.savePlayerData(data);

        // 发放奖励
        if (reward != null && reward.hasAnyReward()) {
            giveReward(playerId, reward);
        }

        // 检查累计奖励
        checkMilestoneRewards(playerId, data.getTotalClaimedDays());

        return true;
    }

    /**
     * 检查并发放累计奖励
     */
    private void checkMilestoneRewards(UUID playerId, int totalDays) {
        for (int milestone : MILESTONE_DAYS) {
            if (totalDays >= milestone && !databaseManager.hasClaimedMilestone(playerId, milestone)) {
                giveMilestoneReward(playerId, milestone);
                databaseManager.recordMilestoneClaim(playerId, milestone);
            }
        }
    }

    /**
     * 发放累计奖励
     */
    private void giveMilestoneReward(UUID playerId, int milestoneDay) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        MonthlyCardData data = getPlayerData(playerId);
        if (!data.hasActiveCard()) return;
        
        // 从cards.yml读取对应月卡类型的累计奖励
        org.bukkit.configuration.file.YamlConfiguration cardsConfig = plugin.getConfigManager().getCardsConfig();
        String path = data.getCardType() + ".milestones." + milestoneDay;
        
        if (!cardsConfig.contains(path)) {
            return;
        }

        long points = cardsConfig.getLong(path + ".points", 0);
        double money = cardsConfig.getDouble(path + ".money", 0);
        List<String> commands = cardsConfig.getStringList(path + ".commands");

        if (points > 0) {
            givePoints(playerId, points);
        }
        
        if (money > 0) {
            giveMoney(playerId, money);
        }
        
        // 执行命令
        for (String cmd : commands) {
            String parsedCmd = cmd.replace("%player%", player.getName())
                                 .replace("%day%", String.valueOf(milestoneDay));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }

        // 发送消息
        String message = plugin.getConfigManager().getSuccessMessage("milestone")
            .replace("%day%", String.valueOf(milestoneDay))
            .replace("%points%", String.valueOf(points));
        player.sendMessage(miniMessage.colorize(message));
    }

    private void updateConsecutiveDays(MonthlyCardData data) {
        String yesterday = LocalDate.now().minusDays(1).toString();
        if (data.getClaimedDays().contains(yesterday)) {
            data.setConsecutiveDays(data.getConsecutiveDays() + 1);
        } else {
            data.setConsecutiveDays(1);
        }
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
                pointsService.addBalance(playerId, amount, "月卡奖励");
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
        databaseManager.savePlayerData(data);
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
        data.setConsecutiveDays(0);
        data.setMakeupCount(0);

        databaseManager.savePlayerData(data);
    }

    public void removeCard(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        data.clear();
        databaseManager.savePlayerData(data);
    }

    public void checkExpiredCards() {
        long now = System.currentTimeMillis();
        org.bukkit.configuration.file.YamlConfiguration autoRenewConfig = plugin.getConfigManager().getAutoRenewConfig();
        int reminderDays = autoRenewConfig.getInt("reminder.days-before", 3);
        long reminderTime = reminderDays * 24L * 60 * 60 * 1000;
        
        List<UUID> allPlayers = databaseManager.getAllPlayerUUIDs();
        for (UUID playerId : allPlayers) {
            MonthlyCardData data = getPlayerData(playerId);
            if (data.isExpired()) {
                Player player = Bukkit.getPlayer(data.getPlayerId());
                if (player != null) {
                    player.sendMessage(Component.text("你的月卡已过期")
                        .color(NamedTextColor.RED));
                }
                data.clear();
                databaseManager.savePlayerData(data);
            } else if (data.getExpireTime() - now <= reminderTime && data.getExpireTime() > now) {
                Player player = Bukkit.getPlayer(data.getPlayerId());
                if (player != null) {
                    long daysLeft = (data.getExpireTime() - now) / (24 * 60 * 60 * 1000);
                    MonthlyCardType cardType = cardTypes.get(data.getCardType());
                    String cardName = cardType != null ? cardType.getDisplayName() : data.getCardType();
                    
                    String reminderMsg = plugin.getConfigManager().getAutoRenewConfig()
                        .getString("messages.reminder", "<yellow>你的<gold>%card%<yellow>将在<red>%days%<yellow>天后到期，请及时续期")
                        .replace("%card%", cardName)
                        .replace("%days%", String.valueOf(daysLeft));
                    player.sendMessage(miniMessage.colorize(reminderMsg));
                }
            }
        }
    }

    public DailyReward getRewardForDay(int day) {
        for (MonthlyCardType type : cardTypes.values()) {
            DailyReward reward = type.getRewardForDay(day);
            if (reward != null) return reward;
        }
        return null;
    }

    public boolean purchaseCard(UUID playerId, String cardTypeId) {
        MonthlyCardType type = cardTypes.get(cardTypeId);
        if (type == null) return false;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;

        long price = calculateCardPrice(playerId, cardTypeId, type);
        PaymentResult payment = tryPaymentByPriority(playerId, price, type.getCurrency());
        
        if (!payment.success) return false;

        recordRevenue(payment.currency, payment.amount);
        return activateCard(playerId, cardTypeId);
    }
    
    private long calculateCardPrice(UUID playerId, String cardTypeId, MonthlyCardType type) {
        long price = type.getPrice();
        MonthlyCardData data = getPlayerData(playerId);
        
        if (data.hasActiveCard() && data.getCardType().equals(cardTypeId)) {
            org.bukkit.configuration.file.YamlConfiguration autoRenewConfig = plugin.getConfigManager().getAutoRenewConfig();
            int discount = autoRenewConfig.getInt("discount.by-card-type." + cardTypeId, 
                autoRenewConfig.getInt("discount.percent", 10));
            price = price * (100 - discount) / 100;
        }
        
        return price;
    }
    
    private static class PaymentResult {
        boolean success;
        String currency;
        long amount;
        
        PaymentResult(boolean success, String currency, long amount) {
            this.success = success;
            this.currency = currency;
            this.amount = amount;
        }
    }
    
    private PaymentResult tryPaymentByPriority(UUID playerId, long price, String defaultCurrency) {
        org.bukkit.configuration.file.YamlConfiguration autoRenewConfig = plugin.getConfigManager().getAutoRenewConfig();
        List<String> priority = autoRenewConfig.getStringList("auto-deduct.priority");
        if (priority.isEmpty()) {
            priority = List.of(defaultCurrency);
        }
        
        for (String currency : priority) {
            if ("points".equalsIgnoreCase(currency)) {
                if (deductPoints(playerId, price)) {
                    return new PaymentResult(true, "points", price);
                }
            } else if ("money".equalsIgnoreCase(currency) || "vault".equalsIgnoreCase(currency)) {
                if (deductMoney(playerId, price)) {
                    return new PaymentResult(true, "money", price);
                }
            }
        }
        
        return new PaymentResult(false, null, 0);
    }
    
    private void recordRevenue(String currency, long amount) {
        if ("points".equalsIgnoreCase(currency)) {
            dailyRevenuePoints += amount;
        } else {
            dailyRevenueMoney += amount;
        }
    }

    private boolean deductPoints(UUID playerId, long amount) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            PointsService pointsService = rpgCore.getServiceRegistry().getService(PointsService.class);
            if (pointsService != null) {
                long balance = pointsService.getBalance(playerId);
                if (balance >= amount) {
                    pointsService.addBalance(playerId, -amount, "购买月卡");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean deductMoney(UUID playerId, double amount) {
        if (plugin.getExternalServices() != null && plugin.getExternalServices().isVaultEnabled()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                double balance = plugin.getExternalServices().getBalance(player);
                if (balance >= amount) {
                    plugin.getExternalServices().withdraw(player, amount);
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 特权系统 ====================

    /**
     * 获取玩家经验加成倍数
     */
    public double getExpBoost(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        if (!data.hasActiveCard()) return 1.0;

        // 从cards.yml读取对应月卡类型的经验加成
        org.bukkit.configuration.file.YamlConfiguration cardsConfig = plugin.getConfigManager().getCardsConfig();
        String path = data.getCardType() + ".boost.exp";
        return cardsConfig.getDouble(path, 1.0);
    }

    /**
     * 获取玩家掉落加成倍数
     */
    public double getDropBoost(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        if (!data.hasActiveCard()) return 1.0;

        // 从cards.yml读取对应月卡类型的掉落加成
        org.bukkit.configuration.file.YamlConfiguration cardsConfig = plugin.getConfigManager().getCardsConfig();
        String path = data.getCardType() + ".boost.drop";
        return cardsConfig.getDouble(path, 1.0);
    }

    /**
     * 获取玩家特权前缀
     */
    public String getPrivilegePrefix(UUID playerId) {
        MonthlyCardData data = getPlayerData(playerId);
        if (!data.hasActiveCard()) return "";

        // 从cards.yml读取月卡显示名称作为前缀
        org.bukkit.configuration.file.YamlConfiguration cardsConfig = plugin.getConfigManager().getCardsConfig();
        return cardsConfig.getString(data.getCardType() + ".name", "");
    }

    // ==================== 统计方法 ====================

    public int getActiveCardCount() {
        return databaseManager.getActiveCardCount();
    }

    public int getTodayClaimCount() {
        return databaseManager.getTodayClaimCount();
    }

    public long getTotalRevenuePoints() {
        return databaseManager.getTotalRevenuePoints();
    }

    public double getTotalRevenueMoney() {
        return databaseManager.getTotalRevenueMoney();
    }

    public int getMonthlyNewCards() {
        return databaseManager.getMonthlyNewCards();
    }

    /**
     * 获取可补签的日期列表
     */
    public List<String> getMissedDays(UUID playerId) {
        List<String> missedDays = new ArrayList<>();
        MonthlyCardData data = getPlayerData(playerId);
        
        if (!data.hasActiveCard()) return missedDays;

        LocalDate today = LocalDate.now();
        LocalDate activationDate = LocalDate.ofEpochDay(data.getActivateTime() / (24 * 60 * 60 * 1000));
        
        for (LocalDate date = activationDate; date.isBefore(today); date = date.plusDays(1)) {
            String dateStr = date.toString();
            if (!data.getClaimedDays().contains(dateStr)) {
                missedDays.add(dateStr);
            }
        }
        
        return missedDays;
    }
}