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

public class ClassAdvanceGUI extends ClassGUI {

    private final ClassService classService;
    private final ClassManager classManager;

    public ClassAdvanceGUI(GuangDianClass plugin, ClassService classService, ClassManager classManager) {
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

        GameClass currentClass = classManager.getClass(data.getClassId());
        if (currentClass == null) {
            playErrorSound();
            player.sendMessage(msg.red("无法获取职业数据！"));
            return;
        }

        List<GameClass> availableClasses = classService.getAvailableClasses(player);

        GUIBuilder builder = GUIBuilder.create("<dark_gray>★ <gold>转职系统 <dark_gray>★", 6);

        applyBorder(builder, 6);

        builder.setItem(4, createCurrentClassItem(currentClass, data));

        int[] slots = {20, 22, 24, 30, 32};
        for (int i = 0; i < availableClasses.size() && i < slots.length; i++) {
            GameClass targetClass = availableClasses.get(i);
            int slot = slots[i];

            builder.setItem(slot, createTargetClassItem(targetClass), (event) -> {
                playClickSound();
                player.closeInventory();

                if (classService.advanceClass(player, targetClass.getId())) {
                    playSuccessSound();
                    player.sendMessage(msg.green("转职成功！"));
                    player.sendMessage(msg.green("你已转职为 ").append(msg.white(targetClass.getName())));
                } else {
                    playErrorSound();
                    player.sendMessage(msg.red("转职失败！请检查是否满足转职条件。"));
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

    private ItemStack createCurrentClassItem(GameClass gameClass, PlayerClassData data) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.gold("当前职业: ").append(msg.white(gameClass.getName()))
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("阶位: ").append(msg.white(data.getTier() + "阶")));
        lore.add(msg.gray("转职: ").append(msg.white(gameClass.getAdvancementName())));
        lore.add(Component.empty());

        long exp = data.getExp();
        long required = plugin.getClassManager().getExpRequiredForNextTier(data.getTier());
        if (required > 0) {
            lore.add(msg.gray("经验: ").append(msg.white(exp + "/" + required)));
            int percent = (int) ((exp * 100) / required);
            lore.add(msg.gray("进度: ").append(msg.green(percent + "%")));
        } else {
            lore.add(msg.gray("经验: ").append(msg.white("MAX")));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTargetClassItem(GameClass gameClass) {
        Material material = getClassMaterial(gameClass.getClassType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.green(gameClass.getName())
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.gray("类型: ").append(msg.white(getClassTypeName(gameClass.getClassType()))));
        lore.add(msg.gray("阶位: ").append(msg.white(gameClass.getTier() + "阶")));
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
            lore.add(msg.gold("职业技能:"));
            for (String skill : gameClass.getSkills()) {
                lore.add(msg.gray("  - ").append(msg.white(skill)));
            }
        }

        lore.add(Component.empty());
        lore.add(msg.yellow("点击确认转职").decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
