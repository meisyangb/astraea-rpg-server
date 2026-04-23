package cn.guangdian.armorstats.config;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 伤害调试配置管理器
 */
public class DamageDebugConfig {

    private static DamageDebugConfig instance;
    private static final GameLogger logger = RPGCore.getInstance().getGameLogger();
    
    private boolean enabled;
    private boolean attackLog;
    private boolean critLog;
    private boolean defenseLog;
    private boolean bossStatsLog;
    private boolean showCalculationSteps;

    public DamageDebugConfig() {
        instance = this;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = GuangDianArmorStats.getInstance().getConfig();
        
        enabled = config.getBoolean("damage_debug.enabled", false);
        
        String prefix = "damage_debug.interceptors.";
        attackLog = config.getBoolean(prefix + "attack", true);
        critLog = config.getBoolean(prefix + "crit", true);
        defenseLog = config.getBoolean(prefix + "defense", true);
        bossStatsLog = config.getBoolean(prefix + "boss_stats", true);
        
        showCalculationSteps = config.getBoolean("damage_debug.show_calculation_steps", true);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public static DamageDebugConfig getInstance() {
        if (instance == null) {
            instance = new DamageDebugConfig();
        }
        return instance;
    }

    public void logAttack(String message) {
        if (enabled && attackLog) {
            logger.info("[AttackInterceptor] " + message);
        }
    }

    public void logCrit(String message) {
        if (enabled && critLog) {
            logger.info("[CritInterceptor] " + message);
        }
    }

    public void logDefense(String message) {
        if (enabled && defenseLog) {
            logger.info("[DefenseInterceptor] " + message);
        }
    }

    public void logBossStats(String message) {
        if (enabled && bossStatsLog) {
            logger.info("[BossStatsInterceptor] " + message);
        }
    }

    public void logBossManager(String message) {
        if (enabled && bossStatsLog) {
            logger.info("[BossStatsManager] " + message);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAttackLog() {
        return attackLog;
    }

    public boolean isCritLog() {
        return critLog;
    }

    public boolean isDefenseLog() {
        return defenseLog;
    }

    public boolean isBossStatsLog() {
        return bossStatsLog;
    }

    public boolean isShowCalculationSteps() {
        return showCalculationSteps;
    }
}
