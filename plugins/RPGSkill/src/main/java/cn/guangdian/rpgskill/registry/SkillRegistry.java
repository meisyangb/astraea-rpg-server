package cn.guangdian.rpgskill.registry;

import cn.guangdian.rpgskill.executor.*;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 技能注册表
 * 管理所有技能定义和执行器
 */
public class SkillRegistry {

    private final Map<String, SkillDefinition> skills = new HashMap<>();
    private final Map<String, SkillExecutor> executors = new HashMap<>();

    public SkillRegistry() {
        // 注册默认执行器
        registerExecutor(new AreaDamageExecutor());
        registerExecutor(new SingleTargetExecutor());
        registerExecutor(new HealExecutor());
    }

    /**
     * 注册执行器
     */
    public void registerExecutor(SkillExecutor executor) {
        executors.put(executor.getTypeId(), executor);
    }

    /**
     * 从配置加载技能
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        if (skillsSection == null) return;

        for (String skillId : skillsSection.getKeys(false)) {
            ConfigurationSection skillConfig = skillsSection.getConfigurationSection(skillId);
            if (skillConfig == null) continue;

            SkillDefinition skill = parseSkillDefinition(skillId, skillConfig);
            if (skill != null) {
                skills.put(skillId, skill);
            }
        }
    }

    private SkillDefinition parseSkillDefinition(String id, ConfigurationSection config) {
        String name = config.getString("name", id);
        String typeStr = config.getString("type", "ACTIVE");
        String triggerStr = config.getString("trigger", "RIGHT_CLICK");
        long cooldown = config.getLong("cooldown", 0);
        double manaCost = config.getDouble("mana-cost", 0);
        String executorType = config.getString("executor", "area_damage");

        // 获取执行器
        SkillExecutor executor = executors.get(executorType);
        if (executor == null) {
            throw new IllegalArgumentException("未知的技能执行器类型: " + executorType);
        }

        // 解析参数
        Map<String, Object> params = new HashMap<>();
        ConfigurationSection paramsSection = config.getConfigurationSection("params");
        if (paramsSection != null) {
            for (String key : paramsSection.getKeys(false)) {
                params.put(key, paramsSection.get(key));
            }
        }

        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .type(cn.guangdian.rpgskill.skill.SkillType.valueOf(typeStr.toUpperCase()))
                .trigger(cn.guangdian.rpgskill.skill.TriggerType.valueOf(triggerStr.toUpperCase()))
                .cooldown(cooldown)
                .manaCost(manaCost)
                .executor(executor)
                .params(params)
                .build();
    }

    public Optional<SkillDefinition> getSkill(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    public Map<String, SkillDefinition> getAllSkills() {
        return new HashMap<>(skills);
    }

    public int getSkillCount() {
        return skills.size();
    }

    public void clear() {
        skills.clear();
    }
}
