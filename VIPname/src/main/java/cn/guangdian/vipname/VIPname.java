package cn.guangdian.vipname;

import cn.guangdian.vipname.command.VIPnameCommand;
import cn.guangdian.vipname.listener.PlayerListener;
import cn.guangdian.vipname.manager.TitleManager;
import cn.guangdian.vipname.placeholder.VIPnamePlaceholder;
import cn.guangdian.vipname.variable.VariableManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * VIPname 称号插件
 * 
 * 功能：
 * - 独立的称号系统
 * - 变量系统用于动态配置和显示
 * - PlaceholderAPI 支持
 * - 支持 MiniMessage 格式
 */
public class VIPname extends JavaPlugin {

    private static VIPname instance;
    private TitleManager titleManager;
    private VariableManager variableManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化数据目录
        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // 初始化管理器
        titleManager = new TitleManager(this);
        titleManager.load();
        
        variableManager = new VariableManager(this);
        variableManager.load();
        
        // 注册命令
        if (getCommand("vipname") != null) {
            getCommand("vipname").setExecutor(new VIPnameCommand(this));
            getCommand("vipname").setTabCompleter(new VIPnameCommand(this));
        }
        
        // 注册监听器
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 注册 PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VIPnamePlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展");
        }
        
        getLogger().info("VIPname 称号插件已启用!");
        getLogger().info("已加载 " + titleManager.getTitleCount() + " 个称号");
    }

    @Override
    public void onDisable() {
        if (titleManager != null) {
            titleManager.save();
        }
        
        getLogger().info("VIPname 称号插件已禁用!");
    }
    
    public void reload() {
        reloadConfig();
        titleManager.load();
        variableManager.load();
        getLogger().info("配置已重载!");
    }
    
    // ==================== 颜色工具 ====================
    
    public static Component color(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return instance.miniMessage.deserialize(text);
    }
    
    public static String colorToString(String text) {
        if (text == null) return "";
        // 支持 & 颜色代码转换为 MiniMessage
        return text
            .replace("&0", "<black>").replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>")
            .replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&k", "<obfuscated>").replace("&l", "<bold>")
            .replace("&m", "<strikethrough>").replace("&n", "<underlined>")
            .replace("&o", "<italic>").replace("&r", "<reset>");
    }
    
    // ==================== Getters ====================
    
    public static VIPname getInstance() { return instance; }
    public TitleManager getTitleManager() { return titleManager; }
    public VariableManager getVariableManager() { return variableManager; }
    public MiniMessage getMiniMessage() { return miniMessage; }
}