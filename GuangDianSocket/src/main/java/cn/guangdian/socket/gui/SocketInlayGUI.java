package cn.guangdian.socket.gui;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.event.SocketInlayEvent;
import cn.guangdian.socket.hook.RPGItemsHook;
import cn.guangdian.socket.manager.SocketService;
import cn.guangdian.socket.util.ItemResolver;
import cn.guangdian.socket.model.AttributeValue;
import cn.guangdian.socket.parser.SocketParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

/**
 * 宝石镶嵌 GUI
 */
public class SocketInlayGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 27;
    private static final int EQUIPMENT_SLOT = 4;
    private static final int[] GEM_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int CONFIRM_SLOT = 22;
    private static final int CANCEL_SLOT = 24;
    private static final int REWORK_SLOT = 25;
    private static final int INFO_SLOT = 26;

    private final GuangDianSocket plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessageService miniMessage;
    private ItemStack equipmentItem;
    private final List<String> socketTypes = new ArrayList<>();
    private final ItemStack[] insertedGems = new ItemStack[GEM_SLOTS.length];
    private final ItemStack[] existingGems = new ItemStack[GEM_SLOTS.length];
    private boolean finalized;
    
    // 记录装备来源
    private int equipmentSourceSlot = -1;  // -1表示主手，>=0表示背包槽位
    private Inventory equipmentSourceInventory = null;

    private enum ReworkFailMode {
        KEEP_ALL, DESTROY_ONE, DESTROY_ALL
    }

    private static class ReworkSettings {
        private boolean enabled;
        private Material costItem;
        private String costRPGItem;
        private int costAmount;
        private int expLevels;
        private double successChance;
        private ReworkFailMode failMode;
        private boolean insuranceEnabled;
        private Material insuranceItem;
        private String insuranceRPGItem;
        private int insuranceAmount;
        private boolean insuranceConsumeOnUse;
    }

    public SocketInlayGUI(GuangDianSocket plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? rpgCore.getMiniMessageService() : null;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
            net.kyori.adventure.text.Component.text("宝石镶嵌", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        refreshInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

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
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (equipmentItem == null) {
            placeEquipment(event.getClickedInventory(), event.getSlot(), clickedItem);
            return;
        }
        placeGem(event.getClickedInventory(), event.getSlot(), clickedItem);
    }

    private void placeEquipment(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!sourceItem.hasItemMeta()) {
            sendMessage("该物品不能镶嵌宝石", NamedTextColor.RED);
            return;
        }

        List<String> sockets = SocketParser.parseSocketGems(sourceItem);
        if (sockets.isEmpty()) {
            sendMessage("该装备没有可用的宝石孔", NamedTextColor.RED);
            return;
        }

        equipmentItem = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        socketTypes.clear();
        socketTypes.addAll(sockets);
        Arrays.fill(insertedGems, null);
        Arrays.fill(existingGems, null);

        List<cn.guangdian.socket.model.GemData> storedGemData = SocketParser.loadGemData(equipmentItem);
        
        plugin.getLogger().info("[DEBUG] placeEquipment: storedGemData.size() = " + storedGemData.size());
        for (int i = 0; i < storedGemData.size(); i++) {
            cn.guangdian.socket.model.GemData gemData = storedGemData.get(i);
            plugin.getLogger().info("[DEBUG] GemData[" + i + "]: slotIndex=" + gemData.getSlotIndex() + ", itemId=" + gemData.getItemId());
            ItemStack gemItem = gemData.toItemStack();
            plugin.getLogger().info("[DEBUG] toItemStack result: " + (gemItem != null ? gemItem.getType().name() : "null"));
            
            // 使用宝石的槽位索引，而不是循环索引
            int slotIndex = gemData.getSlotIndex();
            if (gemItem != null && slotIndex >= 0 && slotIndex < existingGems.length) {
                existingGems[slotIndex] = gemItem;
            }
        }

        refreshInventory();

        int existingCount = 0;
        for (ItemStack g : existingGems) if (g != null) existingCount++;
        int emptySlots = socketTypes.size() - existingCount;
        if (existingCount > 0) {
            sendMessage("已放入装备，已有 " + existingCount + " 颗宝石镶嵌，剩余 " + emptySlots + " 个空孔", NamedTextColor.GREEN);
        } else {
            sendMessage("已放入装备，请继续放入匹配的宝石", NamedTextColor.GREEN);
        }
    }

    private void placeGem(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!SocketParser.isGem(sourceItem)) {
            sendMessage("请放入有效的宝石", NamedTextColor.RED);
            return;
        }

        String gemType = SocketParser.getGemType(sourceItem);
        int targetSlot = findAvailableGemSlot(gemType);
        if (targetSlot < 0) {
            sendMessage("没有可用的匹配宝石孔，该槽位已镶嵌或类型不匹配", NamedTextColor.RED);
            return;
        }

        insertedGems[targetSlot] = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        refreshInventory();
        sendMessage("已放入 " + gemType, NamedTextColor.GREEN);
    }

    private int findAvailableGemSlot(String gemType) {
        for (int i = 0; i < socketTypes.size() && i < insertedGems.length; i++) {
            if (existingGems[i] != null || insertedGems[i] != null) continue;
            if (SocketParser.isGemCompatible(socketTypes.get(i), gemType)) return i;
        }
        return -1;
    }

    private void withdrawEquipment() {
        if (equipmentItem == null) return;

        for (int i = 0; i < insertedGems.length; i++) {
            if (insertedGems[i] != null) {
                giveOrDrop(insertedGems[i]);
                insertedGems[i] = null;
            }
        }

        giveOrDrop(equipmentItem);
        equipmentItem = null;
        socketTypes.clear();
        Arrays.fill(existingGems, null);
        refreshInventory();
    }

    private void withdrawGem(int gemIndex) {
        if (gemIndex < 0 || gemIndex >= insertedGems.length || insertedGems[gemIndex] == null) return;
        giveOrDrop(insertedGems[gemIndex]);
        insertedGems[gemIndex] = null;
        refreshInventory();
    }

    private int getGemSlotIndex(int rawSlot) {
        for (int i = 0; i < GEM_SLOTS.length; i++) {
            if (GEM_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }

    private void confirmInlay() {
        if (equipmentItem == null) {
            sendMessage("请先放入装备", NamedTextColor.RED);
            return;
        }

        // 只收集新放入的宝石
        List<ItemStack> newGems = collectCurrentGems();
        if (newGems.isEmpty()) {
            sendMessage("请至少放入一颗新宝石", NamedTextColor.RED);
            return;
        }

        ItemStack originalEquipment = equipmentItem.clone();

        // 收集所有宝石（包括已镶嵌的 + 新放入的），带槽位索引
        Map<Integer, ItemStack> allGemsBySlot = collectAllGemsBySlot();
        Map<String, AttributeValue> totalAttrs = collectAllGemAttributes(allGemsBySlot);

        ItemStack finalItem = equipmentItem.clone();

        // 对所有宝石进行镶嵌处理（使用槽位索引，避免错位）
        SocketParser.applyInlayBySlot(finalItem, allGemsBySlot, totalAttrs);

        giveOrDrop(finalItem);
        clearSession();
        finalized = true;
        sendMessage("宝石镶嵌成功", NamedTextColor.GREEN);
        player.closeInventory();

        SocketInlayEvent inlayEvent = new SocketInlayEvent(player, finalItem, originalEquipment, newGems, totalAttrs);
        Bukkit.getPluginManager().callEvent(inlayEvent);

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                SocketService service = plugin.getSocketService();
                if (service != null) {
                    service.refreshPlayerStats(player);
                }
            }, 1L);
        }
    }

    private void cancelSession() {
        returnSessionItems();
        finalized = true;
        sendMessage("已取消镶嵌并返还所有物品", NamedTextColor.YELLOW);
        player.closeInventory();
    }

    private void returnSessionItems() {
        if (equipmentItem != null) giveOrDrop(equipmentItem);
        for (ItemStack gem : insertedGems) if (gem != null) giveOrDrop(gem);
        clearSession();
    }

    private void reworkInlay() {
        plugin.getLogger().info("[SocketGUI] reworkInlay: 开始拆卸");
        
        if (equipmentItem == null) {
            sendMessage("请先放入装备", NamedTextColor.RED);
            return;
        }

        ReworkSettings settings = loadReworkSettings();
        plugin.getLogger().info("[SocketGUI] reworkInlay: settings.enabled=" + settings.enabled);
        
        if (!settings.enabled) {
            sendMessage("当前已关闭拆卸镶嵌功能", NamedTextColor.RED);
            return;
        }

        List<ItemStack> storedGems = SocketParser.getStoredInlaidGems(equipmentItem);
        List<ItemStack> pendingGems = collectCurrentGems();
        plugin.getLogger().info("[SocketGUI] reworkInlay: storedGems=" + storedGems.size() + ", pendingGems=" + pendingGems.size());
        
        if (storedGems.isEmpty() && pendingGems.isEmpty()) {
            sendMessage("该装备没有可拆卸的镶嵌宝石", NamedTextColor.RED);
            return;
        }

        plugin.getLogger().info("[SocketGUI] reworkInlay: 检测拆卸石消耗");
        if (!canAffordRework(settings)) {
            plugin.getLogger().info("[SocketGUI] reworkInlay: 拆卸石检测失败");
            return;
        }
        plugin.getLogger().info("[SocketGUI] reworkInlay: 拆卸石检测通过，开始消耗");
        consumeReworkCost(settings);

        boolean success = ThreadLocalRandom.current().nextDouble() <= settings.successChance;
        boolean protectedByInsurance = false;
        if (!success && settings.insuranceEnabled) {
            protectedByInsurance = tryConsumeInsurance(settings);
        }

        if (success || protectedByInsurance || settings.failMode == ReworkFailMode.KEEP_ALL) {
            ItemStack originalEquipment = equipmentItem.clone();

            for (ItemStack gem : pendingGems) giveOrDrop(gem);
            for (ItemStack gem : storedGems) if (gem != null) giveOrDrop(gem);

            equipmentItem = SocketParser.clearInlay(equipmentItem);
            Arrays.fill(insertedGems, null);
            Arrays.fill(existingGems, null);
            socketTypes.clear();
            socketTypes.addAll(SocketParser.parseSocketGems(equipmentItem));
            refreshInventory();

            SocketInlayEvent reworkEvent = new SocketInlayEvent(player, equipmentItem, originalEquipment, true);
            Bukkit.getPluginManager().callEvent(reworkEvent);

            if (success) {
                sendMessage("拆卸成功，已返还全部宝石", NamedTextColor.GREEN);
            } else if (protectedByInsurance) {
                sendMessage("拆卸失败，但保险符生效，已返还全部宝石", NamedTextColor.GOLD);
            } else {
                sendMessage("拆卸失败，但本次未损失宝石", NamedTextColor.YELLOW);
            }
            return;
        }

        List<ItemStack> allGems = new ArrayList<>();
        for (ItemStack gem : storedGems) if (gem != null) allGems.add(gem);
        allGems.addAll(pendingGems);

        if (settings.failMode == ReworkFailMode.DESTROY_ONE && !allGems.isEmpty()) {
            int destroyedIndex = ThreadLocalRandom.current().nextInt(allGems.size());
            allGems.remove(destroyedIndex);
            for (ItemStack gem : allGems) giveOrDrop(gem);
            equipmentItem = SocketParser.clearInlay(equipmentItem);
            Arrays.fill(insertedGems, null);
            Arrays.fill(existingGems, null);
            refreshInventory();
            sendMessage("拆卸失败，损毁1颗宝石，其余已返还", NamedTextColor.RED);
            return;
        }

        equipmentItem = SocketParser.clearInlay(equipmentItem);
        Arrays.fill(insertedGems, null);
        Arrays.fill(existingGems, null);
        refreshInventory();
        sendMessage("拆卸失败，宝石全部损毁", NamedTextColor.RED);
    }

    /**
     * 收集所有宝石（带槽位索引）
     * 使用 Map<槽位索引, 宝石> 保留槽位信息，避免遍历算法导致的错位问题
     */
    private Map<Integer, ItemStack> collectAllGemsBySlot() {
        Map<Integer, ItemStack> gemsBySlot = new HashMap<>();
        
        // 已镶嵌的宝石（保留原始槽位）
        for (int i = 0; i < existingGems.length; i++) {
            if (existingGems[i] != null) {
                ItemStack single = existingGems[i].clone();
                single.setAmount(1);
                gemsBySlot.put(i, single);
            }
        }
        
        // 新放入的宝石（使用目标槽位）
        for (int i = 0; i < insertedGems.length; i++) {
            if (insertedGems[i] != null) {
                ItemStack single = insertedGems[i].clone();
                single.setAmount(1);
                gemsBySlot.put(i, single);
            }
        }
        
        return gemsBySlot;
    }

    /**
     * 收集所有宝石的属性（从带槽位索引的 Map）
     */
    private Map<String, AttributeValue> collectAllGemAttributes(Map<Integer, ItemStack> gemsBySlot) {
        Map<String, AttributeValue> totalAttrs = new HashMap<>();
        for (ItemStack gem : gemsBySlot.values()) {
            if (gem == null) continue;
            Map<String, AttributeValue> attrs = SocketParser.parseGemAttributes(gem);
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
        if (item == null || item.getType() == Material.AIR) return;
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
            ? createPlaceholder(Material.ANVIL, "<yellow>装备槽", "<gray>点击下方背包中的装备放入")
            : createEquipmentPreview());

        for (int i = 0; i < GEM_SLOTS.length; i++) {
            if (i >= socketTypes.size()) {
                inventory.setItem(GEM_SLOTS[i], createPlaceholder(Material.BARRIER, "<dark_gray>未开放", "<gray>该宝石孔不存在"));
            } else if (insertedGems[i] != null) {
                inventory.setItem(GEM_SLOTS[i], insertedGems[i].clone());
            } else if (existingGems[i] != null) {
                inventory.setItem(GEM_SLOTS[i], existingGems[i].clone());
            } else {
                inventory.setItem(GEM_SLOTS[i],
                    createPlaceholder(Material.BEACON, "<aqua>宝石孔 " + (i + 1),
                        "<gray>需求: " + socketTypes.get(i), "<gray>点击下方背包中的宝石放入"));
            }
        }

        inventory.setItem(CONFIRM_SLOT, createButton(Material.EMERALD_BLOCK, "<green>确认镶嵌", "<gray>将当前宝石写入装备"));
        inventory.setItem(CANCEL_SLOT, createButton(Material.BARRIER, "<red>取消并返还", "<gray>关闭并返还装备和宝石"));
        inventory.setItem(REWORK_SLOT, createReworkButton());
        inventory.setItem(INFO_SLOT, createInfoItem());
    }

    private ItemStack createEquipmentPreview() {
        ItemStack preview = equipmentItem.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) return preview;

        List<Component> loreComponents = meta.lore();
        List<String> legacyLore = new ArrayList<>();
        if (loreComponents != null) {
            for (Component comp : loreComponents) {
                String miniMsg = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(comp);
                legacyLore.add(miniMsg);
            }
        }
        
        // 预览时只处理新放入的宝石，已镶嵌的宝石已经在 Lore 中了
        List<ItemStack> newGems = collectCurrentGems();
        Map<Integer, ItemStack> allGemsBySlot = collectAllGemsBySlot();
        Map<String, AttributeValue> allAttrs = collectAllGemAttributes(allGemsBySlot);
        legacyLore = SocketParser.replaceSocketWithInlay(legacyLore, newGems, allAttrs);
        
        List<Component> newLoreComponents = new ArrayList<>();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        for (String line : legacyLore) {
            newLoreComponents.add(mm.deserialize(line));
        }
        meta.lore(newLoreComponents);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack createInfoItem() {
        return createPlaceholder(Material.PAPER, "<white>使用说明",
            "<gray>1. 点击下方背包中的装备放入",
            "<gray>2. 点击下方背包中的宝石依次放入",
            "<gray>3. 点击上方宝石可取回单颗宝石",
            "<gray>4. 点击红石可拆卸原有镶嵌",
            "<gray>5. 点击装备槽可取回装备与全部宝石");
    }

    private ItemStack createReworkButton() {
        ReworkSettings settings = loadReworkSettings();
        String status = settings.enabled ? "<green>已启用" : "<red>已关闭";
        String chance = "<gray>成功率: <white>" + (int) (settings.successChance * 100) + "%";

        String costItemName;
        if (settings.costRPGItem != null) {
            costItemName = settings.costRPGItem + " (RPGItem)";
        } else if (settings.costItem != null) {
            costItemName = settings.costItem.name();
        } else {
            costItemName = "未配置";
        }
        String cost = "<gray>消耗: <white>" + settings.costAmount + "x " + costItemName;
        String levelCost = "<gray>等级消耗: <white>" + settings.expLevels;
        String failMode = "<gray>失败结果: <white>" + switch (settings.failMode) {
            case KEEP_ALL -> "不损失";
            case DESTROY_ONE -> "损毁1颗";
            case DESTROY_ALL -> "全部损毁";
        };

        String insurance;
        if (settings.insuranceEnabled) {
            String insuranceItemName;
            if (settings.insuranceRPGItem != null) {
                insuranceItemName = settings.insuranceRPGItem + " (RPGItem)";
            } else if (settings.insuranceItem != null) {
                insuranceItemName = settings.insuranceItem.name();
            } else {
                insuranceItemName = "未配置";
            }
            insurance = "<gray>保险符: <white>" + settings.insuranceAmount + "x " + insuranceItemName;
        } else {
            insurance = "<gray>保险符: <dark_gray>未启用";
        }

        return createButton(Material.REDSTONE_BLOCK, "<gold>拆卸镶嵌", status, chance, cost, levelCost, failMode, insurance);
    }

    private ReworkSettings loadReworkSettings() {
        ReworkSettings settings = new ReworkSettings();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gem_rework");
        settings.enabled = section == null || section.getBoolean("enabled", true);

        String costItemStr = section == null ? null : section.getString("cost_item");
        if (costItemStr != null && costItemStr.toLowerCase().startsWith("rpgitem:")) {
            settings.costRPGItem = costItemStr.substring(costItemStr.indexOf(':') + 1);
            settings.costItem = null;
        } else {
            settings.costItem = parseMaterial(costItemStr, Material.AMETHYST_SHARD);
            settings.costRPGItem = null;
        }

        settings.costAmount = Math.max(0, section == null ? 1 : section.getInt("cost_amount", 1));
        settings.expLevels = Math.max(0, section == null ? 0 : section.getInt("exp_levels", 0));
        settings.successChance = Math.max(0.0, Math.min(1.0, section == null ? 1.0 : section.getDouble("success_chance", 1.0)));
        settings.failMode = parseFailMode(section == null ? null : section.getString("fail_mode"));

        ConfigurationSection insurance = section == null ? null : section.getConfigurationSection("insurance");
        settings.insuranceEnabled = insurance != null && insurance.getBoolean("enabled", false);

        String insuranceItemStr = insurance == null ? null : insurance.getString("item");
        if (insuranceItemStr != null && insuranceItemStr.toLowerCase().startsWith("rpgitem:")) {
            settings.insuranceRPGItem = insuranceItemStr.substring(insuranceItemStr.indexOf(':') + 1);
            settings.insuranceItem = null;
        } else {
            settings.insuranceItem = parseMaterial(insuranceItemStr, Material.TOTEM_OF_UNDYING);
            settings.insuranceRPGItem = null;
        }

        settings.insuranceAmount = Math.max(1, insurance == null ? 1 : insurance.getInt("amount", 1));
        settings.insuranceConsumeOnUse = insurance == null || insurance.getBoolean("consume_on_use", true);
        return settings;
    }

    private ReworkFailMode parseFailMode(String mode) {
        if (mode == null) return ReworkFailMode.DESTROY_ONE;
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "keep", "keep_all", "none", "不损失" -> ReworkFailMode.KEEP_ALL;
            case "destroy_all", "all", "全部损毁" -> ReworkFailMode.DESTROY_ALL;
            default -> ReworkFailMode.DESTROY_ONE;
        };
    }

    private Material parseMaterial(String value, Material fallback) {
        if (value == null || value.isBlank()) return fallback;
        Material material = Material.matchMaterial(value.trim().toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private boolean canAffordRework(ReworkSettings settings) {
        if (player.getLevel() < settings.expLevels) {
            sendMessage("等级不足，需要 " + settings.expLevels + " 级", NamedTextColor.RED);
            return false;
        }
        if (settings.costAmount > 0) {
            if (settings.costRPGItem != null) {
                if (!hasEnoughRPGItem(settings.costRPGItem, settings.costAmount)) {
                    sendMessage("材料不足，需要 " + settings.costAmount + "x " + settings.costRPGItem, NamedTextColor.RED);
                    return false;
                }
            } else if (settings.costItem != null && !hasEnoughMaterial(settings.costItem, settings.costAmount)) {
                sendMessage("材料不足，需要 " + settings.costAmount + "x " + settings.costItem.name(), NamedTextColor.RED);
                return false;
            }
        }
        return true;
    }

    private void consumeReworkCost(ReworkSettings settings) {
        if (settings.expLevels > 0) player.setLevel(player.getLevel() - settings.expLevels);
        if (settings.costAmount > 0) {
            if (settings.costRPGItem != null) {
                removeRPGItem(settings.costRPGItem, settings.costAmount);
            } else if (settings.costItem != null) {
                removeMaterial(settings.costItem, settings.costAmount);
            }
        }
    }

    private boolean tryConsumeInsurance(ReworkSettings settings) {
        if (settings.insuranceRPGItem != null) {
            if (!hasEnoughRPGItem(settings.insuranceRPGItem, settings.insuranceAmount)) return false;
            if (settings.insuranceConsumeOnUse) removeRPGItem(settings.insuranceRPGItem, settings.insuranceAmount);
            return true;
        } else if (settings.insuranceItem != null) {
            if (!hasEnoughMaterial(settings.insuranceItem, settings.insuranceAmount)) return false;
            if (settings.insuranceConsumeOnUse) removeMaterial(settings.insuranceItem, settings.insuranceAmount);
            return true;
        }
        return false;
    }

    private boolean hasEnoughRPGItem(String itemId, int amount) {
        RPGItemsHook hook = RPGItemsHook.getInstance();
        plugin.getLogger().info("[SocketGUI] hasEnoughRPGItem: 检测 " + itemId + ", 需要 " + amount + " 个");
        
        if (!hook.isEnabled()) {
            plugin.getLogger().warning("[SocketGUI] hasEnoughRPGItem: RPGItemsHook 未启用");
            return false;
        }
        
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            
            // 方式1: 通过 PDC 检测
            boolean isRPG = hook.isRPGItem(item, itemId);
            plugin.getLogger().info("[SocketGUI] hasEnoughRPGItem: 检测物品 " + item.getType() + ", PDC检测=" + isRPG);
            
            // 方式2: 通过显示名称检测（回退机制）
            if (!isRPG && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
                isRPG = displayName.contains(itemId);
                plugin.getLogger().info("[SocketGUI] hasEnoughRPGItem: 名称检测=" + isRPG + ", 显示名=" + displayName);
            }
            
            if (isRPG) {
                total += item.getAmount();
                plugin.getLogger().info("[SocketGUI] hasEnoughRPGItem: 找到RPGItem " + itemId + ", 当前数量=" + total);
                if (total >= amount) {
                    plugin.getLogger().info("[SocketGUI] hasEnoughRPGItem: 数量足够");
                    return true;
                }
            }
        }
        
        plugin.getLogger().warning("[SocketGUI] hasEnoughRPGItem: 数量不足，总数=" + total + ", 需要=" + amount);
        return false;
    }

    private void removeRPGItem(String itemId, int amount) {
        RPGItemsHook hook = RPGItemsHook.getInstance();
        if (!hook.isEnabled()) return;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (hook.isRPGItem(item, itemId)) {
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
        player.getInventory().setContents(contents);
    }

    private boolean hasEnoughMaterial(Material material, int amount) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            total += item.getAmount();
            if (total >= amount) return true;
        }
        return false;
    }

    private void removeMaterial(Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) break;
        }
        player.getInventory().setContents(contents);
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

    private ItemStack createButton(Material material, String name, String... lore) {
        return createPlaceholder(material, name, lore);
    }

    private void sendMessage(String message, net.kyori.adventure.text.format.NamedTextColor color) {
        player.sendMessage(net.kyori.adventure.text.Component.text(message, color));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
