package cn.guangdian.bank;

import cn.guangdian.bank.api.BankServiceAdapter;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.bank.listener.BankListener;
import cn.guangdian.bank.manager.InterestManager;
import cn.guangdian.bank.manager.LoanManager;
import cn.guangdian.bank.placeholder.BankPlaceholder;
import me.clip.placeholderapi.PlaceholderAPI;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GuangDianBank - 光点银行系统插件
 *
 * <p>本插件已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志系统 - 使用 RPGCore GameLogger（带降级兼容）</li>
 *   <li>消息发送 - 使用 RPGCore MiniMessageService（带降级兼容）</li>
 * </ul>
 *
 * <p>当 RPGCore 不可用时，自动降级到 Bukkit 原生实现。</p>
 *
 * @author GuangDian
 * @version 1.1.0
 * @since 1.0.0
 * @see OPTIMIZATION.md 优化详情
 */
public class GuangDianBank extends AbstractRPGPlugin {

    private static GuangDianBank instance;

    private final Map<UUID, BankAccount> accounts = new ConcurrentHashMap<>();

    private BankServiceAdapter serviceAdapter;
    private InterestManager interestManager;
    private LoanManager loanManager;
    private BankPlaceholder bankPlaceholder;

    private File dataFile;
    private YamlConfiguration data;

    private long defaultBalance;
    private double depositInterestRate;
    private double loanInterestRate;
    private long interestInterval;
    private long maxLoanAmount;
    private int minCreditScoreForLoan;

    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();
        loadConfiguration();
        loadData();
        initializeManagers();
        registerServices();
        registerListeners();
        registerPlaceholders();
        startTasks();

