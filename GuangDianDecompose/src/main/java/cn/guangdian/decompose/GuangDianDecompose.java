package cn.guangdian.decompose;

import cn.guangdian.decompose.adapter.DecomposeServiceAdapter;
import cn.guangdian.decompose.command.DecomposeAdminCommand;
import cn.guangdian.decompose.command.DecomposeCommand;
import cn.guangdian.decompose.gui.DecomposeGUI;
import cn.guangdian.decompose.hook.RPGItemsHook;
import cn.guangdian.decompose.listener.DecomposeListener;
import cn.guangdian.decompose.manager.DecomposeManager;
import cn.guangdian.decompose.manager.RuleManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;

import java.io.File;

public class GuangDianDecompose extends AbstractRPGPlugin {

    private static GuangDianDecompose instance;
    private RPGCore rpgCore;
    private RPGItemsHook rpgItemsHook;
    private RuleManager ruleManager;
    private DecomposeManager decomposeManager;
    private DecomposeGUI decomposeGUI;
    private DecomposeServiceAdapter serviceAdapter;

    // RPGCore 服务引用
    private SoundService soundService;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();
        saveDefaultRules();

        rpgItemsHook = new RPGItemsHook();
        ruleManager = new RuleManager(this);
        decomposeManager = new DecomposeManager(this);
        decomposeGUI = new DecomposeGUI(this);

        registerService();
        registerCommands();
        registerListeners();

        getLogger().info("GuangDianDecompose 装备分解系统已启动!");
    }

    /**
     * 初始化 RPGCore 核心服务
     */
    private void initRPGCoreServices() {
        if (rpgCore != null) {
            soundService = rpgCore.getSoundService();
            miniMessage = rpgCore.getMiniMessageService();
            getLogger().info("已连接到 RPGCore 服务 (SoundService, MiniMessageService)");
        }

        // 如果 RPGCore 服务不可用，使用本地降级服务
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null && rpgCore != null) {
            ServiceRegistry registry = rpgCore.getServiceRegistry();
            if (registry != null) {
                registry.unregisterService(DecomposeServiceAdapter.class);
            }
        }
        getLogger().info("GuangDianDecompose 装备分解系统已关闭!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianDecompose";
    }

    private boolean checkDependencies() {
        rpgCore = (RPGCore) getServer().getPluginManager().getPlugin("RPGCore");
        if (rpgCore == null) {
            getLogger().severe("未找到 RPGCore 插件，无法启动!");
            return false;
        }

        if (!getServer().getPluginManager().isPluginEnabled("RPGItems")) {
            getLogger().severe("未找到 RPGItems 插件，无法启动!");
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

    public RPGItemsHook getRPGItemsHook() {
        return rpgItemsHook;
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

    /**
     * 获取 RPGCore SoundService
     * @return SoundService 实例
     */
    public SoundService getSoundService() {
        return soundService;
    }

    /**
     * 获取 RPGCore MiniMessageService
     * @return MiniMessageService 实例
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
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
