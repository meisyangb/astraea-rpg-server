package cn.guangdian.armorstats.config;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 伤害调试配置管理器
 */
public class DamageDebugConfig {

    private static DamageDebugConfig instance;
    
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

    // ========== 日志输出方法 ==========

    public void logAttack(String message) {
        if (enabled && attackLog) {
            System.out.println("[AttackInterceptor] " + message);
        }
    }

    public void logCrit(String message) {
        if (enabled && critLog) {
            System.out.println("[CritInterceptor] " + message);
        }
    }

    public void logDefense(String message) {
        if (enabled && defenseLog) {
            System.out.println("[DefenseInterceptor] " + message);
        }
    }

    public void logBossStats(String message) {
        if (enabled && bossStatsLog) {
            System.out.println("[BossStatsInterceptor] " + message);
        }
    }

    public void logBossManager(String message) {
        if (enabled && bossStatsLog) {
            System.out.println("[BossStatsManager] " + message);
        }
    }

    // ========== Getters ==========

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