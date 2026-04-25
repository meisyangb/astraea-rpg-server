package cn.guangdian.classsystem;

import cn.guangdian.classsystem.adapter.ClassServiceAdapter;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.command.ClassCommandFramework;
import cn.guangdian.classsystem.command.ClassAdminCommandFramework;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.classsystem.gui.ClassAdvanceGUI;
import cn.guangdian.classsystem.gui.ClassInfoGUI;
import cn.guangdian.classsystem.gui.ClassMainGUI;
import cn.guangdian.classsystem.gui.ClassSelectionGUI;
import cn.guangdian.classsystem.gui.ClassAttributeGUI;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.classsystem.placeholder.ClassPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GuangDianClass extends AbstractRPGPlugin {
    
    private static GuangDianClass instance;
    
    private ClassManager classManager;
    private ExpManager expManager;
    private AttributeManager attributeManager;
    private ClassDataHandler dataHandler;
    private ClassServiceAdapter serviceAdapter;
    private ClassPlaceholder placeholder;
    private ClassAttributeGUI attributeGUI;
    private ClassMainGUI mainGUI;
    private ClassSelectionGUI selectionGUI;
    private ClassAdvanceGUI advanceGUI;
    private ClassInfoGUI infoGUI;

    private String defaultClassId;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        reloadConfig();
        
        defaultClassId = getConfig().getString("settings.default-class", "novice");
        
        classManager = new ClassManager(this);
        
        dataHandler = new ClassDataHandler(this);
        
        expManager = new ExpManager(this, classManager, dataHandler);
        
        attributeManager = new AttributeManager(this, dataHandler);
        
        serviceAdapter = new ClassServiceAdapter(this, classManager, expManager, dataHandler);
        
        attributeGUI = new ClassAttributeGUI(this, serviceAdapter, classManager);
        mainGUI = new ClassMainGUI(this, serviceAdapter);
        selectionGUI = new ClassSelectionGUI(this, serviceAdapter, classManager);
        advanceGUI = new ClassAdvanceGUI(this, serviceAdapter, classManager);
        infoGUI = new ClassInfoGUI(this, serviceAdapter, classManager);
        // 注意: InventoryDragEvent 由 RPGCore 的 GUIListener 统一处理

        registerCommands();
        
        registerPlaceholder();
        
        if (rpgCore != null) {
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，部分功能可能受限");
        }
        
        startAutoSave();
        
        getLogger().info("GuangDianClass 职业系统已启动!");
        getLogger().info("阶位系统: 1阶 - 9阶");
        getLogger().info("转职系统: 3阶一转 / 6阶二转 / 8阶三转 / 9阶神级");
        getLogger().info("属性点系统: 职业差异化属性加成");
    }
    
    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            placeholder.unregister();
            placeholder = null;
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (dataHandler != null) {
            dataHandler.saveAll();
            dataHandler.unregister();
        }
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        getLogger().info("GuangDianClass 职业系统已关闭!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianClass";
    }
    
    private ClassCommandFramework classCommand;
    private ClassAdminCommandFramework classAdminCommand;

    private void registerCommands() {
        // 使用 RPGCore CommandFramework 注册命令
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CommandFramework framework = CommandFramework.getInstance();

            // 注册主命令
            classCommand = new ClassCommandFramework(this, serviceAdapter, classManager, expManager, attributeManager);
            framework.registerCommand(classCommand);

            // 注册管理员命令
            classAdminCommand = new ClassAdminCommandFramework(this, serviceAdapter, classManager, attributeManager);
            framework.registerCommand(classAdminCommand);

            getLogger().info("已注册命令到 RPGCore CommandFramework");
        } else {
            // 降级处理：使用传统命令注册
            getLogger().warning("RPGCore 未加载，使用传统命令注册方式");
            registerLegacyCommands();
        }
    }

    /**
     * 传统命令注册方式 (降级处理)
     */
    private void registerLegacyCommands() {
        if (getCommand("class") != null) {
            getCommand("class").setExecutor((sender, command, label, args) -> {
                sender.sendMessage("§cRPGCore 未加载，职业系统功能受限！");
                return true;
            });
        }
        if (getCommand("classadmin") != null) {
            getCommand("classadmin").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("guangdian.class.admin")) {
                    sender.sendMessage("§c您没有权限执行此操作！");
                    return true;
                }
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    sender.sendMessage("§a配置已重新加载！");
                    return true;
                }
                sender.sendMessage("§e用法: /classadmin reload");
                return true;
            });
        }
    }
    
    private void registerPlaceholder() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholder = new ClassPlaceholder(this, serviceAdapter, classManager, expManager, attributeManager);
            placeholder.register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }
    }
    
    private void startAutoSave() {
        long interval = getConfig().getLong("settings.auto-save-interval-minutes", 5) * 60 * 20L;
        
        if (scheduler != null) {
            scheduler.runSyncRepeating(() -> {
                if (dataHandler != null) {
                    dataHandler.saveAll();
                    getLogger().info("自动保存完成，缓存玩家数: " + dataHandler.getCacheSize());
                }
            }, interval, interval);
        }
    }
    
    public void openMainGUI(Player player) {
        mainGUI.open(player);
    }

    public void openClassSelectionGUI(Player player) {
        selectionGUI.open(player);
    }

    public void openClassAdvanceGUI(Player player) {
        advanceGUI.open(player);
    }

    public void openClassInfoGUI(Player player) {
        infoGUI.open(player);
    }

    public void openAttributeGUI(Player player) {
        attributeGUI.open(player);
    }
    
    public static GuangDianClass getInstance() {
        return instance;
    }
    
    public ClassManager getClassManager() {
        return classManager;
    }
    
    public ExpManager getExpManager() {
        return expManager;
    }
    
    public AttributeManager getAttributeManager() {
        return attributeManager;
    }
    
    public ClassDataHandler getDataHandler() {
        return dataHandler;
    }
    
    public ClassService getService() {
        return serviceAdapter;
    }
    
    public String getDefaultClassId() {
        return defaultClassId;
    }
    
    public PlayerClassData getPlayerData(Player player) {
        return dataHandler.getPlayerData(player.getUniqueId());
    }
}
