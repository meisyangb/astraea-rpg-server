package cn.guangdian.killaura;

import cn.guangdian.killaura.adapter.KillAuraServiceAdapter;
import cn.guangdian.killaura.command.KillAuraCommand;
import cn.guangdian.killaura.config.KillAuraConfig;
import cn.guangdian.killaura.listener.KillAuraListener;
import cn.guangdian.killaura.manager.AttackManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.command.PluginCommand;

public class GuangDianKillAura extends AbstractRPGPlugin {

    private static GuangDianKillAura instance;

    private KillAuraConfig killAuraConfig;
    private AttackManager attackManager;
    private KillAuraServiceAdapter serviceAdapter;

    private GameLogger gameLogger;

    @Override
    protected void onPluginEnable() {
        instance = this;

        initCommonServices();
        initRPGCoreServices();

        killAuraConfig = new KillAuraConfig(this);
        killAuraConfig.load();

        attackManager = new AttackManager(this);
        attackManager.startAttackTask();

        getServer().getPluginManager().registerEvents(new KillAuraListener(this, attackManager), this);

        serviceAdapter = new KillAuraServiceAdapter(this, attackManager);

        PluginCommand cmd = getCommand("killaura");
        if (cmd != null) {
            KillAuraCommand commandHandler = new KillAuraCommand(this, attackManager);
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        logInfo("GuangDianKillAura 杀戮模式插件已启用!");
        logInfo("默认攻击范围: " + killAuraConfig.getDefaultAttackRange() + " 格");
        logInfo("默认攻击间隔: " + killAuraConfig.getDefaultAttackIntervalTicks() + " tick");
    }

    @Override
    protected void onPluginDisable() {
        if (attackManager != null) {
            attackManager.stopAttackTask();
            attackManager.clearAll();
        }

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        logInfo("GuangDianKillAura 杀戮模式插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianKillAura";
    }

    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.gameLogger = rpgCore.getGameLogger();
        } else {
            getLogger().warning("RPGCore 不可用，使用备用日志");
        }
    }

    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public static GuangDianKillAura getInstance() {
        return instance;
    }

    public KillAuraConfig getKillAuraConfig() {
        return killAuraConfig;
    }

    public AttackManager getAttackManager() {
        return attackManager;
    }

    public KillAuraServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    public RPGCore getRPGCore() {
        return rpgCore;
    }
}
