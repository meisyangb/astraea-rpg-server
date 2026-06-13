package cn.guangdian.rpgitems.item;

import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 物品工厂
 * 根据模板创建物品实例，将所有属性存储到 PDC
 */
public class ItemFactory {

    // PDC Keys - 物品标识
    private static final NamespacedKey KEY_ID = new NamespacedKey("rpgitems", "id");
    
    // PDC Keys - 攻击属性
    private static final NamespacedKey KEY_ATTACK_MIN = new NamespacedKey("rpgitems", "attack_min");
    private static final NamespacedKey KEY_ATTACK_MAX = new NamespacedKey("rpgitems", "attack_max");
    
    // PDC Keys - 防御属性
    private static final NamespacedKey KEY_DEFENSE_MIN = new NamespacedKey("rpgitems", "defense_min");
    private static final NamespacedKey KEY_DEFENSE_MAX = new NamespacedKey("rpgitems", "defense_max");
    
    // PDC Keys - 生命属性
    private static final NamespacedKey KEY_MAX_HEALTH = new NamespacedKey("rpgitems", "max_health");
    private static final NamespacedKey KEY_HEALTH_REGEN = new NamespacedKey("rpgitems", "health_regen");
    
    // PDC Keys - 暴击属性
    private static final NamespacedKey KEY_CRIT_CHANCE = new NamespacedKey("rpgitems", "crit_chance");
    private static final NamespacedKey KEY_CRIT_DAMAGE = new NamespacedKey("rpgitems", "crit_damage");
    
    // PDC Keys - 生命偷取
    private static final NamespacedKey KEY_LIFESTEAL_CHANCE = new NamespacedKey("rpgitems", "lifesteal_chance");
    private static final NamespacedKey KEY_LIFESTEAL_MULTIPLIER = new NamespacedKey("rpgitems", "lifesteal_multiplier");
    
    // PDC Keys - 闪避与格挡
    private static final NamespacedKey KEY_DODGE_CHANCE = new NamespacedKey("rpgitems", "dodge_chance");
    private static final NamespacedKey KEY_PARRY_CHANCE = new NamespacedKey("rpgitems", "parry_chance");
    
    // PDC Keys - 移动速度
    private static final NamespacedKey KEY_MOVE_SPEED = new NamespacedKey("rpgitems", "move_speed");
    
    // PDC Keys - 减伤
    private static final NamespacedKey KEY_DAMAGE_REDUCTION = new NamespacedKey("rpgitems", "damage_reduction");
    
    // PDC Keys - PVP属性
    private static final NamespacedKey KEY_PVP_ATTACK_MIN = new NamespacedKey("rpgitems", "pvp_attack_min");
    private static final NamespacedKey KEY_PVP_ATTACK_MAX = new NamespacedKey("rpgitems", "pvp_attack_max");
    private static final NamespacedKey KEY_PVP_DEFENSE_MIN = new NamespacedKey("rpgitems", "pvp_defense_min");
    private static final NamespacedKey KEY_PVP_DEFENSE_MAX = new NamespacedKey("rpgitems", "pvp_defense_max");
    
    // PDC Keys - 暴击抵抗
    private static final NamespacedKey KEY_CRIT_RESIST = new NamespacedKey("rpgitems", "crit_resist");
    private static final NamespacedKey KEY_CRIT_DAMAGE_RESIST = new NamespacedKey("rpgitems", "crit_damage_resist");
    
    // PDC Keys - 吸血抵抗
    private static final NamespacedKey KEY_LIFESTEAL_RESIST = new NamespacedKey("rpgitems", "lifesteal_resist");
    
    // PDC Keys - 护甲与穿透
    private static final NamespacedKey KEY_ARMOR = new NamespacedKey("rpgitems", "armor");
    private static final NamespacedKey KEY_ARMOR_STRENGTH = new NamespacedKey("rpgitems", "armor_strength");
    private static final NamespacedKey KEY_ARMOR_PENETRATION = new NamespacedKey("rpgitems", "armor_penetration");
    private static final NamespacedKey KEY_DEFENSE_PENETRATION = new NamespacedKey("rpgitems", "defense_penetration");
    
    // PDC Keys - 伤害反弹
    private static final NamespacedKey KEY_DAMAGE_REFLECT = new NamespacedKey("rpgitems", "damage_reflect");
    private static final NamespacedKey KEY_REFLECT_RATIO = new NamespacedKey("rpgitems", "reflect_ratio");
    
