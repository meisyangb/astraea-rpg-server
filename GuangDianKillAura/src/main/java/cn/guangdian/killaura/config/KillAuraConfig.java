package cn.guangdian.killaura.config;

import cn.guangdian.killaura.GuangDianKillAura;
import org.bukkit.configuration.file.FileConfiguration;

public class KillAuraConfig {

    private final GuangDianKillAura plugin;

    private boolean globalEnabled;
    private double defaultAttackRange;
    private double maxAttackRange;
    private long defaultAttackIntervalTicks;
    private long minAttackIntervalTicks;
    private boolean attackMonsters;
    private boolean attackPlayers;
    private boolean attackAnimals;
    private boolean requireLineOfSight;
    private boolean swingAnimation;
    private boolean damageIndicator;
    private double baseDamage;
    private boolean usePlayerDamage;
    private boolean mythicMobsOnly;
    private int maxTargetsPerScan;

    public KillAuraConfig(GuangDianKillAura plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        globalEnabled = config.getBoolean("global.enabled", true);
        defaultAttackRange = config.getDouble("attack.default-range", 4.5);
        maxAttackRange = config.getDouble("attack.max-range", 6.0);
        defaultAttackIntervalTicks = config.getLong("attack.default-interval-ticks", 20L);
        minAttackIntervalTicks = config.getLong("attack.min-interval-ticks", 10L);
        attackMonsters = config.getBoolean("target.monsters", true);
        attackPlayers = config.getBoolean("target.players", false);
        attackAnimals = config.getBoolean("target.animals", false);
        requireLineOfSight = config.getBoolean("target.require-line-of-sight", true);
        swingAnimation = config.getBoolean("visual.swing-animation", true);
        damageIndicator = config.getBoolean("visual.damage-indicator", true);
        baseDamage = config.getDouble("damage.base-damage", 1.0);
        usePlayerDamage = config.getBoolean("damage.use-player-damage", true);
        mythicMobsOnly = config.getBoolean("mythicmobs.only", false);
        maxTargetsPerScan = config.getInt("performance.max-targets-per-scan", 10);
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public void setGlobalEnabled(boolean globalEnabled) {
        this.globalEnabled = globalEnabled;
    }

    public double getDefaultAttackRange() {
        return defaultAttackRange;
    }

    public double getMaxAttackRange() {
        return maxAttackRange;
    }

    public long getDefaultAttackIntervalTicks() {
        return defaultAttackIntervalTicks;
    }

    public long getMinAttackIntervalTicks() {
        return minAttackIntervalTicks;
    }

    public boolean isAttackMonsters() {
        return attackMonsters;
    }

    public boolean isAttackPlayers() {
        return attackPlayers;
    }

    public boolean isAttackAnimals() {
        return attackAnimals;
    }

    public boolean isRequireLineOfSight() {
        return requireLineOfSight;
    }

    public boolean isSwingAnimation() {
        return swingAnimation;
    }

    public boolean isDamageIndicator() {
        return damageIndicator;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public boolean isUsePlayerDamage() {
        return usePlayerDamage;
    }

    public boolean isMythicMobsOnly() {
        return mythicMobsOnly;
    }

    public int getMaxTargetsPerScan() {
        return maxTargetsPerScan;
    }
}
