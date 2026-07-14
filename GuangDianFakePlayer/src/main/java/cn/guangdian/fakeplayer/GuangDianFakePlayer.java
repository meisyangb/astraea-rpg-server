package cn.guangdian.fakeplayer;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GuangDianFakePlayer - 简化版伪装玩家插件
 *
 * 功能：
 * 1. 修改服务器列表中显示的玩家在线数（包括API查询）
 * 2. 提供PlaceholderAPI占位符支持
 * 3. 在服务器玩家数据目录创建虚拟玩家数据文件
 * 4. 使用ProtocolLib拦截数据包，在TAB列表显示虚拟玩家
 * 5. 动态随机加入/退出虚拟玩家
 *
 * 模式：
 * - FIXED: 固定显示指定数量
 * - RANGE: 在范围内随机显示
 * - MULTIPLIER: 真实在线数乘以倍数
 * - ADD: 真实在线数加上固定值
 */
public class GuangDianFakePlayer extends JavaPlugin implements Listener {

    private FakePlayerConfig config;
    private PlaceholderAPIHook placeholderAPIHook;
    private VirtualPlayerDataManager dataManager;
    private VirtualPlayerPacketInterceptor packetInterceptor;
    private DynamicPlayerScheduler dynamicScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new FakePlayerConfig(this);

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);

        // 注册PlaceholderAPI扩展（如果启用）
        if (config.isPlaceholderapiEnabled() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("PlaceholderAPI 扩展已注册");
        }

        // 初始化虚拟玩家数据管理器（如果启用）
        if (config.isVirtualPlayersEnabled()) {
            dataManager = new VirtualPlayerDataManager(this);
            getLogger().info("虚拟玩家数据管理器已初始化");

            // 创建虚拟玩家数据文件
            dataManager.createVirtualPlayers(config.getVirtualPlayersCount());
            getLogger().info("已创建 " + config.getVirtualPlayersCount() + " 个虚拟玩家数据文件");
        }

        // 初始化数据包拦截器（如果启用且ProtocolLib可用）
        if (config.isVirtualPlayersEnabled() && config.isUseProtocolLib() && dataManager != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                packetInterceptor = new VirtualPlayerPacketInterceptor(this, dataManager);
                getLogger().info("数据包拦截器已初始化");
            } else {
                getLogger().warning("ProtocolLib 未安装，虚拟玩家功能已禁用");
            }
        }

        // 初始化动态调度器（如果启用）
        if (config.isDynamicEnabled() && dataManager != null) {
            dynamicScheduler = new DynamicPlayerScheduler(this, dataManager);
            dynamicScheduler.start();
            getLogger().info("动态玩家调度器已启动");
        }

        // 注册命令处理器
        getCommand("gdfakeplayer").setExecutor(new FakePlayerCommand(this));

        getLogger().info("GuangDianFakePlayer 已启动");
        getLogger().info("模式: " + config.getMode());
        int realOnline = Bukkit.getOnlinePlayers().size();
        int fakeOnline = config.getFakeOnlineCount(realOnline);
        int virtualCount = fakeOnline - realOnline;
        getLogger().info("真实在线: " + realOnline + ", 虚拟人数: " + virtualCount + ", 伪装在线: " + fakeOnline);
        getLogger().info("PlaceholderAPI: " + (config.isPlaceholderapiEnabled() ? "启用" : "禁用"));
        getLogger().info("虚拟玩家显示: " + (config.isVirtualPlayersEnabled() ? "启用（数据文件 + 数据包拦截）" : "禁用"));
    }

    @Override
    public void onDisable() {
        // 注销PlaceholderAPI扩展
        if (placeholderAPIHook != null) {
            placeholderAPIHook.unregister();
        }

        // 清理虚拟玩家数据文件（可选）
        if (dataManager != null) {
            // dataManager.clearAllVirtualPlayers(); // 不删除数据文件，保留虚拟玩家数据
            getLogger().info("虚拟玩家数据文件已保留");
        }

        // 停止动态调度器
        if (dynamicScheduler != null) {
            dynamicScheduler.stop();
            getLogger().info("动态玩家调度器已停止");
        }

        getLogger().info("GuangDianFakePlayer 已关闭");
    }

    /**
     * 拦截服务器列表 ping 事件
     * 这个事件会在服务器列表查询时触发，包括API查询（如 motd.minebbs.com）
     * 伪装在线数 = 真实在线数 + 虚拟人数
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(PaperServerListPingEvent event) {
        int realOnline = Bukkit.getOnlinePlayers().size();
        int fakeOnline = config.getFakeOnlineCount(realOnline); // 真实在线数 + 虚拟人数
        int virtualCount = fakeOnline - realOnline; // 虚拟人数
        int fakeMax = config.getFakeMaxPlayers();

        // 修改在线玩家数（这会影响API查询结果）
        event.setNumPlayers(fakeOnline);

        // 修改最大玩家数
        if (fakeMax > 0) {
            event.setMaxPlayers(fakeMax);
        }

        // 修改 MOTD
        if (config.hasCustomMotd()) {
            String motd = config.getMotd(realOnline, fakeOnline);
            event.setMotd(motd);
        }

        // 调试日志
        if (config.isDebug()) {
            getLogger().info("服务器列表ping拦截: 实在线=" + realOnline + ", 虚拟人数=" + virtualCount + ", 伪装在线=" + fakeOnline);
        }
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        super.reloadConfig();
        FakePlayerConfig oldConfig = config;
        config = new FakePlayerConfig(this);

        // 清理虚拟玩家数据文件（可选）
        if (dataManager != null) {
            // dataManager.clearAllVirtualPlayers(); // 不删除数据文件，保留虚拟玩家数据
            getLogger().info("虚拟玩家数据文件已保留");
        }

        // 先停止旧的动态调度器（如果存在）
        if (dynamicScheduler != null) {
            dynamicScheduler.stop();
            getLogger().info("旧的动态玩家调度器已停止");
        }

        // 先停止旧的数据包拦截器（如果存在）
        if (packetInterceptor != null) {
            packetInterceptor.stop();
            packetInterceptor = null;
            getLogger().info("旧的数据包拦截器已停止");
        }

        // 重新创建虚拟玩家数据文件（仅在启用时）
        if (config.isVirtualPlayersEnabled()) {
            if (dataManager == null) {
                dataManager = new VirtualPlayerDataManager(this);
            } else {
                dataManager.createVirtualPlayers(config.getVirtualPlayersCount());
            }
            getLogger().info("已创建 " + config.getVirtualPlayersCount() + " 个虚拟玩家数据文件");
        }

        // 重新初始化数据包拦截器（仅在启用且 ProtocolLib 可用时）
        if (config.isVirtualPlayersEnabled() && config.isUseProtocolLib() && dataManager != null && Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            packetInterceptor = new VirtualPlayerPacketInterceptor(this, dataManager);
            getLogger().info("数据包拦截器已重新初始化");
        }

        // 重新启动动态调度器（仅在启用时）
        if (config.isDynamicEnabled() && dataManager != null) {
            dynamicScheduler = new DynamicPlayerScheduler(this, dataManager);
            dynamicScheduler.start();
            getLogger().info("动态玩家调度器已启动");
        }

        getLogger().info("配置已重新加载");
        getLogger().info("模式: " + config.getMode());
        int realOnline = Bukkit.getOnlinePlayers().size();
        int fakeOnline = config.getFakeOnlineCount(realOnline);
        int virtualCount = fakeOnline - realOnline;
        getLogger().info("真实在线: " + realOnline + ", 虚拟人数: " + virtualCount + ", 伪装在线: " + fakeOnline);
        getLogger().info("PlaceholderAPI: " + (config.isPlaceholderapiEnabled() ? "启用" : "禁用"));
        getLogger().info("虚拟玩家显示: " + (config.isVirtualPlayersEnabled() ? "启用（数据文件 + 数据包拦截）" : "禁用"));
    }

    public FakePlayerConfig getFakePlayerConfig() {
        return config;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public VirtualPlayerDataManager getDataManager() {
        return dataManager;
    }

    public VirtualPlayerPacketInterceptor getPacketInterceptor() {
        return packetInterceptor;
    }

    public DynamicPlayerScheduler getDynamicScheduler() {
        return dynamicScheduler;
    }
}