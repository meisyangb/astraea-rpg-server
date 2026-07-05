package cn.guangdian.custommodels.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理器
 * 管理插件的所有配置文件
 */
public class CustomModelsConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    // 配置参数
    private String textureSourceDirectory;
    private String outputDirectory;
    private boolean autoScanEnabled;
    private boolean autoGenerateResourcePack;
    private List<String> textureCategories;
    private String modelGenerationMode;
    private int packFormat;
    private String packName;
    private int customModelDataStart;
    private String downloadUrl;
    private boolean autoSendToPlayer;

    public CustomModelsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        textureSourceDirectory = config.getString("textures.source_directory", "textures");

        // 如果是相对路径，转换为插件内部路径
        if (!textureSourceDirectory.contains(":") && !textureSourceDirectory.startsWith("/")) {
            textureSourceDirectory = new File(plugin.getDataFolder(), textureSourceDirectory).getAbsolutePath();
        }
        outputDirectory = config.getString("textures.output_directory", plugin.getDataFolder().getAbsolutePath() + "/output");
        autoScanEnabled = config.getBoolean("textures.auto_scan", true);
        textureCategories = config.getStringList("textures.categories");
        if (textureCategories.isEmpty()) {
            textureCategories = getDefaultCategories();
        }

        modelGenerationMode = config.getString("models.generation_mode", "2d");
        packFormat = config.getInt("resource_pack.pack_format", 48);
        packName = config.getString("resource_pack.pack_name", "GuangDian_CustomModels");
        autoGenerateResourcePack = config.getBoolean("resource_pack.auto_generate_on_startup", true);
        customModelDataStart = config.getInt("items.custom_model_data_start", 10000);
        downloadUrl = config.getString("resource_pack.download_url", "");
        autoSendToPlayer = config.getBoolean("resource_pack.auto_send_to_player", true);

        plugin.getLogger().info("配置已加载");
        plugin.getLogger().info("贴图目录: " + textureSourceDirectory);
        plugin.getLogger().info("输出目录: " + outputDirectory);
        plugin.getLogger().info("模型模式: " + modelGenerationMode);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
            plugin.getLogger().info("配置已保存");
        } catch (IOException e) {
            plugin.getLogger().severe("配置保存失败: " + e.getMessage());
        }
    }

    private List<String> getDefaultCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("SD");   // 剑类
        categories.add("AXE");  // 斧类
        categories.add("BOW");  // 弓类
        categories.add("SPR");  // 矛类
        categories.add("STF");  // 法杖类
        categories.add("DGR");  // 匕首类
        return categories;
    }

    // Getter 方法
    public String getTextureSourceDirectory() {
        return textureSourceDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isAutoScanEnabled() {
        return autoScanEnabled;
    }

    public boolean isAutoGenerateResourcePack() {
        return autoGenerateResourcePack;
    }

    public List<String> getTextureCategories() {
        return textureCategories;
    }

    public String getModelGenerationMode() {
        return modelGenerationMode;
    }

    public int getPackFormat() {
        return packFormat;
    }

    public String getPackName() {
        return packName;
    }

    public int getCustomModelDataStart() {
        return customModelDataStart;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean isAutoSendToPlayer() {
        return autoSendToPlayer;
    }
}