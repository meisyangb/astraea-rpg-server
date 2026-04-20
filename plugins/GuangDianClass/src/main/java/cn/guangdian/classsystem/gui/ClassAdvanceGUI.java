package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.ClassManager;
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

public class ClassAdvanceGUI {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final MiniMessageService msg;

    public ClassAdvanceGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
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

        List<GameClass> availableClasses = classService.getAvailableClasses(player);

        GUIBuilder builder = GUIBuilder.create("§8★ §6转职系统 §8★", 6);

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

        // 当前职业信息
        GameClass currentClass = classManager.getClass(data.getClassId());
        if (currentClass != null) {
            builder.setItem(4, createCurrentClassItem(currentClass, data));
        }

        // 可转职职业
        if (availableClasses.isEmpty()) {
            builder.setItem(22, createNoAdvanceItem());
        } else {
            int[] slots = {20, 22, 24, 30, 32, 34};
            for (int i = 0; i < availableClasses.size() && i < slots.length; i++) {
                GameClass targetClass = availableClasses.get(i);
                int slot = slots[i];

                builder.setItem(slot, createAdvanceClassItem(targetClass), (event) -> {
                    player.closeInventory();
                    if (classService.advanceClass(player, targetClass.getId())) {
                        player.sendMessage(msg.colorize("§6§l★ 恭喜转职成功！★"));
                        player.sendMessage(msg.colorize("§a新职业: §f" + targetClass.getName()));
                        player.sendMessage(msg.colorize("§7使用 §e/class §7打开职业系统界面"));
                    } else {
                        player.sendMessage(msg.red("转职失败！请检查是否满足条件。"));
                    }
                });
            }
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

    private ItemStack createCurrentClassItem(GameClass gameClass, PlayerClassData data) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l当前职业: " + gameClass.getName())
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7阶位: §f" + data.getTier() + "阶"));
        lore.add(Component.text("§7转职: §f" + gameClass.getAdvancementName()));
        lore.add(Component.text(""));

        long exp = data.getExp();
        long required = classManager.getExpRequiredForNextTier(data.getTier());
        if (required > 0) {
            lore.add(Component.text("§7经验: §f" + exp + "/" + required));
            int percent = (int) ((exp * 100) / required);
            lore.add(Component.text("§7进度: §a" + percent + "%"));
        } else {
            lore.add(Component.text("§7经验: §fMAX (已满级)"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoAdvanceItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§l暂无可转职职业")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7当前没有满足条件的转职职业"));
        lore.add(Component.text("§7请继续提升阶位以解锁转职"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdvanceClassItem(GameClass gameClass) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§a§l转职: " + gameClass.getName())
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7类型: §f" + getClassTypeName(gameClass.getClassType())));
        lore.add(Component.text("§7阶位: §f" + gameClass.getTier() + "阶"));
        lore.add(Component.text("§7转职: §f" + gameClass.getAdvancementName()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7描述:"));
        lore.add(Component.text("§f" + gameClass.getDescription()));
        lore.add(Component.text(""));

        // 属性提升
        lore.add(Component.text("§6属性提升:"));
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
            lore.add(Component.text("§6获得技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(Component.text("§7  - §f" + skill));
            }
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§e§l点击确认转职"));

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
