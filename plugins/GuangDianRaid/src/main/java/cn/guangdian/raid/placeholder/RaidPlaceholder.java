package cn.guangdian.raid.placeholder;

import cn.guangdian.raid.GuangDianRaid;
import org.bukkit.Bukkit;

/**
 * PlaceholderAPI 占位符管理器
 */
public class RaidPlaceholder {

    private final GuangDianRaid plugin;
    private RaidPlaceholderExpansion expansion;

    public RaidPlaceholder(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    public void registerExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI 未安装，占位符功能已禁用");
            return;
        }

        try {
            expansion = new RaidPlaceholderExpansion(plugin);
            expansion.register();
            plugin.getLogger().info("PlaceholderAPI 占位符已注册: gdraid");
        } catch (Exception e) {
            plugin.getLogger().warning("注册 PlaceholderAPI 占位符失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregisterExpansion() {
        if (expansion != null) {
            try {
                expansion.unregister();
                plugin.getLogger().info("PlaceholderAPI 占位符已注销");
            } catch (Exception e) {
                plugin.getLogger().warning("注销 PlaceholderAPI 占位符失败: " + e.getMessage());
            }
        }
    }
}
