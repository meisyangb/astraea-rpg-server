package cn.guangdian.rpgitems.registry;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgitems.template.ItemTemplate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * 物品注册表
 * 管理物品模板，从配置加载属性数据
 */
public class ItemRegistry {

    private static final Map<Integer, Material> LEGACY_ID_MAP = Map.ofEntries(
        Map.entry(267, Material.IRON_SWORD),
        Map.entry(268, Material.WOODEN_SWORD),
        Map.entry(272, Material.STONE_SWORD),
        Map.entry(276, Material.DIAMOND_SWORD),
        Map.entry(283, Material.GOLDEN_SWORD),
        Map.entry(298, Material.LEATHER_HELMET),
        Map.entry(299, Material.LEATHER_CHESTPLATE),
        Map.entry(300, Material.LEATHER_LEGGINGS),
        Map.entry(301, Material.LEATHER_BOOTS),
        Map.entry(302, Material.CHAINMAIL_HELMET),
        Map.entry(303, Material.CHAINMAIL_CHESTPLATE),
        Map.entry(304, Material.CHAINMAIL_LEGGINGS),
        Map.entry(305, Material.CHAINMAIL_BOOTS),
        Map.entry(306, Material.IRON_HELMET),
        Map.entry(307, Material.IRON_CHESTPLATE),
        Map.entry(308, Material.IRON_LEGGINGS),
        Map.entry(309, Material.IRON_BOOTS),
        Map.entry(310, Material.DIAMOND_HELMET),
        Map.entry(311, Material.DIAMOND_CHESTPLATE),
        Map.entry(312, Material.DIAMOND_LEGGINGS),
        Map.entry(313, Material.DIAMOND_BOOTS),
        Map.entry(314, Material.GOLDEN_HELMET),
        Map.entry(315, Material.GOLDEN_CHESTPLATE),
        Map.entry(316, Material.GOLDEN_LEGGINGS),
        Map.entry(317, Material.GOLDEN_BOOTS)
    );

    private final Map<String, ItemTemplate> items = new HashMap<>();
    private final MiniMessageService miniMessage = MiniMessageService.getInstance();

