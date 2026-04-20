package cn.guangdian.rpgitems.template;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * 物品模板
 * Lore驱动模式：Lore直接写，程序解析
 */
public class ItemTemplate {

    private final String id;
    private final Material material;
    private final Component displayName;
    private final List<Component> lore;
    private final List<SkillBinding> skillBindings;
    private final ItemOptions options;
    private final List<String> enchantments;

    public ItemTemplate(String id, Material material, Component displayName,
                       List<Component> lore, ItemOptions options,
                       List<SkillBinding> skillBindings, List<String> enchantments) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.options = options;
        this.skillBindings = skillBindings;
        this.enchantments = enchantments;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public List<Component> getLore() {
        return lore;
    }

    public List<SkillBinding> getSkillBindings() {
        return skillBindings;
    }

    public ItemOptions getOptions() {
        return options;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    /**
     * 技能绑定信息
     */
    public static class SkillBinding {
        private final String skillId;
        private final String trigger;
        private final int cooldown;
        private final int chance;

        public SkillBinding(String skillId, String trigger, int cooldown, int chance) {
            this.skillId = skillId;
            this.trigger = trigger;
            this.cooldown = cooldown;
            this.chance = chance;
        }

        public String getSkillId() {
            return skillId;
        }

        public String getTrigger() {
            return trigger;
        }

        public int getCooldown() {
            return cooldown;
        }

        public int getChance() {
            return chance;
        }
    }

    /**
     * 物品选项
     */
    public static class ItemOptions {
        private final boolean unbreakable;
        private final boolean hideAttributes;
        private final Integer customModelData;

        public ItemOptions(boolean unbreakable, boolean hideAttributes, Integer customModelData) {
            this.unbreakable = unbreakable;
            this.hideAttributes = hideAttributes;
            this.customModelData = customModelData;
        }

        public boolean isUnbreakable() {
            return unbreakable;
        }

        public boolean isHideAttributes() {
            return hideAttributes;
        }

        public Integer getCustomModelData() {
            return customModelData;
        }
    }
}
