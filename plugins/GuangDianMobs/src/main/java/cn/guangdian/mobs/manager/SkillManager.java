package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 技能管理器
 */
public class SkillManager {

    private final GuangDianMobs plugin;
    private final Map<String, MobSkill> skillTemplates = new HashMap<>();

    public SkillManager(GuangDianMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载技能配置
     */
    public void loadSkills() {
        skillTemplates.clear();

        File file = new File(plugin.getDataFolder(), "skills.yml");
        if (!file.exists()) {
            plugin.saveResource("skills.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("skills");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection skillSection = section.getConfigurationSection(id);
            if (skillSection == null) continue;

            try {
                MobSkill skill = parseSkill(id, skillSection);
                if (skill.isValid()) {
                    skillTemplates.put(id, skill);
                    plugin.getLogger().info("加载技能: " + id + " - " + skill.getDisplayName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载技能失败: " + id + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("共加载 " + skillTemplates.size() + " 个技能");
    }

    /**
     * 解析技能配置
     */
    private MobSkill parseSkill(String id, ConfigurationSection section) {
        MobSkill skill = new MobSkill(id);

        skill.setDisplayName(section.getString("display-name", id));

        // 解析技能类型，处理无效类型
        String skillTypeStr = section.getString("type", "DAMAGE").toUpperCase();
        try {
            skill.setType(MobSkill.SkillType.valueOf(skillTypeStr));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("技能 " + id + " 使用了无效的技能类型: " + skillTypeStr + "，使用默认类型 DAMAGE");
            skill.setType(MobSkill.SkillType.DAMAGE);
        }
        skill.setDamage(section.getDouble("damage", 0));
        skill.setHealAmount(section.getDouble("heal", 0));
        skill.setCooldown(section.getInt("cooldown", 100));
        skill.setRange(section.getDouble("range", 10));
        skill.setChance(section.getDouble("chance", 0.3));
        skill.setTargetType(section.getString("target", "TARGET"));
        skill.setEffects(section.getStringList("effects"));
        skill.setParticle(section.getString("particle"));
        skill.setSound(section.getString("sound"));
        skill.setMessage(section.getString("message"));

        // 加载条件
        skill.setConditions(section.getStringList("conditions"));

        // 加载元技能
        skill.setSubSkills(section.getStringList("sub-skills"));
        skill.setDelay(section.getInt("delay", 0));

        // 加载目标选择器
        String targetStr = section.getString("target", "TARGET");
        MobSkill.TargetSelector selector = new MobSkill.TargetSelector();

        // 解析目标选择器格式: @PlayersInRadius{r=40}
        if (targetStr.startsWith("@")) {
            String type = targetStr.substring(1);
            double radius = section.getDouble("range", 10);

            // 解析半径参数: PlayersInRadius{r=40}
            if (type.contains("{")) {
                int start = type.indexOf("{");
                int end = type.indexOf("}");
                if (end > start) {
                    String params = type.substring(start + 1, end);
                    type = type.substring(0, start);

                    // 解析 r=40
                    for (String param : params.split(";")) {
                        if (param.startsWith("r=")) {
                            try {
                                radius = Double.parseDouble(param.substring(2));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            selector.setType(type.toUpperCase());
            selector.setRadius(radius);
        } else {
            selector.setType(targetStr.toUpperCase());
            selector.setRadius(section.getDouble("range", 10));
        }
        skill.setTargetSelector(selector);

        return skill;
    }

    /**
     * 获取技能模板
     */
    public MobSkill getSkill(String id) {
        return skillTemplates.get(id);
    }

    /**
     * 获取所有技能
     */
    public Collection<MobSkill> getAllSkills() {
        return skillTemplates.values();
    }

    /**
     * 获取技能数量
     */
    public int getSkillCount() {
        return skillTemplates.size();
    }
}
