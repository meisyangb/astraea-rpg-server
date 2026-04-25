package cn.guangdian.monthlycard.config;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 多配置文件管理器
 * 管理所有分割的配置文件
 */
public class ConfigManager {

    private final GuangDianMonthlyCard plugin;
    
    // 配置文件
    private org.bukkit.configuration.file.FileConfiguration mainConfig;
    private YamlConfiguration cardsConfig;
    private YamlConfiguration messagesConfig;
    private YamlConfiguration guiConfig;
    private YamlConfiguration makeupConfig;
    private YamlConfiguration autoRenewConfig;
    
    // 配置文件路径
    private String cardsFile = "cards.yml";
    private String messagesFile = "messages.yml";
    private String guiFile = "gui.yml";
    private String makeupFile = "makeup.yml";
    private String autoRenewFile = "auto-renew.yml";
    
    // 消息缓存
    private final Map<String, String> messageCache = new HashMap<>();

    public ConfigManager(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置文件
     */
    public void loadAllConfigs() {
        // 加载主配置
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        
        // 读取子配置文件路径
        loadConfigFilePaths();
        
        // 加载各子配置
        cardsConfig = loadConfigFile(cardsFile, "cards.yml");
        messagesConfig = loadConfigFile(messagesFile, "messages.yml");
        guiConfig = loadConfigFile(guiFile, "gui.yml");
        makeupConfig = loadConfigFile(makeupFile, "makeup.yml");
        autoRenewConfig = loadConfigFile(autoRenewFile, "auto-renew.yml");
        
        // 缓存消息
        cacheMessages();
        
        plugin.getLogger().info("已加载所有配置文件");
    }
    
    /**
     * 重新加载所有配置
     */
    public void reloadAllConfigs() {
        messageCache.clear();
        loadAllConfigs();
    }

    /**
     * 从主配置读取子配置文件路径
     */
    private void loadConfigFilePaths() {
        if (mainConfig.contains("config-files.cards")) {
            cardsFile = mainConfig.getString("config-files.cards", "cards.yml");
        }
        if (mainConfig.contains("config-files.messages")) {
            messagesFile = mainConfig.getString("config-files.messages", "messages.yml");
        }
        if (mainConfig.contains("config-files.gui")) {
            guiFile = mainConfig.getString("config-files.gui", "gui.yml");
        }
        if (mainConfig.contains("config-files.makeup")) {
            makeupFile = mainConfig.getString("config-files.makeup", "makeup.yml");
        }
        if (mainConfig.contains("config-files.auto-renew")) {
            autoRenewFile = mainConfig.getString("config-files.auto-renew", "auto-renew.yml");
        }
    }

    /**
     * 加载单个配置文件
     */
    private YamlConfiguration loadConfigFile(String fileName, String defaultResource) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        // 如果文件不存在，从资源复制
        if (!file.exists()) {
            if (plugin.getResource(defaultResource) != null) {
                plugin.saveResource(defaultResource, false);
                // 如果资源名和文件名不同，重命名
                if (!defaultResource.equals(fileName)) {
                    File defaultFile = new File(plugin.getDataFolder(), defaultResource);
                    if (defaultFile.exists()) {
                        defaultFile.renameTo(file);
                    }
                }
            }
        }
        
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        
        // 尝试从资源加载默认配置
        try (InputStream is = plugin.getResource(defaultResource)) {
            if (is != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载默认配置: " + defaultResource);
        }
        
        return new YamlConfiguration();
    }

    /**
     * 缓存消息配置
     */
    private void cacheMessages() {
        if (messagesConfig == null) return;
        
        // 前缀
        messageCache.put("prefix", messagesConfig.getString("prefix", "<gold>[月卡] <reset>"));
        
        // 成功消息
        cacheMessageSection("success", "success");
        
        // 错误消息
        cacheMessageSection("error", "error");
        
        // 提示消息
        cacheMessageSection("info", "info");
        
        // 帮助消息
        cacheMessageSection("help", "help");
    }
    
    private void cacheMessageSection(String section, String prefix) {
        if (!messagesConfig.contains(section)) return;
        
        for (String key : messagesConfig.getConfigurationSection(section).getKeys(false)) {
            String path = section + "." + key;
            String value = messagesConfig.getString(path, "");
            messageCache.put(prefix + "." + key, value);
        }
    }

    // ==================== 获取配置 ====================
    
    public org.bukkit.configuration.file.FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    public YamlConfiguration getCardsConfig() {
        return cardsConfig;
    }
    
    public YamlConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public YamlConfiguration getGuiConfig() {
        return guiConfig;
    }
    
    public YamlConfiguration getMakeupConfig() {
        return makeupConfig;
    }
    
    public YamlConfiguration getAutoRenewConfig() {
        return autoRenewConfig;
    }

    // ==================== 消息获取 ====================
    
    /**
     * 获取带前缀的消息
     */
    public String getMessage(String key) {
        String prefix = messageCache.getOrDefault("prefix", "<gold>[月卡] <reset>");
        String message = messageCache.getOrDefault(key, "");
        return prefix + message;
    }
    
    /**
     * 获取不带前缀的消息
     */
    public String getMessageRaw(String key) {
        return messageCache.getOrDefault(key, "");
    }
    
    /**
     * 获取成功消息
     */
    public String getSuccessMessage(String key) {
        return getMessage("success." + key);
    }
    
    /**
     * 获取错误消息
     */
    public String getErrorMessage(String key) {
        return getMessage("error." + key);
    }
    
    /**
     * 获取提示消息
     */
    public String getInfoMessage(String key) {
        return getMessage("info." + key);
    }

    // ==================== 便捷方法 ====================
    
    /**
     * 检查功能是否启用
     */
    public boolean isFeatureEnabled(String feature) {
        return mainConfig.getBoolean("features." + feature, true);
    }
    
    /**
     * 获取数据库类型
     */
    public String getDatabaseType() {
        return mainConfig.getString("database.type", "sqlite");
    }
    
    /**
     * 获取自动保存间隔(秒)
     */
    public int getAutoSaveInterval() {
        return mainConfig.getInt("settings.auto-save-interval", 300);
    }
    
    /**
     * 获取检查过期间隔(秒)
     */
    public int getCheckExpiredInterval() {
        return mainConfig.getInt("settings.check-expired-interval", 1800);
    }
    
    /**
     * 获取时区
     */
    public String getTimezone() {
        return mainConfig.getString("settings.timezone", "Asia/Shanghai");
    }
}
