package cn.guangdian.monthlycard;

import cn.guangdian.monthlycard.adapter.MonthlyCardServiceAdapter;
import cn.guangdian.monthlycard.api.MonthlyCardService;
import cn.guangdian.monthlycard.command.MonthlyCardCommand;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.monthlycard.lifecycle.MonthlyCardDataHandler;
import cn.guangdian.monthlycard.manager.MonthlyCardManager;
import cn.guangdian.monthlycard.placeholder.MonthlyCardPlaceholder;
import cn.guangdian.monthlycard.storage.MonthlyCardStorage;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class GuangDianMonthlyCard extends AbstractRPGPlugin {
    
    private static GuangDianMonthlyCard instance;
    
    private MonthlyCardManager cardManager;
    private MonthlyCardServiceAdapter serviceAdapter;
    private MonthlyCardDataHandler dataHandler;
    private MonthlyCardPlaceholder placeholder;
    private long checkTaskId;
    private MonthlyCardStorage mcStorage;
    private int mcSaveId = -1;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        cardManager = new MonthlyCardManager(this);
        cardManager.loadCardTypes();
        
        mcStorage = new MonthlyCardStorage(this);
        if (mcStorage.init()) mcStorage.load();
        mcSaveId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> { if (mcStorage != null) mcStorage.saveAsync(); }, 6000L, 6000L).getTaskId();
        
        registerCommands();
        registerLifecycle();
        registerPlaceholder();
        registerService();
        startTasks();
        
        getLogger().info("光点月卡插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
    }
    
    @Override
    protected void onPluginDisable() {
        Bukkit.getScheduler().cancelTask(mcSaveId);
        if (mcStorage != null) { mcStorage.save(); mcStorage.close(); }
        if (scheduler != null) {
            scheduler.cancelTask(checkTaskId);
            scheduler.cancelAllTasks();
        }
        if (cardManager != null) cardManager.saveAllData();
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (dataHandler != null) {
            dataHandler.unregister();
        }
        
        if (placeholder != null) {
            me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(placeholder);
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
}
