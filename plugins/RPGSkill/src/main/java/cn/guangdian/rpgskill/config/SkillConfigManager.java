package cn.guangdian.rpgskill.config;

import cn.guangdian.rpgskill.RPGSkill;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 技能配置管理器
 */
public class SkillConfigManager {

    private final RPGSkill plugin;
    private FileConfiguration skillConfig;

    public SkillConfigManager(RPGSkill plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // 保存默认配置
        plugin.saveDefaultConfig();

        // 加载技能配置
        File skillFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!skillFile.exists()) {
            plugin.saveResource("skills.yml", false);
        }
        skillConfig = YamlConfiguration.loadConfiguration(skillFile);
    }

    public FileConfiguration getSkillConfig() {
        return skillConfig;
    }

    public void reload() {
        loadAll();
    }
}