    // PDC Keys - 状态效果
    private static final NamespacedKey KEY_POISON_CHANCE = new NamespacedKey("rpgitems", "poison_chance");
    private static final NamespacedKey KEY_FREEZE_CHANCE = new NamespacedKey("rpgitems", "freeze_chance");
    private static final NamespacedKey KEY_BLIND_CHANCE = new NamespacedKey("rpgitems", "blind_chance");
    private static final NamespacedKey KEY_BURN_CHANCE = new NamespacedKey("rpgitems", "burn_chance");
    private static final NamespacedKey KEY_SCORCH_CHANCE = new NamespacedKey("rpgitems", "scorch_chance");
    private static final NamespacedKey KEY_IGNITE_CHANCE = new NamespacedKey("rpgitems", "ignite_chance");
    private static final NamespacedKey KEY_SLOW_CHANCE = new NamespacedKey("rpgitems", "slow_chance");
    
    // PDC Keys - 环境抗性
    private static final NamespacedKey KEY_FIRE_RESIST = new NamespacedKey("rpgitems", "fire_resist");
    private static final NamespacedKey KEY_FALL_RESIST = new NamespacedKey("rpgitems", "fall_resist");
    private static final NamespacedKey KEY_DROWNING_RESIST = new NamespacedKey("rpgitems", "drowning_resist");
    private static final NamespacedKey KEY_POISON_RESIST = new NamespacedKey("rpgitems", "poison_resist");
    private static final NamespacedKey KEY_WITHER_RESIST = new NamespacedKey("rpgitems", "wither_resist");
    private static final NamespacedKey KEY_LAVA_RESIST = new NamespacedKey("rpgitems", "lava_resist");
    private static final NamespacedKey KEY_MAGIC_RESIST = new NamespacedKey("rpgitems", "magic_resist");
    private static final NamespacedKey KEY_EXPLOSION_RESIST = new NamespacedKey("rpgitems", "explosion_resist");
    private static final NamespacedKey KEY_PROJECTILE_RESIST = new NamespacedKey("rpgitems", "projectile_resist");
    
    // PDC Keys - 其他属性
    private static final NamespacedKey KEY_KNOCKBACK_RESIST = new NamespacedKey("rpgitems", "knockback_resist");
    private static final NamespacedKey KEY_EXP_BONUS = new NamespacedKey("rpgitems", "exp_bonus");
    private static final NamespacedKey KEY_HEALTH_REGEN_PERCENT = new NamespacedKey("rpgitems", "health_regen_percent");
    private static final NamespacedKey KEY_DODGE_REFLECT_CHANCE = new NamespacedKey("rpgitems", "dodge_reflect_chance");
    private static final NamespacedKey KEY_DODGE_REFLECT_RATIO = new NamespacedKey("rpgitems", "dodge_reflect_ratio");
    
    // PDC Keys - 装备等级和职业
    private static final NamespacedKey KEY_LEVEL = new NamespacedKey("rpgitems", "level");
    private static final NamespacedKey KEY_REQUIRED_CLASS = new NamespacedKey("rpgitems", "required_class");
    
    // PDC Keys - 宝石和槽位
    private static final NamespacedKey KEY_GEM_TYPE = new NamespacedKey("rpgitems", "gem_type");

    // PDC Keys - 阶位
    private static final NamespacedKey KEY_TIER = new NamespacedKey("rpgitems", "tier");

