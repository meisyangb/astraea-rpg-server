package cn.guangdian.classsystem;

import cn.guangdian.classsystem.adapter.ClassServiceAdapter;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.command.ClassCommand;
import cn.guangdian.classsystem.cooldown.CooldownManager;
import cn.guangdian.classsystem.cost.AdvancementCostManager;
import cn.guangdian.classsystem.data.ClassDataHandler;
import cn.guangdian.classsystem.effect.PassiveEffectManager;
import cn.guangdian.classsystem.storage.ClassStorage;
import cn.guangdian.classsystem.gui.AttributeGUI;
import cn.guangdian.classsystem.gui.ClassAdvanceGUI;
import cn.guangdian.classsystem.gui.ClassMainGUI;
import cn.guangdian.classsystem.gui.ClassSelectionGUI;
import cn.guangdian.classsystem.gui.GUIListener;
import cn.guangdian.classsystem.listener.ClassAttributeEventListener;
import cn.guangdian.classsystem.listener.SkillOrbListener;
import cn.guangdian.classsystem.listener.SkillSpaceGUIListener;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.manager.ManaManager;
import cn.guangdian.classsystem.manager.SkillSpaceManager;
import cn.guangdian.classsystem.message.MessageService;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.classsystem.placeholder.ClassPlaceholder;
import cn.guangdian.classsystem.restriction.ClassRestrictionManager;
import cn.guangdian.classsystem.restriction.RestrictionListener;
import cn.guangdian.classsystem.skill.SkillManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GuangDianClass extends AbstractRPGPlugin {
    
    private static GuangDianClass instance;
    
    private ClassManager classManager;
    private ExpManager expManager;
    private AttributeManager attributeManager;
    private ClassDataHandler dataHandler;
    private ClassServiceAdapter serviceAdapter;
    private ClassPlaceholder placeholder;
    private GUIListener guiListener;
    
    private MessageService messageService;
    private ClassRestrictionManager restrictionManager;
    private AdvancementCostManager costManager;
    private PassiveEffectManager effectManager;
    private SkillManager skillManager;
    private CooldownManager cooldownManager;
    private SkillSpaceManager skillSpaceManager;
    private ManaManager manaManager;
    private cn.guangdian.classsystem.skill.SkillEffectExecutor skillEffectExecutor;
    
    private String defaultClassId;
    private ClassStorage classStorage;
    private long classSaveTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        saveDefaultConfig();
        reloadConfig();
        
        defaultClassId = getConfig().getString("settings.default-class", "novice");
        
        messageService = new MessageService(this);
        
        classManager = new ClassManager(this);
        
        dataHandler = new ClassDataHandler(this);
        
        // SQLite 存储初始化（替代不安全的单文件 playerdata.yml）
        classStorage = new ClassStorage(this);
        if (classStorage.init()) classStorage.load();
        
        expManager = new ExpManager(this, classManager, dataHandler);
        
        attributeManager = new AttributeManager(this, dataHandler);
        
        restrictionManager = new ClassRestrictionManager(this);
        
        costManager = new AdvancementCostManager(this);
        
        effectManager = new PassiveEffectManager(this, classManager, attributeManager);
        
        skillManager = new SkillManager(this);
        
        cooldownManager = new CooldownManager(this);
        
        skillSpaceManager = new SkillSpaceManager(this);
        
        manaManager = new ManaManager(this);
        
        skillEffectExecutor = new cn.guangdian.classsystem.skill.SkillEffectExecutor(this);
        
        serviceAdapter = new ClassServiceAdapter(this, classManager, expManager, dataHandler, attributeManager);
        
        guiListener = new GUIListener();
        getServer().getPluginManager().registerEvents(guiListener, this);
        
        getServer().getPluginManager().registerEvents(new RestrictionListener(this, restrictionManager), this);
        
        // 注册属性变更事件监听器（触发属性变更事件）
        getServer().getPluginManager().registerEvents(new ClassAttributeEventListener(), this);

        // 注册经验获取监听器（击杀怪物等）
        cn.guangdian.classsystem.listener.ExpGainListener expGainListener =
            new cn.guangdian.classsystem.listener.ExpGainListener(this, expManager);
        getServer().getPluginManager().registerEvents(expGainListener, this);
        
        // 注册技能球监听器
        getServer().getPluginManager().registerEvents(new SkillOrbListener(this), this);
        
        // 注册技能空间GUI监听器
        getServer().getPluginManager().registerEvents(new SkillSpaceGUIListener(this), this);
        
        // 注册技能球保护监听器
        getServer().getPluginManager().registerEvents(
            new cn.guangdian.classsystem.listener.SkillOrbProtectionListener(this), this);
        
        // 注册技能投射物监听器
        getServer().getPluginManager().registerEvents(
            new cn.guangdian.classsystem.listener.SkillProjectileListener(this), this);
        
        // 注册玩家登录/登出监听器
        getServer().getPluginManager().registerEvents(new PlayerDataListener(), this);

        registerCommands();
        
        registerPlaceholder();
        
        if (rpgCore != null) {
            dataHandler.register();
            getLogger().info("已注册到 RPGCore PlayerLifecycleManager");
        } else {
            getLogger().warning("RPGCore 未启用，部分功能可能受限");
        }
        
        startAutoSave();
        
        startAfkExpTask();
        
        startManaRegenTask();
        
        getLogger().info("GuangDianClass 职业系统已启动!");
        getLogger().info("阶位系统: 1阶 - 9阶");
        getLogger().info("转职系统: 3阶一转 / 6阶二转 / 8阶三转 / 9阶神级");
        getLogger().info("属性点系统: 力量/体质/敏捷/智力/幸运");
        getLogger().info("新增功能: 事件系统/消息配置/职业限制/转职消耗/被动效果/技能系统/冷却系统");
    }
    
    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            try {
                me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(placeholder);
            } catch (Exception e) {
                getLogger().warning("注销占位符时发生错误: " + e.getMessage());
            }
        }
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        if (dataHandler != null) {
            dataHandler.saveAll();
            dataHandler.unregister();
        }
        
        if (classStorage != null) {
            classStorage.saveAll();
            classStorage.close();
        }
        
        if (skillSpaceManager != null) {
            // 保存所有在线玩家的技能数据
            for (Player player : getServer().getOnlinePlayers()) {
                skillSpaceManager.clearPlayerData(player);
            }
        }
        
        // 取消本插件注册的定时任务
        if (scheduler != null && classSaveTaskId >= 0) {
            scheduler.cancelTask(classSaveTaskId);
            classSaveTaskId = -1;
        }
        
        getLogger().info("GuangDianClass 职业系统已关闭!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianClass";
    }
    
    private void registerCommands() {
        ClassCommand commandHandler = new ClassCommand(this, serviceAdapter, classManager, expManager, attributeManager);
        
        var classCmd = getCommand("class");
        if (classCmd != null) {
            classCmd.setExecutor(commandHandler);
            classCmd.setTabCompleter(commandHandler);
        }
        
        var adminCmd = getCommand("classadmin");
        if (adminCmd != null) {
            adminCmd.setExecutor(commandHandler);
            adminCmd.setTabCompleter(commandHandler);
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
            classSaveTaskId = scheduler.runSyncRepeating(() -> {
                if (dataHandler != null) {
                    dataHandler.saveAll();
                }
                if (classStorage != null) {
                    classStorage.saveAllAsync();
                }
            }, interval, interval);
        }
    }
    
    private void startAfkExpTask() {
        if (!getConfig().getBoolean("exp-sources.afk.enabled", true)) {
            return;
        }
        
        long expPerMinute = getConfig().getLong("exp-sources.afk.exp-per-minute", 1);
        boolean messageEnabled = getConfig().getBoolean("exp-sources.afk.message-enabled", true);
        
        if (expPerMinute <= 0) {
            return;
        }
        
        if (scheduler != null) {
            scheduler.runSyncRepeating(() -> {
                for (Player player : getServer().getOnlinePlayers()) {
                    PlayerClassData data = dataHandler.getPlayerData(player.getUniqueId());
                    if (data == null) continue;
                    
                    String classId = data.getClassId();
                    if (classId == null || classId.equals(defaultClassId)) continue;
                    
                    expManager.addExp(player.getUniqueId(), expPerMinute);
                    
                    if (messageEnabled) {
                        messageService.sendFormattedMessage(player, "messages.success.afk-exp", 
                            "amount", String.valueOf(expPerMinute));
                    }
                }
            }, 1200L, 1200L);
            
            getLogger().info("挂机经验任务已启动: 每分钟 " + expPerMinute + " 点经验");
        }
    }
    
    /**
     * 启动魔力自动恢复任务
     * 每秒为所有在线玩家恢复 5% 最大魔力值
     */
    private void startManaRegenTask() {
        if (manaManager == null || scheduler == null) return;
        
        // 每秒执行一次（20 ticks = 1秒）
        scheduler.runSyncRepeating(() -> {
            for (Player player : getServer().getOnlinePlayers()) {
                manaManager.regenMana(player);
            }
        }, 20L, 20L);
        
        getLogger().info("魔力恢复任务已启动: 每秒恢复 5% 最大魔力");
    }
    
    public void openAttributeGUI(Player player) {
        AttributeGUI gui = new AttributeGUI(this, attributeManager, player);
        guiListener.registerGUI(player, gui);
        gui.open();
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
    
    public MessageService getMessageService() {
        return messageService;
    }
    
    public ClassRestrictionManager getRestrictionManager() {
        return restrictionManager;
    }
    
    public AdvancementCostManager getCostManager() {
        return costManager;
    }
    
    public PassiveEffectManager getEffectManager() {
        return effectManager;
    }
    
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public SkillSpaceManager getSkillSpaceManager() {
        return skillSpaceManager;
    }
    
    public ManaManager getManaManager() {
        return manaManager;
    }
    
    public cn.guangdian.classsystem.skill.SkillEffectExecutor getSkillEffectExecutor() {
        return skillEffectExecutor;
    }
    
    public void openClassSelectionGUI(Player player) {
        ClassSelectionGUI gui = new ClassSelectionGUI(this, classManager, player);
        guiListener.registerGUI(player, gui);
        gui.open();
    }
    
    public void openClassAdvanceGUI(Player player) {
        PlayerClassData data = getPlayerData(player);
        if (data == null) return;
        ClassAdvanceGUI gui = new ClassAdvanceGUI(this, classManager, player, data);
        guiListener.registerGUI(player, gui);
        gui.open();
    }
    
    public void openMainGUI(Player player) {
        ClassMainGUI gui = new ClassMainGUI(this, classManager, expManager, attributeManager, player);
        guiListener.registerGUI(player, gui);
        gui.open();
    }
    
    /**
     * 玩家数据监听器
     * 
     * 处理玩家登录/登出时的技能数据加载/保存
     */
    private class PlayerDataListener implements Listener {
        
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            
            // 加载技能数据
            if (skillSpaceManager != null) {
                skillSpaceManager.loadPlayerData(player);
                
                // 根据职业等级解锁技能
                PlayerClassData classData = getPlayerData(player);
                if (classData != null) {
                    skillSpaceManager.unlockSkillsByTier(player, classData.getTier());
                }
            }
            
            // 初始化魔力值
            if (manaManager != null) {
                manaManager.initializeMana(player);
            }
        }
        
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            
            // 保存并清理技能数据
            if (skillSpaceManager != null) {
                skillSpaceManager.clearPlayerData(player);
            }
        }
    }
}
