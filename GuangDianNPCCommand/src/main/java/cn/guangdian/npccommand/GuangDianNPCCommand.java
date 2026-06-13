package cn.guangdian.npccommand;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class GuangDianNPCCommand extends AbstractRPGPlugin {

    private static GuangDianNPCCommand instance;
    private RPGCore rpgCore;
    private SyncScheduler scheduler;
    private ExternalServiceIntegration externalServices;
    private MiniMessageService miniMessage;
    private NPCCommandService npcCommandService;
    private CooldownManager cooldownManager;

    @Override
    protected void onPluginEnable() {
        instance = this;
        rpgCore = RPGCore.getInstance();
        scheduler = rpgCore.getScheduler();
        externalServices = rpgCore.getExternalServices();
        miniMessage = MiniMessageService.getInstance();

        saveDefaultConfig();

        cooldownManager = new CooldownManager(this);
        npcCommandService = new NPCCommandService(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new NPCClickListener(this), this);

        getCommand("npcmd").setExecutor(new NPCCommandExecutor(this));
        getCommand("npcmd").setTabCompleter(new NPCCommandTabCompleter(this));

        getLogger().info(getPluginName() + " 已启动");
    }

    @Override
    protected void onPluginDisable() {
        if (npcCommandService != null) {
            npcCommandService.saveConfig();
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        getLogger().info(getPluginName() + " 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianNPCCommand";
    }

    public static GuangDianNPCCommand getInstance() {
        return instance;
    }

    public RPGCore getRPGCore() {
        return rpgCore;
    }

    public SyncScheduler getScheduler() {
        return scheduler;
    }

    public ExternalServiceIntegration getExternalServices() {
        return externalServices;
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    public NPCCommandService getNPCCommandService() {
        return npcCommandService;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
