package cn.guangdian.devour.gui;

import cn.guangdian.devour.GuangDianDevour;
import cn.guangdian.devour.config.DevourWeaponConfig;
import cn.guangdian.devour.data.AttributeValue;
import cn.guangdian.devour.data.DevourData;
import cn.guangdian.devour.data.DevouredWeaponData;
import cn.guangdian.devour.event.DevourCompleteEvent;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 吞噬 GUI (实现 InventoryHolder)
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 27;
    private static final int WEAPON_SLOT = 4;
    private static final int[] DEVOUR_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int CONFIRM_SLOT = 20;
    private static final int CANCEL_SLOT = 22;
    private static final int CLEAR_SLOT = 24;
    private static final int INFO_SLOT = 26;
    
    private final GuangDianDevour plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessageService mm;
    
    private ItemStack devourWeapon;
    private DevourData devourData;
    private int maxSlots;
    private final List<SlotData> slots = new ArrayList<>();
    private final List<DevouredWeaponData> existingWeapons = new ArrayList<>();
    private boolean finalized = false;
    
    /**
     * 槽位数据
     */
    private static class SlotData {
        int index;
        boolean occupied;
        boolean existing;
        String itemName;
        ItemStack item;
        
        SlotData(int index) {
            this.index = index;
            this.occupied = false;
            this.existing = false;
            this.itemName = "";
            this.item = null;
        }
    }
    
    public DevourGUI(GuangDianDevour plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.mm = plugin.getMiniMessage();
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, 
            Component.text("⚔ 武器吞噬 ⚔", NamedTextColor.GOLD));
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
        if (rawSlot == WEAPON_SLOT) {
            withdrawWeapon();
            return;
        }

        int slotIndex = getDevourSlotIndex(rawSlot);
        if (slotIndex >= 0) {
            // 检查槽位是否已被占用（包括已吞噬的和刚放入的）
            if (slotIndex < slots.size()) {
                SlotData slot = slots.get(slotIndex);
                if (slot.occupied) {
                    // 已占用的槽位，只能取出（如果是新放入的）
                    withdrawSlot(slotIndex);
                    return;
                }
            }
            // 空闲槽位，显示提示
            sendMessage("请点击背包中的武器自动放入空闲槽位", NamedTextColor.YELLOW);
            return;
        }

        if (rawSlot == CONFIRM_SLOT) {
            confirmDevour();
            return;
        }

        if (rawSlot == CANCEL_SLOT) {
            cancelSession();
            return;
        }

        if (rawSlot == CLEAR_SLOT) {
            clearSlots();
        }
    }
    
    private void handlePlayerInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;
        
        if (devourWeapon == null) {
            placeWeapon(event.getClickedInventory(), event.getSlot(), clickedItem);
            return;
        }
        
        placeTarget(event.getClickedInventory(), event.getSlot(), clickedItem);
    }
    
    private void placeWeapon(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!plugin.getDevourManager().isDevourWeapon(sourceItem)) {
            sendMessage("请放入有效的吞噬剑", NamedTextColor.RED);
            playSound("error");
            return;
        }
        
        devourWeapon = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        devourData = plugin.getDevourManager().getDevourData(devourWeapon);
        
        if (devourData == null) {
            sendMessage("无法读取吞噬数据", NamedTextColor.RED);
            playSound("error");
            giveOrDrop(devourWeapon);
            devourWeapon = null;
            return;
        }
        
        String mythicType = plugin.getDevourManager().getMythicType(devourWeapon);
        DevourWeaponConfig config = plugin.getConfigManager().getDevourWeaponConfig(mythicType);
        maxSlots = config != null ? config.getMaxSlots() : 3;
        
        slots.clear();
        for (int i = 0; i < maxSlots; i++) {
            slots.add(new SlotData(i));
        }
        
        loadExistingData();
        refreshInventory();
        sendMessage("已放入吞噬剑，请继续放入要吞噬的武器", NamedTextColor.GREEN);
    }
    
    private void loadExistingData() {
        if (devourWeapon == null) return;

        // 使用新的存储方式加载已吞噬武器
        existingWeapons.clear();
        existingWeapons.addAll(plugin.getDevourManager().loadDevouredWeapons(devourWeapon));

        for (DevouredWeaponData weapon : existingWeapons) {
            int slotIndex = weapon.getSlotIndex();
            if (slotIndex >= 1 && slotIndex <= maxSlots) {
                SlotData slotData = slots.get(slotIndex - 1);
                slotData.occupied = true;
                slotData.existing = true;
                slotData.itemName = weapon.getDisplayName() != null ? weapon.getDisplayName() : weapon.getItemId();
            }
        }
    }
    
    private void placeTarget(Inventory sourceInventory, int sourceSlot, ItemStack sourceItem) {
        if (!plugin.getDevourManager().isValidWeapon(sourceItem)) {
            sendMessage("只能吞噬武器类物品", NamedTextColor.RED);
            playSound("error");
            return;
        }

        // 检查品类去重 - 使用新的存储方式
        String itemId = plugin.getDevourManager().getItemId(sourceItem);
        if (itemId != null && !itemId.isEmpty() && plugin.getDevourManager().isDevoured(devourWeapon, itemId)) {
            sendMessage("该武器已被吞噬，每把武器只能吞噬一次", NamedTextColor.RED);
            playSound("error");
            return;
        }

        // 检查是否已经在本次会话中放入
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                String slotItemId = plugin.getDevourManager().getItemId(slot.item);
                if (itemId != null && itemId.equals(slotItemId)) {
                    sendMessage("该武器已在吞噬槽位中", NamedTextColor.RED);
                    playSound("error");
                    return;
                }
            }
        }

        int targetSlot = findEmptySlot();
        if (targetSlot < 0) {
            sendMessage("没有可用的空闲吞噬槽位，请先确认或清空槽位", NamedTextColor.RED);
            playSound("error");
            return;
        }

        ItemStack taken = takeSingleItem(sourceInventory, sourceSlot, sourceItem);
        String itemName = plugin.getDevourManager().getItemName(taken);

        SlotData slotData = slots.get(targetSlot);
        slotData.occupied = true;
        slotData.existing = false;
        slotData.itemName = itemName;
        slotData.item = taken;

        refreshInventory();
        sendMessage("已自动放入槽位 " + (targetSlot + 1) + ": " + itemName, NamedTextColor.GREEN);
    }
    
    private int findEmptySlot() {
        for (int i = 0; i < slots.size(); i++) {
            SlotData slot = slots.get(i);
            // 空闲槽位：未被占用，或者是已占用但没有实际物品（已吞噬的槽位不能再放）
            if (!slot.occupied) {
                return i;
            }
        }
        return -1;
    }
    
    private void withdrawWeapon() {
        if (devourWeapon == null) return;
        
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                giveOrDrop(slot.item);
            }
        }
        
        giveOrDrop(devourWeapon);
        devourWeapon = null;
        devourData = null;
        slots.clear();
        refreshInventory();
    }
    
    private void withdrawSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        
        SlotData slot = slots.get(slotIndex);
        if (!slot.occupied || slot.existing || slot.item == null) return;
        
        giveOrDrop(slot.item);
        slot.occupied = false;
        slot.item = null;
        slot.itemName = "";
        refreshInventory();
    }
    
    private void confirmDevour() {
        if (devourWeapon == null) {
            sendMessage("请先放入吞噬剑", NamedTextColor.RED);
            playSound("error");
            return;
        }

        List<SlotData> newSlots = new ArrayList<>();
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                newSlots.add(slot);
            }
        }

        if (newSlots.isEmpty()) {
            sendMessage("请至少放入一把武器进行吞噬", NamedTextColor.RED);
            playSound("error");
            return;
        }

        ItemStack finalWeapon = devourWeapon.clone();

        // 先加载现有的已吞噬武器
        List<DevouredWeaponData> allWeapons = new ArrayList<>(existingWeapons);

        for (SlotData slot : newSlots) {
            int slotIndex = slot.index + 1;

            // 检查槽位是否已被占用
            boolean slotOccupied = false;
            for (DevouredWeaponData w : allWeapons) {
                if (w.getSlotIndex() == slotIndex) {
                    slotOccupied = true;
                    break;
                }
            }

            if (slotOccupied) {
                sendMessage("吞噬失败: 槽位 " + slotIndex + " 已被占用", NamedTextColor.RED);
                playSound("error");
                returnSessionItems();
                finalized = true;
                player.closeInventory();
                return;
            }

            // 执行吞噬
            boolean success = plugin.getDevourManager().devour(finalWeapon, slot.item, slotIndex);

            if (!success) {
                sendMessage("吞噬失败: 槽位 " + slotIndex, NamedTextColor.RED);
                playSound("error");
                returnSessionItems();
                finalized = true;
                player.closeInventory();
                return;
            }
        }

        giveOrDrop(finalWeapon);
        clearSession();
        finalized = true;

        sendMessage("吞噬成功！", NamedTextColor.GREEN);
        playSound("success");
        player.closeInventory();

        DevourCompleteEvent event = new DevourCompleteEvent(player, finalWeapon, devourWeapon, newSlots.size());
        Bukkit.getPluginManager().callEvent(event);
    }
    
    private void cancelSession() {
        returnSessionItems();
        finalized = true;
        sendMessage("已取消吞噬并返还所有物品", NamedTextColor.YELLOW);
        player.closeInventory();
    }
    
    private void clearSlots() {
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                giveOrDrop(slot.item);
                slot.occupied = false;
                slot.item = null;
                slot.itemName = "";
            }
        }
        refreshInventory();
        sendMessage("已清空所有新放入的武器", NamedTextColor.YELLOW);
    }
    
    private void returnSessionItems() {
        if (devourWeapon != null) {
            giveOrDrop(devourWeapon);
        }
        
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                giveOrDrop(slot.item);
            }
        }
        
        clearSession();
    }
    
    private void clearSession() {
        devourWeapon = null;
        devourData = null;
        slots.clear();
        existingWeapons.clear();
    }
    
    private void refreshInventory() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, createPlaceholder(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        
        inventory.setItem(WEAPON_SLOT, devourWeapon == null
            ? createPlaceholder(Material.DIAMOND_SWORD, "<yellow>吞噬剑槽", "<gray>点击背包中的吞噬剑放入")
            : createWeaponPreview());
        
        int currentMaxSlots = Math.max(1, maxSlots);
        for (int i = 0; i < DEVOUR_SLOTS.length; i++) {
            if (i >= currentMaxSlots) {
                inventory.setItem(DEVOUR_SLOTS[i],
                    createPlaceholder(Material.BARRIER, "<dark_gray>未开放", "<gray>该槽位不存在"));
            } else if (i < slots.size()) {
                SlotData slot = slots.get(i);
                if (slot.occupied) {
                    if (slot.existing) {
                        // 已吞噬的槽位（从存储加载的）
                        inventory.setItem(DEVOUR_SLOTS[i],
                            createPlaceholder(Material.BEACON, "<green>已吞噬: " + slot.itemName,
                                "<gray>该槽位已被占用"));
                    } else if (slot.item != null) {
                        // 新放入的武器（待吞噬）
                        inventory.setItem(DEVOUR_SLOTS[i],
                            createPlaceholder(Material.IRON_SWORD, "<yellow>待吞噬: " + slot.itemName,
                                "<gray>槽位 " + (i + 1) + " - 点击取出"));
                    } else {
                        // 空槽位
                        inventory.setItem(DEVOUR_SLOTS[i],
                            createPlaceholder(Material.BEACON, "<aqua>吞噬槽位 " + (i + 1),
                                "<gray>点击背包中的武器放入"));
                    }
                } else {
                    // 空闲槽位
                    inventory.setItem(DEVOUR_SLOTS[i],
                        createPlaceholder(Material.BEACON, "<aqua>吞噬槽位 " + (i + 1),
                            "<gray>点击背包中的武器放入"));
                }
            } else {
                // 超出当前槽位列表范围（不应该发生）
                inventory.setItem(DEVOUR_SLOTS[i],
                    createPlaceholder(Material.BEACON, "<aqua>吞噬槽位 " + (i + 1),
                        "<gray>点击背包中的武器放入"));
            }
        }
        
        inventory.setItem(CONFIRM_SLOT, createButton(Material.EMERALD_BLOCK, "<green>确认吞噬", "<gray>执行吞噬操作"));
        inventory.setItem(CANCEL_SLOT, createButton(Material.BARRIER, "<red>取消并返还", "<gray>关闭并返还所有物品"));
        inventory.setItem(CLEAR_SLOT, createButton(Material.REDSTONE_BLOCK, "<gold>清空槽位", "<gray>清空所有新放入的武器"));
        inventory.setItem(INFO_SLOT, createInfoItem());
    }
    
    private ItemStack createWeaponPreview() {
        ItemStack preview = devourWeapon.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) return preview;
        
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        
        Map<String, AttributeValue> totalAttrs = calculateTotalAttributes();
        List<Component> newLore = updateLoreWithAttributes(lore, totalAttrs);
        
        meta.lore(newLore);
        preview.setItemMeta(meta);
        return preview;
    }
    
    private Map<String, AttributeValue> calculateTotalAttributes() {
        Map<String, AttributeValue> total = new HashMap<>();

        if (devourWeapon != null) {
            Map<String, AttributeValue> original = plugin.getDevourManager().getParser().parseWeaponAttributes(devourWeapon);
            total.putAll(original);
        }

        // 累加已存在的被吞噬武器属性
        for (DevouredWeaponData weapon : existingWeapons) {
            if (weapon.getSerializedAttributes() != null && !weapon.getSerializedAttributes().isEmpty()) {
                Map<String, AttributeValue> attrs = deserializeAttributes(weapon.getSerializedAttributes());
                for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                    total.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
                }
            }
        }

        // 累加新放入的武器属性
        for (SlotData slot : slots) {
            if (slot.occupied && !slot.existing && slot.item != null) {
                Map<String, AttributeValue> attrs = plugin.getDevourManager().getParser().parseWeaponAttributes(slot.item);
                for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                    total.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
                }
            }
        }

        return total;
    }

    /**
     * 反序列化属性
     */
    private Map<String, AttributeValue> deserializeAttributes(String data) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        if (data == null || data.isEmpty()) {
            return attrs;
        }

        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                try {
                    String name = parts[0];
                    double value = Double.parseDouble(parts[1]);
                    attrs.put(name, AttributeValue.of(value));
                } catch (NumberFormatException ignored) {}
            }
        }

        return attrs;
    }
    
    private List<Component> updateLoreWithAttributes(List<Component> lore, Map<String, AttributeValue> totalAttrs) {
        List<Component> newLore = new ArrayList<>();
        
        for (Component line : lore) {
            String strippedLine = PlainTextComponentSerializer.plainText().serialize(line);
            
            boolean updated = false;
            for (String attrName : totalAttrs.keySet()) {
                if (strippedLine.contains(attrName + "：") || strippedLine.contains(attrName + ":")) {
                    AttributeValue value = totalAttrs.get(attrName);
                    int colonPos = Math.max(strippedLine.indexOf("："), strippedLine.indexOf(":"));
                    if (colonPos > 0) {
                        String prefix = strippedLine.substring(0, colonPos + 1);
                        newLore.add(mm.colorize("<dark_gray>" + prefix + " <aqua>" + value.format()));
                        updated = true;
                    }
                    break;
                }
            }
            
            if (!updated) {
                newLore.add(line);
            }
        }
        
        return newLore;
    }
    
    private int getDevourSlotIndex(int rawSlot) {
        for (int i = 0; i < DEVOUR_SLOTS.length; i++) {
            if (DEVOUR_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }
    
    private ItemStack createInfoItem() {
        return createPlaceholder(Material.PAPER, "<white>使用说明",
            "<gray>1. 点击背包中的吞噬剑放入",
            "<gray>2. 点击背包中的武器依次放入",
            "<gray>3. 每把武器只能吞噬一次",
            "<gray>4. 点击上方武器可取回",
            "<gray>5. 确认后属性将累加到吞噬剑");
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
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
    
    private ItemStack createPlaceholder(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.colorize(name));
        
        if (lore != null && lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(mm.colorize(line));
            }
            meta.lore(loreComponents);
        }
        
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createButton(Material material, String name, String... lore) {
        return createPlaceholder(material, name, lore);
    }
    
    private void sendMessage(String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }
    
    private void playSound(String type) {
        try {
            String soundName;
            switch (type) {
                case "success":
                    soundName = "ENTITY_PLAYER_LEVELUP";
                    break;
                case "error":
                    soundName = "ENTITY_VILLAGER_NO";
                    break;
                default:
                    return;
            }
            
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // 忽略
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
