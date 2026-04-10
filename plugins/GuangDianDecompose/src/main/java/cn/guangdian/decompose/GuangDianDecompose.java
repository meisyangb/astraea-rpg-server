package cn.guangdian.decompose;

import cn.guangdian.decompose.adapter.DecomposeServiceAdapter;
import cn.guangdian.decompose.command.DecomposeAdminCommand;
import cn.guangdian.decompose.command.DecomposeCommand;
import cn.guangdian.decompose.gui.DecomposeGUI;
import cn.guangdian.decompose.hook.MythicMobsHook;
import cn.guangdian.decompose.listener.DecomposeListener;
import cn.guangdian.decompose.manager.DecomposeManager;
import cn.guangdian.decompose.manager.RuleManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class GuangDianDecompose extends JavaPlugin {

    private static GuangDianDecompose instance;
    private RPGCore rpgCore;
    private MythicMobsHook mythicMobsHook;
    private RuleManager ruleManager;
    private DecomposeManager decomposeManager;
    private DecomposeGUI decomposeGUI;
    private DecomposeServiceAdapter serviceAdapter;

    @Override
    public void onEnable() {
        instance = this;

        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        saveDefaultRules();

        mythicMobsHook = new MythicMobsHook();
        ruleManager = new RuleManager(this);
        decomposeManager = new DecomposeManager(this);
        decomposeGUI = new DecomposeGUI(this);

        registerService();
        registerCommands();
        registerListeners();

        getLogger().info("GuangDianDecompose 装备分解系统已启动!");
    }

    @Override
    public void onDisable() {
        if (serviceAdapter != null && rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.unregisterService(DecomposeServiceAdapter.class);
            }
        }
        getLogger().info("GuangDianDecompose 装备分解系统已关闭!");
    }

    private boolean checkDependencies() {
        rpgCore = (RPGCore) getServer().getPluginManager().getPlugin("RPGCore");
        if (rpgCore == null) {
            getLogger().severe("未找到 RPGCore 插件，无法启动!");
            return false;
        }

        if (!getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            getLogger().severe("未找到 MythicMobs 插件，无法启动!");
            return false;
        }

        return true;
    }

    private void saveDefaultRules() {
        File rulesFile = new File(getDataFolder(), "rules.yml");
        if (!rulesFile.exists()) {
            saveResource("rules.yml", false);
        }
    }

    private void registerService() {
        serviceAdapter = new DecomposeServiceAdapter(this);
        ServiceRegistry registry = rpgCore.getServiceRegistry();
        if (registry != null) {
            registry.registerService(DecomposeServiceAdapter.class, serviceAdapter);
            getLogger().info("已注册 DecomposeService 到 RPGCore!");
        }
    }

    private void registerCommands() {
        getCommand("decompose").setExecutor(new DecomposeCommand(this));
        getCommand("decomposeadmin").setExecutor(new DecomposeAdminCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DecomposeListener(this), this);
    }

    public void reloadAllConfig() {
        reloadConfig();
        ruleManager.loadRules();
        getLogger().info("配置已重载!");
    }

    public static GuangDianDecompose getInstance() {
        return instance;
    }

    public RPGCore getRpgCore() {
        return rpgCore;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public DecomposeManager getDecomposeManager() {
        return decomposeManager;
    }

    public DecomposeGUI getDecomposeGUI() {
        return decomposeGUI;
    }

    public DecomposeServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    public void debug(String message) {
        if (isDebug()) {
            getLogger().info("[调试] " + message);
        }
    }
}
