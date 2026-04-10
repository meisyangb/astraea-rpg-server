package cn.guangdian.bank;

import cn.guangdian.bank.api.BankServiceAdapter;
import cn.guangdian.bank.data.BankAccount;
import cn.guangdian.bank.listener.BankListener;
import cn.guangdian.bank.manager.InterestManager;
import cn.guangdian.bank.manager.LoanManager;
import cn.guangdian.bank.placeholder.BankPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
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
import java.util.logging.Level;

public class GuangDianBank extends AbstractRPGPlugin {

    private static GuangDianBank instance;
    
    private final Map<UUID, BankAccount> accounts = new ConcurrentHashMap<>();
    
    private BankServiceAdapter serviceAdapter;
    private InterestManager interestManager;
    private LoanManager loanManager;
    
    private File dataFile;
    private YamlConfiguration data;
    
    private long defaultBalance;
    private double depositInterestRate;
    private double loanInterestRate;
    private long interestInterval;
    private long maxLoanAmount;
    private int minCreditScoreForLoan;

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        loadConfiguration();
        loadData();
        initializeManagers();
        registerServices();
        registerListeners();
        registerPlaceholders();
        startTasks();
        
        getLogger().info(getPluginName() + " 已启动");
        getLogger().info("存款利率: " + depositInterestRate + "%");
        getLogger().info("贷款利率: " + loanInterestRate + "%");
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (interestManager != null) {
            interestManager.shutdown();
        }
        
        if (loanManager != null) {
            loanManager.shutdown();
        }
        
        saveData();
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        getLogger().info(getPluginName() + " 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianBank";
    }
    
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
                getLogger().severe("无法创建数据文件: " + e.getMessage());
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
                getLogger().warning("加载账户失败: " + key + " - " + e.getMessage());
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
            getLogger().severe("保存数据失败: " + e.getMessage());
        }
    }
    
    private void initializeManagers() {
        interestManager = new InterestManager(this);
        loanManager = new LoanManager(this);
        
        getLogger().info("利息管理器已初始化");
        getLogger().info("贷款管理器已初始化");
    }
    
    private void registerServices() {
        serviceAdapter = new BankServiceAdapter(this);
        
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已注册到 RPGCore 服务系统");
        }
    }
    
    private void registerListeners() {
        if (rpgCore != null) {
            BankListener listener = new BankListener(this);
            listener.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，使用传统事件监听");
        }
    }
    
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BankPlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展");
        }
    }
    
    private void startTasks() {
        if (interestInterval > 0) {
            long taskId = scheduler.runSyncRepeating(() -> {
                interestManager.processAllInterest();
            }, interestInterval, interestInterval);
            getLogger().info("利息结算任务已启动，任务ID: " + taskId);
        }
        
        long saveTaskId = scheduler.runSyncRepeating(() -> {
            saveData();
        }, 6000L, 6000L);
        getLogger().info("自动保存任务已启动，任务ID: " + saveTaskId);
    }
    
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
    
    public void sendMessage(Player player, String message) {
        player.sendMessage(Component.text(message).color(NamedTextColor.YELLOW));
    }
    
    public void sendError(Player player, String message) {
        player.sendMessage(Component.text(message).color(NamedTextColor.RED));
    }
    
    public void sendSuccess(Player player, String message) {
        player.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
    }
    
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
