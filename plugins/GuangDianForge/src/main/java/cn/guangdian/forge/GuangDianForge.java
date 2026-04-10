package cn.guangdian.forge;

import cn.guangdian.forge.adapter.ForgeServiceAdapter;
import cn.guangdian.forge.command.ForgeCommand;
import cn.guangdian.forge.command.ForgeGiveCommand;
import cn.guangdian.forge.command.ForgeAdminCommand;
import cn.guangdian.forge.hook.MythicMobsHook;
import cn.guangdian.forge.listener.ForgeListener;
import cn.guangdian.forge.listener.LearnRecipeListener;
import cn.guangdian.forge.listener.PlayerJoinQuitListener;
import cn.guangdian.forge.manager.PlayerDataManager;
import cn.guangdian.forge.manager.RecipeManager;
import cn.guangdian.forge.placeholder.ForgePlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * ForgePlugin - 锻造插件主类
 * 
 * <p>支持 RPGCore 服务框架集成，提供 ForgeService 服务。</p>
 * <p>支持 MythicMobs 自定义物品作为材料和锻造结果。</p>
 */
public class GuangDianForge extends JavaPlugin {
    private static GuangDianForge instance;
    private RecipeManager recipeManager;
    private PlayerDataManager playerDataManager;
    private ForgeServiceAdapter serviceAdapter;
    private MythicMobsHook mythicMobsHook;
    private boolean useRPGCore;

    @Override
    public void onEnable() {
        instance = this;
        
        // 检查 RPGCore 是否可用
        useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        saveDefaultConfig();
        saveResource("recipes.yml", false);
        
        // 初始化 MythicMobs Hook
        mythicMobsHook = new MythicMobsHook();
        mythicMobsHook.init();
        
        recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();
        
        playerDataManager = new PlayerDataManager(this);
        
        // 初始化服务适配器 (注册到 RPGCore)
        serviceAdapter = new ForgeServiceAdapter(this);
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new ForgeListener(this), this);
        getServer().getPluginManager().registerEvents(new LearnRecipeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        
        // 注册命令
        getCommand("forge").setExecutor(new ForgeCommand(this));
        getCommand("forgegive").setExecutor(new ForgeGiveCommand(this));
        getCommand("forgeadmin").setExecutor(new ForgeAdminCommand(this));
        
        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ForgePlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }
        
        getLogger().info("GuangDianForge 已启动! 加载了 " + recipeManager.getAllRecipes().size() + " 个图纸");
        if (useRPGCore) {
            getLogger().info("RPGCore 集成模式已启用");
        }
        if (mythicMobsHook.isEnabled()) {
            getLogger().info("MythicMobs 物品集成已启用");
        }
    }

    @Override
    public void onDisable() {
        // 注销服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        // 保存所有在线玩家数据 (使用 RPGCore 异步执行器或同步保存)
        Optional<AsyncExecutor> asyncExecutor = getAsyncExecutor();
        for (var player : getServer().getOnlinePlayers()) {
            var data = playerDataManager.get(player.getUniqueId());
            if (asyncExecutor.isPresent()) {
                asyncExecutor.get().execute(() -> playerDataManager.save(data));
            } else {
                playerDataManager.save(data);
            }
        }
        getLogger().info("GuangDianForge 已关闭，数据已保存");
    }

    public static GuangDianForge getInstance() { return instance; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ForgeServiceAdapter getServiceAdapter() { return serviceAdapter; }
    public MythicMobsHook getMythicMobsHook() { return mythicMobsHook; }
    
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