    /**
     * 从配置加载物品
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        for (String itemId : config.getKeys(false)) {
            if (itemId.startsWith("#")) continue;

            ConfigurationSection itemConfig = config.getConfigurationSection(itemId);
            if (itemConfig == null) continue;

            ItemTemplate item = parseItemTemplate(itemId, itemConfig);
            if (item != null) {
                items.put(itemId, item);
            }
        }
    }

    private ItemTemplate parseItemTemplate(String id, ConfigurationSection config) {
        try {
            // 材质
            String materialStr = config.getString("Id", "STONE");
            Material material = parseMaterial(materialStr);

            // 显示名称
            String displayNameStr = config.getString("Display", id);
            Component displayName = miniMessage.colorize(displayNameStr);

            // Lore
            List<Component> lore = new ArrayList<>();
            List<String> loreStrings = config.getStringList("Lore");
            for (String line : loreStrings) {
                lore.add(miniMessage.colorize(line));
            }

            // 选项
            ItemTemplate.ItemOptions options = parseOptions(config.getConfigurationSection("Options"));

            // 属性数据
            ItemTemplate.Attributes attributes = parseAttributes(config.getConfigurationSection("Attributes"));

            // 阶位 - 直接从配置读取，不解析 Lore
            String tier = config.getString("Tier", "");

            return new ItemTemplate(id, material, displayName, lore, options, attributes, tier);

        } catch (Exception e) {
            return null;
        }
    }

    private Material parseMaterial(String materialStr) {
        if (materialStr == null || materialStr.isEmpty()) {
            return Material.STONE;
        }

        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        try {
            int id = Integer.parseInt(materialStr);
            return getMaterialFromId(id);
        } catch (NumberFormatException ignored) {}

        return Material.STONE;
    }

    private Material getMaterialFromId(int id) {
        return LEGACY_ID_MAP.getOrDefault(id, Material.STONE);
    }

    private ItemTemplate.ItemOptions parseOptions(ConfigurationSection section) {
        if (section == null) {
            return new ItemTemplate.ItemOptions(false, false, null);
        }

        boolean unbreakable = section.getBoolean("Unbreakable", false);
        boolean hideAttributes = section.getBoolean("HideAttributes", false);
        Integer customModelData = section.contains("CustomModelData")
                ? section.getInt("CustomModelData")
                : null;

        return new ItemTemplate.ItemOptions(unbreakable, hideAttributes, customModelData);
    }

    /**
     * 解析属性数据
     */
    private ItemTemplate.Attributes parseAttributes(ConfigurationSection section) {
        if (section == null) {
            return ItemTemplate.Attributes.empty();
        }

        // 攻击属性
        double attackMin = section.getDouble("AttackMin", 0);
        double attackMax = section.getDouble("AttackMax", attackMin);
        
        // 防御属性
        double defenseMin = section.getDouble("DefenseMin", 0);
        double defenseMax = section.getDouble("DefenseMax", defenseMin);
        
        // 生命属性
        double maxHealth = section.getDouble("MaxHealth", 0);
        double healthRegen = section.getDouble("HealthRegen", 0);
        
        // 暴击属性
        double critChance = section.getDouble("CritChance", 0);
        double critDamage = section.getDouble("CritDamage", 0);
        
        // 生命偷取
        double lifestealChance = section.getDouble("LifestealChance", 0);
        double lifestealMultiplier = section.getDouble("LifestealMultiplier", 0);
        
        // 闪避与格挡
        double dodgeChance = section.getDouble("DodgeChance", 0);
        double parryChance = section.getDouble("ParryChance", 0);
        
        // 移动速度
        double moveSpeed = section.getDouble("MoveSpeed", 0);
        
        // 减伤
        double damageReduction = section.getDouble("DamageReduction", 0);
        
        // PVP属性
        double pvpAttackMin = section.getDouble("PvpAttackMin", 0);
        double pvpAttackMax = section.getDouble("PvpAttackMax", pvpAttackMin);
        double pvpDefenseMin = section.getDouble("PvpDefenseMin", 0);
        double pvpDefenseMax = section.getDouble("PvpDefenseMax", pvpDefenseMin);
        
        // 暴击抵抗属性
        double critResist = section.getDouble("CritResist", 0);
        double critDamageResist = section.getDouble("CritDamageResist", 0);
        
        // 吸血抵抗
        double lifestealResist = section.getDouble("LifestealResist", 0);
        
        // 护甲与穿透系统
        double armor = section.getDouble("Armor", 0);
        double armorStrength = section.getDouble("ArmorStrength", 0);
        double armorPenetration = section.getDouble("ArmorPenetration", 0);
        double defensePenetration = section.getDouble("DefensePenetration", 0);
        
        // 伤害反弹
        double damageReflect = section.getDouble("DamageReflect", 0);
        double reflectRatio = section.getDouble("ReflectRatio", 0);
        
        // 状态效果概率
        double poisonChance = section.getDouble("PoisonChance", 0);
        double freezeChance = section.getDouble("FreezeChance", 0);
        double blindChance = section.getDouble("BlindChance", 0);
        double burnChance = section.getDouble("BurnChance", 0);
        double scorchChance = section.getDouble("ScorchChance", 0);
        double igniteChance = section.getDouble("IgniteChance", 0);
        double slowChance = section.getDouble("SlowChance", 0);
        
        // 环境抗性
        double fireResist = section.getDouble("FireResist", 0);
        double fallResist = section.getDouble("FallResist", 0);
        double drowningResist = section.getDouble("DrowningResist", 0);
        double poisonResist = section.getDouble("PoisonResist", 0);
        double witherResist = section.getDouble("WitherResist", 0);
        double lavaResist = section.getDouble("LavaResist", 0);
        double magicResist = section.getDouble("MagicResist", 0);
        double explosionResist = section.getDouble("ExplosionResist", 0);
        double projectileResist = section.getDouble("ProjectileResist", 0);
        
        // 其他属性
        double knockbackResist = section.getDouble("KnockbackResist", 0);
        double expBonus = section.getDouble("ExpBonus", 0);
        double healthRegenPercent = section.getDouble("HealthRegenPercent", 0);
        double dodgeReflectChance = section.getDouble("DodgeReflectChance", 0);
        double dodgeReflectRatio = section.getDouble("DodgeReflectRatio", 0);
        
        // 装备等级
        int level = section.getInt("Level", 0);
        
        // 需要职业
        String requiredClass = section.getString("RequiredClass", "");
        
        // 宝石类型
        String gemType = section.getString("GemType", null);
        
        // 强化石类型
        String enhanceStone = section.getString("EnhanceStone", null);
        
        // 槽位定义
        List<String> sockets = section.getStringList("Sockets");
        if (sockets.isEmpty()) {
            sockets = null;
        }

        return new ItemTemplate.Attributes(
            attackMin, attackMax,
            defenseMin, defenseMax,
            maxHealth, healthRegen,
            critChance, critDamage,
            lifestealChance, lifestealMultiplier,
            dodgeChance, parryChance,
            moveSpeed, damageReduction,
            pvpAttackMin, pvpAttackMax, pvpDefenseMin, pvpDefenseMax,
            critResist, critDamageResist, lifestealResist,
            armor, armorStrength, armorPenetration, defensePenetration,
            damageReflect, reflectRatio,
            poisonChance, freezeChance, blindChance, burnChance, scorchChance, igniteChance, slowChance,
            fireResist, fallResist, drowningResist, poisonResist, witherResist,
            lavaResist, magicResist, explosionResist, projectileResist,
            knockbackResist, expBonus, healthRegenPercent, dodgeReflectChance, dodgeReflectRatio,
            level, requiredClass, gemType, enhanceStone, sockets
        );
    }

    public Optional<ItemTemplate> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Map<String, ItemTemplate> getAllItems() {
        return new HashMap<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
