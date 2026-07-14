package cn.guangdian.lottery;

import cn.guangdian.lottery.adapter.LotteryServiceAdapter;
import cn.guangdian.lottery.gui.LotteryGUI;
import cn.guangdian.lottery.manager.CooldownManager;
import cn.guangdian.lottery.manager.LotteryManager;
import cn.guangdian.lottery.model.LotteryPool;
import cn.guangdian.lottery.model.Prize;
import cn.guangdian.lottery.storage.LotteryStorage;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class GuangDianLottery extends AbstractRPGPlugin implements Listener, TabCompleter {

    private static GuangDianLottery instance;
    
    private MiniMessageService msg;
    private LotteryManager lotteryManager;
    private CooldownManager cooldownManager;
    private LotteryServiceAdapter serviceAdapter;
    private LotteryGUI lotteryGUI;
    
    private File poolsFile;
    private YamlConfiguration poolsConfig;
    
    private final Map<String, LotteryPool> pools = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerHistory = new ConcurrentHashMap<>();
    
    private int defaultCooldownSeconds;
    private int maxHistorySize;
    private boolean broadcastRarePrizes;
    private String broadcastPermission;
    private int autoSaveTaskId = -1;
    private LotteryStorage storage;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        initCommonServices();
        msg = MiniMessageService.getInstance();
        
        saveDefaultConfig();
        loadConfiguration();
        loadPools();
        
        lotteryManager = new LotteryManager(this);
        cooldownManager = new CooldownManager(this);
        lotteryGUI = new LotteryGUI(this);
        
        // SQLite 存储
        storage = new LotteryStorage(this);
        if (storage.init()) { storage.load(); playerCooldowns.putAll(storage.cooldowns()); playerHistory.putAll(storage.history()); }
        startAutoSave();
        
        registerEvents();
        registerAPI();
        
        getLogger().info("光点抽奖插件已启用! (SQLite) 版本: " + getDescription().getVersion());
        getLogger().info("已加载 " + pools.size() + " 个抽奖池");
    }
    
    private void startAutoSave() {
        autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (storage != null) { storage.cooldowns().clear(); storage.cooldowns().putAll(playerCooldowns); storage.history().clear(); storage.history().putAll(playerHistory); storage.saveAsync(); }
        }, 6000L, 6000L).getTaskId();
    }
    
    @Override
    protected void onPluginDisable() {
        Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        if (storage != null) { storage.cooldowns().clear(); storage.cooldowns().putAll(playerCooldowns); storage.history().clear(); storage.history().putAll(playerHistory); storage.save(); storage.close(); }
        if (serviceAdapter != null) serviceAdapter.unregister();
        if (scheduler != null) scheduler.cancelAllTasks();
        if (lotteryGUI != null) lotteryGUI.closeAll();
        getLogger().info("光点抽奖插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianLottery";
    }
    
    private void loadConfiguration() {
        FileConfiguration config = getConfig();
        defaultCooldownSeconds = config.getInt("settings.default-cooldown-seconds", 60);
        maxHistorySize = config.getInt("settings.max-history-size", 50);
        broadcastRarePrizes = config.getBoolean("settings.broadcast-rare-prizes", true);
        broadcastPermission = config.getString("settings.broadcast-permission", "guangdian.lottery.broadcast");
    }
    
    private void loadPools() {
        poolsFile = new File(getDataFolder(), "pools.yml");
        if (!poolsFile.exists()) {
            saveResource("pools.yml", false);
        }
        poolsConfig = YamlConfiguration.loadConfiguration(poolsFile);
        
        pools.clear();
        ConfigurationSection poolsSection = poolsConfig.getConfigurationSection("pools");
        if (poolsSection == null) {
            getLogger().warning("未找到抽奖池配置!");
            return;
        }
        
        for (String poolId : poolsSection.getKeys(false)) {
            ConfigurationSection poolSection = poolsSection.getConfigurationSection(poolId);
            if (poolSection == null) continue;
            
            LotteryPool pool = loadPool(poolId, poolSection);
            if (pool != null) {
                pools.put(poolId, pool);
                getLogger().info("加载抽奖池: " + poolId + " (" + pool.getPrizes().size() + " 个奖品)");
            }
        }
    }
    
    private LotteryPool loadPool(String poolId, ConfigurationSection section) {
        String displayName = section.getString("display-name", poolId);
        Material iconMaterial = Material.matchMaterial(section.getString("icon.material", "CHEST"));
        if (iconMaterial == null) iconMaterial = Material.CHEST;
        int iconCustomModelData = section.getInt("icon.custom-model-data", 0);
        int cooldownSeconds = section.getInt("cooldown-seconds", defaultCooldownSeconds);
        String permission = section.getString("permission", "");
        String currencyType = section.getString("currency.type", "POINTS");
        int cost = section.getInt("currency.cost", 100);
        
        List<Prize> prizes = new ArrayList<>();
        ConfigurationSection prizesSection = section.getConfigurationSection("prizes");
        if (prizesSection == null) {
            getLogger().warning("抽奖池 " + poolId + " 没有奖品配置!");
            return null;
        }
        
        double totalWeight = 0;
        for (String prizeId : prizesSection.getKeys(false)) {
            ConfigurationSection prizeSection = prizesSection.getConfigurationSection(prizeId);
            if (prizeSection == null) continue;
            
            Prize prize = loadPrize(prizeId, prizeSection);
            if (prize != null) {
                prizes.add(prize);
                totalWeight += prize.getWeight();
            }
        }
        
        if (prizes.isEmpty()) {
            getLogger().warning("抽奖池 " + poolId + " 没有有效奖品!");
            return null;
        }
        
        return new LotteryPool(poolId, displayName, iconMaterial, iconCustomModelData, 
            cooldownSeconds, permission, currencyType, cost, prizes, totalWeight);
    }
    
    private Prize loadPrize(String prizeId, ConfigurationSection section) {
        String displayName = section.getString("display-name", prizeId);
        Material material = Material.matchMaterial(section.getString("material", "DIAMOND"));
        if (material == null) material = Material.DIAMOND;
        int customModelData = section.getInt("custom-model-data", 0);
        int amount = section.getInt("amount", 1);
        double weight = section.getDouble("weight", 1.0);
        double chance = section.getDouble("chance", -1);
        boolean isRare = section.getBoolean("rare", false);
        String rarityColor = section.getString("rarity-color", "<gold>");
        
        List<String> commands = section.getStringList("commands");
        List<String> messages = section.getStringList("messages");
        
        String mythicMobsItem = section.getString("mythicmobs-item", "");
        
        return new Prize(prizeId, displayName, material, customModelData, amount, 
            weight, chance, isRare, rarityColor, commands, messages, mythicMobsItem);
    }
    
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(lotteryGUI, this);
        getCommand("lottery").setTabCompleter(this);
    }
    
    private void registerAPI() {
        serviceAdapter = new LotteryServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }
    }
    
    public Prize drawPrize(String poolId) {
        LotteryPool pool = pools.get(poolId);
        if (pool == null) return null;
        
        double random = ThreadLocalRandom.current().nextDouble() * pool.getTotalWeight();
        double currentWeight = 0;
        
        for (Prize prize : pool.getPrizes()) {
            currentWeight += prize.getWeight();
            if (random < currentWeight) {
                return prize;
            }
        }
        
        return pool.getPrizes().isEmpty() ? null : pool.getPrizes().get(0);
    }
    
    public boolean canDraw(Player player, String poolId) {
        LotteryPool pool = pools.get(poolId);
        if (pool == null) return false;
        
        if (!pool.getPermission().isEmpty() && !player.hasPermission(pool.getPermission())) {
            return false;
        }
        
        return !isOnCooldown(player.getUniqueId(), poolId);
    }
    
    public boolean isOnCooldown(UUID playerId, String poolId) {
        LotteryPool pool = pools.get(poolId);
        if (pool == null) return false;
        
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return false;
        
        Long lastDraw = cooldowns.get(poolId);
        if (lastDraw == null) return false;
        
        long cooldownMs = pool.getCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastDraw < cooldownMs;
    }
    
    public long getRemainingCooldown(UUID playerId, String poolId) {
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return 0;
        
        Long lastDraw = cooldowns.get(poolId);
        if (lastDraw == null) return 0;
        
        LotteryPool pool = pools.get(poolId);
        if (pool == null) return 0;
        
        long cooldownMs = pool.getCooldownSeconds() * 1000L;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastDraw);
        return Math.max(0, remaining);
    }
    
    public void setCooldown(UUID playerId, String poolId) {
        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(poolId, System.currentTimeMillis());
    }
    
    public void addToHistory(UUID playerId, String prizeInfo) {
        List<String> history = playerHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        history.add(0, prizeInfo);
        if (history.size() > maxHistorySize) {
            history.remove(history.size() - 1);
        }
    }
    
    public List<String> getHistory(UUID playerId) {
        return playerHistory.getOrDefault(playerId, new ArrayList<>());
    }
    
    public void givePrize(Player player, Prize prize) {
        if (!prize.getMythicMobsItem().isEmpty()) {
            giveMythicMobsItem(player, prize);
        } else {
            ItemStack item = new ItemStack(prize.getMaterial(), prize.getAmount());
            if (prize.getCustomModelData() > 0) {
                item.editMeta(meta -> meta.setCustomModelData(prize.getCustomModelData()));
            }
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage(msg.colorize("<yellow>背包已满，部分物品已掉落在地上!"));
            }
        }
        
        // TODO: 命令执行是临时方案，如果相关插件提供 API，应改用直接调用
        for (String cmd : prize.getCommands()) {
            String parsedCmd = cmd.replace("%player%", player.getName())
                .replace("%prize%", prize.getDisplayName())
                .replace("%amount%", String.valueOf(prize.getAmount()));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        for (String message : prize.getMessages()) {
            player.sendMessage(msg.colorize(message.replace("%prize%", prize.getDisplayName())));
        }
        
        if (prize.isRare() && broadcastRarePrizes) {
            String broadcast = "<gold>★ <yellow>" + player.getName() + " <white>在抽奖中获得了 " + 
                prize.getRarityColor() + prize.getDisplayName() + " <white>!";
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (broadcastPermission.isEmpty() || online.hasPermission(broadcastPermission)) {
                    online.sendMessage(msg.colorize(broadcast));
                }
            }
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    private void giveMythicMobsItem(Player player, Prize prize) {
        // TODO: 这是临时方案，通过 dispatchCommand 调用 MythicMobs 命令效率较低
        // MythicMobs 提供 API: MythicBukkit.inst().getItemManager().getItemStack(type)
        // 应改用: ItemStack item = MythicBukkit.inst().getItemManager().getItemStack(prize.getMythicMobsItem()).orElse(null);
        String cmd = "mm items give " + player.getName() + " " + prize.getMythicMobsItem();
        if (prize.getAmount() > 1) {
            cmd += " " + prize.getAmount();
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
    
    public boolean chargeCurrency(Player player, String currencyType, int amount) {
        switch (currencyType.toUpperCase()) {
            case "POINTS":
                return chargePoints(player, amount);
            case "MONEY":
            case "VAULT":
                return chargeMoney(player, amount);
            default:
                getLogger().warning("未知的货币类型: " + currencyType);
                return false;
        }
    }
    
    private boolean chargePoints(Player player, int amount) {
        // TODO: 这是临时方案，通过 dispatchCommand 执行 points 命令效率较低
        // PlayerPoints 提供 API: PlayerPointsPlugin.getAPI().take(uuid, amount)
        // 应改用直接 API 调用
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "points take " + player.getName() + " " + amount);
        return true;
    }
    
    private boolean chargeMoney(Player player, int amount) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            return externalServices.withdraw(player, amount);
        }
        return false;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerCooldowns.remove(playerId);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
                return true;
            }
            lotteryGUI.openMainMenu((Player) sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "draw":
                return handleDrawCommand(sender, args);
            case "pools":
                return handlePoolsCommand(sender);
            case "history":
                return handleHistoryCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleDrawCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(msg.colorize("<red>用法: /lottery draw <抽奖池ID>"));
            return true;
        }
        
        Player player = (Player) sender;
        String poolId = args[1].toLowerCase();
        
        LotteryPool pool = pools.get(poolId);
        if (pool == null) {
            player.sendMessage(msg.colorize("<red>抽奖池不存在!"));
            return true;
        }
        
        if (!canDraw(player, poolId)) {
            if (!pool.getPermission().isEmpty() && !player.hasPermission(pool.getPermission())) {
                player.sendMessage(msg.colorize("<red>你没有权限使用此抽奖池!"));
            } else {
                long remaining = getRemainingCooldown(player.getUniqueId(), poolId);
                int seconds = (int) (remaining / 1000);
                player.sendMessage(msg.colorize("<red>冷却中，请等待 " + seconds + " 秒!"));
            }
            return true;
        }
        
        if (!chargeCurrency(player, pool.getCurrencyType(), pool.getCost())) {
            player.sendMessage(msg.colorize("<red>货币不足! 需要 " + pool.getCost() + " " + pool.getCurrencyType()));
            return true;
        }
        
        Prize prize = drawPrize(poolId);
        if (prize == null) {
            player.sendMessage(msg.colorize("<red>抽奖失败，请联系管理员!"));
            return true;
        }
        
        setCooldown(player.getUniqueId(), poolId);
        givePrize(player, prize);
        addToHistory(player.getUniqueId(), prize.getDisplayName());
        
        player.sendMessage(msg.colorize("<green>恭喜你获得了 " + prize.getRarityColor() + prize.getDisplayName() + "<green>!"));
        
        return true;
    }
    
    private boolean handlePoolsCommand(CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold>===== 抽奖池列表 ====="));
        for (Map.Entry<String, LotteryPool> entry : pools.entrySet()) {
            LotteryPool pool = entry.getValue();
            String status = "";
            if (sender instanceof Player) {
                if (!pool.getPermission().isEmpty() && !sender.hasPermission(pool.getPermission())) {
                    status = "<red> [无权限]";
                } else if (isOnCooldown(((Player) sender).getUniqueId(), entry.getKey())) {
                    status = "<yellow> [冷却中]";
                } else {
                    status = "<green> [可抽奖]";
                }
            }
            sender.sendMessage(msg.colorize("<yellow>" + pool.getDisplayName() + 
                " <gray>- <white>" + pool.getCost() + " " + pool.getCurrencyType() + status));
        }
        return true;
    }
    
    private boolean handleHistoryCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }
        
        Player player = (Player) sender;
        List<String> history = getHistory(player.getUniqueId());
        
        if (history.isEmpty()) {
            player.sendMessage(msg.colorize("<yellow>你还没有抽奖记录!"));
            return true;
        }
        
        player.sendMessage(msg.colorize("<gold>===== 抽奖记录 ====="));
        for (int i = 0; i < Math.min(10, history.size()); i++) {
            player.sendMessage(msg.colorize("<white>" + (i + 1) + ". " + history.get(i)));
        }
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("guangdian.lottery.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }
        
        reloadConfig();
        loadConfiguration();
        loadPools();
        
        sender.sendMessage(msg.colorize("<green>配置已重新加载!"));
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold>===== 抽奖系统帮助 ====="));
        sender.sendMessage(msg.colorize("<yellow>/lottery <gray>- 打开抽奖界面"));
        sender.sendMessage(msg.colorize("<yellow>/lottery draw <池ID> <gray>- 抽奖"));
        sender.sendMessage(msg.colorize("<yellow>/lottery pools <gray>- 查看抽奖池"));
        sender.sendMessage(msg.colorize("<yellow>/lottery history <gray>- 查看抽奖记录"));
        if (sender.hasPermission("guangdian.lottery.admin")) {
            sender.sendMessage(msg.colorize("<yellow>/lottery reload <gray>- 重载配置"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        
        if (args.length == 1) {
            list.add("draw");
            list.add("pools");
            list.add("history");
            list.add("help");
            if (sender.hasPermission("guangdian.lottery.admin")) {
                list.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("draw")) {
            list.addAll(pools.keySet());
        }
        
        return list;
    }
    
    public static GuangDianLottery getInstance() {
        return instance;
    }
    
    public MiniMessageService getMsg() {
        return msg;
    }
    
    public Map<String, LotteryPool> getPools() {
        return pools;
    }
    
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public LotteryGUI getLotteryGUI() {
        return lotteryGUI;
    }
}
