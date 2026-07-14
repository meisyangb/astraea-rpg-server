package cn.guangdian.rpgitems.item;

import cn.guangdian.rpgitems.attribute.CompoundAttributeCodec;
import cn.guangdian.rpgitems.attribute.RPGItemsKeys;
import cn.guangdian.rpgitems.template.ItemTemplate;
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
        pdc.set(RPGItemsKeys.ID, PersistentDataType.STRING, template.id());

        // 3. 将所有属性序列化为复合 byte[]（替代 48 个独立 PDC key）
        ItemTemplate.Attributes attrs = template.attributes();
        if (attrs != null) {
            double[] values = new double[CompoundAttributeCodec.COUNT];
            values[CompoundAttributeCodec.ATTACK_MIN] = attrs.attackMin();
            values[CompoundAttributeCodec.ATTACK_MAX] = attrs.attackMax();
            values[CompoundAttributeCodec.DEFENSE_MIN] = attrs.defenseMin();
            values[CompoundAttributeCodec.DEFENSE_MAX] = attrs.defenseMax();
            values[CompoundAttributeCodec.MAX_HEALTH] = attrs.maxHealth();
            values[CompoundAttributeCodec.HEALTH_REGEN] = attrs.healthRegen();
            values[CompoundAttributeCodec.CRIT_CHANCE] = attrs.critChance();
            values[CompoundAttributeCodec.CRIT_DAMAGE] = attrs.critDamage();
            values[CompoundAttributeCodec.LIFESTEAL_CHANCE] = attrs.lifestealChance();
            values[CompoundAttributeCodec.LIFESTEAL_MULTIPLIER] = attrs.lifestealMultiplier();
            values[CompoundAttributeCodec.DODGE_CHANCE] = attrs.dodgeChance();
            values[CompoundAttributeCodec.PARRY_CHANCE] = attrs.parryChance();
            values[CompoundAttributeCodec.MOVE_SPEED] = attrs.moveSpeed();
            values[CompoundAttributeCodec.DAMAGE_REDUCTION] = attrs.damageReduction();
            values[CompoundAttributeCodec.PVP_ATTACK_MIN] = attrs.pvpAttackMin();
            values[CompoundAttributeCodec.PVP_ATTACK_MAX] = attrs.pvpAttackMax();
            values[CompoundAttributeCodec.PVP_DEFENSE_MIN] = attrs.pvpDefenseMin();
            values[CompoundAttributeCodec.PVP_DEFENSE_MAX] = attrs.pvpDefenseMax();
            values[CompoundAttributeCodec.CRIT_RESIST] = attrs.critResist();
            values[CompoundAttributeCodec.CRIT_DAMAGE_RESIST] = attrs.critDamageResist();
            values[CompoundAttributeCodec.LIFESTEAL_RESIST] = attrs.lifestealResist();
            values[CompoundAttributeCodec.ARMOR] = attrs.armor();
            values[CompoundAttributeCodec.ARMOR_STRENGTH] = attrs.armorStrength();
            values[CompoundAttributeCodec.ARMOR_PENETRATION] = attrs.armorPenetration();
            values[CompoundAttributeCodec.DEFENSE_PENETRATION] = attrs.defensePenetration();
            values[CompoundAttributeCodec.DAMAGE_REFLECT] = attrs.damageReflect();
            values[CompoundAttributeCodec.REFLECT_RATIO] = attrs.reflectRatio();
            values[CompoundAttributeCodec.POISON_CHANCE] = attrs.poisonChance();
            values[CompoundAttributeCodec.FREEZE_CHANCE] = attrs.freezeChance();
            values[CompoundAttributeCodec.BLIND_CHANCE] = attrs.blindChance();
            values[CompoundAttributeCodec.BURN_CHANCE] = attrs.burnChance();
            values[CompoundAttributeCodec.SCORCH_CHANCE] = attrs.scorchChance();
            values[CompoundAttributeCodec.IGNITE_CHANCE] = attrs.igniteChance();
            values[CompoundAttributeCodec.SLOW_CHANCE] = attrs.slowChance();
            values[CompoundAttributeCodec.FIRE_RESIST] = attrs.fireResist();
            values[CompoundAttributeCodec.FALL_RESIST] = attrs.fallResist();
            values[CompoundAttributeCodec.DROWNING_RESIST] = attrs.drowningResist();
            values[CompoundAttributeCodec.POISON_RESIST] = attrs.poisonResist();
            values[CompoundAttributeCodec.WITHER_RESIST] = attrs.witherResist();
            values[CompoundAttributeCodec.LAVA_RESIST] = attrs.lavaResist();
            values[CompoundAttributeCodec.MAGIC_RESIST] = attrs.magicResist();
            values[CompoundAttributeCodec.EXPLOSION_RESIST] = attrs.explosionResist();
            values[CompoundAttributeCodec.PROJECTILE_RESIST] = attrs.projectileResist();
            values[CompoundAttributeCodec.KNOCKBACK_RESIST] = attrs.knockbackResist();
            values[CompoundAttributeCodec.EXP_BONUS] = attrs.expBonus();
            values[CompoundAttributeCodec.HEALTH_REGEN_PERCENT] = attrs.healthRegenPercent();
            values[CompoundAttributeCodec.DODGE_REFLECT_CHANCE] = attrs.dodgeReflectChance();
            values[CompoundAttributeCodec.DODGE_REFLECT_RATIO] = attrs.dodgeReflectRatio();

            byte[] compoundData = CompoundAttributeCodec.serialize(values);
            pdc.set(RPGItemsKeys.ATTRS, PersistentDataType.BYTE_ARRAY, compoundData);

            // 装备等级（必须设置，即使为0）
            pdc.set(RPGItemsKeys.LEVEL, PersistentDataType.INTEGER, attrs.level());

            // 需要职业（必须设置，即使为空）
            if (attrs.requiredClass() != null && !attrs.requiredClass().isEmpty()) {
                pdc.set(RPGItemsKeys.REQUIRED_CLASS, PersistentDataType.STRING, attrs.requiredClass());
            }

            // 宝石类型（用于宝石物品）
            if (attrs.gemType() != null && !attrs.gemType().isEmpty()) {
                pdc.set(RPGItemsKeys.GEM_TYPE, PersistentDataType.STRING, attrs.gemType());
            }

            // 强化石类型（用于强化石物品）
            if (attrs.enhanceStone() != null && !attrs.enhanceStone().isEmpty()) {
                pdc.set(RPGItemsKeys.ENHANCE_STONE, PersistentDataType.STRING, attrs.enhanceStone());
            }

            // 槽位定义（用于装备物品）
            if (attrs.sockets() != null && !attrs.sockets().isEmpty()) {
                for (int i = 0; i < attrs.sockets().size() && i < 7; i++) {
                    String socketType = attrs.sockets().get(i);
                    pdc.set(RPGItemsKeys.SOCKET[i], PersistentDataType.STRING, socketType);
                    pdc.set(RPGItemsKeys.GEM[i], PersistentDataType.STRING, "");
                }
            }
        }

        // 5. 存储阶位到 PDC (用于分解匹配)
        if (template.tier() != null && !template.tier().isEmpty()) {
            pdc.set(RPGItemsKeys.TIER, PersistentDataType.STRING, template.tier());
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
        if (pdc.has(RPGItemsKeys.ID, PersistentDataType.STRING)) {
            return pdc.get(RPGItemsKeys.ID, PersistentDataType.STRING);
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
        if (pdc.has(RPGItemsKeys.TIER, PersistentDataType.STRING)) {
            return pdc.get(RPGItemsKeys.TIER, PersistentDataType.STRING);
        }
        return null;
    }
}
