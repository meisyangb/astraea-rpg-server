package cn.guangdian.rpgitems;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgitems.api.ItemAttributeAPI;
import cn.guangdian.rpgitems.api.ItemAttributeAPIImpl;
import cn.guangdian.rpgitems.command.ItemCommand;
import cn.guangdian.rpgitems.config.ItemConfigManager;
import cn.guangdian.rpgitems.item.ItemFactory;
import cn.guangdian.rpgitems.listener.RPGItemsEventListener;
import cn.guangdian.rpgitems.registry.ItemRegistry;
import cn.guangdian.rpgitems.service.ItemService;

/**
 * RPGItems 插件 - 简化版
 * 只提供装备描述功能
 */
public final class RPGItems extends AbstractRPGPlugin {

    private static RPGItems instance;
    private ItemConfigManager configManager;
    private ItemRegistry itemRegistry;
    private ItemFactory itemFactory;
    private ItemService itemService;
    private ItemAttributeAPI attributeAPI;

    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
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
        miniMessage = MiniMessageService.getInstance();

        // 初始化配置管理器
        configManager = new ItemConfigManager(this);
        configManager.loadAll();

        // 初始化物品注册表
        itemRegistry = new ItemRegistry();
        itemRegistry.loadFromConfig(configManager.getItemConfig());

        // 初始化物品工厂
        itemFactory = new ItemFactory();

        // 初始化物品服务
        itemService = new ItemService(this, itemRegistry, itemFactory);

        // 初始化并注册属性 API
        attributeAPI = new ItemAttributeAPIImpl();
        getServer().getServicesManager().register(
            ItemAttributeAPI.class,
            attributeAPI,
            this,
            org.bukkit.plugin.ServicePriority.Normal
        );

        // 注册事件监听器（触发属性变更事件）
        getServer().getPluginManager().registerEvents(
            new RPGItemsEventListener(attributeAPI),
            this
        );

        // 注册命令
        if (getCommand("rpgitem") != null) {
            var cmd = new ItemCommand(this, itemService, itemRegistry);
            getCommand("rpgitem").setExecutor(cmd);
            getCommand("rpgitem").setTabCompleter(cmd);
        }

        getLogger().info("RPGItems 已启动，加载了 " + itemRegistry.getItemCount() + " 个物品");
        getLogger().info("ItemAttributeAPI 已注册，可供其他插件调用");
        getLogger().info("事件驱动架构已启用，将触发属性变更事件");
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

    /**
     * 获取物品属性 API
     * 其他插件可以通过此方法获取 API
     */
    public ItemAttributeAPI getAttributeAPI() {
        return attributeAPI;
    }
}
