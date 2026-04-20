package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassMainGUI {

    private final GuangDianClass plugin;
    private final ClassService classService;
    private final MiniMessageService msg;

    public ClassMainGUI(GuangDianClass plugin, ClassService classService) {
        this.plugin = plugin;
        this.classService = classService;
        this.msg = MiniMessageService.getInstance();
    }

    public void open(Player player) {
        PlayerClassData data = classService.getPlayerData(player);

        GUIBuilder builder = GUIBuilder.create("§8★ §6职业系统 §8★", 6);

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

        if (data == null || data.getClassId() == null || data.getClassId().isEmpty()) {
            // 未选择职业 - 显示职业选择按钮
            builder.setItem(20, createChooseClassItem(), (event) -> {
                plugin.openClassSelectionGUI(player);
            });
        } else {
            // 已选择职业 - 显示各个功能按钮
            GameClass gameClass = classService.getClass(data.getClassId());

            // 职业信息
            builder.setItem(20, createClassInfoItem(data, gameClass), (event) -> {
                plugin.openClassInfoGUI(player);
            });

            // 属性加点
            builder.setItem(22, createAttributeItem(data), (event) -> {
                plugin.openAttributeGUI(player);
            });

            // 转职
            List<GameClass> availableClasses = classService.getAvailableClasses(player);
            builder.setItem(24, createAdvanceItem(data, availableClasses), (event) -> {
                if (availableClasses.isEmpty()) {
                    player.sendMessage(msg.red("当前没有可转职的职业！"));
                    return;
                }
                plugin.openClassAdvanceGUI(player);
            });

            // 重置职业
            builder.setItem(40, createResetItem(), (event) -> {
                player.closeInventory();
                player.sendMessage(msg.yellow("请输入 /class reset 确认重置职业"));
            });
        }

        // 关闭按钮
        builder.setItem(49, createCloseItem(), (event) -> player.closeInventory());

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

    private ItemStack createChooseClassItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§a§l选择职业")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7你还没有选择职业"));
        lore.add(Component.text("§7点击选择一个适合你的职业"));
        lore.add(Component.text(""));
        lore.add(Component.text("§e点击打开职业选择界面"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassInfoItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§b§l职业信息")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7当前职业: §f" + gameClass.getName()));
        lore.add(Component.text("§7阶位: §f" + data.getTier() + "阶"));
        lore.add(Component.text("§7转职: §f" + gameClass.getAdvancementName()));
        lore.add(Component.text(""));

        long exp = data.getExp();
        long required = plugin.getClassManager().getExpRequiredForNextTier(data.getTier());
        String expDisplay = required > 0 ? exp + "/" + required : exp + " (MAX)";
        lore.add(Component.text("§7经验: §f" + expDisplay));
        lore.add(Component.text(""));
        lore.add(Component.text("§e点击查看详细信息"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(PlayerClassData data) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§d§l属性加点")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7可用属性点: §a" + data.getAvailableAttributePoints()));
        lore.add(Component.text("§7已分配: §f" + data.getUsedAttributePoints()));
        lore.add(Component.text(""));
        lore.add(Component.text("§e点击打开属性加点界面"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdvanceItem(PlayerClassData data, List<GameClass> availableClasses) {
        boolean canAdvance = !availableClasses.isEmpty();
        Material material = canAdvance ? Material.ENDER_PEARL : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String title = canAdvance ? "§6§l转职系统" : "§c§l转职系统";
        meta.displayName(Component.text(title)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));

        if (canAdvance) {
            lore.add(Component.text("§7你有 §a" + availableClasses.size() + " §7个可转职的职业"));
            lore.add(Component.text(""));
            for (GameClass gc : availableClasses) {
                lore.add(Component.text("§a→ §f" + gc.getName()));
            }
            lore.add(Component.text(""));
            lore.add(Component.text("§e点击打开转职界面"));
        } else {
            lore.add(Component.text("§7当前没有可转职的职业"));
            lore.add(Component.text("§7继续提升阶位以解锁转职"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§l重置职业")
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7警告: 这将重置你的职业数据"));
        lore.add(Component.text("§7所有进度将会丢失！"));
        lore.add(Component.text(""));
        lore.add(Component.text("§c点击后请在聊天栏输入确认命令"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§l关闭")
            .decoration(TextDecoration.ITALIC, false));

        item.setItemMeta(meta);
        return item;
    }
}
