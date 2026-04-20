package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.model.GameClass;
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

public class ClassSelectionGUI {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final MiniMessageService msg;

    public ClassSelectionGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.msg = MiniMessageService.getInstance();
    }

    public void open(Player player) {
        List<GameClass> baseClasses = classManager.getBaseClasses();

        GUIBuilder builder = GUIBuilder.create("§8★ §6选择你的职业 §8★", 6);

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

        // 标题
        builder.setItem(4, createTitleItem());

        // 显示基础职业
        int[] slots = {20, 22, 24, 30, 32};
        for (int i = 0; i < baseClasses.size() && i < slots.length; i++) {
            GameClass gameClass = baseClasses.get(i);
            int slot = slots[i];

            builder.setItem(slot, createClassItem(gameClass), (event) -> {
                player.closeInventory();
                if (classService.chooseClass(player, gameClass.getId())) {
                    player.sendMessage(msg.green("§a§l成功选择职业: §f" + gameClass.getName()));
                    player.sendMessage(msg.colorize("§7使用 §e/class §7打开职业系统界面"));
                } else {
                    player.sendMessage(msg.red("选择职业失败！你可能已经拥有职业。"));
                }
            });
        }

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

    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l选择你的职业")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7请选择一个职业开始你的冒险"));
        lore.add(Component.text("§7每个职业都有独特的属性和技能"));
        lore.add(Component.text(""));
        lore.add(Component.text("§e点击职业图标进行选择"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassItem(GameClass gameClass) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§a§l" + gameClass.getName())
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7类型: §f" + getClassTypeName(gameClass.getClassType())));
        lore.add(Component.text(""));
        lore.add(Component.text("§7描述:"));
        lore.add(Component.text("§f" + gameClass.getDescription()));
        lore.add(Component.text(""));

        // 基础属性
        lore.add(Component.text("§6基础属性:"));
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

        // 技能
        if (!gameClass.getSkills().isEmpty()) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6初始技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(Component.text("§7  - §f" + skill));
            }
        }

        // 转职路线
        if (!gameClass.getNextClasses().isEmpty()) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6可转职为:"));
            for (String nextClassId : gameClass.getNextClasses()) {
                GameClass nextClass = classManager.getClass(nextClassId);
                if (nextClass != null) {
                    lore.add(Component.text("§7  → §f" + nextClass.getName()));
                }
            }
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§e§l点击选择此职业"));

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
