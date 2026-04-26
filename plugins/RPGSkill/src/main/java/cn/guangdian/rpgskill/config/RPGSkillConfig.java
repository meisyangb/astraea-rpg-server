package cn.guangdian.rpgskill.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.HashMap;
import java.util.Map;

/**
 * RPGSkill 配置数据类
 * 使用 Configurate 进行配置管理
 */
@ConfigSerializable
public class RPGSkillConfig {

    @Setting("general")
    private GeneralConfig general = new GeneralConfig();

    @Setting("cooldown")
    private CooldownConfig cooldown = new CooldownConfig();

    @Setting("skills")
    private Map<String, SkillDefinitionConfig> skills = new HashMap<>();

    // Getters
    public GeneralConfig getGeneral() { return general; }
    public CooldownConfig getCooldown() { return cooldown; }
    public Map<String, SkillDefinitionConfig> getSkills() { return skills; }

    @ConfigSerializable
    public static class GeneralConfig {
        @Setting("debug")
        private boolean debug = false;

        @Setting("default-cooldown-seconds")
        private int defaultCooldownSeconds = 10;

        @Setting("allow-skill-in-combat-only")
        private boolean allowSkillInCombatOnly = false;

        public boolean isDebug() { return debug; }
        public int getDefaultCooldownSeconds() { return defaultCooldownSeconds; }
        public boolean isAllowSkillInCombatOnly() { return allowSkillInCombatOnly; }
    }

    @ConfigSerializable
    public static class CooldownConfig {
        @Setting("persist-after-logout")
        private boolean persistAfterLogout = true;

        @Setting("display-actionbar")
        private boolean displayActionbar = true;

        @Setting("actionbar-update-interval")
        private int actionbarUpdateInterval = 5;

        public boolean isPersistAfterLogout() { return persistAfterLogout; }
        public boolean isDisplayActionbar() { return displayActionbar; }
        public int getActionbarUpdateInterval() { return actionbarUpdateInterval; }
    }

    @ConfigSerializable
    public static class SkillDefinitionConfig {
        @Setting("name")
        private String name = "";

        @Setting("description")
        private String description = "";

        @Setting("cooldown-seconds")
        private int cooldownSeconds = 10;

        @Setting("mana-cost")
        private int manaCost = 0;

        @Setting("enabled")
        private boolean enabled = true;

        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getCooldownSeconds() { return cooldownSeconds; }
        public int getManaCost() { return manaCost; }
        public boolean isEnabled() { return enabled; }
    }
}
