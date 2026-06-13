package cn.guangdian.soulbind;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.soulbind.adapter.SoulBindServiceAdapter;
import cn.guangdian.soulbind.api.SoulBindService;
import cn.guangdian.soulbind.command.SoulBindCommand;
import cn.guangdian.soulbind.hook.MythicMobsHook;
import cn.guangdian.soulbind.listener.SoulBindListener;
import cn.guangdian.soulbind.manager.ConfigManager;
import org.bukkit.Bukkit;

public class GuangDianSoulBind extends AbstractRPGPlugin {

    private static GuangDianSoulBind instance;
    private SoulBindServiceAdapter serviceAdapter;
    private MythicMobsHook mythicMobsHook;
    private ConfigManager configManager;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        miniMessage = MiniMessageService.getInstance();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        mythicMobsHook = new MythicMobsHook();
        mythicMobsHook.init();

        serviceAdapter = new SoulBindServiceAdapter(this);

        Bukkit.getPluginManager().registerEvents(new SoulBindListener(this), this);

        SoulBindCommand command = new SoulBindCommand(this);
        var pluginCommand = getCommand("soulbind");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("灵魂绑定系统已启动");
    }

    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        getLogger().info("灵魂绑定系统已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianSoulBind";
    }

    public static GuangDianSoulBind getInstance() {
        return instance;
    }

    public SoulBindService getService() {
        return serviceAdapter;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }
}
