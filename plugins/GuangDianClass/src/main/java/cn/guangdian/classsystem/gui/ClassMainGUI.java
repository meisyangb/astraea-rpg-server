package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
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

public class ClassMainGUI extends ClassGUI {

    private final ClassService classService;

    public ClassMainGUI(GuangDianClass plugin, ClassService classService) {
        super(plugin, null);
        this.classService = classService;
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

        GUIBuilder builder = GUIBuilder.create("<dark_gray>★ <gold>职业系统 <dark_gray>★", 6);

        applyBorder(builder, 6);

        if (data == null || data.getClassId() == null || data.getClassId().isEmpty()) {
            builder.setItem(20, createChooseClassItem(), (event) -> {
                playClickSound();
                plugin.openClassSelectionGUI(player);
            });
        } else {
            GameClass gameClass = classService.getClass(data.getClassId());

            builder.setItem(20, createClassInfoItem(data, gameClass), (event) -> {
                playClickSound();
                plugin.openClassInfoGUI(player);
            });

            builder.setItem(22, createAttributeItem(data), (event) -> {
                playClickSound();
                plugin.openAttributeGUI(player);
            });

            List<GameClass> availableClasses = classService.getAvailableClasses(player);
            builder.setItem(24, createAdvanceItem(data, availableClasses), (event) -> {
                if (availableClasses.isEmpty()) {
                    playErrorSound();
                    player.sendMessage(msg.red("当前没有可转职的职业！"));
                    return;
                }
                playClickSound();
                plugin.openClassAdvanceGUI(player);
            });

            builder.setItem(40, createResetItem(), (event) -> {
                playClickSound();
                player.closeInventory();
                player.sendMessage(msg.yellow("请输入 /class reset 确认重置职业"));
            });
        }

        builder.setItem(49, createCloseItem(), (event) -> player.closeInventory());

        GUI gui = builder.build();
        gui.open(player);
    }

    private ItemStack createChooseClassItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.green("选择职业")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("你还没有选择职业"));
        lore.add(msg.gray("点击选择一个适合你的职业"));
        lore.add(Component.empty());
        lore.add(msg.yellow("点击打开职业选择界面"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClassInfoItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.aqua("职业信息")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("当前职业: ").append(msg.white(gameClass.getName())));
        lore.add(msg.gray("阶位: ").append(msg.white(data.getTier() + "阶")));
        lore.add(msg.gray("转职: ").append(msg.white(gameClass.getAdvancementName())));
        lore.add(Component.empty());

        long exp = data.getExp();
        long required = plugin.getClassManager().getExpRequiredForNextTier(data.getTier());
        String expDisplay = required > 0 ? exp + "/" + required : exp + " (MAX)";
        lore.add(msg.gray("经验: ").append(msg.white(expDisplay)));
        lore.add(Component.empty());
        lore.add(msg.yellow("点击查看详细信息"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(PlayerClassData data) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.lightPurple("属性加点")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("可用属性点: ").append(msg.green(String.valueOf(data.getAvailableAttributePoints()))));
        lore.add(msg.gray("已分配: ").append(msg.white(String.valueOf(data.getUsedAttributePoints()))));
        lore.add(Component.empty());
        lore.add(msg.yellow("点击打开属性加点界面"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdvanceItem(PlayerClassData data, List<GameClass> availableClasses) {
        boolean canAdvance = !availableClasses.isEmpty();
        Material material = canAdvance ? Material.ENDER_PEARL : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName((canAdvance ? msg.gold("转职系统") : msg.red("转职系统"))
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (canAdvance) {
            lore.add(msg.gray("你有 ").append(msg.green(String.valueOf(availableClasses.size()))).append(msg.gray(" 个可转职的职业")));
            lore.add(Component.empty());
            for (GameClass gc : availableClasses) {
                lore.add(msg.green("→ ").append(msg.white(gc.getName())));
            }
            lore.add(Component.empty());
            lore.add(msg.yellow("点击打开转职界面"));
        } else {
            lore.add(msg.gray("当前没有可转职的职业"));
            lore.add(msg.gray("继续提升阶位以解锁转职"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.red("重置职业")
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("警告: 这将重置你的职业数据"));
        lore.add(msg.gray("所有进度将会丢失！"));
        lore.add(Component.empty());
        lore.add(msg.red("点击后请在聊天栏输入确认命令"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
