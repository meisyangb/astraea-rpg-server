package cn.guangdian.signmenu.config;

import cn.guangdian.signmenu.GuangDianSignMenu;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignMenuConfig {

    private final GuangDianSignMenu plugin;
    private FileConfiguration config;
    private File configFile;
    
    // 告示牌文本(小写) -> 命令列表
    private final Map<String, List<String>> signCommands = new HashMap<>();

    public SignMenuConfig(GuangDianSignMenu plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        loadSignCommands();
    }

    public void reload() {
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        signCommands.clear();
        loadSignCommands();
    }

    private void loadSignCommands() {
        var section = config.getConfigurationSection("sign-commands");
        if (section == null) {
            return;
        }

        for (String signText : section.getKeys(false)) {
            List<String> commands = section.getStringList(signText);
            if (!commands.isEmpty()) {
                signCommands.put(signText.trim().toLowerCase(), commands);
            }
        }
        
        plugin.getLogger().info("已加载 " + signCommands.size() + " 个告示牌命令规则");
    }

    public List<String> getCommands(String signText) {
        return signCommands.get(signText.trim().toLowerCase());
    }

    public String getPrefix() {
        return config.getString("prefix", "§8[§6告示牌§8] ");
    }
}
