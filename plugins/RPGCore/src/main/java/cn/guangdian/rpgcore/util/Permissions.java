package cn.guangdian.rpgcore.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一权限节点常量类
 * 
 * <p>定义所有自研插件的统一权限节点命名规范。</p>
 * 
 * <h3>命名规范：</h3>
 * <ul>
 *   <li>基础权限: guangdian.{plugin}.{action}</li>
 *   <li>管理员权限: guangdian.{plugin}.admin</li>
 *   <li>特殊权限: guangdian.{plugin}.{feature}.{action}</li>
 * </ul>
 * 
 * <h3>示例：</h3>
 * <pre>
 * guangdian.points.balance     - 查看余额
 * guangdian.points.admin       - 管理员权限
 * guangdian.armor.reload       - 重载配置
 * </pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public final class Permissions {

    private Permissions() {
        // 常量类，不允许实例化
    }

    // ==================== 权限节点前缀 ====================
    
    /** 统一前缀 */
    public static final String PREFIX = "guangdian.";
    
    /** 管理员权限后缀 */
    public static final String ADMIN_SUFFIX = ".admin";

    // ==================== ArmorStats 权限 ====================
    
    /** ArmorStats 管理员权限 */
    public static final String ARMOR_ADMIN = PREFIX + "armor" + ADMIN_SUFFIX;
    
    /** ArmorStats 重载配置 */
    public static final String ARMOR_RELOAD = PREFIX + "armor.reload";
    
    /** ArmorStats 查看属性 */
    public static final String ARMOR_VIEW = PREFIX + "armor.view";
    
    /** ArmorStats 重置属性 */
    public static final String ARMOR_RESET = PREFIX + "armor.reset";

    // ==================== Points 权限 ====================
    
    /** Points 管理员权限 */
    public static final String POINTS_ADMIN = PREFIX + "points" + ADMIN_SUFFIX;
    
    /** Points 查看余额 */
    public static final String POINTS_BALANCE = PREFIX + "points.balance";
    
    /** Points 给予点券 */
    public static final String POINTS_GIVE = PREFIX + "points.give";
    
    /** Points 扣除点券 */
    public static final String POINTS_TAKE = PREFIX + "points.take";
    
    /** Points 设置余额 */
    public static final String POINTS_SET = PREFIX + "points.set";
    
    /** Points 重载配置 */
    public static final String POINTS_RELOAD = PREFIX + "points.reload";

    // ==================== Cave 权限 ====================
    
    /** Cave 管理员权限 */
    public static final String CAVE_ADMIN = PREFIX + "cave" + ADMIN_SUFFIX;
    
    /** Cave 创建洞府 */
    public static final String CAVE_CREATE = PREFIX + "cave.create";
    
    /** Cave 进入洞府 */
    public static final String CAVE_HOME = PREFIX + "cave.home";
    
    /** Cave 邀请玩家 */
    public static final String CAVE_INVITE = PREFIX + "cave.invite";
    
    /** Cave 升级洞府 */
    public static final String CAVE_UPGRADE = PREFIX + "cave.upgrade";
    
    /** Cave 重载配置 */
    public static final String CAVE_RELOAD = PREFIX + "cave.reload";

    // ==================== Forge 权限 ====================
    
    /** Forge 管理员权限 */
    public static final String FORGE_ADMIN = PREFIX + "forge" + ADMIN_SUFFIX;
    
    /** Forge 使用锻造 */
    public static final String FORGE_USE = PREFIX + "forge.use";
    
    /** Forge 快速锻造 */
    public static final String FORGE_QUICK = PREFIX + "forge.quick";
    
    /** Forge 重载配置 */
    public static final String FORGE_RELOAD = PREFIX + "forge.reload";

    // ==================== Guild 权限 ====================
    
    /** Guild 管理员权限 */
    public static final String GUILD_ADMIN = PREFIX + "guild" + ADMIN_SUFFIX;
    
    /** Guild 创建公会 */
    public static final String GUILD_CREATE = PREFIX + "guild.create";
    
    /** Guild 加入公会 */
    public static final String GUILD_JOIN = PREFIX + "guild.join";
    
    /** Guild 管理公会 */
    public static final String GUILD_MANAGE = PREFIX + "guild.manage";

    // ==================== Market 权限 ====================
    
    /** Market 管理员权限 */
    public static final String MARKET_ADMIN = PREFIX + "market" + ADMIN_SUFFIX;
    
    /** Market 使用市场 */
    public static final String MARKET_USE = PREFIX + "market.use";
    
    /** Market 上架物品 */
    public static final String MARKET_LIST = PREFIX + "market.list";
    
    /** Market 购买物品 */
    public static final String MARKET_BUY = PREFIX + "market.buy";

    // ==================== Trade 权限 ====================
    
    /** Trade 管理员权限 */
    public static final String TRADE_ADMIN = PREFIX + "trade" + ADMIN_SUFFIX;
    
    /** Trade 发起交易 */
    public static final String TRADE_REQUEST = PREFIX + "trade.request";
    
    /** Trade 接受交易 */
    public static final String TRADE_ACCEPT = PREFIX + "trade.accept";

    // ==================== Marriage 权限 ====================
    
    /** Marriage 管理员权限 */
    public static final String MARRIAGE_ADMIN = PREFIX + "marriage" + ADMIN_SUFFIX;
    
    /** Marriage 求婚 */
    public static final String MARRIAGE_PROPOSE = PREFIX + "marriage.propose";
    
    /** Marriage 接受求婚 */
    public static final String MARRIAGE_ACCEPT = PREFIX + "marriage.accept";
    
    /** Marriage 离婚 */
    public static final String MARRIAGE_DIVORCE = PREFIX + "marriage.divorce";

    // ==================== Name 权限 ====================
    
    /** Name 管理员权限 */
    public static final String NAME_ADMIN = PREFIX + "name" + ADMIN_SUFFIX;
    
    /** Name 使用称号 */
    public static final String NAME_USE = PREFIX + "name.use";
    
    /** Name 设置称号 */
    public static final String NAME_SET = PREFIX + "name.set";

    // ==================== DropControl 权限 ====================
    
    /** DropControl 管理员权限 */
    public static final String DROP_ADMIN = PREFIX + "drop" + ADMIN_SUFFIX;
    
    /** DropControl 绕过保护 */
    public static final String DROP_BYPASS = PREFIX + "drop.bypass";
    
    /** DropControl 重载配置 */
    public static final String DROP_RELOAD = PREFIX + "drop.reload";

    // ==================== Menu 权限 ====================
    
    /** Menu 管理员权限 */
    public static final String MENU_ADMIN = PREFIX + "menu" + ADMIN_SUFFIX;
    
    /** Menu 打开菜单 */
    public static final String MENU_OPEN = PREFIX + "menu.open";

    // ==================== ItemTrigger 权限 ====================
    
    /** ItemTrigger 管理员权限 */
    public static final String TRIGGER_ADMIN = PREFIX + "trigger" + ADMIN_SUFFIX;
    
    /** ItemTrigger 使用触发器 */
    public static final String TRIGGER_USE = PREFIX + "trigger.use";
    
    /** ItemTrigger 重载配置 */
    public static final String TRIGGER_RELOAD = PREFIX + "trigger.reload";

    // ==================== 辅助方法 ====================

    /**
     * 获取旧权限节点到新权限节点的映射
     * 
     * @return 权限映射表
     */
    public static Map<String, String> getLegacyPermissionMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // ArmorStats
        mapping.put("armorstats.admin", ARMOR_ADMIN);
        mapping.put("armorstats.reload", ARMOR_RELOAD);
        
        // Points
        mapping.put("gdpoints.admin", POINTS_ADMIN);
        mapping.put("gdpoints.balance", POINTS_BALANCE);
        
        // Cave
        mapping.put("gdcave.admin", CAVE_ADMIN);
        mapping.put("gdcave.use", CAVE_HOME);
        
        // Forge
        mapping.put("forge.admin", FORGE_ADMIN);
        
        // Guild
        mapping.put("gdguild.admin", GUILD_ADMIN);
        
        // Market
        mapping.put("gdmarket.admin", MARKET_ADMIN);
        
        // DropControl
        mapping.put("gddrop.admin", DROP_ADMIN);
        
        return mapping;
    }

    /**
     * 检查是否有管理员权限
     * 
     * @param pluginName 插件名称（不带GuangDian前缀）
     * @return 管理员权限节点
     */
    public static String getAdminPermission(String pluginName) {
        return PREFIX + pluginName.toLowerCase() + ADMIN_SUFFIX;
    }

    /**
     * 构建权限节点
     * 
     * @param pluginName 插件名称
     * @param action 操作名称
     * @return 权限节点
     */
    public static String build(String pluginName, String action) {
        return PREFIX + pluginName.toLowerCase() + "." + action.toLowerCase();
    }
}