    /**
     * 根据模板创建物品
     * 只为非零属性创建 PDC Key，避免资源浪费
     */
    public ItemStack createItem(ItemTemplate template) {
        ItemStack item = new ItemStack(template.material());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // 1. 设置展示层（Lore）- 仅用于玩家查看
        meta.displayName(template.displayName());
        meta.lore(template.lore());

        // 2. 设置核心数据层（PDC）- 用于属性计算
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 物品 ID（必须设置）
        pdc.set(KEY_ID, PersistentDataType.STRING, template.id());

        // 3. 只为非零属性设置 PDC Key
        ItemTemplate.Attributes attrs = template.attributes();
        if (attrs != null) {
            // 调试日志
            System.out.println("[ItemFactory] 创建物品: " + template.id());

            // 攻击属性
            setIfNotZero(pdc, KEY_ATTACK_MIN, attrs.attackMin());
            setIfNotZero(pdc, KEY_ATTACK_MAX, attrs.attackMax());
            if (attrs.attackMin() != 0 || attrs.attackMax() != 0) {
                System.out.println("[ItemFactory] 属性 - 攻击: " + attrs.attackMin() + "-" + attrs.attackMax());
            }

            // 防御属性
            setIfNotZero(pdc, KEY_DEFENSE_MIN, attrs.defenseMin());
            setIfNotZero(pdc, KEY_DEFENSE_MAX, attrs.defenseMax());
            if (attrs.defenseMin() != 0 || attrs.defenseMax() != 0) {
                System.out.println("[ItemFactory] 属性 - 防御: " + attrs.defenseMin() + "-" + attrs.defenseMax());
            }

            // 生命属性
            setIfNotZero(pdc, KEY_MAX_HEALTH, attrs.maxHealth());
            setIfNotZero(pdc, KEY_HEALTH_REGEN, attrs.healthRegen());
            if (attrs.maxHealth() != 0) {
                System.out.println("[ItemFactory] 属性 - 生命: " + attrs.maxHealth());
            }

            // 暴击属性
            setIfNotZero(pdc, KEY_CRIT_CHANCE, attrs.critChance());
            setIfNotZero(pdc, KEY_CRIT_DAMAGE, attrs.critDamage());

            // 生命偷取
            setIfNotZero(pdc, KEY_LIFESTEAL_CHANCE, attrs.lifestealChance());
            setIfNotZero(pdc, KEY_LIFESTEAL_MULTIPLIER, attrs.lifestealMultiplier());

            // 闪避与格挡
            setIfNotZero(pdc, KEY_DODGE_CHANCE, attrs.dodgeChance());
            setIfNotZero(pdc, KEY_PARRY_CHANCE, attrs.parryChance());

            // 移动速度
            setIfNotZero(pdc, KEY_MOVE_SPEED, attrs.moveSpeed());
            if (attrs.moveSpeed() != 0) {
                System.out.println("[ItemFactory] 属性 - 移动速度: " + attrs.moveSpeed());
            }

            // 减伤
            setIfNotZero(pdc, KEY_DAMAGE_REDUCTION, attrs.damageReduction());

            // PVP属性
            setIfNotZero(pdc, KEY_PVP_ATTACK_MIN, attrs.pvpAttackMin());
            setIfNotZero(pdc, KEY_PVP_ATTACK_MAX, attrs.pvpAttackMax());
            setIfNotZero(pdc, KEY_PVP_DEFENSE_MIN, attrs.pvpDefenseMin());
            setIfNotZero(pdc, KEY_PVP_DEFENSE_MAX, attrs.pvpDefenseMax());

            // 暴击抵抗
            setIfNotZero(pdc, KEY_CRIT_RESIST, attrs.critResist());
            setIfNotZero(pdc, KEY_CRIT_DAMAGE_RESIST, attrs.critDamageResist());

            // 吸血抵抗
            setIfNotZero(pdc, KEY_LIFESTEAL_RESIST, attrs.lifestealResist());

            // 护甲与穿透
            setIfNotZero(pdc, KEY_ARMOR, attrs.armor());
            setIfNotZero(pdc, KEY_ARMOR_STRENGTH, attrs.armorStrength());
            setIfNotZero(pdc, KEY_ARMOR_PENETRATION, attrs.armorPenetration());
            setIfNotZero(pdc, KEY_DEFENSE_PENETRATION, attrs.defensePenetration());

            // 伤害反弹
            setIfNotZero(pdc, KEY_DAMAGE_REFLECT, attrs.damageReflect());
            setIfNotZero(pdc, KEY_REFLECT_RATIO, attrs.reflectRatio());

            // 状态效果
            setIfNotZero(pdc, KEY_POISON_CHANCE, attrs.poisonChance());
            setIfNotZero(pdc, KEY_FREEZE_CHANCE, attrs.freezeChance());
            setIfNotZero(pdc, KEY_BLIND_CHANCE, attrs.blindChance());
            setIfNotZero(pdc, KEY_BURN_CHANCE, attrs.burnChance());
            setIfNotZero(pdc, KEY_SCORCH_CHANCE, attrs.scorchChance());
            setIfNotZero(pdc, KEY_IGNITE_CHANCE, attrs.igniteChance());
            setIfNotZero(pdc, KEY_SLOW_CHANCE, attrs.slowChance());

            // 环境抗性
            setIfNotZero(pdc, KEY_FIRE_RESIST, attrs.fireResist());
            setIfNotZero(pdc, KEY_FALL_RESIST, attrs.fallResist());
            setIfNotZero(pdc, KEY_DROWNING_RESIST, attrs.drowningResist());
            setIfNotZero(pdc, KEY_POISON_RESIST, attrs.poisonResist());
            setIfNotZero(pdc, KEY_WITHER_RESIST, attrs.witherResist());
            setIfNotZero(pdc, KEY_LAVA_RESIST, attrs.lavaResist());
            setIfNotZero(pdc, KEY_MAGIC_RESIST, attrs.magicResist());
            setIfNotZero(pdc, KEY_EXPLOSION_RESIST, attrs.explosionResist());
            setIfNotZero(pdc, KEY_PROJECTILE_RESIST, attrs.projectileResist());

            // 其他属性
            setIfNotZero(pdc, KEY_KNOCKBACK_RESIST, attrs.knockbackResist());
            setIfNotZero(pdc, KEY_EXP_BONUS, attrs.expBonus());
            setIfNotZero(pdc, KEY_HEALTH_REGEN_PERCENT, attrs.healthRegenPercent());
            setIfNotZero(pdc, KEY_DODGE_REFLECT_CHANCE, attrs.dodgeReflectChance());
            setIfNotZero(pdc, KEY_DODGE_REFLECT_RATIO, attrs.dodgeReflectRatio());

            // 装备等级（必须设置，即使为0）
            pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, attrs.level());

            // 需要职业（必须设置，即使为空）
            if (attrs.requiredClass() != null && !attrs.requiredClass().isEmpty()) {
                pdc.set(KEY_REQUIRED_CLASS, PersistentDataType.STRING, attrs.requiredClass());
            }
            
            // 宝石类型（用于宝石物品）
            if (attrs.gemType() != null && !attrs.gemType().isEmpty()) {
                pdc.set(KEY_GEM_TYPE, PersistentDataType.STRING, attrs.gemType());
                System.out.println("[ItemFactory] 宝石类型: " + attrs.gemType());
            }
            
            // 槽位定义（用于装备物品）
            if (attrs.sockets() != null && !attrs.sockets().isEmpty()) {
                for (int i = 0; i < attrs.sockets().size(); i++) {
                    String socketType = attrs.sockets().get(i);
                    NamespacedKey socketKey = new NamespacedKey("rpgitems", "socket_" + i);
                    pdc.set(socketKey, PersistentDataType.STRING, socketType);

                    // 初始化空宝石槽
                    NamespacedKey gemKey = new NamespacedKey("rpgitems", "gem_" + i);
                    pdc.set(gemKey, PersistentDataType.STRING, "");
                }
                System.out.println("[ItemFactory] 槽位数量: " + attrs.sockets().size());
            }
        }

        // 5. 存储阶位到 PDC (用于分解匹配)
        if (template.tier() != null && !template.tier().isEmpty()) {
            pdc.set(KEY_TIER, PersistentDataType.STRING, template.tier());
            System.out.println("[ItemFactory] 阶位: " + template.tier());
        }

        // 4. 应用选项
        ItemTemplate.ItemOptions options = template.options();
        if (options != null) {
            meta.setUnbreakable(options.unbreakable());
            if (options.hideAttributes()) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            if (options.customModelData() != null) {
                meta.setCustomModelData(options.customModelData());
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 只为非零值设置 PDC Key
     */
    private void setIfNotZero(PersistentDataContainer pdc, NamespacedKey key, double value) {
        if (value != 0.0) {
            pdc.set(key, PersistentDataType.DOUBLE, value);
        }
    }
    
    /**
     * 从 PDC 读取物品 ID
     */
    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_ID, PersistentDataType.STRING)) {
            return pdc.get(KEY_ID, PersistentDataType.STRING);
        }
        return null;
    }

    /**
     * 从 PDC 读取物品阶位
     */
    public static String getItemTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_TIER, PersistentDataType.STRING)) {
            return pdc.get(KEY_TIER, PersistentDataType.STRING);
        }
        return null;
    }
}
