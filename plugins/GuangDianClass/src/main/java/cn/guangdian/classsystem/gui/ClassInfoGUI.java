package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassInfoGUI extends ClassGUI {

    private final ClassService classService;
    private final ClassManager classManager;

    public ClassInfoGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
        super(plugin, null);
        this.classService = classService;
        this.classManager = classManager;
    }

    public void open(Player player) {
        this.player = player;
        build();
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Use open(Player) instead");
    }

    @Override
    protected void build() {
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            playErrorSound();
            player.sendMessage(msg.red("无法获取玩家数据！"));
            return;
        }

        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) {
            playErrorSound();
            player.sendMessage(msg.red("无法获取职业数据！"));
            return;
        }

        GUIBuilder builder = GUIBuilder.create("<dark_gray>★ <gold>职业详情 <dark_gray>★", 6);

        applyBorder(builder, 6);

        builder.setItem(4, createClassHeaderItem(gameClass, data));
        builder.setItem(20, createStatsItem(gameClass, data));
        builder.setItem(22, createAttributePointsItem(data, gameClass));
        builder.setItem(24, createSkillsItem(gameClass));
        builder.setItem(30, createAdvancementPathItem(gameClass));
        builder.setItem(32, createAttributeEffectsItem(gameClass));

        builder.setItem(49, createBackItem("职业系统"), (event) -> {
            playClickSound();
            plugin.openMainGUI(player);
        });

        GUI gui = builder.build();
        gui.open(player);
    }

    private ItemStack createClassHeaderItem(GameClass gameClass, PlayerClassData data) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.gold(gameClass.getName())
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("类型: ").append(msg.white(getClassTypeName(gameClass.getClassType()))));
        lore.add(msg.gray("阶位: ").append(msg.white(data.getTier() + "阶")));
        lore.add(msg.gray("转职: ").append(msg.white(gameClass.getAdvancementName())));
        lore.add(Component.empty());
        lore.add(msg.gray("描述:"));
        lore.add(msg.white(gameClass.getDescription()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(GameClass gameClass, PlayerClassData data) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.aqua("基础属性")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        long exp = data.getExp();
        long required = plugin.getClassManager().getExpRequiredForNextTier(data.getTier());
        if (required > 0) {
            lore.add(msg.gray("经验: ").append(msg.white(exp + "/" + required)));
            int percent = (int) ((exp * 100) / required);
            lore.add(msg.gray("进度: ").append(msg.green(percent + "%")));
        } else {
            lore.add(msg.gray("经验: ").append(msg.white("MAX (已满级)")));
        }
        lore.add(Component.empty());

        lore.add(msg.gold("职业基础属性:"));
        if (gameClass.getStats().containsKey("health")) {
            lore.add(msg.gray("  生命: ").append(msg.red(String.valueOf(gameClass.getStats().get("health").intValue()))));
        }
        if (gameClass.getStats().containsKey("attack")) {
            lore.add(msg.gray("  攻击: ").append(msg.red(String.valueOf(gameClass.getStats().get("attack").intValue()))));
        }
        if (gameClass.getStats().containsKey("defense")) {
            lore.add(msg.gray("  防御: ").append(msg.red(String.valueOf(gameClass.getStats().get("defense").intValue()))));
        }
        if (gameClass.getStats().containsKey("mana")) {
            lore.add(msg.gray("  法力: ").append(msg.red(String.valueOf(gameClass.getStats().get("mana").intValue()))));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributePointsItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.lightPurple("属性点分配")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("可用点数: ").append(msg.green(String.valueOf(data.getAvailableAttributePoints()))));
        lore.add(msg.gray("已分配: ").append(msg.white(String.valueOf(data.getUsedAttributePoints()))));
        lore.add(msg.gray("每级获得: ").append(msg.white(gameClass.getPointsPerLevel() + "点")));
        lore.add(Component.empty());
        lore.add(msg.gold("当前分配:"));

        for (AttributeType type : gameClass.getAvailableAttributes()) {
            int allocated = data.getAllocatedAttribute(type);
            if (allocated > 0) {
                lore.add(msg.gray("  " + type.getDisplayName() + ": ").append(msg.green(String.valueOf(allocated))));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSkillsItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.yellow("职业技能")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (gameClass.getSkills().isEmpty()) {
            lore.add(msg.gray("该职业没有特殊技能"));
        } else {
            lore.add(msg.gold("当前职业技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(msg.gray("  - ").append(msg.white(skill)));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdvancementPathItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.gold("转职路线")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        List<Component> path = new ArrayList<>();
        buildAdvancementPath(gameClass, path);

        if (path.isEmpty()) {
            lore.add(msg.gray("已达到最高阶位"));
        } else {
            lore.add(msg.gold("转职路线:"));
            for (Component line : path) {
                lore.add(line);
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void buildAdvancementPath(GameClass currentClass, List<Component> path) {
        if (currentClass.getNextClasses().isEmpty()) {
            return;
        }

        for (String nextClassId : currentClass.getNextClasses()) {
            GameClass nextClass = classManager.getClass(nextClassId);
            if (nextClass != null) {
                path.add(msg.gray("  → ").append(msg.white(nextClass.getName())).append(msg.gray(" (" + nextClass.getTier() + "阶)")));
                buildAdvancementPath(nextClass, path);
            }
        }
    }

    private ItemStack createAttributeEffectsItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.darkPurple("属性加成效果")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (gameClass.getAvailableAttributes().isEmpty()) {
            lore.add(msg.gray("该职业没有属性加成"));
        } else {
            lore.add(msg.gold("每点属性加成:"));
            for (AttributeType type : gameClass.getAvailableAttributes()) {
                var effect = gameClass.getAttributeEffect(type);
                if (effect != null) {
                    lore.add(msg.gray(type.getDisplayName() + ":"));
                    for (String desc : effect.getEffectDescriptions(1)) {
                        lore.add(msg.white("  " + desc));
                    }
                }
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
