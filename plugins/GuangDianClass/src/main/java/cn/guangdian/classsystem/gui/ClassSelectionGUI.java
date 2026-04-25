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

public class ClassSelectionGUI extends ClassGUI {

    private final ClassService classService;
    private final ClassManager classManager;

    public ClassSelectionGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
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
        List<GameClass> baseClasses = classManager.getBaseClasses();

        GUIBuilder builder = GUIBuilder.create("<dark_gray>★ <gold>选择你的职业 <dark_gray>★", 6);

        applyBorder(builder, 6);

        builder.setItem(4, createTitleItem());

        int[] slots = {20, 22, 24, 30, 32};
        for (int i = 0; i < baseClasses.size() && i < slots.length; i++) {
            GameClass gameClass = baseClasses.get(i);
            int slot = slots[i];

            builder.setItem(slot, createClassItem(gameClass), (event) -> {
                playClickSound();
                player.closeInventory();
                if (classService.chooseClass(player, gameClass.getId())) {
                    playSuccessSound();
                    player.sendMessage(msg.green("成功选择职业: ").append(msg.white(gameClass.getName())));
                    player.sendMessage(msg.gray("使用 ").append(msg.yellow("/class")).append(msg.gray(" 打开职业系统界面")));
                } else {
                    playErrorSound();
                    player.sendMessage(msg.red("选择职业失败！你可能已经拥有职业。"));
                }
            });
        }

        builder.setItem(49, createBackItem("职业系统"), (event) -> {
            playClickSound();
            plugin.openMainGUI(player);
        });

        GUI gui = builder.build();
        gui.open(player);
    }

    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.gold("选择你的职业")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("请选择一个职业开始你的冒险"));
        lore.add(msg.gray("每个职业都有独特的属性和技能"));
        lore.add(Component.empty());
        lore.add(msg.yellow("点击职业图标进行选择"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassItem(GameClass gameClass) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.green(gameClass.getName())
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("类型: ").append(msg.white(getClassTypeName(gameClass.getClassType()))));
        lore.add(Component.empty());
        lore.add(msg.gray("描述:"));
        lore.add(msg.white(gameClass.getDescription()));
        lore.add(Component.empty());

        lore.add(msg.gold("基础属性:"));
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

        if (!gameClass.getSkills().isEmpty()) {
            lore.add(Component.empty());
            lore.add(msg.gold("初始技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(msg.gray("  - ").append(msg.white(skill)));
            }
        }

        if (!gameClass.getNextClasses().isEmpty()) {
            lore.add(Component.empty());
            lore.add(msg.gold("可转职为:"));
            for (String nextClassId : gameClass.getNextClasses()) {
                GameClass nextClass = classManager.getClass(nextClassId);
                if (nextClass != null) {
                    lore.add(msg.gray("  → ").append(msg.white(nextClass.getName())));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(msg.yellow("点击选择此职业").decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
