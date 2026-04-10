package cn.guangdian.aggro;

import cn.guangdian.aggro.adapter.AggroServiceAdapter;
import cn.guangdian.aggro.hook.MythicMobsHook;
import cn.guangdian.aggro.listener.AggroListener;
import cn.guangdian.aggro.manager.AggroManager;
import cn.guangdian.aggro.placeholder.AggroPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

public class GuangDianAggro extends AbstractRPGPlugin {

    private static GuangDianAggro instance;

    private AggroManager aggroManager;
    private MythicMobsHook mythicMobsHook;
    private AggroServiceAdapter serviceAdapter;
    private AggroPlaceholder placeholder;

    @Override
    protected void onPluginEnable() {
        instance = this;

        saveDefaultConfig();

        mythicMobsHook = new MythicMobsHook();
        mythicMobsHook.init();

        aggroManager = new AggroManager(this, mythicMobsHook);
        aggroManager.loadConfig();

        getServer().getPluginManager().registerEvents(new AggroListener(this, aggroManager), this);

        serviceAdapter = new AggroServiceAdapter(this, aggroManager);

        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            placeholder = new AggroPlaceholder(this, aggroManager);
            placeholder.register();
        }

        getLogger().info("GuangDianAggro 仇恨系统插件已启用!");
        getLogger().info("MythicMobs 集成: " + (mythicMobsHook.isEnabled() ? "已启用" : "未启用"));
    }

    @Override
    protected void onPluginDisable() {
        if (placeholder != null) {
            PlaceholderAPI.unregisterExpansion(placeholder);
            placeholder = null;
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        if (aggroManager != null) {
            aggroManager.stopDecayTask();
            aggroManager.clearAll();
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianAggro 仇恨系统插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianAggro";
    }

    public static GuangDianAggro getInstance() {
        return instance;
    }

    public AggroManager getAggroManager() {
        return aggroManager;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public RPGCore getRPGCore() {
        return rpgCore;
    }
}
