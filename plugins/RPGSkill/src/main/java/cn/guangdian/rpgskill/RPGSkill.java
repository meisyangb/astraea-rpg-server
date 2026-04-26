package cn.guangdian.rpgskill;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.logging.LoggerFactory;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgskill.api.RPGSkillAPI;
import cn.guangdian.rpgskill.command.SkillCommand;
import cn.guangdian.rpgskill.config.SkillConfigManager;
import cn.guangdian.rpgskill.config.RPGSkillConfigurateManager;
import cn.guangdian.rpgskill.cooldown.CooldownManager;
import cn.guangdian.rpgskill.data.SkillDataHandler;
import cn.guangdian.rpgskill.registry.SkillRegistry;
import cn.guangdian.rpgskill.service.SkillService;
import org.bukkit.Bukkit;
import org.slf4j.Logger;

public final class RPGSkill extends AbstractRPGPlugin {

    private static final Logger logger = LoggerFactory.getLogger(RPGSkill.class);

    private static RPGSkill instance;
    private SkillConfigManager configManager;
    private RPGSkillConfigurateManager configurateManager;
    private SkillRegistry skillRegistry;
    private CooldownManager cooldownManager;
    private SkillService skillService;
    private SkillDataHandler skillDataHandler;
    private RPGSkillAPI api;

    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected ExternalServiceIntegration externalServices;
    protected MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;

        rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            getLogger().severe("RPGCore 未加载，RPGSkill 无法启动");
            return;
        }

        scheduler = rpgCore.getScheduler();
        externalServices = rpgCore.getExternalServices();
        miniMessage = MiniMessageService.getInstance();

        // 初始化传统配置管理器
        configManager = new SkillConfigManager(this);
        configManager.loadAll();

        // 初始化 Configurate 配置管理器
        configurateManager = new RPGSkillConfigurateManager(this);
        configurateManager.load();

        // 初始化技能注册表
        skillRegistry = new SkillRegistry();
        skillRegistry.loadFromConfig(configManager.getSkillConfig());

        // 初始化冷却管理器
        cooldownManager = new CooldownManager();

        // 初始化技能服务
        skillService = new SkillService(this, skillRegistry, cooldownManager);

        // 初始化 AbstractPlayerDataHandler
        skillDataHandler = new SkillDataHandler(this, cooldownManager);
        skillDataHandler.register();

        // 初始化API
        api = new RPGSkillAPI(skillService);

        // 注册服务到 RPGCore 服务注册表
        rpgCore.getServiceRegistry().registerService(RPGSkillAPI.class, api);

        // 注册事件监听
        Bukkit.getPluginManager().registerEvents(new SkillListener(skillService), this);

        // 注册命令
        if (getCommand("rpgskill") != null) {
            getCommand("rpgskill").setExecutor(new SkillCommand(this));
        }

        logger.info("RPGSkill 已启动，加载了 {} 个技能", skillRegistry.getSkillCount());
        logger.info("Configurate 配置管理: {}", configurateManager.isConfigurateAvailable() ? "已启用" : "降级模式");
    }

    @Override
    protected void onPluginDisable() {
        // 注销 AbstractPlayerDataHandler
        if (skillDataHandler != null) {
            skillDataHandler.unregister();
        }

        // 注销服务
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(RPGSkillAPI.class);
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }

        logger.info("RPGSkill 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "RPGSkill";
    }

    public static RPGSkill getInstance() {
        return instance;
    }

    public RPGSkillAPI getAPI() {
        return api;
    }

    public SkillService getSkillService() {
        return skillService;
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public SkillConfigManager getConfigManager() {
        return configManager;
    }

    public RPGSkillConfigurateManager getConfigurateManager() {
        return configurateManager;
    }
}
