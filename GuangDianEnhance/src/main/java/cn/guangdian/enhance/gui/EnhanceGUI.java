package cn.guangdian.enhance.gui;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.enhance.stone.EnhanceStoneType;
import cn.guangdian.enhance.stone.StoneEffect;
import cn.guangdian.enhance.util.EnhanceAttributeHelper;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 强化 GUI — 参考 SocketInlayGUI 设计模式
 * 
 * 布局(27格):
 * 行0(0-8):  [G][G][G][材料(3)][装备(4)][G][G][G][G]
 * 行1(9-17): [G][G][幸运(10)][保护(12)][安全(14)][必成(16)][G][G][G]
 * 行2(18-26):[G][G][信息(20)][G][强化(22)][G][关闭(24)][G][G][G]
 *
 * 每种石头都有专属槽位，点击背包物品自动放入对应槽位
 */
public class EnhanceGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 27;
    private static final int MATERIAL_SLOT = 3;
    private static final int EQUIPMENT_SLOT = 4;
    private static final int LUCKY_SLOT = 10;
    private static final int PROTECT_SLOT = 12;
    private static final int SAFETY_SLOT = 14;
    private static final int GUARANTEE_SLOT = 16;
    private static final int INFO_SLOT = 20;
    private static final int ENHANCE_SLOT = 22;
    private static final int CLOSE_SLOT = 24;

    private final GuangDianEnhance plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessageService miniMessage;
    private final EnhanceManager manager;
    private final EnhanceConfig config;

    private ItemStack equipmentItem;
    private ItemStack materialItem;
    private ItemStack luckyStoneItem;
    private ItemStack protectStoneItem;
    private ItemStack safetyStoneItem;
    private ItemStack guaranteeStoneItem;
    private boolean finalized;

    public EnhanceGUI(GuangDianEnhance plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.manager = plugin.getEnhanceManager();
        this.config = plugin.getEnhanceConfig();
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? rpgCore.getMiniMessageService() : null;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
            Component.text("强化系统", NamedTextColor.GOLD));
        refreshInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    // ════════════════════════════════════════
    // 事件处理 (由 EnhanceListener 委托)
    // ════════════════════════════════════════

    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;

        if (event.getClickedInventory().equals(inventory)) {
            handleTopInventoryClick(event.getRawSlot());
            return;
        }
        handlePlayerInventoryClick(event);
    }

    public void handleInventoryDrag(InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void handleInventoryClose() {
        if (!finalized) {
            returnSessionItems();
            finalized = true;
        }
    }

    // ════════════════════════════════════════
    // GUI 槽位点击 — 取回物品
    // ════════════════════════════════════════

    private void handleTopInventoryClick(int rawSlot) {
        if (rawSlot == EQUIPMENT_SLOT) {
            withdrawEquipment();
            return;
        }
        if (rawSlot == MATERIAL_SLOT) {
            withdrawFromSlot(materialItem, "materialItem");
            return;
        }
        if (rawSlot == LUCKY_SLOT) {
            withdrawFromSlot(luckyStoneItem, "luckyStoneItem");
            return;
        }
        if (rawSlot == PROTECT_SLOT) {
            withdrawFromSlot(protectStoneItem, "protectStoneItem");
            return;
        }
        if (rawSlot == SAFETY_SLOT) {
            withdrawFromSlot(safetyStoneItem, "safetyStoneItem");
            return;
        }
        if (rawSlot == GUARANTEE_SLOT) {
            withdrawFromSlot(guaranteeStoneItem, "guaranteeStoneItem");
            return;
        }
        if (rawSlot == ENHANCE_SLOT) {
            performEnhance();
            return;
        }
        if (rawSlot == CLOSE_SLOT) {
            cancelSession();
        }
    }

    /** 点击 GUI 槽位取回物品的统一方法 */
    private void withdrawFromSlot(ItemStack slotItem, String fieldName) {
        if (slotItem == null) return;
        giveOrDrop(slotItem);
        setFieldNull(fieldName);
        refreshInventory();
    }

    private void setFieldNull(String fieldName) {
        switch (fieldName) {
            case "materialItem" -> materialItem = null;
            case "luckyStoneItem" -> luckyStoneItem = null;
            case "protectStoneItem" -> protectStoneItem = null;
            case "safetyStoneItem" -> safetyStoneItem = null;
            case "guaranteeStoneItem" -> guaranteeStoneItem = null;
        }
    }

    // ════════════════════════════════════════
    // 点击背包物品 — 自动放入对应槽位
    // ════════════════════════════════════════

    private void handlePlayerInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 优先放入装备
        if (equipmentItem == null) {
            if (EnhanceAttributeHelper.isEnhanceable(clickedItem)) {
                placeEquipment(event.getClickedInventory(), event.getSlot(), clickedItem);
                return;
            }
            sendMessage("请先放入可强化装备", NamedTextColor.RED);
            return;
        }

        // 判断强化石类型，放入对应专属槽位
        Optional<EnhanceStoneType> stoneType = EnhanceStoneType.detect(clickedItem);
        if (stoneType.isEmpty()) {
            sendMessage("请放入强化石或可强化装备", NamedTextColor.RED);
            return;
        }

        EnhanceStoneType st = stoneType.get();
        switch (st.getEffect()) {
            case MATERIAL -> placeMaterial(event.getClickedInventory(), event.getSlot(), clickedItem, st);
            case SUCCESS_BONUS -> placeToStoneSlot(event.getClickedInventory(), event.getSlot(), clickedItem, st, LUCKY_SLOT, "幸运石");
            case PREVENT_DEGRADE -> placeToStoneSlot(event.getClickedInventory(), event.getSlot(), clickedItem, st, PROTECT_SLOT, "保护石");
            case PREVENT_DESTROY -> placeToStoneSlot(event.getClickedInventory(), event.getSlot(), clickedItem, st, SAFETY_SLOT, "安全石");
            case GUARANTEE -> placeToStoneSlot(event.getClickedInventory(), event.getSlot(), clickedItem, st, GUARANTEE_SLOT, "必成石");
        }
    }

    private void placeEquipment(org.bukkit.inventory.Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        equipmentItem = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        refreshInventory();
        int currentLevel = manager.getLevel(equipmentItem);
        sendMessage("已放入装备，当前强化等级: +" + currentLevel, NamedTextColor.GREEN);
    }

    private void placeMaterial(org.bukkit.inventory.Inventory sourceInventory, int sourceSlot, ItemStack sourceItem, EnhanceStoneType stoneType) {
        if (equipmentItem == null) {
            sendMessage("请先放入装备", NamedTextColor.RED);
            return;
        }

        int currentLevel = manager.getLevel(equipmentItem);
        int targetLevel = currentLevel + 1;

        if (!isMaterialMatchLevel(stoneType, targetLevel)) {
            String requiredName = getRequiredMaterialName(targetLevel);
            sendMessage("当前需要: " + requiredName + "，而非 " + stoneType.getDisplayName(), NamedTextColor.RED);
            return;
        }

        // 替换旧材料
        if (materialItem != null) giveOrDrop(materialItem);
        materialItem = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        refreshInventory();
        sendMessage("已放入 " + stoneType.getDisplayName(), NamedTextColor.GREEN);
    }

    /** 放入辅助石到专属槽位 (幸运/保护/安全/必成) */
    private void placeToStoneSlot(org.bukkit.inventory.Inventory sourceInventory, int sourceSlot,
                                   ItemStack sourceItem, EnhanceStoneType stoneType,
                                   int guiSlot, String slotName) {
        if (equipmentItem == null) {
            sendMessage("请先放入装备", NamedTextColor.RED);
            return;
        }

        // 获取当前槽位已有的物品引用
        ItemStack existing = getItemForSlot(guiSlot);
        if (existing != null) {
            giveOrDrop(existing);
        }

        ItemStack taken = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        setItemForSlot(guiSlot, taken);
        refreshInventory();
        sendMessage("已放入 " + stoneType.getDisplayName() + " → " + slotName + "槽", NamedTextColor.GREEN);
    }

    // ════════════════════════════════════════
    // 槽位 ↔ 字段映射
    // ════════════════════════════════════════

    private ItemStack getItemForSlot(int guiSlot) {
        if (guiSlot == LUCKY_SLOT) return luckyStoneItem;
        if (guiSlot == PROTECT_SLOT) return protectStoneItem;
        if (guiSlot == SAFETY_SLOT) return safetyStoneItem;
        if (guiSlot == GUARANTEE_SLOT) return guaranteeStoneItem;
        if (guiSlot == MATERIAL_SLOT) return materialItem;
        return null;
    }

    private void setItemForSlot(int guiSlot, ItemStack item) {
        if (guiSlot == LUCKY_SLOT) luckyStoneItem = item;
        else if (guiSlot == PROTECT_SLOT) protectStoneItem = item;
        else if (guiSlot == SAFETY_SLOT) safetyStoneItem = item;
        else if (guiSlot == GUARANTEE_SLOT) guaranteeStoneItem = item;
        else if (guiSlot == MATERIAL_SLOT) materialItem = item;
    }

    // ════════════════════════════════════════
    // 取回装备（连带返还所有石头）
    // ════════════════════════════════════════

    private void withdrawEquipment() {
        if (equipmentItem == null) return;
        giveOrDrop(equipmentItem);
        returnAllStones();
        clearSession();
        refreshInventory();
    }

    private void returnAllStones() {
        if (materialItem != null) { giveOrDrop(materialItem); materialItem = null; }
        if (luckyStoneItem != null) { giveOrDrop(luckyStoneItem); luckyStoneItem = null; }
        if (protectStoneItem != null) { giveOrDrop(protectStoneItem); protectStoneItem = null; }
        if (safetyStoneItem != null) { giveOrDrop(safetyStoneItem); safetyStoneItem = null; }
        if (guaranteeStoneItem != null) { giveOrDrop(guaranteeStoneItem); guaranteeStoneItem = null; }
    }

    // ════════════════════════════════════════
    // 淬炼石等级匹配
    // ════════════════════════════════════════

    private boolean isMaterialMatchLevel(EnhanceStoneType stoneType, int targetLevel) {
        if (stoneType.getEffect() != StoneEffect.MATERIAL) return false;
        if (targetLevel >= 1 && targetLevel <= 5) return stoneType == EnhanceStoneType.QUENCH;
        if (targetLevel >= 6 && targetLevel <= 10) return stoneType == EnhanceStoneType.QUENCH_ADV;
        if (targetLevel >= 11 && targetLevel <= 15) return stoneType == EnhanceStoneType.QUENCH_SUP;
        return false;
    }

    private String getRequiredMaterialName(int targetLevel) {
        if (targetLevel >= 1 && targetLevel <= 5) return "淬炼石";
        if (targetLevel >= 6 && targetLevel <= 10) return "强化淬炼石";
        if (targetLevel >= 11 && targetLevel <= 15) return "极品淬炼石";
        return "未知";
    }

    // ════════════════════════════════════════
    // 强化执行
    // ════════════════════════════════════════

    private void performEnhance() {
        if (equipmentItem == null) {
            sendMessage("请先放入装备", NamedTextColor.RED);
            return;
        }

        int currentLevel = manager.getLevel(equipmentItem);
        int maxLevel = getMaxLevelForItem(equipmentItem);
        if (currentLevel >= maxLevel) {
            sendMessage("已达到最高强化等级", NamedTextColor.RED);
            return;
        }

        int targetLevel = currentLevel + 1;

        // 检查材料槽
        if (materialItem == null) {
            sendMessage("请放入淬炼石: " + getRequiredMaterialName(targetLevel), NamedTextColor.RED);
            return;
        }
        Optional<EnhanceStoneType> matType = EnhanceStoneType.detect(materialItem);
        if (matType.isEmpty() || !isMaterialMatchLevel(matType.get(), targetLevel)) {
            sendMessage("淬炼石类型不匹配，需要: " + getRequiredMaterialName(targetLevel), NamedTextColor.RED);
            return;
        }

        // 先收集辅助石效果（在消耗前）
        boolean hasGuarantee = guaranteeStoneItem != null;
        boolean hasProtect = protectStoneItem != null;
        boolean hasSafety = safetyStoneItem != null;
        boolean hasProtection = hasProtect || hasSafety;

        double luckyBonus = 0.0;
        if (luckyStoneItem != null) {
            Optional<EnhanceStoneType> luckyType = EnhanceStoneType.detect(luckyStoneItem);
            if (luckyType.isPresent() && luckyType.get().getEffect() == StoneEffect.SUCCESS_BONUS) {
                luckyBonus = luckyType.get().getValue();
            }
        }

        // 消耗所有放入的石头
        materialItem = null;
        luckyStoneItem = null;
        protectStoneItem = null;
        safetyStoneItem = null;
        guaranteeStoneItem = null;

        // 计算成功率
        double baseRate = manager.getSuccessRate(currentLevel, equipmentItem);
        double pityBonus = manager.getPityBonusForPlayer(player.getUniqueId(), currentLevel);
        double totalRate = Math.min(1.0, baseRate + pityBonus + luckyBonus);

        if (hasGuarantee) totalRate = 1.0;

        boolean success = hasGuarantee || ThreadLocalRandom.current().nextDouble() < totalRate;

        if (success) {
            manager.pityReset(player.getUniqueId(), currentLevel);
            int newLevel = currentLevel + 1;
            equipmentItem = plugin.getEnhanceStorage().setLevel(equipmentItem, newLevel);
            manager.updateItemAttributes(equipmentItem, newLevel);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            double mult = manager.getAttributeMultiplier(newLevel);
            sendMessage("强化成功! 等级: +" + newLevel + " 倍率: " + String.format("%.2f", mult) + "x", NamedTextColor.GREEN);
        } else {
            manager.pityIncrement(player.getUniqueId(), currentLevel);
            String failureType = config.getFailureType();

            if (hasProtection) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 0.8f);
                String protMsg = hasProtect ? "保护石生效，防止降级" : "安全石生效，防止破碎";
                sendMessage("强化失败，" + protMsg, NamedTextColor.YELLOW);
            } else if ("destroy".equals(failureType) && ThreadLocalRandom.current().nextDouble() < config.getDestroyChance()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                sendMessage("强化失败，装备已破碎!", NamedTextColor.DARK_RED);
                equipmentItem = null;
            } else if ("degrade".equals(failureType) && currentLevel > 0 && ThreadLocalRandom.current().nextDouble() < config.getDegradeChance()) {
                int degradedLevel = currentLevel - 1;
                equipmentItem = plugin.getEnhanceStorage().setLevel(equipmentItem, degradedLevel);
                manager.updateItemAttributes(equipmentItem, degradedLevel);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 0.6f);
                sendMessage("强化失败，等级下降至 +" + degradedLevel, NamedTextColor.RED);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 0.8f);
                sendMessage("强化失败，等级不变", NamedTextColor.RED);
            }
        }

        refreshInventory();
    }

    private boolean hasEffect(ItemStack item, StoneEffect expected) {
        if (item == null) return false;
        Optional<EnhanceStoneType> type = EnhanceStoneType.detect(item);
        return type.isPresent() && type.get().getEffect() == expected;
    }

    // ════════════════════════════════════════
    // 关闭/取消
    // ════════════════════════════════════════

    private void cancelSession() {
        returnSessionItems();
        finalized = true;
        sendMessage("已关闭强化界面并返还所有物品", NamedTextColor.YELLOW);
        player.closeInventory();
    }

    private void returnSessionItems() {
        if (equipmentItem != null) giveOrDrop(equipmentItem);
        returnAllStones();
    }

    private void clearSession() {
        equipmentItem = null;
        materialItem = null;
        luckyStoneItem = null;
        protectStoneItem = null;
        safetyStoneItem = null;
        guaranteeStoneItem = null;
    }

    // ════════════════════════════════════════
    // 刷新界面
    // ════════════════════════════════════════

    private void refreshInventory() {
        // 先填玻璃背景
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // 装备槽
        if (equipmentItem != null) {
            inventory.setItem(EQUIPMENT_SLOT, equipmentItem.clone());
        } else {
            inventory.setItem(EQUIPMENT_SLOT, createPlaceholder(Material.ANVIL,
                "<yellow>装备槽", "<gray>点击下方背包中的可强化装备放入"));
        }

        // 材料槽 (淬炼石) — 空时显示信标
        refreshStoneSlot(MATERIAL_SLOT, materialItem, Material.BEACON,
            "<gold>材料槽",
            equipmentItem != null ? new String[]{
                "<gray>请放入: <yellow>" + getRequiredMaterialName(manager.getLevel(equipmentItem) + 1),
                "<gray>点击下方背包中的淬炼石放入"
            } : new String[]{"<gray>放入装备后显示所需淬炼石"});

        // 幸运石槽 — 空时显示信标
        refreshStoneSlot(LUCKY_SLOT, luckyStoneItem, Material.BEACON,
            "<aqua>幸运石槽",
            new String[]{"<gray>增加强化成功率 +25%", "<gray>点击下方背包中的幸运石放入"});

        // 保护石槽 — 空时显示信标
        refreshStoneSlot(PROTECT_SLOT, protectStoneItem, Material.BEACON,
            "<light_purple>保护石槽",
            new String[]{"<gray>强化失败时防止降级", "<gray>点击下方背包中的保护石放入"});

        // 安全石槽 — 空时显示信标
        refreshStoneSlot(SAFETY_SLOT, safetyStoneItem, Material.BEACON,
            "<dark_aqua>安全石槽",
            new String[]{"<gray>强化失败时防止破碎", "<gray>点击下方背包中的安全石放入"});

        // 必成石槽 — 空时显示信标
        refreshStoneSlot(GUARANTEE_SLOT, guaranteeStoneItem, Material.BEACON,
            "<#FFAA00>必成石槽",
            new String[]{"<gray>100% 强化成功！", "<gray>点击下方背包中的必成石放入"});

        // 信息按钮
        refreshInfoSlot();

        // 强化按钮
        refreshEnhanceButton();

        // 关闭按钮
        inventory.setItem(CLOSE_SLOT, createButton(Material.BARRIER,
            "<red>关闭并返还", "<gray>关闭界面并返还装备与强化石"));
    }

    private void refreshStoneSlot(int slot, ItemStack item, Material placeholderMat, String name, String[] emptyLore) {
        if (item != null) {
            inventory.setItem(slot, item.clone());
        } else {
            inventory.setItem(slot, createPlaceholder(placeholderMat, name, emptyLore));
        }
    }

    private void refreshInfoSlot() {
        if (equipmentItem == null || !EnhanceAttributeHelper.isEnhanceable(equipmentItem)) {
            inventory.setItem(INFO_SLOT, createButton(Material.BOOK,
                "<white>强化信息", "<gray>放入装备后显示详细信息"));
            return;
        }

        int level = manager.getLevel(equipmentItem);
        int maxLv = getMaxLevelForItem(equipmentItem);
        double mult = manager.getAttributeMultiplier(level);
        // 【枚举法】直接获取下一级倍率
        double nextMult = manager.getAttributeMultiplier(level + 1);

        List<String> info = new ArrayList<>();
        info.add("<yellow>当前等级: <green>+" + level + " <gray>/ " + maxLv);
        info.add("<yellow>属性倍率: <aqua>" + String.format("%.2f", mult) + "x");
        if (level < maxLv) {
            info.add("");
            info.add("<yellow>目标等级: <green>+" + (level + 1));
            info.add("<yellow>目标倍率: <aqua>" + String.format("%.2f", nextMult) + "x");
            info.add("<yellow>需要材料: <gold>" + getRequiredMaterialName(level + 1));

            double baseRate = manager.getSuccessRate(level, equipmentItem);
            double pityBonus = manager.getPityBonusForPlayer(player.getUniqueId(), level);
            double total = Math.min(1.0, baseRate + pityBonus);

            // 加上已放入的幸运石加成
            if (luckyStoneItem != null) {
                Optional<EnhanceStoneType> lt = EnhanceStoneType.detect(luckyStoneItem);
                if (lt.isPresent()) total = lt.get().apply(total);
            }
            if (guaranteeStoneItem != null) total = 1.0;

            info.add("");
            info.add("<yellow>基础成功率: <white>" + String.format("%.0f%%", baseRate * 100));
            if (pityBonus > 0)
                info.add("<yellow>保底加成: <green>+" + String.format("%.0f%%", pityBonus * 100));
            if (luckyStoneItem != null)
                info.add("<aqua>幸运石加成: <green>+25%");
            if (guaranteeStoneItem != null)
                info.add("<#FFAA00>必成石: 100%成功");
            String rc = total >= 0.7 ? "<green>" : total >= 0.4 ? "<yellow>" : "<red>";
            info.add("<gold>最终成功率: " + rc + String.format("%.0f%%", total * 100));

            if (protectStoneItem != null)
                info.add("<light_purple>保护石: 失败时防止降级");
            if (safetyStoneItem != null)
                info.add("<dark_aqua>安全石: 失败时防止破碎");
        } else {
            info.add("");
            info.add("<red>已达最大强化等级");
        }
        inventory.setItem(INFO_SLOT, createButton(Material.BOOK, "<white>强化信息", info));
    }

    private void refreshEnhanceButton() {
        if (equipmentItem == null || !EnhanceAttributeHelper.isEnhanceable(equipmentItem)) {
            inventory.setItem(ENHANCE_SLOT, createButton(Material.BARRIER,
                "<red>无法强化", "<gray>请放入可强化装备"));
            return;
        }

        int level = manager.getLevel(equipmentItem);
        int maxLv = getMaxLevelForItem(equipmentItem);
        if (level >= maxLv) {
            inventory.setItem(ENHANCE_SLOT, createButton(Material.BARRIER,
                "<red>已满级", "<gray>此装备已达最大强化等级"));
            return;
        }

        // 检查材料是否就绪
        boolean matReady = materialItem != null;
        if (matReady) {
            Optional<EnhanceStoneType> matOpt = EnhanceStoneType.detect(materialItem);
            matReady = matOpt.isPresent() && isMaterialMatchLevel(matOpt.get(), level + 1);
        }

        if (matReady) {
            inventory.setItem(ENHANCE_SLOT, createButton(Material.EMERALD_BLOCK,
                "<green>开始强化",
                "<yellow>目标: +" + (level + 1),
                "<aqua>倍率: " + String.format("%.2f", manager.getAttributeMultiplier(level + 1)) + "x"));
        } else {
            inventory.setItem(ENHANCE_SLOT, createButton(Material.OBSIDIAN,
                "<red>材料未就绪",
                "<gray>请放入: <yellow>" + getRequiredMaterialName(level + 1),
                "<gray>缺少淬炼石无法强化"));
        }
    }

    // ════════════════════════════════════════
    // 工具方法
    // ════════════════════════════════════════

    private int getMaxLevelForItem(ItemStack item) {
        String tierStr = EnhanceAttributeHelper.getItemTier(item);
        if (tierStr != null) {
            try { return config.getMaxLevelForTier(Integer.parseInt(tierStr)); }
            catch (NumberFormatException ignored) {}
        }
        return config.getMaxLevel();
    }

    private ItemStack takeSingleItem(org.bukkit.inventory.Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        ItemStack taken = sourceItem.clone();
        taken.setAmount(1);
        if (sourceItem.getAmount() <= 1) {
            sourceInventory.setItem(sourceSlot, null);
        } else {
            sourceItem.setAmount(sourceItem.getAmount() - 1);
            sourceInventory.setItem(sourceSlot, sourceItem);
        }
        return taken;
    }

    private void giveOrDrop(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private ItemStack createPlaceholder(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (miniMessage != null) {
            meta.setDisplayName(miniMessage.legacyColorize(name));
            if (lore != null && lore.length > 0) {
                List<String> translatedLore = new ArrayList<>();
                for (String line : lore) translatedLore.add(miniMessage.legacyColorize(line));
                meta.setLore(translatedLore);
            }
        } else {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        return createPlaceholder(material, name, lore.toArray(new String[0]));
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        return createPlaceholder(material, name, lore);
    }

    private void sendMessage(String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
