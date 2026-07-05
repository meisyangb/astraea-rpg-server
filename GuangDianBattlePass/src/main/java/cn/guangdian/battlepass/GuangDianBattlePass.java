package cn.guangdian.battlepass;

import cn.guangdian.battlepass.adapter.BattlePassServiceAdapter;
import cn.guangdian.battlepass.command.BattlePassAdminCommand;
import cn.guangdian.battlepass.command.BattlePassCommand;
import cn.guangdian.battlepass.gui.BattlePassGUI;
import cn.guangdian.battlepass.lifecycle.BattlePassDataHandler;
import cn.guangdian.battlepass.listener.BattlePassListener;
import cn.guangdian.battlepass.manager.BattlePassManager;
import cn.guangdian.battlepass.manager.ExpTriggerManager;
import cn.guangdian.battlepass.manager.RewardManager;
import cn.guangdian.battlepass.manager.SeasonManager;
import cn.guangdian.battlepass.storage.BattlePassStorage;
import cn.guangdian.battlepass.placeholder.BattlePassPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GuangDianBattlePass extends AbstractRPGPlugin {
    
    private static GuangDianBattlePass instance;
    
    private SeasonManager seasonManager;
    private RewardManager rewardManager;
    private BattlePassManager battlePassManager;
    private BattlePassGUI battlePassGUI;
    private ExpTriggerManager expTriggerManager;
    
    private BattlePassServiceAdapter serviceAdapter;
    private BattlePassDataHandler dataHandler;
    private BattlePassStorage bpStorage;
    private int bpSaveTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        seasonManager = new SeasonManager(this);
        rewardManager = new RewardManager(this);
        battlePassManager = new BattlePassManager(this, seasonManager, rewardManager);
        battlePassGUI = new BattlePassGUI(this);
        expTriggerManager = new ExpTriggerManager(this);
        
        registerCommands();
        // SQLite
        bpStorage = new BattlePassStorage(this);
        if (bpStorage.init()) bpStorage.load();
        bpSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> { if (bpStorage != null) bpStorage.saveAllAsync(); }, 6000L, 6000L).getTaskId();
        
        registerListeners();
        registerAPI();
        startTasks();
        
        getLogger().info("战令系统已启动! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Astraea RPG Team");
    }
    
    @Override
    protected void onPluginDisable() {
        Bukkit.getScheduler().cancelTask(bpSaveTaskId);
        if (bpStorage != null) { bpStorage.saveAll(); bpStorage.close(); }
        if (serviceAdapter != null) serviceAdapter.unregister();
        if (dataHandler != null) dataHandler.unregister();
        if (scheduler != null) scheduler.cancelAllTasks();
        getLogger().info("战令系统已关闭!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianBattlePass";
    }
    
    private void registerCommands() {
        getCommand("battlepass").setExecutor(new BattlePassCommand(this));
        getCommand("battlepassadmin").setExecutor(new BattlePassAdminCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BattlePassListener(this), this);
        getServer().getPluginManager().registerEvents(battlePassGUI, this);
        
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            dataHandler = new BattlePassDataHandler(this);
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        }
    }
    
    private void registerAPI() {
        serviceAdapter = new BattlePassServiceAdapter(this);
        
        // TODO: 添加 PlaceholderAPI.jar 到 libs 目录后启用
        // if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
        //     new BattlePassPlaceholder(this).register();
        //     getLogger().info("已注册 PlaceholderAPI 扩展!");
        // }
    }
    
    private void startTasks() {
        scheduler.runSyncRepeating(() -> {
            expTriggerManager.resetDailyCounts();
        }, 0L, 20L * 60 * 60 * 24);
    }
    
    public void givePoints(Player player, int amount) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            externalServices.deposit(player, amount);
        }
    }
    
    public void giveMoney(Player player, int amount) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            externalServices.deposit(player, amount);
        }
    }
    
    public boolean takePoints(Player player, int amount) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            return externalServices.withdraw(player, amount);
        }
        return false;
    }
    
    public long getPoints(Player player) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            return (long) externalServices.getBalance(player);
        }
        return 0;
    }
    
    public static GuangDianBattlePass getInstance() {
        return instance;
    }
    
    public cn.guangdian.rpgcore.api.SyncScheduler getScheduler() {
        return scheduler;
    }
    
    public SeasonManager getSeasonManager() {
        return seasonManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public BattlePassManager getBattlePassManager() {
        return battlePassManager;
    }
    
    public BattlePassGUI getBattlePassGUI() {
        return battlePassGUI;
    }
    
    public ExpTriggerManager getExpTriggerManager() {
        return expTriggerManager;
    }
}
