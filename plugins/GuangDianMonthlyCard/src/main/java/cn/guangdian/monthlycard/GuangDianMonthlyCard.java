package cn.guangdian.monthlycard;

import cn.guangdian.monthlycard.adapter.MonthlyCardServiceAdapter;
import cn.guangdian.monthlycard.api.MonthlyCardService;
import cn.guangdian.monthlycard.command.MonthlyCardCommand;
import cn.guangdian.monthlycard.config.ConfigManager;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.monthlycard.gui.MonthlyCardGUI;
import cn.guangdian.monthlycard.lifecycle.MonthlyCardDataHandler;
import cn.guangdian.monthlycard.listener.PrivilegeListener;
import cn.guangdian.monthlycard.manager.MonthlyCardManager;
import cn.guangdian.monthlycard.placeholder.MonthlyCardPlaceholder;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class GuangDianMonthlyCard extends AbstractRPGPlugin {

    private static GuangDianMonthlyCard instance;

    private ConfigManager configManager;
    private MonthlyCardManager cardManager;
    private MonthlyCardServiceAdapter serviceAdapter;
    private MonthlyCardDataHandler dataHandler;
    private MonthlyCardPlaceholder placeholder;
    private MonthlyCardGUI monthlyCardGUI;
    private long checkTaskId;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();
        
        cardManager = new MonthlyCardManager(this);
        cardManager.init(); // 初始化数据库
        cardManager.loadCardTypes();

        monthlyCardGUI = new MonthlyCardGUI(this);

        registerCommands();
        registerLifecycle();
        registerPlaceholder();
        registerService();
        registerListeners();
        startTasks();
        
        getLogger().info("光点月卡插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
    }
    
    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelTask(checkTaskId);
            scheduler.cancelAllTasks();
        }
        
        if (cardManager != null) {
            cardManager.shutdown();
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        if (placeholder != null) {
            placeholder.unregister();
        }
        
        getLogger().info("光点月卡插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianMonthlyCard";
    }
    
    private void registerCommands() {
        MonthlyCardCommand command = new MonthlyCardCommand(this);
        getCommand("monthlycard").setExecutor(command);
        getCommand("monthlycard").setTabCompleter(command);
    }
    
    private void registerLifecycle() {
        if (rpgCore != null) {
            dataHandler = new MonthlyCardDataHandler(this);
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，数据生命周期管理可能不完整");
        }
    }
    
    private void registerPlaceholder() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholder = new MonthlyCardPlaceholder(this);
            placeholder.register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }
    }
    
    private void registerService() {
        serviceAdapter = new MonthlyCardServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PrivilegeListener(this), this);
        getLogger().info("已注册特权监听器 (经验/掉落加成)!");
    }
    
    private void startTasks() {
        if (scheduler != null) {
            checkTaskId = scheduler.runSyncRepeating(() -> {
                cardManager.checkExpiredCards();
            }, 20L * 60 * 30, 20L * 60 * 30);
        }
    }
    
    public static GuangDianMonthlyCard getInstance() {
        return instance;
    }
    
    public MonthlyCardManager getCardManager() {
        return cardManager;
    }
    
    public MonthlyCardService getService() {
        return serviceAdapter;
    }
    
    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }
    
    public boolean hasActiveCard(Player player) {
        return cardManager.hasActiveCard(player.getUniqueId());
    }
    
    public boolean hasActiveCard(UUID playerId) {
        return cardManager.hasActiveCard(playerId);
    }
    
    public Optional<MonthlyCardType> getCardType(String typeId) {
        return cardManager.getCardType(typeId);
    }
    
    public MonthlyCardData getPlayerData(UUID playerId) {
        return cardManager.getPlayerData(playerId);
    }
    
    public int getRemainingDays(UUID playerId) {
        return (int) cardManager.getRemainingDays(playerId);
    }
    
    public boolean canClaimToday(UUID playerId) {
        return cardManager.canClaimToday(playerId);
    }
    
    public boolean claimDailyReward(UUID playerId) {
        return cardManager.claimDailyReward(playerId);
    }

    public MonthlyCardGUI getMonthlyCardGUI() {
        return monthlyCardGUI;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
