package cn.guangdian.rpgitems;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgitems.api.RPGItemsAPI;
import cn.guangdian.rpgitems.command.ItemCommand;
import cn.guangdian.rpgitems.config.ItemConfigManager;
import cn.guangdian.rpgitems.integration.RPGSkillIntegration;
import cn.guangdian.rpgitems.item.ItemFactory;
import cn.guangdian.rpgitems.listener.ItemSkillListener;
import cn.guangdian.rpgitems.registry.ItemRegistry;
import cn.guangdian.rpgitems.service.ItemService;
import org.bukkit.Bukkit;

public final class RPGItems extends AbstractRPGPlugin {

    private static RPGItems instance;
    private ItemConfigManager configManager;
    private ItemRegistry itemRegistry;
    private ItemFactory itemFactory;
    private ItemService itemService;
    private RPGItemsAPI api;
    private RPGSkillIntegration skillIntegration;

    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected ExternalServiceIntegration externalServices;
    protected MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            getLogger().severe("RPGCore 未加载，RPGItems 无法启动");
            return;
        }

        scheduler = rpgCore.getScheduler();
        externalServices = rpgCore.getExternalServices();
        miniMessage = MiniMessageService.getInstance();

        // 初始化配置管理器
        configManager = new ItemConfigManager(this);
        configManager.loadAll();

        // 初始化物品注册表
        itemRegistry = new ItemRegistry();
        itemRegistry.loadFromConfigs(configManager.getAllConfigs());

        // 初始化物品工厂
        itemFactory = new ItemFactory(this);

        // 初始化物品服务
        itemService = new ItemService(this, itemRegistry, itemFactory);

        // 初始化API
        api = new RPGItemsAPI(itemService);

        // 初始化 RPGSkill 集成（延迟加载，等待 RPGSkill 启动）
        scheduler.runSyncLater(() -> {
            skillIntegration = new RPGSkillIntegration(this);
            // 注册物品技能监听器
            Bukkit.getPluginManager().registerEvents(
                new ItemSkillListener(this, skillIntegration), this);
        }, 20L);

        // 注册命令
        if (getCommand("rpgitem") != null) {
            var cmd = new ItemCommand(this, itemService, itemRegistry);
            getCommand("rpgitem").setExecutor(cmd);
            getCommand("rpgitem").setTabCompleter(cmd);
        }

        getLogger().info("RPGItems 已启动，加载了 " + itemRegistry.getItemCount() + " 个物品");
    }

    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("RPGItems 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "RPGItems";
    }

    public static RPGItems getInstance() {
        return instance;
    }

    public RPGItemsAPI getAPI() {
        return api;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public ItemConfigManager getConfigManager() {
        return configManager;
    }

    public RPGSkillIntegration getSkillIntegration() {
        return skillIntegration;
    }
}
