package cn.guangdian.points;

import cn.guangdian.points.adapter.PointsServiceAdapter;
import cn.guangdian.points.command.PointsCommand;
import cn.guangdian.points.lifecycle.PointsDataHandler;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.message.MessageServiceImpl;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.points.monitor.OperationTimer;
import cn.guangdian.points.monitor.PerformanceMonitor;
import cn.guangdian.points.monitor.PerformanceReport;
import cn.guangdian.points.placeholder.PointsPlaceholder;
import cn.guangdian.points.transaction.TransactionLogger;
import cn.guangdian.points.transaction.UnfinishedTransaction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 光点点卷插件主类
 * 
 * <p>基于 RPGCore 微服务架构的点卷管理系统，支持：</p>
 * <ul>
 *   <li>玩家点卷余额管理</li>
 *   <li>点卷转账与交易</li>
 *   <li>事务日志记录</li>
 *   <li>并发安全控制</li>
 *   <li>PlaceholderAPI 支持</li>
 * </ul>
 * 
 * <h3>版本历史：</h3>
 * <ul>
 *   <li><b>2026-04-14</b> - v1.1.0: 迁移到 MiniMessage，使用 RPGCore 消息服务</li>
 *   <li><b>2025-04</b> - v1.0.0: 初始版本发布</li>
 * </ul>
 * 
 * <h3>技术栈：</h3>
 * <ul>
 *   <li>Paper 1.21.6</li>
 *   <li>RPGCore 微服务架构</li>
 *   <li>Adventure MiniMessage API</li>
 *   <li>ConcurrentHashMap 线程安全</li>
 * </ul>
 * 
 * @author Gumin
 * @version 1.1.0
 * @since 2025-04
 * @see AbstractRPGPlugin
 * @see MiniMessageService
 */
public class GuangDianPoints extends AbstractRPGPlugin implements Listener {

    private static GuangDianPoints instance;
    private FileConfiguration config;
    private File dataFile;
    private YamlConfiguration data;

    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();

    private long defaultBalance;

    // 优化组件
    private TransactionLogger transactionLogger;
    private PlayerLockManager lockManager;
    private PerformanceMonitor performanceMonitor;

    // RPGCore 适配器
    private PointsServiceAdapter serviceAdapter;
    private PointsDataHandler dataHandler;

    // 命令系统
    private PointsCommand pointsCommand;
    private boolean usingCommandFramework = false;

    // MessageServiceImpl 用于消息颜色处理
    private MessageServiceImpl msg;

    // 配置选项
    private boolean transactionLogEnabled;
    private boolean concurrencyEnabled;
    private boolean asyncSaveEnabled;
    private long lockTimeoutMs;
    private String lockTimeoutMessage;

    /**
     * 插件启用时调用
     * 
     * <p>初始化流程：</p>
     * <ol>
     *   <li>初始化 MiniMessage 服务（用于消息颜色处理）</li>
     *   <li>加载配置文件</li>
     *   <li>加载玩家数据</li>
     *   <li>初始化优化组件（事务日志、锁管理器、性能监控）</li>
     *   <li>恢复未完成的事务</li>
     *   <li>注册事件监听器</li>
     *   <li>启动定时任务</li>
     *   <li>注册 RPGCore 服务</li>
     * </ol>
     * 
     * @since 1.0.0
     * @see MiniMessageService#getInstance()
     * @see #loadData()
     * @see #registerAPI()
     */
    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 MessageServiceImpl 用于消息颜色处理
        msg = MessageServiceImpl.getInstance();

        saveDefaultConfig();
        config = getConfig();
        loadData();
        loadSettings();
        initOptimizationComponents();
        recoverUnfinishedTransactions();
        registerEvents();
        startTasks();
        registerAPI();

        getLogger().info("光点点卷插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
        logOptimizationStatus();
    }

