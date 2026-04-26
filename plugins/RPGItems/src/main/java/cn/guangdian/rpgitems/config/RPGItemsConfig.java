package cn.guangdian.rpgitems.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPGItems 配置数据类
 * 使用 Configurate 进行配置管理
 */
@ConfigSerializable
public class RPGItemsConfig {

    @Setting("general")
    private GeneralConfig general = new GeneralConfig();

    @Setting("items")
    private ItemsConfig items = new ItemsConfig();

    @Setting("integration")
    private IntegrationConfig integration = new IntegrationConfig();

    // Getters
    public GeneralConfig getGeneral() { return general; }
    public ItemsConfig getItems() { return items; }
    public IntegrationConfig getIntegration() { return integration; }

    @ConfigSerializable
    public static class GeneralConfig {
        @Setting("debug")
        private boolean debug = false;

        @Setting("auto-save-interval")
        private int autoSaveInterval = 300;

        public boolean isDebug() { return debug; }
        public int getAutoSaveInterval() { return autoSaveInterval; }
    }

    @ConfigSerializable
    public static class ItemsConfig {
        @Setting("default-durability")
        private int defaultDurability = 100;

        @Setting("max-lore-lines")
        private int maxLoreLines = 10;

        @Setting("allow-unbreakable")
        private boolean allowUnbreakable = true;

        @Setting("enchantment-glow")
        private boolean enchantmentGlow = true;

        public int getDefaultDurability() { return defaultDurability; }
        public int getMaxLoreLines() { return maxLoreLines; }
        public boolean isAllowUnbreakable() { return allowUnbreakable; }
        public boolean isEnchantmentGlow() { return enchantmentGlow; }
    }

    @ConfigSerializable
    public static class IntegrationConfig {
        @Setting("rpg-skill")
        private RPGSkillIntegrationConfig rpgSkill = new RPGSkillIntegrationConfig();

        @Setting("placeholderapi")
        private PlaceholderAPIConfig placeholderapi = new PlaceholderAPIConfig();

        public RPGSkillIntegrationConfig getRpgSkill() { return rpgSkill; }
        public PlaceholderAPIConfig getPlaceholderapi() { return placeholderapi; }
    }

    @ConfigSerializable
    public static class RPGSkillIntegrationConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("skill-bind-keywords")
        private List<String> skillBindKeywords = new ArrayList<>();

        public RPGSkillIntegrationConfig() {
            skillBindKeywords.add("技能:");
            skillBindKeywords.add("Skill:");
            skillBindKeywords.add("【技能】");
        }

        public boolean isEnabled() { return enabled; }
        public List<String> getSkillBindKeywords() { return skillBindKeywords; }
    }

    @ConfigSerializable
    public static class PlaceholderAPIConfig {
        @Setting("enabled")
        private boolean enabled = true;

        @Setting("placeholder-prefix")
        private String placeholderPrefix = "rpgitems";

        public boolean isEnabled() { return enabled; }
        public String getPlaceholderPrefix() { return placeholderPrefix; }
    }
}
