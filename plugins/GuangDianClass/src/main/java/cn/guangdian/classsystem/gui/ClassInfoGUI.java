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

public class ClassInfoGUI {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final MiniMessageService msg;

    public ClassInfoGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.msg = MiniMessageService.getInstance();
    }

    public void open(Player player) {
        PlayerClassData data = classService.getPlayerData(player);
        if (data == null) {
            player.sendMessage(msg.red("无法获取玩家数据！"));
            return;
        }

        GameClass gameClass = classManager.getClass(data.getClassId());
        if (gameClass == null) {
            player.sendMessage(msg.red("无法获取职业数据！"));
            return;
        }

        GUIBuilder builder = GUIBuilder.create("§8★ §6职业详情 §8★", 6);

        // 装饰边框
        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 9; i++) {
            builder.setItem(i, borderItem);
            builder.setItem(45 + i, borderItem);
        }
        for (int i = 1; i < 5; i++) {
            builder.setItem(i * 9, borderItem);
            builder.setItem(i * 9 + 8, borderItem);
        }

        // 职业基本信息
        builder.setItem(4, createClassHeaderItem(gameClass, data));

        // 基础属性
        builder.setItem(20, createStatsItem(gameClass, data));

        // 属性点分配
        builder.setItem(22, createAttributePointsItem(data, gameClass));

        // 技能列表
        builder.setItem(24, createSkillsItem(gameClass));

        // 转职路线
        builder.setItem(30, createAdvancementPathItem(gameClass));

        // 属性加成详情
        builder.setItem(32, createAttributeEffectsItem(gameClass));

        // 返回按钮
        builder.setItem(49, createBackItem(), (event) -> {
            plugin.openMainGUI(player);
        });

        GUI gui = builder.build();
        gui.open(player);
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassHeaderItem(GameClass gameClass, PlayerClassData data) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l" + gameClass.getName())
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7类型: §f" + getClassTypeName(gameClass.getClassType())));
        lore.add(Component.text("§7阶位: §f" + data.getTier() + "阶"));
        lore.add(Component.text("§7转职: §f" + gameClass.getAdvancementName()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7描述:"));
        lore.add(Component.text("§f" + gameClass.getDescription()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(GameClass gameClass, PlayerClassData data) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§b§l基础属性")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));

        long exp = data.getExp();
        long required = plugin.getClassManager().getExpRequiredForNextTier(data.getTier());
        if (required > 0) {
            lore.add(Component.text("§7经验: §f" + exp + "/" + required));
            int percent = (int) ((exp * 100) / required);
            lore.add(Component.text("§7进度: §a" + percent + "%"));
        } else {
            lore.add(Component.text("§7经验: §fMAX (已满级)"));
        }
        lore.add(Component.text(""));

        // 职业基础属性
        lore.add(Component.text("§6职业基础属性:"));
        if (gameClass.getStats().containsKey("health")) {
            lore.add(Component.text("§7  生命: §c" + gameClass.getStats().get("health").intValue()));
        }
        if (gameClass.getStats().containsKey("attack")) {
            lore.add(Component.text("§7  攻击: §c" + gameClass.getStats().get("attack").intValue()));
        }
        if (gameClass.getStats().containsKey("defense")) {
            lore.add(Component.text("§7  防御: §c" + gameClass.getStats().get("defense").intValue()));
        }
        if (gameClass.getStats().containsKey("mana")) {
            lore.add(Component.text("§7  法力: §c" + gameClass.getStats().get("mana").intValue()));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributePointsItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§d§l属性点分配")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7可用点数: §a" + data.getAvailableAttributePoints()));
        lore.add(Component.text("§7已分配: §f" + data.getUsedAttributePoints()));
        lore.add(Component.text("§7每级获得: §f" + gameClass.getPointsPerLevel() + "点"));
        lore.add(Component.text(""));
        lore.add(Component.text("§6当前分配:"));

        for (AttributeType type : gameClass.getAvailableAttributes()) {
            int allocated = data.getAllocatedAttribute(type);
            if (allocated > 0) {
                lore.add(Component.text("§7  " + type.getDisplayName() + ": §a" + allocated));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSkillsItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l职业技能")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));

        if (gameClass.getSkills().isEmpty()) {
            lore.add(Component.text("§7该职业没有特殊技能"));
        } else {
            lore.add(Component.text("§6当前职业技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(Component.text("§7  - §f" + skill));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdvancementPathItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l转职路线")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));

        // 显示转职树
        List<String> path = new ArrayList<>();
        buildAdvancementPath(gameClass, path);

        if (path.isEmpty()) {
            lore.add(Component.text("§7已达到最高阶位"));
        } else {
            lore.add(Component.text("§6转职路线:"));
            for (String line : path) {
                lore.add(Component.text(line));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void buildAdvancementPath(GameClass currentClass, List<String> path) {
        if (currentClass.getNextClasses().isEmpty()) {
            return;
        }

        for (String nextClassId : currentClass.getNextClasses()) {
            GameClass nextClass = classManager.getClass(nextClassId);
            if (nextClass != null) {
                path.add("§7  → §f" + nextClass.getName() + " §7(" + nextClass.getTier() + "阶)");
                buildAdvancementPath(nextClass, path);
            }
        }
    }

    private ItemStack createAttributeEffectsItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§5§l属性加成效果")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));

        if (gameClass.getAvailableAttributes().isEmpty()) {
            lore.add(Component.text("§7该职业没有属性加成"));
        } else {
            lore.add(Component.text("§6每点属性加成:"));
            for (AttributeType type : gameClass.getAvailableAttributes()) {
                var effect = gameClass.getAttributeEffect(type);
                if (effect != null) {
                    lore.add(Component.text("§7" + type.getDisplayName() + ":"));
                    for (String desc : effect.getEffectDescriptions(1)) {
                        lore.add(Component.text("§f  " + desc));
                    }
                }
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§l返回")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7返回主菜单"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getClassMaterial(String classType) {
        return switch (classType.toUpperCase()) {
            case "WARRIOR" -> Material.IRON_SWORD;
            case "MAGE" -> Material.BLAZE_ROD;
            case "ARCHER" -> Material.BOW;
            case "ASSASSIN" -> Material.NETHERITE_SWORD;
            case "PRIEST" -> Material.TOTEM_OF_UNDYING;
            default -> Material.BOOK;
        };
    }

    private String getClassTypeName(String classType) {
        return switch (classType.toUpperCase()) {
            case "WARRIOR" -> "战士系";
            case "MAGE" -> "法师系";
            case "ARCHER" -> "弓箭手系";
            case "ASSASSIN" -> "刺客系";
            case "PRIEST" -> "牧师系";
            default -> "未知";
        };
    }
}
