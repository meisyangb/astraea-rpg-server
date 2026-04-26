package cn.guangdian.rpgcore.permission;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 统一权限管理器
 * 
 * <p>提供所有GuangDian插件的统一权限节点管理。</p>
 * <p>支持权限注册、检查、前缀规范。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PermissionManager {

    private final JavaPlugin plugin;
    private final String prefix;
    private final Map<String, PermissionNode> permissions = new HashMap<>();

    /**
     * 创建权限管理器
     * 
     * @param plugin 插件实例
     * @param prefix 权限前缀（如 "guangdian.armorstats"）
     */
    public PermissionManager(JavaPlugin plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
    }

    /**
     * 注册权限节点
     * 
     * @param node 权限节点（不含前缀）
     * @param description 描述
     * @param defaultValue 默认值（true=所有人, op=仅OP, false=无人）
     * @return 完整权限节点
     */
    public String register(String node, String description, DefaultPermission defaultValue) {
        String fullPath = prefix + "." + node;
        permissions.put(fullPath, new PermissionNode(fullPath, description, defaultValue));
        return fullPath;
    }

    /**
     * 注册命令权限
     */
    public String registerCommand(String command, String description) {
        return register("command." + command, description, DefaultPermission.OP);
    }

    /**
     * 注册管理权限
     */
    public String registerAdmin(String node, String description) {
        return register("admin." + node, description, DefaultPermission.OP);
    }

    /**
     * 注册功能权限
     */
    public String registerFeature(String node, String description, DefaultPermission defaultValue) {
        return register("feature." + node, description, defaultValue);
    }

    /**
     * 检查玩家是否有权限
     */
    public boolean hasPermission(Player player, String node) {
        if (node == null || node.isEmpty()) {
            return false;
        }
        
        // 验证权限节点不包含路径穿越字符
        if (node.contains("..") || node.contains("//")) {
            return false;
        }
        
        String fullPath = permissions.containsKey(node) ? node : prefix + "." + node;
        return player.hasPermission(fullPath);
    }

    /**
     * 检查玩家是否有管理权限
     */
    public boolean hasAdminPermission(Player player) {
        return player.hasPermission(prefix + ".admin") || player.isOp();
    }

    /**
     * 获取所有已注册的权限
     */
    public Set<String> getRegisteredPermissions() {
        return permissions.keySet();
    }

    /**
     * 获取权限信息
     */
    public PermissionNode getPermissionInfo(String node) {
        return permissions.get(node);
    }

    /**
     * 生成权限配置（用于plugin.yml）
     */
    public String generatePluginYmlPermissions() {
        StringBuilder sb = new StringBuilder();
        sb.append("permissions:\n");
        
        for (Map.Entry<String, PermissionNode> entry : permissions.entrySet()) {
            PermissionNode perm = entry.getValue();
            sb.append("  ").append(perm.getFullPath().replace(prefix + ".", "")).append(":\n");
            sb.append("    description: ").append(perm.getDescription()).append("\n");
            sb.append("    default: ").append(perm.getDefaultValue().getYmlValue()).append("\n");
        }
        
        return sb.toString();
    }

    // ========== 预定义权限模板 ==========

    /**
     * 注册标准权限集（命令+管理+基础功能）
     */
    public void registerStandardPermissions() {
        // 管理员权限
        registerAdmin("*", "所有管理权限");
        
        // 重载权限
        registerCommand("reload", "重载插件配置");
        
        // 帮助权限
        registerCommand("help", "查看帮助");
        
        // 信息权限
        registerCommand("info", "查看插件信息");
    }

    /**
     * 权限节点数据类
     */
    public static class PermissionNode {
        private final String fullPath;
        private final String description;
        private final DefaultPermission defaultValue;

        public PermissionNode(String fullPath, String description, DefaultPermission defaultValue) {
            this.fullPath = fullPath;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getFullPath() { return fullPath; }
        public String getDescription() { return description; }
        public DefaultPermission getDefaultValue() { return defaultValue; }
    }

    /**
     * 默认权限枚举
     */
    public enum DefaultPermission {
        TRUE("true"),
        OP("op"),
        FALSE("false");

        private final String ymlValue;

        DefaultPermission(String ymlValue) {
            this.ymlValue = ymlValue;
        }

        public String getYmlValue() {
            return ymlValue;
        }
    }
}