        logInfo(getPluginName() + " 已启动");
        logInfo("存款利率: " + depositInterestRate + "%");
        logInfo("贷款利率: " + loanInterestRate + "%");
    }

    /**
     * 初始化 RPGCore 服务
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.gameLogger = rpgCore.getGameLogger();
            this.miniMessage = rpgCore.getMiniMessageService();
        } else {
            // 降级：使用 Bukkit 原生
            getLogger().warning("RPGCore 不可用，使用备用日志和消息服务");
        }
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        if (interestManager != null) {
            interestManager.shutdown();
        }

        if (loanManager != null) {
            loanManager.shutdown();
        }

        if (bankPlaceholder != null) {
            PlaceholderAPI.unregisterExpansion(bankPlaceholder);
        }

        saveData();

        logInfo(getPluginName() + " 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianBank";
    }

    // ==================== RPGCore 服务访问 ====================

    public GameLogger getGameLogger() {
        return gameLogger;
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    /**
     * 检查是否使用 RPGCore 服务
     */
    public boolean isUsingRPGCore() {
        return gameLogger != null;
    }

    // ==================== 日志快捷方法 ====================

    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public void logSevere(String message, Throwable throwable) {
        if (gameLogger != null) {
            gameLogger.severe(message, throwable);
        } else {
            getLogger().severe(message + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    // ==================== 配置加载 ====================

    private void loadConfiguration() {
        FileConfiguration config = getConfig();

        defaultBalance = config.getLong("settings.default-balance", 0);
        depositInterestRate = config.getDouble("settings.deposit-interest-rate", 0.5);
        loanInterestRate = config.getDouble("settings.loan-interest-rate", 5.0);
        interestInterval = config.getLong("settings.interest-interval-minutes", 1440) * 60 * 20L;
        maxLoanAmount = config.getLong("settings.max-loan-amount", 1000000);
        minCreditScoreForLoan = config.getInt("settings.min-credit-score-for-loan", 60);
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                logSevere("无法创建数据文件", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        loadAccounts();
    }

    private void loadAccounts() {
        if (!data.contains("accounts")) return;

        for (String key : data.getConfigurationSection("accounts").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                BankAccount account = new BankAccount(playerId);

                String path = "accounts." + key + ".";
                account.setBalance(data.getLong(path + "balance", 0));
                account.setCreditScore(data.getInt(path + "credit-score", 100));
                account.setLastInterestTime(data.getLong(path + "last-interest", System.currentTimeMillis()));

                accounts.put(playerId, account);
            } catch (Exception e) {
                logWarning("加载账户失败: " + key + " - " + e.getMessage());
            }
        }
    }

    private void saveData() {
        data.set("accounts", null);

        for (Map.Entry<UUID, BankAccount> entry : accounts.entrySet()) {
            String path = "accounts." + entry.getKey().toString() + ".";
            BankAccount account = entry.getValue();

            data.set(path + "balance", account.getBalance());
            data.set(path + "credit-score", account.getCreditScore());
            data.set(path + "last-interest", account.getLastInterestTime());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            logSevere("保存数据失败", e);
        }
    }

    private void initializeManagers() {
        interestManager = new InterestManager(this);
        loanManager = new LoanManager(this);

        logInfo("利息管理器已初始化");
        logInfo("贷款管理器已初始化");
    }

    private void registerServices() {
        serviceAdapter = new BankServiceAdapter(this);

        if (serviceAdapter.isUsingRPGCore()) {
            logInfo("已注册到 RPGCore 服务系统");
        }
    }

    private void registerListeners() {
        if (rpgCore != null) {
            BankListener listener = new BankListener(this);
            listener.register();
            logInfo("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            logWarning("RPGCore 未启用，使用传统事件监听");
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            bankPlaceholder = new BankPlaceholder(this);
            bankPlaceholder.register();
            logInfo("已注册 PlaceholderAPI 扩展");
        }
    }

    private void startTasks() {
        if (interestInterval > 0) {
            long taskId = scheduler.runSyncRepeating(() -> {
                interestManager.processAllInterest();
            }, interestInterval, interestInterval);
            logInfo("利息结算任务已启动，任务ID: " + taskId);
        }

        long saveTaskId = scheduler.runSyncRepeating(() -> {
            saveData();
        }, 6000L, 6000L);
        logInfo("自动保存任务已启动，任务ID: " + saveTaskId);
    }

    // ==================== 业务方法 ====================

    public static GuangDianBank getInstance() {
        return instance;
    }

    public Map<UUID, BankAccount> getAccounts() {
        return accounts;
    }

    public BankAccount getAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> {
            BankAccount account = new BankAccount(id);
            account.setBalance(defaultBalance);
            return account;
        });
    }

    public boolean hasAccount(UUID playerId) {
        return accounts.containsKey(playerId);
    }

    // ==================== 消息发送（使用 MiniMessage）====================

    public void sendMessage(Player player, String message) {
        if (miniMessage != null) {
            player.sendMessage(miniMessage.yellow(message));
        } else {
            player.sendMessage(Component.text(message).color(NamedTextColor.YELLOW));
        }
    }

    public void sendError(Player player, String message) {
        if (miniMessage != null) {
            player.sendMessage(miniMessage.red(message));
        } else {
            player.sendMessage(Component.text(message).color(NamedTextColor.RED));
        }
    }

    public void sendSuccess(Player player, String message) {
        if (miniMessage != null) {
            player.sendMessage(miniMessage.green(message));
        } else {
            player.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
        }
    }

    public void sendMessageWithPlaceholders(Player player, String messageKey, String... keyValues) {
        if (miniMessage != null) {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            player.sendMessage(miniMessage.parseUnified(template, keyValues));
        } else {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            for (int i = 0; i < keyValues.length - 1; i += 2) {
                template = template.replace("%" + keyValues[i] + "%", keyValues[i + 1]);
                template = template.replace("{" + keyValues[i] + "}", keyValues[i + 1]);
            }
            player.sendMessage(Component.text(template).color(NamedTextColor.YELLOW));
        }
    }

    public void sendSuccessWithPlaceholders(Player player, String messageKey, String... keyValues) {
        if (miniMessage != null) {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            player.sendMessage(miniMessage.parseUnified("<green>" + template, keyValues));
        } else {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            for (int i = 0; i < keyValues.length - 1; i += 2) {
                template = template.replace("%" + keyValues[i] + "%", keyValues[i + 1]);
                template = template.replace("{" + keyValues[i] + "}", keyValues[i + 1]);
            }
            player.sendMessage(Component.text(template).color(NamedTextColor.GREEN));
        }
    }

    public void sendErrorWithPlaceholders(Player player, String messageKey, String... keyValues) {
        if (miniMessage != null) {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            player.sendMessage(miniMessage.parseUnified("<red>" + template, keyValues));
        } else {
            String template = getConfig().getString("messages." + messageKey, messageKey);
            for (int i = 0; i < keyValues.length - 1; i += 2) {
                template = template.replace("%" + keyValues[i] + "%", keyValues[i + 1]);
                template = template.replace("{" + keyValues[i] + "}", keyValues[i + 1]);
            }
            player.sendMessage(Component.text(template).color(NamedTextColor.RED));
        }
    }

    // ==================== 配置访问器 ====================

    public long getDefaultBalance() {
        return defaultBalance;
    }

    public double getDepositInterestRate() {
        return depositInterestRate;
    }

    public double getLoanInterestRate() {
        return loanInterestRate;
    }

    public long getMaxLoanAmount() {
        return maxLoanAmount;
    }

    public int getMinCreditScoreForLoan() {
        return minCreditScoreForLoan;
    }

    public InterestManager getInterestManager() {
        return interestManager;
    }

    public LoanManager getLoanManager() {
        return loanManager;
    }
}
