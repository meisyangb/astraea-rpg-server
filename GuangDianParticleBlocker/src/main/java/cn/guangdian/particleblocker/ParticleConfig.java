package cn.guangdian.particleblocker;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 粒子配置管理
 */
public class ParticleConfig {

    private final GuangDianParticleBlocker plugin;

    // 全局屏蔽所有粒子
    private boolean globalBlocked = false;

    // 监听模式：不屏蔽任何粒子，只记录所有粒子名
    private boolean monitorMode = false;

    // 屏蔽特定类型的粒子
    private Set<String> blockedTypes = new HashSet<>();

    // 白名单模式：只显示这些粒子
    private boolean whitelistMode = false;
    private Set<String> whitelist = new HashSet<>();

    // 玩家个人屏蔽设置
    private boolean perPlayerEnabled = true;

    public ParticleConfig(GuangDianParticleBlocker plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        var config = plugin.getConfig();

        globalBlocked = config.getBoolean("global.block-all", false);
        monitorMode = config.getBoolean("global.monitor-mode", false);
        whitelistMode = config.getBoolean("global.whitelist-mode", false);
        perPlayerEnabled = config.getBoolean("global.per-player-enabled", true);

        // 加载屏蔽列表
        List<String> blockedList = config.getStringList("blocked-types");
        blockedTypes = new HashSet<>(blockedList);

        // 加载白名单
        List<String> whitelistList = config.getStringList("whitelist");
        whitelist = new HashSet<>(whitelistList);

        // 如果配置为屏蔽所有，自动添加所有粒子类型
        if (globalBlocked) {
            for (Particle particle : Particle.values()) {
                blockedTypes.add(particle.name());
            }
        }

        plugin.getLogger().info("配置加载完成:");
        plugin.getLogger().info("  全局屏蔽: " + globalBlocked);
        plugin.getLogger().info("  监听模式: " + monitorMode);
        plugin.getLogger().info("  白名单模式: " + whitelistMode);
        plugin.getLogger().info("  屏蔽粒子数: " + blockedTypes.size());
        plugin.getLogger().info("  白名单粒子数: " + whitelist.size());
    }

    public void save() {
        var config = plugin.getConfig();

        config.set("global.block-all", globalBlocked);
        config.set("global.monitor-mode", monitorMode);
        config.set("global.whitelist-mode", whitelistMode);
        config.set("global.per-player-enabled", perPlayerEnabled);
        config.set("blocked-types", blockedTypes.stream().toList());
        config.set("whitelist", whitelist.stream().toList());

        plugin.saveConfig();
    }

    /**
     * 检查粒子是否应该被屏蔽（基于 Bukkit Particle 枚举）
     */
    public boolean shouldBlock(Particle particle) {
        return shouldBlockByName(particle.name());
    }

    /**
     * 检查粒子是否应该被屏蔽（基于注册名，如 "FLAME", "HEART"）
     */
    public boolean shouldBlockByName(String particleName) {
        String name = particleName.toUpperCase();

        // 白名单模式：只显示白名单中的粒子
        if (whitelistMode) {
            return !whitelist.contains(name);
        }

        // 黑名单模式：屏蔽黑名单中的粒子
        return blockedTypes.contains(name);
    }

    /**
     * 添加屏蔽粒子类型
     */
    public void addBlockedType(String particleName) {
        blockedTypes.add(particleName.toUpperCase());
        save();
    }

    /**
     * 移除屏蔽粒子类型
     */
    public void removeBlockedType(String particleName) {
        blockedTypes.remove(particleName.toUpperCase());
        save();
    }

    /**
     * 添加白名单粒子类型
     */
    public void addWhitelist(String particleName) {
        whitelist.add(particleName.toUpperCase());
        save();
    }

    /**
     * 移除白名单粒子类型
     */
    public void removeWhitelist(String particleName) {
        whitelist.remove(particleName.toUpperCase());
        save();
    }

    // Getters
    public boolean isGlobalBlocked() { return globalBlocked; }
    public boolean isMonitorMode() { return monitorMode; }
    public Set<String> getBlockedTypes() { return blockedTypes; }
    public boolean isWhitelistMode() { return whitelistMode; }
    public Set<String> getWhitelist() { return whitelist; }
    public boolean isPerPlayerEnabled() { return perPlayerEnabled; }

    // Setters
    public void setGlobalBlocked(boolean globalBlocked) {
        this.globalBlocked = globalBlocked;
        save();
    }

    public void setWhitelistMode(boolean whitelistMode) {
        this.whitelistMode = whitelistMode;
        save();
    }
}