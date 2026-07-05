package cn.guangdian.rpgitems.lore;

import cn.guangdian.rpgitems.template.ItemTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Lore自动生成器
 * 根据Attributes自动生成格式化的Lore,减少YML文件大小
 */
public class LoreGenerator {

    private static final String SEPARATOR = "<font:minecraft:uniform><i:false><dark_red><strikethrough><bold>一一一一一一一一";
    private static final String GEM_SECTION_START = "<font:minecraft:uniform><i:false><gray>*******<aqua>宝石镶嵌<gray>*******";
    private static final String GEM_SECTION_END = "<font:minecraft:uniform><i:false><gray>*******<aqua>宝石镶嵌<gray>*******";
    private static final String UNBREAKABLE_TEXT = "<font:minecraft:uniform><i:false><blue>无限耐久";

    /**
     * 根据Attributes自动生成Lore
     * @param attributes 物品属性
     * @param unbreakable 是否无限耐久
     * @return 自动生成的Lore列表
     */
    public static List<Component> generateLore(ItemTemplate.Attributes attributes, boolean unbreakable) {
        List<Component> lore = new ArrayList<>();

        // 1. 物品类型标题 (如"近战武器                        亚神器")
        if (attributes.itemTypeTitle() != null && !attributes.itemTypeTitle().isEmpty()) {
            lore.add(parseLegacy("<font:minecraft:uniform><i:false><yellow>" + attributes.itemTypeTitle()));
        }

        // 2. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 3. 觉醒进度星级
        if (attributes.awakenProgress() != null && !attributes.awakenProgress().isEmpty()) {
            lore.add(parseLegacy("<font:minecraft:uniform><i:false><dark_purple>觉醒进度 <aqua>" + attributes.awakenProgress()));
        }

        // 4. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 5. 属性列表
        addAttributesLore(lore, attributes);

        // 6. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 7. 物品描述文本 (背景故事)
        if (attributes.descriptions() != null && !attributes.descriptions().isEmpty()) {
            for (String desc : attributes.descriptions()) {
                lore.add(parseLegacy("<font:minecraft:uniform><i:false><yellow>" + desc));
            }
        }

        // 8. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 9. 分解产出
        if (attributes.decomposeItem() != null && !attributes.decomposeItem().isEmpty() && attributes.decomposeCount() > 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_purple>分解后可获得<dark_gray>[<yellow>%s<dark_gray>]<dark_purple>%d颗",
                attributes.decomposeItem(), attributes.decomposeCount()
            )));
        }

        // 10. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 11. 装备等级
        if (attributes.level() > 0) {
            lore.add(parseLegacy("<font:minecraft:uniform><i:false><dark_gray>装备等级： <aqua>" + attributes.level()));
        }

        // 12. 宝石槽
        if (attributes.sockets() != null && !attributes.sockets().isEmpty()) {
            for (String socketType : attributes.sockets()) {
                lore.add(parseLegacy(String.format(
                    "<font:minecraft:uniform><i:false><dark_aqua>[<gray>可镶嵌<%s><dark_aqua>]",
                    socketType
                )));
            }
        }

        // 13. 分割线
        lore.add(parseLegacy(SEPARATOR));

        // 14. 无限耐久
        if (unbreakable) {
            lore.add(parseLegacy(UNBREAKABLE_TEXT));
        }

        return lore;
    }

    /**
     * 添加属性Lore行
     * 只显示非零属性,按照固定顺序显示
     */
    private static void addAttributesLore(List<Component> lore, ItemTemplate.Attributes attrs) {
        // 攻击属性
        if (attrs.attackMin() != 0 || attrs.attackMax() != 0) {
            if (attrs.attackMin() != 0 && attrs.attackMax() != 0 && attrs.attackMin() != attrs.attackMax()) {
                lore.add(parseLegacy(String.format(
                    "<font:minecraft:uniform><i:false><dark_gray>攻击力: <aqua>%d-%d",
                    (int) attrs.attackMin(), (int) attrs.attackMax()
                )));
            } else {
                // 单值攻击(用于宝石)
                if (attrs.attackMin() != 0) {
                    lore.add(parseLegacy(String.format(
                        "<font:minecraft:uniform><i:false><gray>攻击力: <aqua>+%d-%d",
                        (int) attrs.attackMin(), (int) attrs.attackMax()
                    )));
                }
            }
        }

        // 防御属性
        if (attrs.defenseMin() != 0 || attrs.defenseMax() != 0) {
            if (attrs.defenseMin() != 0 && attrs.defenseMax() != 0 && attrs.defenseMin() != attrs.defenseMax()) {
                lore.add(parseLegacy(String.format(
                    "<font:minecraft:uniform><i:false><dark_gray>防御力: <aqua>%d-%d",
                    (int) attrs.defenseMin(), (int) attrs.defenseMax()
                )));
            }
        }

        // 护甲强度
        if (attrs.armor() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>护甲强度: <aqua>%d%%",
                (int) attrs.armor()
            )));
        }

        // 生命上限
        if (attrs.maxHealth() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>生命上限: <aqua>%d",
                (int) attrs.maxHealth()
            )));
        }

        // 每秒回血
        if (attrs.healthRegen() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>每秒回血: <aqua>%d",
                (int) attrs.healthRegen()
            )));
        }

        // 生命恢复百分比
        if (attrs.healthRegenPercent() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>生命恢复: <aqua>%d%%",
                (int) attrs.healthRegenPercent()
            )));
        }

        // 暴击属性
        if (attrs.critChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>暴击几率: <aqua>%d%%",
                (int) attrs.critChance()
            )));
        }

        if (attrs.critDamage() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>暴击伤害: <aqua>%d%%",
                (int) attrs.critDamage()
            )));
        }

        // 招架
        if (attrs.parryChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>招架: <aqua>%d%%",
                (int) attrs.parryChance()
            )));
        }

        // 闪避
        if (attrs.dodgeChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>闪避: <aqua>%d%%",
                (int) attrs.dodgeChance()
            )));
        }

        // 生命吸取
        if (attrs.lifestealChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>生命吸取: <aqua>%d%%",
                (int) attrs.lifestealChance()
            )));
        }

        if (attrs.lifestealMultiplier() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>吸血倍率: <aqua>%d%%",
                (int) attrs.lifestealMultiplier()
            )));
        }

        // 移动速度
        if (attrs.moveSpeed() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>移动速度: <aqua>%d%%",
                (int) attrs.moveSpeed()
            )));
        }

        // 燃烧
        if (attrs.burnChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>燃烧: <aqua>%d%%",
                (int) attrs.burnChance()
            )));
        }

        // 其他状态效果
        if (attrs.poisonChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>中毒: <aqua>%d%%",
                (int) attrs.poisonChance()
            )));
        }

        if (attrs.freezeChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>冰冻: <aqua>%d%%",
                (int) attrs.freezeChance()
            )));
        }

        if (attrs.blindChance() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>致盲: <aqua>%d%%",
                (int) attrs.blindChance()
            )));
        }

        // PVP属性
        if (attrs.pvpAttackMin() != 0 || attrs.pvpAttackMax() != 0) {
            if (attrs.pvpAttackMin() != 0 && attrs.pvpAttackMax() != 0) {
                lore.add(parseLegacy(String.format(
                    "<font:minecraft:uniform><i:false><dark_gray>PVP攻击: <aqua>%d-%d",
                    (int) attrs.pvpAttackMin(), (int) attrs.pvpAttackMax()
                )));
            }
        }

        if (attrs.pvpDefenseMin() != 0 || attrs.pvpDefenseMax() != 0) {
            if (attrs.pvpDefenseMin() != 0 && attrs.pvpDefenseMax() != 0) {
                lore.add(parseLegacy(String.format(
                    "<font:minecraft:uniform><i:false><dark_gray>PVP防御: <aqua>%d-%d",
                    (int) attrs.pvpDefenseMin(), (int) attrs.pvpDefenseMax()
                )));
            }
        }

        // 减伤
        if (attrs.damageReduction() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>减伤: <aqua>%d%%",
                (int) attrs.damageReduction()
            )));
        }

        // 护甲穿透
        if (attrs.armorPenetration() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>护甲穿透: <aqua>%d%%",
                (int) attrs.armorPenetration()
            )));
        }

        // 伤害反弹
        if (attrs.damageReflect() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>伤害反弹: <aqua>%d",
                (int) attrs.damageReflect()
            )));
        }

        // 经验加成
        if (attrs.expBonus() != 0) {
            lore.add(parseLegacy(String.format(
                "<font:minecraft:uniform><i:false><dark_gray>经验加成: <aqua>%d%%",
                (int) attrs.expBonus()
            )));
        }
    }

    /**
     * 获取宝石颜色
     */
    private static String getGemColor(String gemType) {
        switch (gemType) {
            case "红宝石":
                return "dark_red";
            case "绿宝石":
                return "dark_green";
            case "蓝宝石":
                return "dark_blue";
            case "黄宝石":
                return "yellow";
            default:
                return "gray";
        }
    }

    /**
     * 解析Legacy格式文本为Component
     */
    private static Component parseLegacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}