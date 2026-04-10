package cn.guangdian.armorstats.gui;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.event.GemInlayEvent;
import cn.guangdian.armorstats.hook.MythicMobsHook;
import cn.guangdian.armorstats.parser.GemParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

public class GemInlayGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 27;
    private static final int EQUIPMENT_SLOT = 4;
    private static final int[] GEM_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int CONFIRM_SLOT = 22;
    private static final int CANCEL_SLOT = 24;
    private static final int REWORK_SLOT = 25;
    private static final int INFO_SLOT = 26;

    private final Player player;
    private final Inventory inventory;
    private ItemStack equipmentItem;
    private final List<String> socketTypes = new ArrayList<>();
    private final ItemStack[] insertedGems = new ItemStack[GEM_SLOTS.length];
    // 已镶嵌的宝石（从装备加载）
    private final ItemStack[] existingGems = new ItemStack[GEM_SLOTS.length];
    private boolean finalized;
    
    private enum ReworkFailMode {
        KEEP_ALL,
        DESTROY_ONE,
        DESTROY_ALL
    }

    private static class ReworkSettings {
        private boolean enabled;
        private Material costItem;              // 原版材料
        private String costMythicMobsItem;     // MythicMobs物品ID
        private int costAmount;
        private int expLevels;
        private double successChance;
        private ReworkFailMode failMode;
        private boolean insuranceEnabled;
        private Material insuranceItem;              // 原版保险材料
        private String insuranceMythicMobsItem;     // MythicMobs保险物品ID
        private int insuranceAmount;
        private boolean insuranceConsumeOnUse;
    }

    public GemInlayGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.GOLD + "宝石镶嵌");
        refreshInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }

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

    private void handleTopInventoryClick(int rawSlot) {
        if (rawSlot == EQUIPMENT_SLOT) {
            withdrawEquipment();
            return;
        }

        int gemIndex = getGemSlotIndex(rawSlot);
        if (gemIndex >= 0) {
            withdrawGem(gemIndex);
            return;
        }

        if (rawSlot == CONFIRM_SLOT) {
            confirmInlay();
            return;
        }

        if (rawSlot == CANCEL_SLOT) {
            cancelSession();
            return;
        }

        if (rawSlot == REWORK_SLOT) {
            reworkInlay();
        }
    }

    private void handlePlayerInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (equipmentItem == null) {
            placeEquipment(event.getClickedInventory(), event.getSlot(), clickedItem);
            return;
        }

        placeGem(event.getClickedInventory(), event.getSlot(), clickedItem);
    }

    private void placeEquipment(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!sourceItem.hasItemMeta()) {
            player.sendMessage(ChatColor.RED + "该物品不能镶嵌宝石");
            return;
        }

        List<String> sockets = GemParser.parseSocketGems(sourceItem);
        if (sockets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "该装备没有可用的宝石孔");
            return;
        }

        equipmentItem = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        socketTypes.clear();
        socketTypes.addAll(sockets);
        Arrays.fill(insertedGems, null);
        Arrays.fill(existingGems, null);

        // 加载已镶嵌的宝石
        List<ItemStack> storedGems = GemParser.getStoredInlaidGems(equipmentItem);
        for (int i = 0; i < storedGems.size() && i < existingGems.length; i++) {
            existingGems[i] = storedGems.get(i);
        }

        refreshInventory();

        int existingCount = 0;
        for (ItemStack g : existingGems) {
            if (g != null) existingCount++;
        }
        if (existingCount > 0) {
            player.sendMessage(ChatColor.GREEN + "已放入装备，已有 " + existingCount + " 颗宝石镶嵌");
        } else {
            player.sendMessage(ChatColor.GREEN + "已放入装备，请继续放入匹配的宝石");
        }
    }

    private void placeGem(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!GemParser.isGem(sourceItem)) {
            player.sendMessage(ChatColor.RED + "请放入有效的宝石");
            return;
        }

        String gemType = GemParser.getGemType(sourceItem);
        int targetSlot = findAvailableGemSlot(gemType);
        if (targetSlot < 0) {
            player.sendMessage(ChatColor.RED + "没有可用的匹配宝石孔，该槽位已镶嵌或类型不匹配");
            return;
        }

        insertedGems[targetSlot] = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        refreshInventory();
        player.sendMessage(ChatColor.GREEN + "已放入 " + gemType);
    }

    private int findAvailableGemSlot(String gemType) {
        for (int i = 0; i < socketTypes.size() && i < insertedGems.length; i++) {
            // 跳过已镶嵌的槽位（原有或新放入的）
            if (existingGems[i] != null || insertedGems[i] != null) {
                continue;
            }
            if (GemParser.isGemCompatible(socketTypes.get(i), gemType)) {
                return i;
            }
        }
        return -1;
    }

    private void withdrawEquipment() {
        if (equipmentItem == null) {
            return;
        }

        // 返还新放入的宝石
        for (int i = 0; i < insertedGems.length; i++) {
            if (insertedGems[i] != null) {
                giveOrDrop(insertedGems[i]);
                insertedGems[i] = null;
            }
        }

        // 返还装备（保留原有镶嵌）
        giveOrDrop(equipmentItem);
        equipmentItem = null;
        socketTypes.clear();
        Arrays.fill(existingGems, null);
        refreshInventory();
    }

    private void withdrawGem(int gemIndex) {
        // 只能取回新放入的宝石，已镶嵌的不能直接取回（需要通过拆卸功能）
        if (gemIndex < 0 || gemIndex >= insertedGems.length || insertedGems[gemIndex] == null) {
            return;
        }

        giveOrDrop(insertedGems[gemIndex]);
        insertedGems[gemIndex] = null;
        refreshInventory();
    }

    private int getGemSlotIndex(int rawSlot) {
        for (int i = 0; i < GEM_SLOTS.length; i++) {
            if (GEM_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    private void confirmInlay() {
        if (equipmentItem == null) {
            player.sendMessage(ChatColor.RED + "请先放入装备");
            return;
        }

        // 保存原始装备用于事件
        ItemStack originalEquipment = equipmentItem.clone();

        // 合并已有宝石和新放入的宝石
        List<ItemStack> allGems = collectAllGems();
        Map<String, AttributeValue> totalAttrs = collectAllGemAttributes(allGems);

        if (totalAttrs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "请至少放入一颗宝石");
            return;
        }

        ItemStack finalItem = equipmentItem.clone();
        GemParser.applyInlay(finalItem, allGems, totalAttrs);

        giveOrDrop(finalItem);
        clearSession();
        finalized = true;
        player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功");
        player.closeInventory();

        // 触发宝石镶嵌事件（同步触发，让缓存系统响应）
        GemInlayEvent inlayEvent = new GemInlayEvent(player, finalItem, originalEquipment, allGems, totalAttrs);
        Bukkit.getPluginManager().callEvent(inlayEvent);

        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                GuangDianArmorStats.getInstance().getStatsManager().refreshPlayerStats(player);
                if (GuangDianArmorStats.getInstance().getBossBarManager() != null) {
                    GuangDianArmorStats.getInstance().getBossBarManager().updateBossBar(player);
                }
            }, 1L);
        }
    }

    private void cancelSession() {
        returnSessionItems();
        finalized = true;
        player.sendMessage(ChatColor.YELLOW + "已取消镶嵌并返还所有物品");
        player.closeInventory();
    }

    private void returnSessionItems() {
        if (equipmentItem != null) {
            giveOrDrop(equipmentItem);
        }

        for (ItemStack gem : insertedGems) {
            if (gem != null) {
                giveOrDrop(gem);
            }
        }

        clearSession();
    }

    private void reworkInlay() {
        if (equipmentItem == null) {
            player.sendMessage(ChatColor.RED + "请先放入装备");
            return;
        }

        ReworkSettings settings = loadReworkSettings();
        if (!settings.enabled) {
            player.sendMessage(ChatColor.RED + "当前已关闭拆卸镶嵌功能");
            return;
        }

        List<ItemStack> storedGems = GemParser.getStoredInlaidGems(equipmentItem);
        List<ItemStack> pendingGems = collectCurrentGems();
        if (storedGems.isEmpty() && pendingGems.isEmpty()) {
            player.sendMessage(ChatColor.RED + "该装备没有可拆卸的镶嵌宝石");
            return;
        }

        if (!canAffordRework(settings)) {
            return;
        }
        consumeReworkCost(settings);

        boolean success = ThreadLocalRandom.current().nextDouble() <= settings.successChance;
        boolean protectedByInsurance = false;
        if (!success && settings.insuranceEnabled) {
            protectedByInsurance = tryConsumeInsurance(settings);
        }

        if (success || protectedByInsurance || settings.failMode == ReworkFailMode.KEEP_ALL) {
            // 保存原始装备用于事件
            ItemStack originalEquipment = equipmentItem.clone();
            
            for (ItemStack gem : pendingGems) {
                giveOrDrop(gem);
            }
            for (ItemStack gem : storedGems) {
                if (gem != null) {
                    giveOrDrop(gem);
                }
            }
            equipmentItem = GemParser.clearInlay(equipmentItem);
            Arrays.fill(insertedGems, null);
            refreshInventory();
            
            // 触发拆卸事件（用于缓存联动）
            GemInlayEvent reworkEvent = new GemInlayEvent(player, equipmentItem, originalEquipment, true);
            Bukkit.getPluginManager().callEvent(reworkEvent);
            
            if (success) {
                player.sendMessage(ChatColor.GREEN + "拆卸成功，已返还全部宝石");
            } else if (protectedByInsurance) {
                player.sendMessage(ChatColor.GOLD + "拆卸失败，但保险符生效，已返还全部宝石");
            } else {
                player.sendMessage(ChatColor.YELLOW + "拆卸失败，但本次未损失宝石");
            }
            return;
        }

        List<ItemStack> allGems = new ArrayList<>();
        for (ItemStack gem : storedGems) {
            if (gem != null) {
                allGems.add(gem);
            }
        }
        allGems.addAll(pendingGems);

        if (settings.failMode == ReworkFailMode.DESTROY_ONE && !allGems.isEmpty()) {
            int destroyedIndex = ThreadLocalRandom.current().nextInt(allGems.size());
            allGems.remove(destroyedIndex);
            for (ItemStack gem : allGems) {
                giveOrDrop(gem);
            }
            equipmentItem = GemParser.clearInlay(equipmentItem);
            Arrays.fill(insertedGems, null);
            refreshInventory();
            player.sendMessage(ChatColor.RED + "拆卸失败，损毁1颗宝石，其余已返还");
            return;
        }

        equipmentItem = GemParser.clearInlay(equipmentItem);
        Arrays.fill(insertedGems, null);
        refreshInventory();
        player.sendMessage(ChatColor.RED + "拆卸失败，宝石全部损毁");
    }

    /**
     * 收集所有宝石（已镶嵌 + 新放入）
     */
    private List<ItemStack> collectAllGems() {
        List<ItemStack> allGems = new ArrayList<>();
        // 先添加已镶嵌的宝石
        for (ItemStack gem : existingGems) {
            if (gem != null) {
                ItemStack single = gem.clone();
                single.setAmount(1);
                allGems.add(single);
            }
        }
        // 再添加新放入的宝石
        for (ItemStack gem : insertedGems) {
            if (gem != null) {
                ItemStack single = gem.clone();
                single.setAmount(1);
                allGems.add(single);
            }
        }
        return allGems;
    }

    /**
     * 收集所有宝石的属性
     */
    private Map<String, AttributeValue> collectAllGemAttributes(List<ItemStack> gems) {
        Map<String, AttributeValue> totalAttrs = new HashMap<>();
        for (ItemStack gem : gems) {
            if (gem == null) {
                continue;
            }
            Map<String, AttributeValue> attrs = GemParser.parseGemAttributes(gem);
            for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                totalAttrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
            }
        }
        return totalAttrs;
    }

    private Map<String, AttributeValue> collectGemAttributes() {
        Map<String, AttributeValue> totalAttrs = new HashMap<>();
        for (ItemStack gem : insertedGems) {
            if (gem == null) {
                continue;
            }
            Map<String, AttributeValue> attrs = GemParser.parseGemAttributes(gem);
            for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                totalAttrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
            }
        }
        return totalAttrs;
    }

    private List<ItemStack> collectCurrentGems() {
        List<ItemStack> gems = new ArrayList<>();
        for (ItemStack gem : insertedGems) {
            if (gem != null) {
                ItemStack single = gem.clone();
                single.setAmount(1);
                gems.add(single);
            }
        }
        return gems;
    }

    private ItemStack takeSingleItem(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
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
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void clearSession() {
        equipmentItem = null;
        socketTypes.clear();
        Arrays.fill(insertedGems, null);
        refreshInventory();
    }

    private void refreshInventory() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        inventory.setItem(EQUIPMENT_SLOT, equipmentItem == null
                ? createPlaceholder(Material.ANVIL, "&e装备槽", "&7点击下方背包中的装备放入")
                : createEquipmentPreview());

        for (int i = 0; i < GEM_SLOTS.length; i++) {
            if (i >= socketTypes.size()) {
                // 超出槽位范围
                inventory.setItem(GEM_SLOTS[i], createPlaceholder(Material.BARRIER, "&8未开放", "&7该宝石孔不存在"));
            } else if (insertedGems[i] != null) {
                // 新放入的宝石
                inventory.setItem(GEM_SLOTS[i], insertedGems[i].clone());
            } else if (existingGems[i] != null) {
                // 已镶嵌的宝石 - 显示为已镶嵌状态（不可点击取回）
                ItemStack gem = existingGems[i];
                ItemStack display = gem.clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore())
                            : new ArrayList<>();
                    lore.add(0, ChatColor.GRAY + "--- 已镶嵌 ---");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inventory.setItem(GEM_SLOTS[i], display);
            } else {
                // 空槽位
                inventory.setItem(
                        GEM_SLOTS[i],
                        createPlaceholder(Material.BEACON, "&b宝石孔 " + (i + 1), "&7需求: " + socketTypes.get(i), "&7点击下方背包中的宝石放入")
                );
            }
        }

        inventory.setItem(CONFIRM_SLOT, createButton(Material.EMERALD_BLOCK, "&a确认镶嵌", "&7将当前宝石写入装备"));
        inventory.setItem(CANCEL_SLOT, createButton(Material.BARRIER, "&c取消并返还", "&7关闭并返还装备和宝石"));
        inventory.setItem(REWORK_SLOT, createReworkButton());
        inventory.setItem(INFO_SLOT, createInfoItem());
    }

    private ItemStack createEquipmentPreview() {
        ItemStack preview = equipmentItem.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) {
            return preview;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        // 显示所有宝石（已有 + 新放入）的属性预览
        List<ItemStack> allGems = collectAllGems();
        Map<String, AttributeValue> allAttrs = collectAllGemAttributes(allGems);
        lore = GemParser.appendInlaySection(lore, allAttrs);
        meta.setLore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack createInfoItem() {
        return createPlaceholder(
                Material.PAPER,
                "&f使用说明",
                "&71. 点击下方背包中的装备放入",
                "&72. 点击下方背包中的宝石依次放入",
                "&73. 点击上方宝石可取回单颗宝石",
                "&74. 点击红石可拆卸原有镶嵌",
                "&75. 点击装备槽可取回装备与全部宝石"
        );
    }

    private ItemStack createReworkButton() {
        ReworkSettings settings = loadReworkSettings();
        String status = settings.enabled ? "&a已启用" : "&c已关闭";
        String chance = "&7成功率: &f" + (int) (settings.successChance * 100) + "%";
        
        // 显示消耗物品名称
        String costItemName;
        if (settings.costMythicMobsItem != null) {
            costItemName = settings.costMythicMobsItem + " (MythicMobs)";
        } else if (settings.costItem != null) {
            costItemName = settings.costItem.name();
        } else {
            costItemName = "未配置";
        }
        String cost = "&7消耗: &f" + settings.costAmount + "x " + costItemName;
        
        String levelCost = "&7等级消耗: &f" + settings.expLevels;
        String failMode = "&7失败结果: &f" + switch (settings.failMode) {
            case KEEP_ALL -> "不损失";
            case DESTROY_ONE -> "损毁1颗";
            case DESTROY_ALL -> "全部损毁";
        };
        
        // 显示保险物品名称
        String insurance;
        if (settings.insuranceEnabled) {
            String insuranceItemName;
            if (settings.insuranceMythicMobsItem != null) {
                insuranceItemName = settings.insuranceMythicMobsItem + " (MythicMobs)";
            } else if (settings.insuranceItem != null) {
                insuranceItemName = settings.insuranceItem.name();
            } else {
                insuranceItemName = "未配置";
            }
            insurance = "&7保险符: &f" + settings.insuranceAmount + "x " + insuranceItemName;
        } else {
            insurance = "&7保险符: &8未启用";
        }
        
        return createButton(Material.REDSTONE_BLOCK, "&6拆卸镶嵌", status, chance, cost, levelCost, failMode, insurance);
    }

    private ReworkSettings loadReworkSettings() {
        ReworkSettings settings = new ReworkSettings();
        ConfigurationSection section = GuangDianArmorStats.getInstance().getConfig().getConfigurationSection("gem_rework");
        settings.enabled = section == null || section.getBoolean("enabled", true);
        
        // 支持MythicMobs物品配置
        String costItemStr = section == null ? null : section.getString("cost_item");
        if (costItemStr != null && costItemStr.toLowerCase().startsWith("mythicmobs:")) {
            settings.costMythicMobsItem = costItemStr.substring(costItemStr.indexOf(':') + 1);
            settings.costItem = null;
        } else {
            settings.costItem = parseMaterial(costItemStr, Material.AMETHYST_SHARD);
            settings.costMythicMobsItem = null;
        }
        
        settings.costAmount = Math.max(0, section == null ? 1 : section.getInt("cost_amount", 1));
        settings.expLevels = Math.max(0, section == null ? 0 : section.getInt("exp_levels", 0));
        settings.successChance = Math.max(0.0, Math.min(1.0, section == null ? 1.0 : section.getDouble("success_chance", 1.0)));
        settings.failMode = parseFailMode(section == null ? null : section.getString("fail_mode"));

        ConfigurationSection insurance = section == null ? null : section.getConfigurationSection("insurance");
        settings.insuranceEnabled = insurance != null && insurance.getBoolean("enabled", false);
        
        // 支持MythicMobs保险物品
        String insuranceItemStr = insurance == null ? null : insurance.getString("item");
        if (insuranceItemStr != null && insuranceItemStr.toLowerCase().startsWith("mythicmobs:")) {
            settings.insuranceMythicMobsItem = insuranceItemStr.substring(insuranceItemStr.indexOf(':') + 1);
            settings.insuranceItem = null;
        } else {
            settings.insuranceItem = parseMaterial(insuranceItemStr, Material.TOTEM_OF_UNDYING);
            settings.insuranceMythicMobsItem = null;
        }
        
        settings.insuranceAmount = Math.max(1, insurance == null ? 1 : insurance.getInt("amount", 1));
        settings.insuranceConsumeOnUse = insurance == null || insurance.getBoolean("consume_on_use", true);
        return settings;
    }

    private ReworkFailMode parseFailMode(String mode) {
        if (mode == null) {
            return ReworkFailMode.DESTROY_ONE;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "keep", "keep_all", "none", "不损失" -> ReworkFailMode.KEEP_ALL;
            case "destroy_all", "all", "全部损毁" -> ReworkFailMode.DESTROY_ALL;
            default -> ReworkFailMode.DESTROY_ONE;
        };
    }

    private Material parseMaterial(String value, Material fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.trim().toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private boolean canAffordRework(ReworkSettings settings) {
        if (player.getLevel() < settings.expLevels) {
            player.sendMessage(ChatColor.RED + "等级不足，需要 " + settings.expLevels + " 级");
            return false;
        }
        if (settings.costAmount > 0) {
            // 检查MythicMobs物品或原版材料
            if (settings.costMythicMobsItem != null) {
                if (!hasEnoughMythicMobsItem(settings.costMythicMobsItem, settings.costAmount)) {
                    player.sendMessage(ChatColor.RED + "材料不足，需要 " + settings.costAmount + "x " + settings.costMythicMobsItem);
                    return false;
                }
            } else if (settings.costItem != null && !hasEnoughMaterial(settings.costItem, settings.costAmount)) {
                player.sendMessage(ChatColor.RED + "材料不足，需要 " + settings.costAmount + "x " + settings.costItem.name());
                return false;
            }
        }
        return true;
    }

    private void consumeReworkCost(ReworkSettings settings) {
        if (settings.expLevels > 0) {
            player.setLevel(player.getLevel() - settings.expLevels);
        }
        if (settings.costAmount > 0) {
            // 消耗MythicMobs物品或原版材料
            if (settings.costMythicMobsItem != null) {
                removeMythicMobsItem(settings.costMythicMobsItem, settings.costAmount);
            } else if (settings.costItem != null) {
                removeMaterial(settings.costItem, settings.costAmount);
            }
        }
    }

    private boolean tryConsumeInsurance(ReworkSettings settings) {
        // 检查MythicMobs保险物品或原版保险材料
        if (settings.insuranceMythicMobsItem != null) {
            if (!hasEnoughMythicMobsItem(settings.insuranceMythicMobsItem, settings.insuranceAmount)) {
                return false;
            }
            if (settings.insuranceConsumeOnUse) {
                removeMythicMobsItem(settings.insuranceMythicMobsItem, settings.insuranceAmount);
            }
            return true;
        } else if (settings.insuranceItem != null) {
            if (!hasEnoughMaterial(settings.insuranceItem, settings.insuranceAmount)) {
                return false;
            }
            if (settings.insuranceConsumeOnUse) {
                removeMaterial(settings.insuranceItem, settings.insuranceAmount);
            }
            return true;
        }
        return false;
    }
    
    /**
     * 检查玩家是否有足够的MythicMobs物品
     */
    private boolean hasEnoughMythicMobsItem(String itemId, int amount) {
        MythicMobsHook hook = MythicMobsHook.getInstance();
        if (!hook.isEnabled()) return false;
        
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (hook.isMythicItem(item, itemId)) {
                total += item.getAmount();
                if (total >= amount) return true;
            }
        }
        return false;
    }
    
    /**
     * 移除玩家的MythicMobs物品
     */
    private void removeMythicMobsItem(String itemId, int amount) {
        MythicMobsHook hook = MythicMobsHook.getInstance();
        if (!hook.isEnabled()) return;
        
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (hook.isMythicItem(item, itemId)) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    private boolean hasEnoughMaterial(Material material, int amount) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }
            total += item.getAmount();
            if (total >= amount) {
                return true;
            }
        }
        return false;
    }

    private void removeMaterial(Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) {
                break;
            }
        }
        player.getInventory().setContents(contents);
    }

    private ItemStack createPlaceholder(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && lore.length > 0) {
            List<String> translatedLore = new ArrayList<>();
            for (String line : lore) {
                translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(translatedLore);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        return createPlaceholder(material, name, lore);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
