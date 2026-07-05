package cn.guangdian.forge;

import cn.guangdian.forge.adapter.ForgeServiceAdapter;
import cn.guangdian.forge.command.ForgeCommand;
import cn.guangdian.forge.command.ForgeAdminCommand;
import cn.guangdian.forge.command.ForgeTabCompleter;
import cn.guangdian.forge.hook.RPGItemsHook;
import cn.guangdian.forge.listener.ForgeListener;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.listener.PlayerJoinQuitListener;
import cn.guangdian.forge.manager.PlayerDataManager;
import cn.guangdian.forge.manager.RecipeManager;
import cn.guangdian.forge.storage.ForgeStorage;
import cn.guangdian.forge.placeholder.ForgePlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.Optional;

/**
 * GuangDianForge - 光点锻造插件主类
 * 
 * <p>本插件已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志系统 - 使用 RPGCore GameLogger（带降级兼容）</li>
 *   <li>异步执行 - 使用 RPGCore AsyncExecutor</li>
 *   <li>消息发送 - 使用 MiniMessage 格式</li>
 * </ul>
 * 
 * <p>支持 RPGItems 自定义物品作为材料和锻造结果。</p>
 * 
 * @author GuangDian
 * @version 1.1.0
 * @since 1.0.0
 * @see OPTIMIZATION.md 优化详情
 */
public class GuangDianForge extends AbstractRPGPlugin {
    private static GuangDianForge instance;
    private RecipeManager recipeManager;
    private PlayerDataManager playerDataManager;
    private ForgeServiceAdapter serviceAdapter;
    private RPGItemsHook rpgItemsHook;
    private boolean useRPGCore;
    private ForgeStorage forgeStorage;
    private int forgeSaveId = -1;
    
    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;

    @Override
    protected void onPluginEnable() {
        instance = this;
        
        // 检查 RPGCore 是否可用
        useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        // 初始化 RPGCore 服务
        initRPGCoreServices();
        
        saveDefaultConfig();
        saveResource("recipes.yml", false);
        
        // 初始化 RPGItems Hook
        rpgItemsHook = new RPGItemsHook();
        rpgItemsHook.init();
        
        recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();
        
        playerDataManager = new PlayerDataManager(this);
        
        forgeStorage = new ForgeStorage(this);
        if (forgeStorage.init()) forgeStorage.load();
        forgeSaveId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> { if (forgeStorage != null) forgeStorage.saveAsync(); }, 6000L, 6000L).getTaskId();
        
        // 初始化服务适配器 (注册到 RPGCore)
        serviceAdapter = new ForgeServiceAdapter(this);
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new ForgeListener(this), this);
        getServer().getPluginManager().registerEvents(new LearnRecipeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        
        // 注册命令
        var forgeCmd = getCommand("forge");
        if (forgeCmd != null) {
            forgeCmd.setExecutor(new ForgeCommand(this));
            forgeCmd.setTabCompleter(new ForgeTabCompleter(this));
        }
        
        var adminCmd = getCommand("forgeadmin");
        if (adminCmd != null) {
            adminCmd.setExecutor(new ForgeAdminCommand(this));
            adminCmd.setTabCompleter(new ForgeTabCompleter(this));
        }
        
        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ForgePlaceholder(this).register();
            logInfo("已注册 PlaceholderAPI 扩展!");
        }
        
        logInfo("GuangDianForge 已启动! 加载了 " + recipeManager.getAllRecipes().size() + " 个图纸");
        if (useRPGCore) {
            logInfo("RPGCore 集成模式已启用");
        }
        if (rpgItemsHook.isEnabled()) {
            logInfo("RPGItems 物品集成已启用");
        }
    }
    
    /**
     * 初始化 RPGCore 核心服务
     */
    private void initRPGCoreServices() {
        if (useRPGCore) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                gameLogger = rpgCore.getGameLogger();
                miniMessage = rpgCore.getMiniMessageService();
                if (miniMessage != null) {
                    miniMessageParser = miniMessage.getMiniMessage();
                }
                logInfo("已连接到 RPGCore 服务 (GameLogger, MiniMessageService)");
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
            logInfo("使用本地 MiniMessageService（降级）");
        }
    }

    @Override
    protected void onPluginDisable() {
        getServer().getScheduler().cancelTask(forgeSaveId);
        if (forgeStorage != null) { forgeStorage.save(); forgeStorage.close(); }
        if (serviceAdapter != null) serviceAdapter.unregister();
        if (scheduler != null) scheduler.cancelAllTasks();
        for (var player : getServer().getOnlinePlayers()) {
            var data = playerDataManager.get(player.getUniqueId());
            if (data != null) playerDataManager.save(data);
        }
        logInfo("GuangDianForge 已关闭，数据已保存");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianForge";
    }

    // ==================== RPGCore 服务访问 ====================

    public GameLogger getGameLogger() {
        return gameLogger;
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    /**
     * 获取 MiniMessage 解析器
     * @return MiniMessage 实例
     */
    public MiniMessage getMiniMessageParser() {
        return miniMessageParser;
    }

    /**
     * 检查是否使用 RPGCore 服务
     */
    public boolean isUsingRPGCoreLogger() {
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
    
    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        }
    }

    // ==================== 业务方法 ====================

    public static GuangDianForge getInstance() { return instance; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ForgeServiceAdapter getServiceAdapter() { return serviceAdapter; }
    public RPGItemsHook getRPGItemsHook() { return rpgItemsHook; }
    
    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() { return useRPGCore; }
    
    /**
     * 获取 RPGCore 异步执行器 (如果可用)
     */
    public Optional<AsyncExecutor> getAsyncExecutor() {
        if (useRPGCore && RPGCore.getInstance() != null) {
            return Optional.of(RPGCore.getInstance().getAsyncExecutor());
        }
        return Optional.empty();
    }
}