    @Override
    protected void onPluginDisable() {
        // 注销 CommandFramework 命令
        if (usingCommandFramework && pointsCommand != null) {
            try {
                CommandFramework framework = CommandFramework.getInstance();
                if (framework != null) {
                    framework.unregisterCommand("points");
                    getLogger().info("已从 CommandFramework 注销命令");
                }
            } catch (Exception e) {
                getLogger().warning("注销 CommandFramework 命令失败: " + e.getMessage());
            }
        }

        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        if (dataHandler != null) {
            dataHandler.unregister();
        }

        saveData();

        // 关闭优化组件
        if (transactionLogger != null) {
            transactionLogger.close();
        }

        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("光点点卷插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianPoints";
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

        if (data.contains("balances") && data.getConfigurationSection("balances") != null) {
            for (String key : data.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long balance = data.getLong("balances." + key);
                    balances.put(uuid, balance);
                } catch (Exception e) {
                    getLogger().warning("加载余额失败: " + key);
                }
            }
        }
    }

    private void saveData() {
        data.set("balances", null);
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            data.set("balances." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("保存数据失败: " + e.getMessage());
        }
    }

    private void loadSettings() {
        defaultBalance = config.getLong("settings.default-balance", 0);

        // 加载优化配置
        ConfigurationSection optConfig = config.getConfigurationSection("optimization");
        if (optConfig == null) {
            optConfig = config.createSection("optimization");
        }

        // 事务日志配置
        ConfigurationSection txnLogConfig = optConfig.getConfigurationSection("transaction-log");
        transactionLogEnabled = txnLogConfig != null && txnLogConfig.getBoolean("enabled", true);

        // 并发控制配置
        ConfigurationSection concurrencyConfig = optConfig.getConfigurationSection("concurrency");
        concurrencyEnabled = concurrencyConfig != null && concurrencyConfig.getBoolean("enabled", true);
        lockTimeoutMs = concurrencyConfig != null ? concurrencyConfig.getLong("lock-timeout-ms", 3000) : 3000;
        lockTimeoutMessage = concurrencyConfig != null ?
            concurrencyConfig.getString("lock-timeout-message", "<red>操作繁忙，请稍后重试") :
            "<red>操作繁忙，请稍后重试";

        // 异步保存配置
        ConfigurationSection asyncConfig = optConfig.getConfigurationSection("async-save");
        asyncSaveEnabled = asyncConfig != null && asyncConfig.getBoolean("enabled", true);
    }

    /**
     * 初始化优化组件
     * PlayerLockManager 优先使用 RPGCore 的服务，PerformanceMonitor 使用本地实现
     */
    private void initOptimizationComponents() {
        // 初始化性能监控器（使用本地实现，API兼容性更好）
        performanceMonitor = new PerformanceMonitor("GuangDianPoints");

        // 初始化事务日志记录器
        if (transactionLogEnabled) {
            String logFileName = config.getString("optimization.transaction-log.file", "transactions.log");
            File logFile = new File(getDataFolder(), logFileName);
            transactionLogger = new TransactionLogger(this, logFile);
            getLogger().info("事务日志已启用: " + logFileName);
        }

        // 初始化玩家锁管理器 - 优先使用 RPGCore
        if (concurrencyEnabled) {
            boolean rpgCoreAvailable = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
            if (rpgCoreAvailable) {
                try {
                    lockManager = RPGCore.getInstance().getLockManager();
                    getLogger().info("使用 RPGCore 的 PlayerLockManager（统一服务）");
                } catch (Exception e) {
                    getLogger().warning("无法获取 RPGCore PlayerLockManager，使用本地实现: " + e.getMessage());
                    lockManager = new PlayerLockManager(getLogger(), lockTimeoutMs);
                }
            } else {
                lockManager = new PlayerLockManager(getLogger(), lockTimeoutMs);
                getLogger().info("RPGCore 不可用，使用本地 PlayerLockManager，锁超时: " + lockTimeoutMs + "ms");
            }
        }
    }

    /**
     * 恢复未完成的事务
     */
    private void recoverUnfinishedTransactions() {
        if (transactionLogger == null) return;

        List<UnfinishedTransaction> unfinished = transactionLogger.recoverUnfinishedTransactions();

        for (UnfinishedTransaction txn : unfinished) {
            getLogger().warning("发现未完成事务: " + txn.getTransactionId() +
                ", 玩家: " + txn.getPlayerUuid() +
                ", 类型: " + txn.getType() +
                ", 金额: " + txn.getAmount());

            // 根据事务类型进行恢复
            if (txn.getBalanceBefore() >= 0) {
                // 如果有操作前余额，恢复到操作前状态
                balances.put(txn.getPlayerUuid(), txn.getBalanceBefore());
                getLogger().info("已恢复玩家 " + txn.getPlayerUuid() + " 的余额为 " + txn.getBalanceBefore());

                // 记录回滚
                transactionLogger.rollbackTransaction(txn.getTransactionId(), "服务器重启恢复");
            }
        }

        // 保存恢复后的数据
        if (!unfinished.isEmpty()) {
            saveData();
        }
    }

