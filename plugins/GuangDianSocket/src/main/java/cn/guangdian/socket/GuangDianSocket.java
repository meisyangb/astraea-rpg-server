package cn.guangdian.socket;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.socket.command.SocketCommand;
import cn.guangdian.socket.listener.SocketListener;
import cn.guangdian.socket.manager.SocketService;
import cn.guangdian.socket.parser.SocketParser;
import cn.guangdian.socket.storage.GemStorage;

/**
 * GuangDianSocket - 宝石镶嵌插件
 *
 * 职责：提供装备宝石镶嵌功能
 * - 宝石镶嵌/拆卸
 * - 宝石属性管理
 * - 镶嵌槽位管理
 * - 宝石数据独立存储
 *
 * 数据管理：
 * - 使用 PDC (PersistentDataContainer) 存储宝石数据到装备
 * - 完全独立管理，不依赖其他插件
 *
 * RPGCore 服务集成:
 * - MiniMessageService: 使用 RPGCore 统一消息服务进行文本格式化
 * - SyncScheduler: 使用 RPGCore 调度器进行任务调度
 * - ServiceRegistry: 注册 SocketService 供其他插件查询
 */
public class GuangDianSocket extends AbstractRPGPlugin {

    private static GuangDianSocket instance;
    private GemStorage gemStorage;
    private SocketService socketService;
    private CommandFramework commandFramework;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化配置
        saveDefaultConfig();

        // 初始化宝石数据存储
        gemStorage = new GemStorage(this);

        // 初始化解析器
        SocketParser.initialize(
            getConfig().getConfigurationSection("socket_patterns"),
            getConfig().getConfigurationSection("gem_types")
        );

        // 初始化服务
        socketService = new SocketService(this);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new SocketListener(this), this);

        // 初始化 RPGCore CommandFramework
        initCommandFramework();

        // 注册服务到 RPGCore
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(SocketService.class, socketService);
        }

        getLogger().info("GuangDianSocket 已启动 - 宝石镶嵌系统就绪");
        getLogger().info("宝石数据存储: PDC (PersistentDataContainer)");
    }
    
    /**
     * 初始化 RPGCore CommandFramework
     */
    private void initCommandFramework() {
        if (rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry.hasService(CommandFramework.class)) {
                commandFramework = registry.getService(CommandFramework.class);
                commandFramework.registerCommand(new SocketCommand(this));
                getLogger().info("已注册 RPGCore CommandFramework 命令");
            } else {
                getLogger().warning("CommandFramework 不可用，使用备用命令注册");
                registerCommandsFallback();
            }
        } else {
            getLogger().warning("RPGCore 不可用，使用备用命令注册");
            registerCommandsFallback();
        }
    }
    
    /**
     * 备用命令注册（当 RPGCore 不可用时）
     */
    private void registerCommandsFallback() {
        org.bukkit.command.PluginCommand socketCmd = getCommand("socket");
        if (socketCmd != null) {
            socketCmd.setExecutor(new SocketCommand(this));
        }
    }

    @Override
    protected void onPluginDisable() {
        // 注销服务
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(SocketService.class);
        }

        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianSocket 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianSocket";
    }

    public static GuangDianSocket getInstance() {
        return instance;
    }

    /**
     * 获取宝石数据存储管理器
     */
    public GemStorage getGemStorage() {
        return gemStorage;
    }

    /**
     * 获取宝石镶嵌服务
     */
    public SocketService getSocketService() {
        return socketService;
    }
}
