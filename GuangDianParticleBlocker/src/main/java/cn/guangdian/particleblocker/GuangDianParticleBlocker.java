package cn.guangdian.particleblocker;

import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;

/**
 * 粒子屏蔽插件
 *
 * 功能：
 * - 使用 ProtocolLib 拦截粒子数据包，减少客户端卡顿
 * - 可配置屏蔽特定类型的粒子
 * - 支持 Per-Player 粒子控制
 *
 * 性能优化：
 * - 在数据包发送前拦截，减少网络开销
 * - 避免大量粒子导致客户端未响应
 */
public class GuangDianParticleBlocker extends AbstractRPGPlugin {

    private static GuangDianParticleBlocker instance;
    private ParticleConfig config;
    private ParticleListener listener;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 检查 ProtocolLib 是否可用
        if (!isProtocolLibEnabled()) {
            getLogger().severe("========================================");
            getLogger().severe("GuangDianParticleBlocker 需要 ProtocolLib!");
            getLogger().severe("请安装 ProtocolLib 插件后重试");
            getLogger().severe("下载地址: https://ci.dmulloy2.net/job/ProtocolLib/");
            getLogger().severe("========================================");
            return;
        }

        // 加载配置
        config = new ParticleConfig(this);
        config.load();

        // 注册监听器（会同时初始化 ProtocolLib 监听器）
        listener = new ParticleListener(this, config);
        getServer().getPluginManager().registerEvents(listener, this);

        // 注册命令
        var cmd = getCommand("particleblocker");
        if (cmd != null) {
            var handler = new ParticleCommand(this, config);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        getLogger().info("========================================");
        getLogger().info("GuangDianParticleBlocker 粒子屏蔽插件已启用!");
        getLogger().info("使用 ProtocolLib 进行粒子拦截");
        getLogger().info("全局屏蔽: " + config.isGlobalBlocked());
        getLogger().info("屏蔽粒子类型数量: " + config.getBlockedTypes().size());
        getLogger().info("========================================");
    }

    @Override
    protected void onPluginDisable() {
        // 取消注册 ProtocolLib 监听器
        if (listener != null) {
            listener.unregister();
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianParticleBlocker 粒子屏蔽插件已禁用!");
    }

    /**
     * 检查 ProtocolLib 是否可用
     */
    private boolean isProtocolLibEnabled() {
        var pm = getServer().getPluginManager();
        var protocollib = pm.getPlugin("ProtocolLib");
        return protocollib != null && protocollib.isEnabled();
    }

    @Override
    protected String getPluginName() {
        return "GuangDianParticleBlocker";
    }

    public static GuangDianParticleBlocker getInstance() {
        return instance;
    }

    public ParticleConfig getParticleConfig() {
        return config;
    }

    public ParticleListener getListener() {
        return listener;
    }
}