    /**
     * 输出优化组件状态
     */
    private void logOptimizationStatus() {
        getLogger().info("========== 优化组件状态 ==========");
        getLogger().info("命令系统: " + (usingCommandFramework ? "CommandFramework" : "传统Bukkit"));
        getLogger().info("性能监控: " + (performanceMonitor != null && performanceMonitor.isEnabled() ? "已启用" : "未启用"));
        getLogger().info("事务日志: " + (transactionLogger != null ? "已启用" : "未启用"));
        getLogger().info("并发控制: " + (lockManager != null ? "已启用" : "未启用"));
        getLogger().info("异步保存: " + (asyncSaveEnabled ? "已启用" : "未启用"));
        getLogger().info("==================================");
    }

    private void registerEvents() {
        // 注册命令系统 - 优先使用 CommandFramework
        if (registerCommandFramework()) {
            getLogger().info("命令系统已注册到 RPGCore CommandFramework");
        } else {
            // 降级到传统命令处理
            getCommand("points").setExecutor(this);
            getCommand("points").setTabCompleter(this);
            getLogger().warning("RPGCore CommandFramework 不可用，使用传统命令处理");
        }

        // 注册数据生命周期管理
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            dataHandler = new PointsDataHandler(this);
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().warning("RPGCore 未启用，使用传统事件监听");
        }
    }

    /**
     * 注册 CommandFramework 命令系统
     * @return 是否成功注册
     */
    private boolean registerCommandFramework() {
        try {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore == null) {
                getLogger().warning("RPGCore 未初始化，无法使用 CommandFramework");
                return false;
            }

            CommandFramework framework = CommandFramework.getInstance();
            if (framework == null) {
                getLogger().warning("CommandFramework 不可用");
                return false;
            }

            pointsCommand = new PointsCommand(this);
            framework.registerCommand(pointsCommand);
            usingCommandFramework = true;
            return true;
        } catch (Exception e) {
            getLogger().warning("注册 CommandFramework 失败: " + e.getMessage());
            return false;
        }
    }

    private void startTasks() {
        long saveInterval = config.getLong("settings.auto-save-interval-minutes", 5) * 60 * 20L;

        if (scheduler != null) {
            scheduler.runSyncRepeating(this::saveData, saveInterval, saveInterval);
        }

        if (transactionLogger != null) {
            long retentionDays = config.getLong("optimization.transaction-log.retention-days", 30);
            if (scheduler != null) {
                scheduler.runAsyncRepeating(() -> {
                    transactionLogger.cleanupOldLogs(retentionDays);
                }, 20L * 60 * 60 * 24, 20L * 60 * 60 * 24);
            }
        }
    }

    private void registerAPI() {
        // 保留旧的 Bukkit API 注册（向后兼容）
        getServer().getServicesManager().register(PointsAPI.class, new PointsAPI(), this, ServicePriority.Normal);

        // 注册 RPGCore 服务适配器
        serviceAdapter = new PointsServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PointsPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
    }

    public static GuangDianPoints getInstance() {
        return instance;
    }

    public Map<UUID, Long> getBalances() {
        return balances;
    }

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, defaultBalance);
    }

    /**
     * 设置玩家余额（带事务保护）
     */
    public void setBalance(UUID uuid, long amount) {
        setBalance(uuid, amount, null);
    }

    /**
     * 设置玩家余额（带事务保护和来源信息）
     */
    public void setBalance(UUID uuid, long amount, String source) {
        if (lockManager != null) {
            try {
                lockManager.executeWithLock(uuid, () -> setBalanceInternal(uuid, amount, source));
            } catch (LockTimeoutException e) {
                getLogger().warning("设置余额时获取锁超时: " + uuid);
            }
        } else {
            setBalanceInternal(uuid, amount, source);
        }
    }

    private void setBalanceInternal(UUID uuid, long amount, String source) {
        long oldBalance = getBalance(uuid);
        long newBalance = Math.max(0, amount);
        balances.put(uuid, newBalance);

        // 记录事务日志
        if (transactionLogger != null && oldBalance != newBalance) {
            TransactionLogger.TransactionType type = TransactionLogger.TransactionType.ADMIN_SET;
            transactionLogger.beginTransaction(uuid, type, newBalance - oldBalance,
                oldBalance, newBalance, null, source);
            transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
        }
    }

    /**
     * 增加玩家余额（带事务保护）
     */
    public void addBalance(UUID uuid, long amount) {
        addBalance(uuid, amount, null);
    }

    /**
     * 增加玩家余额（带事务保护和来源信息）
     */
    public void addBalance(UUID uuid, long amount, String source) {
        if (lockManager != null) {
            try {
                lockManager.executeWithLock(uuid, () -> addBalanceInternal(uuid, amount, source));
            } catch (LockTimeoutException e) {
                getLogger().warning("增加余额时获取锁超时: " + uuid);
            }
        } else {
            addBalanceInternal(uuid, amount, source);
        }
    }

    private void addBalanceInternal(UUID uuid, long amount, String source) {
        long oldBalance = getBalance(uuid);
        long newBalance = oldBalance + amount;
        balances.put(uuid, newBalance);

        // 记录事务日志
        if (transactionLogger != null) {
            TransactionLogger.TransactionType type = TransactionLogger.TransactionType.EARN;
            transactionLogger.beginTransaction(uuid, type, amount,
                oldBalance, newBalance, null, source);
            transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
        }
    }

    /**
     * 扣除玩家余额（带事务保护）
     */
    public boolean removeBalance(UUID uuid, long amount) {
        return removeBalance(uuid, amount, null);
    }

    /**
     * 扣除玩家余额（带事务保护和来源信息）
     */
    public boolean removeBalance(UUID uuid, long amount, String source) {
        if (lockManager != null) {
            try {
                return lockManager.executeWithLock(uuid, () -> removeBalanceInternal(uuid, amount, source));
            } catch (LockTimeoutException e) {
                getLogger().warning("扣除余额时获取锁超时: " + uuid);
                return false;
            }
        } else {
            return removeBalanceInternal(uuid, amount, source);
        }
    }

    private boolean removeBalanceInternal(UUID uuid, long amount, String source) {
        long current = getBalance(uuid);
        if (current < amount) {
            return false;
        }

        long newBalance = current - amount;
        balances.put(uuid, newBalance);

        // 记录事务日志
        if (transactionLogger != null) {
            TransactionLogger.TransactionType type = TransactionLogger.TransactionType.SPEND;
            transactionLogger.beginTransaction(uuid, type, amount,
                current, newBalance, null, source);
            transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
        }

        return true;
    }

    /**
     * 转账（带完整的事务保护）
     */
    public boolean transferBalance(UUID from, UUID to, long amount) {
        if (amount <= 0) return false;

        // 使用双锁保护转账操作
        if (lockManager != null) {
            try {
                return lockManager.executeWithDualLock(from, to,
                    () -> transferBalanceInternal(from, to, amount));
            } catch (LockTimeoutException e) {
                getLogger().warning("转账时获取锁超时: " + from + " -> " + to);
                return false;
            }
        } else {
            return transferBalanceInternal(from, to, amount);
        }
    }

    private boolean transferBalanceInternal(UUID from, UUID to, long amount) {
        // 性能监控计时
        try (OperationTimer timer = performanceMonitor.startOperation("transfer")) {
            long fromBalance = getBalance(from);
            if (fromBalance < amount) {
                return false;
            }

            String txnId = null;
            long fromOldBalance = fromBalance;
            long toOldBalance = getBalance(to);

            // 开始事务
            if (transactionLogger != null) {
                txnId = transactionLogger.beginTransaction(from,
                    TransactionLogger.TransactionType.TRANSFER_OUT, amount,
                    fromOldBalance, -1, to, "转账给 " + to);
            }

            try {
                // 执行转账
                balances.put(from, fromOldBalance - amount);
                balances.put(to, toOldBalance + amount);

                // 提交事务
                if (transactionLogger != null && txnId != null) {
                    transactionLogger.commitTransaction(txnId, fromOldBalance - amount);
                }

                // 记录缓存命中
                performanceMonitor.recordCacheHit("balance");

                return true;

            } catch (Exception e) {
                // 回滚事务
                getLogger().log(Level.SEVERE, "转账异常，正在回滚: " + from + " -> " + to, e);

                if (transactionLogger != null && txnId != null) {
                    transactionLogger.rollbackTransaction(txnId, e.getMessage());
                }

                // 恢复余额
                balances.put(from, fromOldBalance);
                balances.put(to, toOldBalance);

                return false;
            }
        }
    }

    /**
     * 管理员给予点券
     */
    public void adminGive(UUID uuid, long amount, UUID admin) {
        if (lockManager != null) {
            try {
                lockManager.executeWithLock(uuid, () -> {
                    long oldBalance = getBalance(uuid);
                    long newBalance = oldBalance + amount;
                    balances.put(uuid, newBalance);

                    if (transactionLogger != null) {
                        transactionLogger.beginTransaction(uuid,
                            TransactionLogger.TransactionType.ADMIN_GIVE, amount,
                            oldBalance, newBalance, admin, "管理员给予");
                        transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
                    }
                });
            } catch (LockTimeoutException e) {
                getLogger().warning("管理员给予时获取锁超时: " + uuid);
            }
        } else {
            long oldBalance = getBalance(uuid);
            long newBalance = oldBalance + amount;
            balances.put(uuid, newBalance);

            if (transactionLogger != null) {
                transactionLogger.beginTransaction(uuid,
                    TransactionLogger.TransactionType.ADMIN_GIVE, amount,
                    oldBalance, newBalance, admin, "管理员给予");
                transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
            }
        }
    }

    /**
     * 管理员扣除点券
     */
    public boolean adminTake(UUID uuid, long amount, UUID admin) {
        if (lockManager != null) {
            try {
                return lockManager.executeWithLock(uuid, () -> {
                    long oldBalance = getBalance(uuid);
                    if (oldBalance < amount) return false;

                    long newBalance = oldBalance - amount;
                    balances.put(uuid, newBalance);

                    if (transactionLogger != null) {
                        transactionLogger.beginTransaction(uuid,
                            TransactionLogger.TransactionType.ADMIN_TAKE, amount,
                            oldBalance, newBalance, admin, "管理员扣除");
                        transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
                    }
                    return true;
                });
            } catch (LockTimeoutException e) {
                getLogger().warning("管理员扣除时获取锁超时: " + uuid);
                return false;
            }
        } else {
            long oldBalance = getBalance(uuid);
            if (oldBalance < amount) return false;

            long newBalance = oldBalance - amount;
            balances.put(uuid, newBalance);

            if (transactionLogger != null) {
                transactionLogger.beginTransaction(uuid,
                    TransactionLogger.TransactionType.ADMIN_TAKE, amount,
                    oldBalance, newBalance, admin, "管理员扣除");
                transactionLogger.commitTransaction(transactionLogger.getCurrentTransactionId() + "");
            }
            return true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!balances.containsKey(player.getUniqueId())) {
            balances.put(player.getUniqueId(), defaultBalance);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 清理玩家锁
        if (lockManager != null) {
            lockManager.cleanup(player.getUniqueId());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleBalanceCommand(sender, new String[0]);
        }

        switch (args[0].toLowerCase()) {
            case "balance":
            case "bal":
                return handleBalanceCommand(sender, args);
            case "give":
                return handleGiveCommand(sender, args);
            case "take":
                return handleTakeCommand(sender, args);
            case "set":
                return handleSetCommand(sender, args);
            case "pay":
                return handlePayCommand(sender, args);
            case "top":
                return handleTopCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "perfmon":
                return handlePerfmonCommand(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleBalanceCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
                return true;
            }
            Player player = (Player) sender;
            long balance = getBalance(player.getUniqueId());
            String balanceMsg = config.getString("messages.balance-display", "<yellow>你当前有 <gold>%balance% <yellow>点卷")
                .replace("%balance%", formatNumber(balance));
            player.sendMessage(msg.colorize(balanceMsg));
            return true;
        }

        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg.colorize(config.getString("messages.player-not-found", "<red>玩家不在线!")));
            return true;
        }

        long balance = getBalance(target.getUniqueId());
        sender.sendMessage(msg.colorize("<yellow>" + target.getName() + " 当前有 <gold>" + formatNumber(balance) + " <yellow>点卷"));
        return true;
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.colorize("<red>用法: /points give <玩家> <数量>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg.colorize(config.getString("messages.player-not-found", "<red>玩家不在线!")));
            return true;
        }

        try {
            long amount = parseAmount(args[2]);
            if (amount <= 0) {
                sender.sendMessage(msg.colorize("<red>数量必须大于0!"));
                return true;
            }

            UUID adminUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            adminGive(target.getUniqueId(), amount, adminUuid);

            String giveMsg = config.getString("messages.give-success", "<green>已给予 %player% %amount% 点卷!")
                .replace("%player%", target.getName()).replace("%amount%", formatNumber(amount));
            sender.sendMessage(msg.colorize(giveMsg));
            String receiveMsg = config.getString("messages.receive-points", "<yellow>你收到了 <gold>%amount% <yellow>点卷!")
                .replace("%amount%", formatNumber(amount));
            target.sendMessage(msg.colorize(receiveMsg));
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.colorize("<red>无效的数量!"));
        }

        return true;
    }

    private boolean handleTakeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.colorize("<red>用法: /points take <玩家> <数量>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg.colorize(config.getString("messages.player-not-found", "<red>玩家不在线!")));
            return true;
        }

        try {
            long amount = parseAmount(args[2]);
            if (amount <= 0) {
                sender.sendMessage(msg.colorize("<red>数量必须大于0!"));
                return true;
            }

            UUID adminUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            if (adminTake(target.getUniqueId(), amount, adminUuid)) {
                sender.sendMessage(msg.colorize("<green>已扣除 " + target.getName() + " " + formatNumber(amount) + " 点卷!"));
                target.sendMessage(msg.colorize("<red>你被扣除了 " + formatNumber(amount) + " 点卷!"));
            } else {
                sender.sendMessage(msg.colorize(config.getString("messages.insufficient-funds", "<red>玩家点卷不足!")));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.colorize("<red>无效的数量!"));
        }

        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.colorize("<red>用法: /points set <玩家> <数量>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg.colorize(config.getString("messages.player-not-found", "<red>玩家不在线!")));
            return true;
        }

        try {
            long amount = parseAmount(args[2]);
            setBalance(target.getUniqueId(), amount, "管理员设置");
            sender.sendMessage(msg.colorize("<green>已设置 " + target.getName() + " 的点卷为 " + formatNumber(amount) + "!"));
            target.sendMessage(msg.colorize("<yellow>你的点卷已被设置为 <gold>" + formatNumber(amount) + "<yellow>!"));
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.colorize("<red>无效的数量!"));
        }

        return true;
    }

    private boolean handlePayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.colorize("<red>用法: /points pay <玩家> <数量>"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(msg.colorize(config.getString("messages.player-not-found", "<red>玩家不在线!")));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(msg.colorize("<red>不能给自己转账!"));
            return true;
        }

        try {
            long amount = parseAmount(args[2]);
            if (amount <= 0) {
                player.sendMessage(msg.colorize("<red>数量必须大于0!"));
                return true;
            }

            if (transferBalance(player.getUniqueId(), target.getUniqueId(), amount)) {
                String payMsg = config.getString("messages.pay-success", "<green>已转账 %amount% 点卷给 %player%!")
                    .replace("%amount%", formatNumber(amount)).replace("%player%", target.getName());
                player.sendMessage(msg.colorize(payMsg));
                String receiveMsg = config.getString("messages.receive-transfer", "<yellow>你收到了 %player% 转账的 <gold>%amount% <yellow>点卷!")
                    .replace("%player%", player.getName()).replace("%amount%", formatNumber(amount));
                target.sendMessage(msg.colorize(receiveMsg));
            } else {
                player.sendMessage(msg.colorize(config.getString("messages.insufficient-funds", "<red>点卷不足!")));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(msg.colorize("<red>无效的数量!"));
        }

        return true;
    }

    private boolean handleTopCommand(CommandSender sender) {
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(balances.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        sender.sendMessage(msg.colorize("<gold>===== 点卷排行榜 ====="));
        int count = Math.min(10, sorted.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<UUID, Long> entry = sorted.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offlinePlayer.getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            sender.sendMessage(msg.colorize("<yellow>" + (i + 1) + ". <white>" + name + " <gray>- <gold>" + formatNumber(entry.getValue())));
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        reloadConfig();
        config = getConfig();
        loadSettings();

        sender.sendMessage(msg.colorize("<green>配置已重新加载!"));
        return true;
    }

    /**
     * 处理性能监控命令
     */
    private boolean handlePerfmonCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<red>没有权限!"));
            return true;
        }

        if (performanceMonitor == null) {
            sender.sendMessage(msg.colorize("<red>性能监控未启用!"));
            return true;
        }

        String subCommand = args.length > 1 ? args[1].toLowerCase() : "status";

        switch (subCommand) {
            case "status":
                sender.sendMessage(msg.colorize("<gold>===== 性能监控状态 ====="));
                sender.sendMessage(msg.colorize(performanceMonitor.getSummary()));
                if (lockManager != null) {
                    sender.sendMessage(msg.colorize("<yellow>" + lockManager.getStats().toFormattedString()));
                }
                return true;

            case "report":
                PerformanceReport report = performanceMonitor.generateReport();
                String[] lines = report.toFormattedString().split("\n");
                for (String line : lines) {
                    sender.sendMessage(msg.colorize("<white>" + line));
                }
                return true;

            case "reset":
                performanceMonitor.reset();
                if (lockManager != null) {
                    lockManager.getStats().reset();
                }
                sender.sendMessage(msg.colorize("<green>性能统计已重置!"));
                return true;

            case "enable":
                performanceMonitor.enable();
                sender.sendMessage(msg.colorize("<green>性能监控已启用!"));
                return true;

            case "disable":
                performanceMonitor.disable();
                sender.sendMessage(msg.colorize("<red>性能监控已禁用!"));
                return true;

            default:
                sender.sendMessage(msg.colorize("<red>用法: /points perfmon [status|report|reset|enable|disable]"));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.colorize("<gold>===== 点卷系统帮助 ====="));
        sender.sendMessage(msg.colorize("<yellow>/points <gray>- 查看余额"));
        sender.sendMessage(msg.colorize("<yellow>/points pay <玩家> <数量> <gray>- 转账"));
        sender.sendMessage(msg.colorize("<yellow>/points top <gray>- 排行榜"));
        if (sender.hasPermission("guangdian.points.admin")) {
            sender.sendMessage(msg.colorize("<yellow>/points give <玩家> <数量> <gray>- 给予点券"));
            sender.sendMessage(msg.colorize("<yellow>/points take <玩家> <数量> <gray>- 扣除点券"));
            sender.sendMessage(msg.colorize("<yellow>/points set <玩家> <数量> <gray>- 设置点券"));
            sender.sendMessage(msg.colorize("<yellow>/points reload <gray>- 重载配置"));
            sender.sendMessage(msg.colorize("<yellow>/points perfmon [status|report|reset] <gray>- 性能监控"));
        }
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

    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("balance");
            list.add("pay");
            list.add("top");
            list.add("help");
            if (sender.hasPermission("guangdian.points.admin")) {
                list.add("give");
                list.add("take");
                list.add("set");
                list.add("reload");
                list.add("perfmon");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("perfmon")) {
                list.add("status");
                list.add("report");
                list.add("reset");
                list.add("enable");
                list.add("disable");
            } else if (!args[0].equalsIgnoreCase("help") && !args[0].equalsIgnoreCase("top") && !args[0].equalsIgnoreCase("reload")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
            }
        }

        return list;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public PlayerLockManager getLockManager() {
        return lockManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    /**
     * 获取默认余额
     */
    public long getDefaultBalance() {
        return defaultBalance;
    }

    public Map<UUID, Long> getAllBalances() {
        return new HashMap<>(balances);
    }

    public class PointsAPI {
        public long getBalance(UUID uuid) {
            return GuangDianPoints.this.getBalance(uuid);
        }

        public void setBalance(UUID uuid, long amount) {
            GuangDianPoints.this.setBalance(uuid, amount);
        }

        public void addBalance(UUID uuid, long amount) {
            GuangDianPoints.this.addBalance(uuid, amount);
        }

        public boolean removeBalance(UUID uuid, long amount) {
            return GuangDianPoints.this.removeBalance(uuid, amount);
        }

        public boolean transfer(UUID from, UUID to, long amount) {
            return GuangDianPoints.this.transferBalance(from, to, amount);
        }
    }
}