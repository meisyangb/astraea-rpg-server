package cn.guangdian.market;

import cn.guangdian.market.adapter.MarketServiceAdapter;
import cn.guangdian.market.gui.MarketGUI;
import cn.guangdian.market.lifecycle.MarketDataHandler;
import cn.guangdian.market.placeholder.MarketPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.points.GuangDianPoints.PointsAPI;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuangDianMarket extends AbstractRPGPlugin implements Listener, TabCompleter {

    private static GuangDianMarket instance;
    private FileConfiguration config;
    private File dataFile;
    private YamlConfiguration data;

    final Map<UUID, List<MarketItem>> playerListings = new ConcurrentHashMap<>();
    final List<MarketItem> globalMarket = new CopyOnWriteArrayList<>();
    private final Map<UUID, MarketItem> marketIndex = new ConcurrentHashMap<>();
    private final Map<UUID, MarketGUI> openGUIs = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> offlineReturns = new ConcurrentHashMap<>();
    private final Set<UUID> searchModePlayers = ConcurrentHashMap.newKeySet(); // 搜索模式玩家

    private long transactionFee;
    private double feePercent;
    private int maxListingsPerPlayer;
    private int listingDuration;
    private int itemsPerPage;
    private PointsAPI pointsAPI;
    private Economy economy; // Vault经济系统
    private MarketServiceAdapter serviceAdapter;
    private MarketDataHandler dataHandler;
    private long autoSaveTaskId = -1;
    private long expireCheckTaskId = -1;
    private boolean pointsEnabled; // 点券是否启用
    private boolean economyEnabled; // 经济是否启用
    
    // RPGCore 服务引用
    private SoundService soundService;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        saveDefaultConfig();
        config = getConfig();
        
        // 初始化 RPGCore 服务
        initRPGCoreServices();
        
        loadData();
        loadSettings();
        registerEvents();
        setupPoints();
        setupEconomy(); // 初始化Vault经济系统
        startTasks();
        // 注册RPGCore服务适配器
        serviceAdapter = new MarketServiceAdapter(this);

        // 注册PlaceholderAPI扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MarketPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }

        getLogger().info("光点全球市场插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
        getLogger().info("点券系统: " + (pointsEnabled ? "已启用" : "未启用"));
        getLogger().info("经济系统: " + (economyEnabled ? "已启用" : "未启用"));
    }

    @Override
    protected void onPluginDisable() {
        // 取消定时任务
        if (autoSaveTaskId != -1 && scheduler != null) {
            scheduler.cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
        if (expireCheckTaskId != -1 && scheduler != null) {
            scheduler.cancelTask(expireCheckTaskId);
            expireCheckTaskId = -1;
        }

        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        saveData();
        // 注销RPGCore服务适配器
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        getLogger().info("光点全球市场插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianMarket";
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        if (data.contains("market")) {
            List<Map<?, ?>> items = data.getMapList("market");
            for (Map<?, ?> item : items) {
                try {
                    MarketItem mi = MarketItem.fromMap(item);
                    if (mi != null && !mi.isExpired()) {
                        globalMarket.add(mi);
                        marketIndex.put(mi.id, mi);
                        playerListings.computeIfAbsent(mi.seller, k -> new ArrayList<>()).add(mi);
                    }
                } catch (Exception e) {
                    getLogger().warning("加载市场物品失败: " + e.getMessage());
                }
            }
        }
    }

    private void saveData() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (MarketItem item : globalMarket) {
            if (!item.isExpired()) {
                items.add(item.toMap());
            }
        }
        data.set("market", items);
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("保存数据失败: " + e.getMessage());
        }
    }

    private void initRPGCoreServices() {
        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                soundService = rpgCore.getSoundService();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore 服务 (SoundService, MiniMessageService)");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore 服务: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    private void loadSettings() {
        transactionFee = config.getLong("settings.transaction-fee", 0);
        feePercent = config.getDouble("settings.fee-percent", 5.0);
        maxListingsPerPlayer = config.getInt("settings.max-listings-per-player", 10);
        listingDuration = config.getInt("settings.listing-duration-hours", 168) * 3600 * 1000;
        itemsPerPage = config.getInt("settings.items-per-page", 45);
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("market").setTabCompleter(this);
    }

    private void startTasks() {
        if (scheduler != null) {
            autoSaveTaskId = scheduler.runSyncRepeating(this::saveData, 6000L, 6000L);

            expireCheckTaskId = scheduler.runSyncRepeating(() -> {
                long now = System.currentTimeMillis();
                globalMarket.removeIf(item -> {
                    if (item.isExpired()) {
                        returnExpiredItem(item);
                        marketIndex.remove(item.id);
                        // ✅ 优化：避免创建新ArrayList
                        List<MarketItem> listings = playerListings.get(item.seller);
                        if (listings != null) {
                            listings.remove(item);
                        }
                        return true;
                    }
                    return false;
                });
            }, 12000L, 12000L);
        } else {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSyncRepeating(this::saveData, 6000L, 6000L);
                rpgCore.getScheduler().runSyncRepeating(() -> {
                    long now = System.currentTimeMillis();
                    globalMarket.removeIf(item -> {
                        if (item.isExpired()) {
                        returnExpiredItem(item);
                        marketIndex.remove(item.id);
                        // ✅ 优化：避免创建新ArrayList
                        List<MarketItem> listings = playerListings.get(item.seller);
                        if (listings != null) {
                            listings.remove(item);
                        }
                        return true;
                    }
                    return false;
                });
            }, 12000L, 12000L);
            }
        }
    }

    public static GuangDianMarket getInstance() {
        return instance;
    }

    // ==================== 公开API方法（供RPGCore服务调用） ====================

    /**
     * 获取市场物品总数
     * 
     * @return 市场物品数量
     */
    public int getMarketSizeAPI() {
        return globalMarket.size();
    }

    /**
     * 获取玩家上架物品数量
     * 
     * @param sellerId 玩家UUID
     * @return 上架物品数量
     */
    public int getPlayerListingCountAPI(UUID sellerId) {
        List<MarketItem> listings = playerListings.get(sellerId);
        return listings != null ? listings.size() : 0;
    }

    /**
     * 获取玩家上架物品列表
     * 
     * @param sellerId 玩家UUID
     * @return 上架物品列表
     */
    public List<MarketItem> getPlayerListingsAPI(UUID sellerId) {
        List<MarketItem> listings = playerListings.get(sellerId);
        return listings != null ? new ArrayList<>(listings) : new ArrayList<>();
    }

    /**
     * 获取全部市场物品
     * 
     * @return 市场物品列表
     */
    public List<MarketItem> getAllMarketItemsAPI() {
        return new ArrayList<>(globalMarket);
    }

    /**
     * 通过UUID上架物品
     * 
     * @param sellerId 卖家UUID
     * @param item 物品
     * @param price 价格
     * @return 是否成功
     */
    public boolean listItemAPI(UUID sellerId, ItemStack item, long price) {
        Player player = Bukkit.getPlayer(sellerId);
        if (player == null) return false;
        return listItem(player, item, price);
    }

    /**
     * 取消上架 - API方法（带防重复检查）
     *
     * @param sellerId 卖家UUID
     * @param listingId 上架ID
     * @return 是否成功
     */
    public boolean cancelListingAPI(UUID sellerId, UUID listingId) {
        // ✅ 防止重复下架：先检查物品是否存在
        MarketItem item = marketIndex.get(listingId);
        if (item == null || !item.seller.equals(sellerId)) {
            getLogger().warning("API调用: 下架失败，物品不存在或不属于该玩家 ID: " + listingId);
            return false;
        }

        // ✅ 从所有集合中移除物品（原子操作）
        boolean removedFromMarket = globalMarket.remove(item);
        marketIndex.remove(item.id);

        List<MarketItem> listings = playerListings.get(sellerId);
        if (listings != null) {
            listings.remove(item);
        }

        // ✅ 只有成功移除后才返还物品
        if (removedFromMarket) {
            Player player = Bukkit.getPlayer(sellerId);
            if (player != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.item.clone());
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                getLogger().info("API调用: 玩家 " + player.getName() + " 成功下架物品 ID: " + listingId);
            }
            return true;
        } else {
            getLogger().warning("API调用: 下架失败，物品已被重复处理 ID: " + listingId);
            return false;
        }
    }

    /**
     * 购买市场物品 - API方法
     *
     * @param buyerId 买家UUID
     * @param listingId 上架物品ID
     * @return 是否成功
     */
    public boolean purchaseItemAPI(UUID buyerId, UUID listingId) {
        Player buyer = Bukkit.getPlayer(buyerId);
        if (buyer == null) return false;

        MarketItem item = marketIndex.get(listingId);
        if (item != null) {
            return purchaseItem(buyer, item);
        }
        return false;
    }

    /**
     * 获取待领取的过期物品数量 - API方法
     *
     * @param playerId 玩家UUID
     * @return 待领取物品数量
     */
    public int getPendingReturnsCount(UUID playerId) {
        List<ItemStack> returns = offlineReturns.get(playerId);
        return returns != null ? returns.size() : 0;
    }

    /**
     * 获取玩家最大上架数量 - API方法
     *
     * @return 最大上架数量
     */
    public int getMaxListingsPerPlayer() {
        return maxListingsPerPlayer;
    }

    /**
     * 获取手续费百分比 - API方法
     *
     * @return 手续费百分比
     */
    public double getFeePercent() {
        return feePercent;
    }

    /**
     * 获取市场第N贵物品价格 - API方法
     *
     * @param index 索引 (0开始)
     * @return 价格，不存在返回0
     */
    public long getTopPrice(int index) {
        if (index < 0 || globalMarket.isEmpty()) return 0;

        // 按价格降序排序
        List<MarketItem> sorted = new ArrayList<>(globalMarket);
        sorted.sort((a, b) -> Long.compare(b.price, a.price));

        if (index < sorted.size()) {
            return sorted.get(index).price;
        }
        return 0;
    }

    /**
     * 获取市场第N贵物品名称 - API方法
     *
     * @param index 索引 (0开始)
     * @return 物品名称，不存在返回 "-"
     */
    public String getTopItemName(int index) {
        if (index < 0 || globalMarket.isEmpty()) return "-";

        // 按价格降序排序
        List<MarketItem> sorted = new ArrayList<>(globalMarket);
        sorted.sort((a, b) -> Long.compare(b.price, a.price));

        if (index < sorted.size()) {
            ItemStack item = sorted.get(index).item;
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }
            // 返回物品类型名称
            return item.getType().name();
        }
        return "-";
    }

    private void setupPoints() {
        // 检查配置是否启用点券
        if (!config.getBoolean("currency.points.enabled", true)) {
            getLogger().info("点券系统已在配置中禁用");
            pointsEnabled = false;
            return;
        }
        
        GuangDianPoints pointsPlugin = (GuangDianPoints) Bukkit.getPluginManager().getPlugin("GuangDianPoints");
        if (pointsPlugin != null) {
            RegisteredServiceProvider<PointsAPI> provider = Bukkit.getServicesManager().getRegistration(PointsAPI.class);
            if (provider != null) {
                pointsAPI = provider.getProvider();
                pointsEnabled = true;
                getLogger().info("已连接到点券系统!");
                return;
            }
        }
        pointsEnabled = false;
        getLogger().warning("未找到 GuangDianPoints 插件! 点券功能将无法使用!");
    }
    
    /**
     * 初始化Vault经济系统
     */
    private void setupEconomy() {
        // 检查配置是否启用经济
        if (!config.getBoolean("currency.economy.enabled", true)) {
            getLogger().info("经济系统已在配置中禁用");
            economyEnabled = false;
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economyEnabled = false;
            getLogger().warning("未找到 Vault 插件! 经济功能将无法使用!");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            economyEnabled = true;
            getLogger().info("已连接到Vault经济系统! 经济插件: " + economy.getName());
        } else {
            economyEnabled = false;
            getLogger().warning("未找到经济服务! 经济功能将无法使用!");
        }
    }

    // ==================== 货币操作方法 ====================
    
    /**
     * 获取玩家点券余额
     */
    public long getPointsBalance(UUID uuid) {
        if (pointsAPI != null && pointsEnabled) {
            return pointsAPI.getBalance(uuid);
        }
        return 0;
    }
    
    /**
     * 获取玩家经济余额
     */
    public double getEconomyBalance(UUID uuid) {
        if (economy != null && economyEnabled) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return economy.getBalance(player);
            }
        }
        return 0;
    }
    
    /**
     * 扣除点券
     */
    private boolean removePointsBalance(UUID uuid, long amount) {
        if (pointsAPI != null && pointsEnabled) {
            return pointsAPI.removeBalance(uuid, amount);
        }
        return false;
    }
    
    /**
     * 扣除经济
     */
    private boolean removeEconomyBalance(UUID uuid, double amount) {
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null && rpgCore.getExternalServices() != null && rpgCore.getExternalServices().isVaultEnabled()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return rpgCore.getExternalServices().withdraw(player, amount);
            }
        }
        return false;
    }
    
    /**
     * 增加点券
     */
    private void addPointsBalance(UUID uuid, long amount) {
        if (pointsAPI != null && pointsEnabled) {
            pointsAPI.addBalance(uuid, amount);
        }
    }
    
    /**
     * 增加经济
     */
    private void addEconomyBalance(UUID uuid, double amount) {
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null && rpgCore.getExternalServices() != null && rpgCore.getExternalServices().isVaultEnabled()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                rpgCore.getExternalServices().deposit(player, amount);
            }
        }
    }
    
    /**
     * 检查玩家是否有足够货币
     */
    private boolean hasEnoughCurrency(UUID uuid, CurrencyType type, double amount) {
        if (type == CurrencyType.POINTS) {
            return getPointsBalance(uuid) >= amount;
        } else {
            return getEconomyBalance(uuid) >= amount;
        }
    }
    
    /**
     * 扣除货币
     */
    private boolean removeCurrency(UUID uuid, CurrencyType type, double amount) {
        if (type == CurrencyType.POINTS) {
            return removePointsBalance(uuid, (long) amount);
        } else {
            return removeEconomyBalance(uuid, amount);
        }
    }
    
    /**
     * 增加货币
     */
    private void addCurrency(UUID uuid, CurrencyType type, double amount) {
        if (type == CurrencyType.POINTS) {
            addPointsBalance(uuid, (long) amount);
        } else {
            addEconomyBalance(uuid, amount);
        }
    }
    
    /**
     * 获取货币名称
     */
    private String getCurrencyName(CurrencyType type) {
        if (type == CurrencyType.POINTS) {
            return config.getString("currency.points.name", "点券");
        } else {
            return config.getString("currency.economy.name", "金币");
        }
    }
    
    /**
     * 格式化货币金额
     */
    private String formatCurrency(CurrencyType type, double amount) {
        if (type == CurrencyType.POINTS) {
            return formatNumber((long) amount);
        } else {
            if (economy != null) {
                return economy.format(amount);
            }
            return String.format("%.2f", amount);
        }
    }

    // 保留旧方法以兼容现有代码
    private long getBalance(UUID uuid) {
        return getPointsBalance(uuid);
    }

    private boolean removeBalance(UUID uuid, long amount) {
        return removePointsBalance(uuid, amount);
    }

    private void addBalance(UUID uuid, long amount) {
        addPointsBalance(uuid, amount);
    }

    public boolean listItem(Player player, ItemStack item, long price) {
        // 默认使用点券上架
        return listItem(player, item, price, CurrencyType.POINTS);
    }
    
    /**
     * 上架物品（支持双货币）
     */
    public boolean listItem(Player player, ItemStack item, long price, CurrencyType currencyType) {
        UUID uuid = player.getUniqueId();
        List<MarketItem> listings = playerListings.getOrDefault(uuid, new ArrayList<>());
        
        if (listings.size() >= maxListingsPerPlayer) {
            String msgTemplate = config.getString("messages.max-listings-reached", "<red>你已达到最大上架数量 (<gold><max><red>)!");
            player.sendMessage(miniMessage.parseWithPlaceholders(msgTemplate, "max", String.valueOf(maxListingsPerPlayer)));
            return false;
        }
        
        if (price <= 0) {
            player.sendMessage(colorize(config.getString("messages.invalid-price", "<red>无效的价格!")));
            return false;
        }
        
        String currencyName = getCurrencyName(currencyType);
        
        MarketItem marketItem = new MarketItem(
            UUID.randomUUID(),
            uuid,
            player.getName(),
            item.clone(),
            price,
            System.currentTimeMillis() + listingDuration,
            currencyType
        );
        
        globalMarket.add(marketItem);
        marketIndex.put(marketItem.id, marketItem);
        listings.add(marketItem);
        playerListings.put(uuid, listings);
        
        player.getInventory().removeItem(item);
        
        String msgTemplate = config.getString("messages.item-listed", "<green>成功上架物品! 价格: <price> <currency>");
        player.sendMessage(miniMessage.parseWithMultiplePlaceholders(msgTemplate,
            "price", formatCurrency(currencyType, price), "currency", currencyName));
        playSound(player, "listing-success");
        return true;
    }

    public boolean purchaseItem(Player buyer, MarketItem item) {
        if (item.seller.equals(buyer.getUniqueId())) {
            buyer.sendMessage(colorize(config.getString("messages.cannot-buy-own", "<red>不能购买自己的物品!")));
            return false;
        }

        // ✅ 防止重复购买：检查物品是否仍在市场
        if (!marketIndex.containsKey(item.id)) {
            buyer.sendMessage(colorize("<red>该物品已售出或不存在!"));
            getLogger().warning("玩家 " + buyer.getName() + " 尝试购买不存在的物品 ID: " + item.id);
            return false;
        }

        CurrencyType currencyType = item.getCurrencyType();
        double price = item.price;
        double fee = transactionFee + (price * feePercent / 100);
        double totalCost = price + fee;
        String currencyName = getCurrencyName(currencyType);

        if (!hasEnoughCurrency(buyer.getUniqueId(), currencyType, totalCost)) {
            String msgTemplate = config.getString("messages.insufficient-funds", "<red><currency>不足!");
            buyer.sendMessage(miniMessage.parseWithPlaceholders(msgTemplate, "currency", currencyName));
            playSound(buyer, "purchase-fail");
            return false;
        }

        // ✅ 扣款前先从市场移除物品（防止并发购买）
        boolean removedFromMarket = globalMarket.remove(item);
        marketIndex.remove(item.id);

        List<MarketItem> listings = playerListings.get(item.seller);
        if (listings != null) {
            listings.remove(item);
        }

        // ✅ 只有成功移除物品后才扣款和给物品
        if (removedFromMarket) {
            removeCurrency(buyer.getUniqueId(), currencyType, totalCost);
            addCurrency(item.seller, currencyType, price);

            HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(item.item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                }
            }

            Player seller = Bukkit.getPlayer(item.seller);
            if (seller != null && seller.isOnline()) {
                String sellerMsgTemplate = config.getString("messages.item-sold", "<green>你的物品已售出! 获得 <price> <currency>");
                seller.sendMessage(miniMessage.parseWithMultiplePlaceholders(sellerMsgTemplate,
                    "price", formatCurrency(currencyType, price), "currency", currencyName));
            }

            String buyerMsgTemplate = config.getString("messages.purchase-success", "<green>购买成功! 花费 <price> <currency>");
            buyer.sendMessage(miniMessage.parseWithMultiplePlaceholders(buyerMsgTemplate,
                "price", formatCurrency(currencyType, totalCost), "currency", currencyName));
            playSound(buyer, "purchase-success");

            getLogger().info("玩家 " + buyer.getName() + " 成功购买物品 ID: " + item.id + " 价格: " + formatCurrency(currencyType, price));

            // ✅ 立即保存数据，防止数据不同步
            saveData();

            return true;
        } else {
            buyer.sendMessage(colorize("<red>购买失败: 物品已被他人购买!"));
            getLogger().warning("玩家 " + buyer.getName() + " 购买失败，物品已被并发处理 ID: " + item.id);
            return false;
        }
    }

    public void cancelListing(Player player, MarketItem item) {
        if (!item.seller.equals(player.getUniqueId())) {
            player.sendMessage(colorize(config.getString("messages.not-your-item", "<red>这不是你的物品!")));
            return;
        }

        // ✅ 防止重复下架：先检查物品是否仍在市场中
        if (!marketIndex.containsKey(item.id)) {
            player.sendMessage(colorize("<red>该物品已下架或不存在!"));
            getLogger().warning("玩家 " + player.getName() + " 尝试下架不存在的物品 ID: " + item.id);
            return;
        }

        // ✅ 从所有集合中移除物品（原子操作）
        boolean removedFromMarket = globalMarket.remove(item);
        marketIndex.remove(item.id);

        List<MarketItem> sellerListings = playerListings.get(item.seller);
        if (sellerListings != null) {
            sellerListings.remove(item);
        }

        // ✅ 只有当物品成功从市场移除后才返还给玩家
        if (removedFromMarket) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            player.sendMessage(colorize(config.getString("messages.listing-cancelled", "<green>已取消上架!")));
            getLogger().info("玩家 " + player.getName() + " 成功下架物品: " + item.item.getType() + " ID: " + item.id);

            // ✅ 立即保存数据，防止数据不同步
            saveData();
        } else {
            player.sendMessage(colorize("<red>下架失败: 物品已被处理!"));
            getLogger().warning("玩家 " + player.getName() + " 下架物品失败，物品可能已被重复处理 ID: " + item.id);
        }
    }

    private void returnExpiredItem(MarketItem item) {
        // ✅ 防止重复返还：检查物品是否已被移除
        if (!marketIndex.containsKey(item.id)) {
            getLogger().warning("过期返还失败: 物品已被处理 ID: " + item.id);
            return;
        }

        // ✅ 先从市场中移除物品
        boolean removedFromMarket = globalMarket.remove(item);
        marketIndex.remove(item.id);

        List<MarketItem> listings = playerListings.get(item.seller);
        if (listings != null) {
            listings.remove(item);
        }

        // ✅ 只有成功移除后才返还物品
        if (removedFromMarket) {
            Player player = Bukkit.getPlayer(item.seller);
            if (player != null && player.isOnline()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.item.clone());
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                player.sendMessage(colorize(config.getString("messages.item-expired", "<red>你的物品已过期并已返还!")));
                getLogger().info("过期物品返还给在线玩家: " + player.getName() + " ID: " + item.id);
            } else {
                offlineReturns.computeIfAbsent(item.seller, k -> new ArrayList<>()).add(item.item.clone());
                getLogger().info("过期物品暂存给离线玩家: " + item.sellerName + " ID: " + item.id);
            }
        } else {
            getLogger().warning("过期返还失败: 物品未成功从市场移除 ID: " + item.id);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> returns = offlineReturns.remove(player.getUniqueId());
        if (returns != null && !returns.isEmpty()) {
            for (ItemStack item : returns) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
            String msgTemplate = config.getString("messages.offline-returns", "<yellow>你有 <count> 个过期物品已返还!");
            player.sendMessage(miniMessage.parseWithPlaceholders(msgTemplate, "count", String.valueOf(returns.size())));
        }
    }

    public void openMarketGUI(Player player, int page) {
        MarketGUI gui = new MarketGUI(this, player);
        player.openInventory(gui.getInventory());
        openGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 设置搜索模式
     */
    public void setSearchMode(Player player, boolean enabled) {
        if (enabled) {
            searchModePlayers.add(player.getUniqueId());
        } else {
            searchModePlayers.remove(player.getUniqueId());
        }
    }
    
    /**
     * 是否在搜索模式
     */
    public boolean isInSearchMode(UUID playerId) {
        return searchModePlayers.contains(playerId);
    }
    
    /**
     * 获取全局市场列表
     */
    public List<MarketItem> getGlobalMarket() {
        return globalMarket;
    }
    
    /**
     * 获取玩家上架列表
     */
    public Map<UUID, List<MarketItem>> getPlayerListings() {
        return playerListings;
    }
    
    /**
     * 获取Economy实例
     */
    public Economy getEconomy() {
        return economy;
    }

    public void openMyListingsGUI(Player player) {
        List<MarketItem> listings = playerListings.getOrDefault(player.getUniqueId(), new ArrayList<>());
        
        Component title = colorize(config.getString("gui.my-listings-title", "<gold>我的上架"));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        for (int i = 0; i < Math.min(listings.size(), 45); i++) {
            MarketItem item = listings.get(i);
            ItemStack displayItem = createMyListingDisplayItem(item);
            inv.setItem(i, displayItem);
        }
        
        player.openInventory(inv);
    }

    public ItemStack createMarketDisplayItem(MarketItem item) {
        ItemStack display = item.item.clone();
        ItemMeta meta = display.getItemMeta();
        
        CurrencyType currencyType = item.getCurrencyType();
        String currencyName = getCurrencyName(currencyType);
        String priceStr = formatCurrency(currencyType, item.price);
        
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(miniMessage.parseWithMultiplePlaceholders("<yellow>价格: <gold><price> <currency>",
            "price", priceStr, "currency", currencyName));
        lore.add(miniMessage.parseWithPlaceholders(config.getString("gui.seller-display", "<gray>卖家: <white><seller>"),
            "seller", item.sellerName));
        lore.add(miniMessage.parseWithPlaceholders(config.getString("gui.time-remaining", "<gray>剩余时间: <white><time>"),
            "time", formatTime(item.expireTime - System.currentTimeMillis())));
        lore.add(Component.empty());
        lore.add(colorize(config.getString("gui.click-to-buy", "<green>点击购买")));
        
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createMyListingDisplayItem(MarketItem item) {
        ItemStack display = item.item.clone();
        ItemMeta meta = display.getItemMeta();
        
        CurrencyType currencyType = item.getCurrencyType();
        String currencyName = getCurrencyName(currencyType);
        String priceStr = formatCurrency(currencyType, item.price);
        
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(miniMessage.parseWithMultiplePlaceholders("<yellow>价格: <gold><price> <currency>",
            "price", priceStr, "currency", currencyName));
        lore.add(miniMessage.parseWithPlaceholders(config.getString("gui.time-remaining", "<gray>剩余时间: <white><time>"),
            "time", formatTime(item.expireTime - System.currentTimeMillis())));
        lore.add(Component.empty());
        lore.add(colorize(config.getString("gui.click-to-cancel", "<red>点击下架")));
        
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name));
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (hours > 24) {
            return (hours / 24) + "天 " + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时 " + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    public String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }

    private void playSound(Player player, String soundKey) {
        String soundName = config.getString("sounds." + soundKey, "");
        if (!soundName.isEmpty() && soundService != null) {
            soundService.playSound(player, soundName, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        openGUIs.remove(event.getPlayer().getUniqueId());
        searchModePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        openGUIs.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * 处理聊天输入（搜索功能）
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!searchModePlayers.contains(player.getUniqueId())) return;
        
        event.setCancelled(true);
        String message = event.getMessage();
        
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(colorize("<yellow>已取消搜索"));
                    setSearchMode(player, false);
                    openMarketGUI(player, 1);
                    return;
                }
                
                MarketGUI gui = openGUIs.get(player.getUniqueId());
                if (gui != null) {
                    gui.setSearchQuery(message);
                    gui.refreshItems();
                    player.openInventory(gui.getInventory());
                    player.sendMessage(colorize("<green>搜索: <white>" + message));
                }
                setSearchMode(player, false);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        MarketGUI gui = openGUIs.get(player.getUniqueId());
        
        if (gui == null) return;
        
        // 检查是否点击的是新GUI
        if (event.getInventory().getHolder() instanceof MarketGUI) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                gui.handleClick(slot);
            }
            return;
        }
        
        // 旧的GUI处理（我的上架界面）- 需要验证玩家确实在查看市场相关GUI
        // 验证玩家当前打开的确实是市场相关界面，防止关闭GUI后仍被拦截
        String viewTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().serialize(event.getView().title());
        String myListingsTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().serialize(colorize(config.getString("gui.my-listings-title", "<gold>我的上架")));
        if (!viewTitle.contains(myListingsTitle)) {
            openGUIs.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 45) {
            List<MarketItem> listings = playerListings.getOrDefault(player.getUniqueId(), new ArrayList<>());
            if (slot < listings.size()) {
                MarketItem item = listings.get(slot);
                cancelListing(player, item);
                openMyListingsGUI(player);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("guangdian.market.use")) {
            player.sendMessage(colorize(config.getString("messages.no-permission", "<red>你没有权限使用市场!")));
            return true;
        }
        
        if (args.length == 0) {
            openMarketGUI(player, 1);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "sell":
                return handleSellCommand(player, args);
            case "my":
                openMyListingsGUI(player);
                return true;
            case "help":
                sendHelp(player);
                return true;
            default:
                openMarketGUI(player, 1);
                return true;
        }
    }

    private boolean handleSellCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(colorize("<red>用法: /market sell <价格> [货币类型]"));
            player.sendMessage(colorize("<gray>货币类型: points(点券) / eco(金币)"));
            player.sendMessage(colorize("<gray>默认使用点券"));
            return true;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(colorize(config.getString("messages.no-item-in-hand", "<red>请手持要出售的物品!")));
            return true;
        }
        
        try {
            long price = parseAmount(args[1]);
            
            // 解析货币类型
            CurrencyType currencyType = CurrencyType.POINTS;
            if (args.length >= 3) {
                String typeArg = args[2].toLowerCase();
                if (typeArg.equals("eco") || typeArg.equals("economy") || typeArg.equals("金币") || typeArg.equals("金")) {
                    if (!economyEnabled) {
                        player.sendMessage(colorize("<red>经济系统未启用，无法使用金币上架!"));
                        return true;
                    }
                    currencyType = CurrencyType.ECONOMY;
                } else if (typeArg.equals("points") || typeArg.equals("point") || typeArg.equals("点券") || typeArg.equals("点")) {
                    if (!pointsEnabled) {
                        player.sendMessage(colorize("<red>点券系统未启用，无法使用点券上架!"));
                        return true;
                    }
                    currencyType = CurrencyType.POINTS;
                }
            } else {
                // 默认使用点券
                if (!pointsEnabled && economyEnabled) {
                    currencyType = CurrencyType.ECONOMY;
                    player.sendMessage(colorize("<yellow>点券系统未启用，自动使用金币作为货币"));
                }
            }
            
            if (listItem(player, item, price, currencyType)) {
                // 消息已在listItem中发送
            }
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("<red>无效的价格!"));
        }
        
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(colorize("<gold>===== 全球市场帮助 ====="));
        player.sendMessage(colorize("<yellow>/market <gray>- 打开市场"));
        player.sendMessage(colorize("<yellow>/market sell <价格> [货币] <gray>- 上架手持物品"));
        player.sendMessage(colorize("<yellow>/market my <gray>- 查看我的上架"));
        player.sendMessage(colorize("<gray>货币: points(点券) / eco(金币)"));
    }

    private long parseAmount(String str) throws NumberFormatException {
        str = str.toLowerCase().replace(",", "");
        if (str.endsWith("k")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000);
        } else if (str.endsWith("m")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 1000000);
        } else if (str.endsWith("w") || str.endsWith("万")) {
            return (long) (Double.parseDouble(str.substring(0, str.length() - 1)) * 10000);
        }
        return Long.parseLong(str);
    }

    public Map<UUID, List<ItemStack>> getOfflineReturns() {
        return offlineReturns;
    }
    
    public Map<UUID, MarketGUI> getOpenGUIs() {
        return openGUIs;
    }
    
    public Set<UUID> getSearchModePlayers() {
        return searchModePlayers;
    }

    /**
     * 使用 MiniMessageService 解析 MiniMessage 标签
     */
    public Component colorize(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return miniMessage.colorize(text);
    }

    /**
     * 获取 SoundService
     * @return SoundService 实例（可能为本地降级实现）
     */
    public SoundService getSoundService() {
        return soundService;
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例（可能为本地降级实现）
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        
        if (args.length == 1) {
            list.add("sell");
            list.add("my");
            list.add("help");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            list.add("points");
            list.add("eco");
        }
        
        return list;
    }

    public enum GUIType {
        MARKET,
        MY_LISTINGS
    }
    
    /**
     * 货币类型枚举
     */
    public enum CurrencyType {
        POINTS,
        ECONOMY
    }

    public static class MarketItem {
        public UUID id;
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public long price;
        public long expireTime;
        public CurrencyType currencyType; // 货币类型
        
        MarketItem(UUID id, UUID seller, String sellerName, ItemStack item, long price, long expireTime) {
            this(id, seller, sellerName, item, price, expireTime, CurrencyType.POINTS);
        }
        
        MarketItem(UUID id, UUID seller, String sellerName, ItemStack item, long price, long expireTime, CurrencyType currencyType) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
            this.expireTime = expireTime;
            this.currencyType = currencyType;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
        
        public CurrencyType getCurrencyType() {
            return currencyType != null ? currencyType : CurrencyType.POINTS;
        }
        
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id.toString());
            map.put("seller", seller.toString());
            map.put("sellerName", sellerName);
            map.put("item", item.serialize());
            map.put("price", price);
            map.put("expireTime", expireTime);
            map.put("currencyType", currencyType != null ? currencyType.name() : "POINTS");
            return map;
        }
        
        static MarketItem fromMap(Map<?, ?> map) {
            try {
                UUID id = UUID.fromString((String) map.get("id"));
                UUID seller = UUID.fromString((String) map.get("seller"));
                String sellerName = (String) map.get("sellerName");
                ItemStack item = ItemStack.deserialize((Map<String, Object>) map.get("item"));
                long price = ((Number) map.get("price")).longValue();
                long expireTime = ((Number) map.get("expireTime")).longValue();
                
                // 兼容旧数据，默认为点券
                CurrencyType currencyType = CurrencyType.POINTS;
                if (map.containsKey("currencyType")) {
                    try {
                        currencyType = CurrencyType.valueOf((String) map.get("currencyType"));
                    } catch (Exception e) {
                        instance.getLogger().warning("解析货币类型失败: " + map.get("currencyType") + " - 使用默认 POINTS");
                    }
                }
                
                return new MarketItem(id, seller, sellerName, item, price, expireTime, currencyType);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
