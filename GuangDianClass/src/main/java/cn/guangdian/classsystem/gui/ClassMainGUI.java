package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassMainGUI implements InventoryHolder {
    
    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE = "职业系统";
    
    private final GuangDianClass plugin;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final AttributeManager attributeManager;
    private final Player player;
    private Inventory inventory;
    
    public ClassMainGUI(GuangDianClass plugin, ClassManager classManager, 
                       ExpManager expManager, AttributeManager attributeManager, Player player) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.expManager = expManager;
        this.attributeManager = attributeManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text(GUI_TITLE));
        setupItems();
    }
    
    private void setupItems() {
        inventory.clear();
        
        PlayerClassData data = plugin.getPlayerData(player);
        GameClass gameClass = data != null ? classManager.getClass(data.getClassId()) : null;
        
        ItemStack infoItem = createInfoItem(data, gameClass);
        inventory.setItem(4, infoItem);
        
        if (gameClass == null || data.getClassId().equals(plugin.getDefaultClassId())) {
            ItemStack selectItem = createSelectClassItem();
            inventory.setItem(19, selectItem);
        } else {
            ItemStack advanceItem = createAdvanceItem(data);
            inventory.setItem(19, advanceItem);
        }
        
        ItemStack attrItem = createAttributeItem(data);
        inventory.setItem(21, attrItem);
        
        ItemStack skillItem = createSkillItem(gameClass);
        inventory.setItem(23, skillItem);
        
        ItemStack statsItem = createStatsItem(gameClass);
        inventory.setItem(25, statsItem);
        
        ItemStack tierInfoItem = createTierInfoItem(data);
        inventory.setItem(28, tierInfoItem);
        
        ItemStack expInfoItem = createExpInfoItem(data);
        inventory.setItem(30, expInfoItem);
        
        ItemStack bonusItem = createBonusItem(data);
        inventory.setItem(32, bonusItem);
        
        ItemStack helpItem = createHelpItem();
        inventory.setItem(34, helpItem);
        
        fillEmptySlots();
    }
    
    private ItemStack createInfoItem(PlayerClassData data, GameClass gameClass) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        String className = gameClass != null ? gameClass.getName() : "未选择";
        int tier = data != null ? data.getTier() : 1;
        String advancement = data != null ? data.getAdvancementName() : "未转职";
        
        meta.displayName(Component.text("职业信息")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("玩家: " + player.getName()).color(NamedTextColor.YELLOW));
        lore.add(Component.text("职业: " + className).color(NamedTextColor.GREEN));
        lore.add(Component.text("阶位: " + tier + "阶").color(NamedTextColor.AQUA));
        lore.add(Component.text("转职: " + advancement).color(NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.empty());
        lore.add(Component.text("属性点: " + (data != null ? data.getAvailableAttributePoints() : 0) + " 点可用")
            .color(NamedTextColor.WHITE));
        
        // 添加魔力值信息
        if (plugin.getManaManager() != null) {
            double currentMana = plugin.getManaManager().getCurrentMana(player);
            double maxMana = plugin.getManaManager().getMaxMana(player);
            lore.add(Component.text("魔力值: ").color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(String.format("%.1f", currentMana)).color(NamedTextColor.AQUA))
                .append(Component.text(" / ").color(NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", maxMana)).color(NamedTextColor.AQUA)));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSelectClassItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("选择职业")
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("选择一个职业开始你的冒险").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("[点击打开职业选择界面]").color(NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAdvanceItem(PlayerClassData data) {
        List<GameClass> available = classManager.getAvailableClasses(data);
        boolean canAdvance = !available.isEmpty();
        
        ItemStack item = new ItemStack(canAdvance ? Material.DIAMOND : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("职业转职")
            .color(canAdvance ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        if (canAdvance) {
            lore.add(Component.text("可转职的职业:").color(NamedTextColor.YELLOW));
            for (GameClass gc : available) {
                lore.add(Component.text("  - " + gc.getName()).color(NamedTextColor.WHITE));
            }
            lore.add(Component.empty());
            lore.add(Component.text("[点击打开转职界面]").color(NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("当前无法转职").color(NamedTextColor.RED));
            lore.add(Component.text("可能需要提升阶位").color(NamedTextColor.GRAY));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAttributeItem(PlayerClassData data) {
        int available = data != null ? data.getAvailableAttributePoints() : 0;
        int used = data != null ? data.getTotalAllocatedPoints() : 0;
        
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("属性加点")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("可用点数: " + available).color(NamedTextColor.YELLOW));
        lore.add(Component.text("已分配: " + used).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("力量/体质/敏捷/智力/幸运").color(NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(Component.text("[点击打开属性加点界面]").color(NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSkillItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§6§l技能空间")
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7管理你的技能球"));
        lore.add(Component.text("§7绑定技能到快捷栏"));
        lore.add(Component.empty());
        lore.add(Component.text("§a[点击打开技能空间]"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createStatsItem(GameClass gameClass) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("职业属性")
            .color(NamedTextColor.BLUE)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        if (gameClass != null) {
            lore.add(Component.text("生命: " + gameClass.getStats().getOrDefault("health", 0.0).intValue())
                .color(NamedTextColor.RED));
            lore.add(Component.text("攻击: " + gameClass.getStats().getOrDefault("attack", 0.0).intValue())
                .color(NamedTextColor.YELLOW));
            lore.add(Component.text("防御: " + gameClass.getStats().getOrDefault("defense", 0.0).intValue())
                .color(NamedTextColor.GREEN));
            lore.add(Component.text("魔力: " + gameClass.getStats().getOrDefault("mana", 0.0).intValue())
                .color(NamedTextColor.LIGHT_PURPLE));
        } else {
            lore.add(Component.text("未选择职业").color(NamedTextColor.GRAY));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTierInfoItem(PlayerClassData data) {
        int tier = data != null ? data.getTier() : 1;
        int maxTier = plugin.getConfig().getInt("settings.max-tier", 9);
        
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("阶位信息")
            .color(NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("当前阶位: " + tier + "阶").color(NamedTextColor.YELLOW));
        lore.add(Component.text("最高阶位: " + maxTier + "阶").color(NamedTextColor.GRAY));
        
        if (tier < maxTier) {
            long required = classManager.getExpRequiredForNextTier(tier);
            lore.add(Component.text("升级需求: " + required + " 经验").color(NamedTextColor.WHITE));
        } else {
            lore.add(Component.text("已达最高阶位！").color(NamedTextColor.GOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createExpInfoItem(PlayerClassData data) {
        long exp = data != null ? data.getExp() : 0;
        long totalExp = data != null ? data.getTotalExp() : 0;
        
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("经验信息")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("当前经验: " + exp).color(NamedTextColor.YELLOW));
        lore.add(Component.text("累计经验: " + totalExp).color(NamedTextColor.GRAY));
        
        if (data != null) {
            double progress = expManager.getExpProgress(data);
            lore.add(Component.text("进度: " + String.format("%.1f", progress * 100) + "%")
                .color(NamedTextColor.AQUA));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBonusItem(PlayerClassData data) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("属性加成")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        if (data != null) {
            AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player);
            lore.add(Component.text("生命加成: +" + (int)bonus.health).color(NamedTextColor.RED));
            lore.add(Component.text("攻击加成: +" + String.format("%.1f", bonus.attack)).color(NamedTextColor.YELLOW));
            lore.add(Component.text("防御加成: +" + String.format("%.1f", bonus.defense)).color(NamedTextColor.GREEN));
            lore.add(Component.text("暴击几率: +" + String.format("%.1f", bonus.critChance) + "%").color(NamedTextColor.GOLD));
            lore.add(Component.text("暴击伤害: +" + String.format("%.1f", bonus.critDamage) + "%").color(NamedTextColor.GOLD));
            lore.add(Component.text("闪避几率: +" + String.format("%.1f", bonus.dodge) + "%").color(NamedTextColor.AQUA));
            lore.add(Component.text("魔力加成: +" + (int)bonus.mana).color(NamedTextColor.LIGHT_PURPLE));
        } else {
            lore.add(Component.text("无属性加成").color(NamedTextColor.GRAY));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createHelpItem() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("帮助信息")
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("/class info - 查看职业信息").color(NamedTextColor.YELLOW));
        lore.add(Component.text("/class choose <职业> - 选择职业").color(NamedTextColor.YELLOW));
        lore.add(Component.text("/class advance - 进行转职").color(NamedTextColor.YELLOW));
        lore.add(Component.text("/class attr - 属性加点").color(NamedTextColor.YELLOW));
        lore.add(Component.text("/class reset - 重置职业").color(NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillEmptySlots() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        
        for (int i = 0; i < GUI_SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    public void handleClick(int slot) {
        switch (slot) {
            case 19 -> {
                PlayerClassData data = plugin.getPlayerData(player);
                if (data == null || data.getClassId().equals(plugin.getDefaultClassId())) {
                    player.closeInventory();
                    plugin.openClassSelectionGUI(player);
                } else {
                    player.closeInventory();
                    plugin.openClassAdvanceGUI(player);
                }
            }
            case 21 -> {
                player.closeInventory();
                plugin.openAttributeGUI(player);
            }
            case 23 -> {
                // 打开技能空间GUI
                player.closeInventory();
                cn.guangdian.classsystem.gui.SkillSpaceGUI skillSpaceGUI = 
                    new cn.guangdian.classsystem.gui.SkillSpaceGUI(plugin);
                skillSpaceGUI.open(player);
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
}
