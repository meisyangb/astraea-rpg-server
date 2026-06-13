package cn.guangdian.soulbag;

import cn.guangdian.soulbag.command.SoulBagCommand;
import cn.guangdian.soulbag.data.SoulBagDataHandler;
import cn.guangdian.soulbag.gui.SoulBagGUI;
import cn.guangdian.soulbag.manager.SoulBagManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.PlayerLifecycleManager;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

public class GuangDianSoulBag extends AbstractRPGPlugin {
    
    private static GuangDianSoulBag instance;
    
    private SoulBagManager bagManager;
    private SoulBagGUI bagGUI;
    private SoulBagDataHandler dataHandler;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        if (!hookRPGCore()) {
            getLogger().severe("无法连接到 RPGCore，插件无法启动!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        saveDefaultConfig();
        
        initializeManagers();
        registerCommands();
        registerDataHandler();
        startAutoSave();
        
        getLogger().info(getPluginName() + " v" + getDescription().getVersion() + " 已启动");
        getLogger().info("背包容量: " + bagManager.getDefaultRows() + " 行 (" + bagManager.getDefaultSize() + " 格)");
    }
    
    @Override
    protected void onPluginDisable() {
        if (bagGUI != null) {
            bagGUI.closeAllBags();
        }
        
        cancelAllTasks();
        
        if (bagManager != null) {
            bagManager.saveData();
        }
        
        unregisterDataHandler();
        
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianSoulBag";
    }
    
    private void initializeManagers() {
        bagManager = new SoulBagManager(this);
        bagManager.loadConfiguration();
        bagManager.loadData();
        
        bagGUI = new SoulBagGUI(this);
        
        getLogger().info("管理器初始化完成");
    }
    
    private void registerCommands() {
        SoulBagCommand commandHandler = new SoulBagCommand(this);
        
        PluginCommand soulBagCmd = getCommand("soulbag");
        if (soulBagCmd != null) {
            soulBagCmd.setExecutor(commandHandler);
            soulBagCmd.setTabCompleter(commandHandler);
        }
        
        PluginCommand adminCmd = getCommand("soulbagadmin");
        if (adminCmd != null) {
            adminCmd.setExecutor(commandHandler);
            adminCmd.setTabCompleter(commandHandler);
        }
        
        getLogger().info("命令注册完成");
    }
    
    private void registerDataHandler() {
        if (rpgCore != null) {
            dataHandler = new SoulBagDataHandler(this);
            
            PlayerLifecycleManager lifecycleManager = rpgCore.getPlayerLifecycle();
            if (lifecycleManager != null) {
                lifecycleManager.registerHandler(dataHandler);
                getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
            } else {
                getLogger().warning("RPGCore PlayerLifecycleManager 不可用");
            }
        } else {
            getLogger().warning("RPGCore 未启用，数据处理器未注册");
        }
    }
    
    private void unregisterDataHandler() {
        if (rpgCore != null && dataHandler != null) {
            PlayerLifecycleManager lifecycleManager = rpgCore.getPlayerLifecycle();
            if (lifecycleManager != null) {
                lifecycleManager.unregisterHandler(dataHandler);
            }
        }
    }
    
    private void startAutoSave() {
        if (scheduler != null) {
            long saveTaskId = scheduler.runSyncRepeating(() -> {
                bagManager.saveData();
            }, 6000L, 6000L);
            getLogger().info("自动保存任务已启动，任务ID: " + saveTaskId);
        } else {
            getLogger().warning("调度器不可用，自动保存未启动");
        }
    }
    
    private boolean hookRPGCore() {
        var plugin = Bukkit.getPluginManager().getPlugin("RPGCore");
        if (plugin instanceof RPGCore core) {
            this.rpgCore = core;
            return true;
        }
        return false;
    }
    
    public static GuangDianSoulBag getInstance() {
        return instance;
    }
    
    public SoulBagManager getBagManager() {
        return bagManager;
    }
    
    public SoulBagGUI getBagGUI() {
        return bagGUI;
    }
}
