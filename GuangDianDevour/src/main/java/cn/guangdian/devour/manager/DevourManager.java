package cn.guangdian.devour.manager;

import cn.guangdian.devour.GuangDianDevour;
import cn.guangdian.devour.config.DevourWeaponConfig;
import cn.guangdian.devour.data.AttributeValue;
import cn.guangdian.devour.data.DevourData;
import cn.guangdian.devour.data.DevouredWeaponData;
import cn.guangdian.devour.parser.DevourParser;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 吞噬管理器
 * 参考镶嵌插件 GemStorage 设计，每把被吞噬的武器作为一个整体存储
 *
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevourManager {

    private final GuangDianDevour plugin;
    private final DevourParser parser;
    private final MiniMessageService mm;

    // PDC Keys - 参考 GemStorage 设计
    private final NamespacedKey DEVOUR_DATA_KEY;
    private final NamespacedKey DEVOUR_COUNT_KEY;
    private final NamespacedKey DEVOUR_UUID_KEY;

    public DevourManager(GuangDianDevour plugin) {
        this.plugin = plugin;
        this.parser = new DevourParser(plugin);
        this.mm = plugin.getMiniMessage();

        this.DEVOUR_DATA_KEY = new NamespacedKey(plugin, "devour_weapons");
        this.DEVOUR_COUNT_KEY = new NamespacedKey(plugin, "devour_count");
        this.DEVOUR_UUID_KEY = new NamespacedKey(plugin, "devour_uuid");
    }

    /**
     * 检查物品是否为吞噬剑
     */
    public boolean isDevourWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        String mythicType = getMythicType(item);
        if (mythicType == null) {
            return false;
        }

        return plugin.getConfigManager().isDevourWeapon(mythicType);
    }

    /**
     * 获取MythicMobs物品类型
     */
    public String getMythicType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return pdc.get(typeKey, PersistentDataType.STRING);
    }

    /**
     * 获取吞噬数据 (用于兼容旧代码)
     */
    public DevourData getDevourData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String mythicType = getMythicType(item);
        if (mythicType == null) {
            return null;
        }

        DevourWeaponConfig config = plugin.getConfigManager().getDevourWeaponConfig(mythicType);
        if (config == null) {
            return null;
        }

        UUID itemUUID = UUID.randomUUID();
        String uuidStr = pdc.get(DEVOUR_UUID_KEY, PersistentDataType.STRING);
        if (uuidStr != null) {
            try {
                itemUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }

        DevourData data = new DevourData(itemUUID, mythicType, config.getMaxSlots());

        // 加载已吞噬的武器
        List<DevouredWeaponData> weapons = loadDevouredWeapons(item);
        for (DevouredWeaponData weapon : weapons) {
            data.addDevoured(weapon.getSlotIndex(), weapon.getItemId(), weapon.getDisplayName());
        }

        return data;
    }

    /**
     * 加载已吞噬的武器列表
     */
    public List<DevouredWeaponData> loadDevouredWeapons(ItemStack item) {
        List<DevouredWeaponData> weapons = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return weapons;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return weapons;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(DEVOUR_DATA_KEY, PersistentDataType.STRING)) {
            return weapons;
        }

        String dataStr = pdc.get(DEVOUR_DATA_KEY, PersistentDataType.STRING);
        if (dataStr == null || dataStr.isEmpty()) {
            return weapons;
        }

        // 格式: weapon1|weapon2|weapon3
        String[] weaponEntries = dataStr.split("\\|");
        for (String entry : weaponEntries) {
            DevouredWeaponData weapon = DevouredWeaponData.deserialize(entry);
            if (weapon != null) {
                weapons.add(weapon);
            }
        }

        return weapons;
    }

    /**
     * 保存已吞噬的武器列表
     */
    public boolean saveDevouredWeapons(ItemStack item, List<DevouredWeaponData> weapons) {
        if (item == null || weapons == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 序列化所有武器数据
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weapons.size(); i++) {
            DevouredWeaponData weapon = weapons.get(i);
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(weapon.serialize());
        }

        pdc.set(DEVOUR_DATA_KEY, PersistentDataType.STRING, sb.toString());
        pdc.set(DEVOUR_COUNT_KEY, PersistentDataType.INTEGER, weapons.size());

        // 保存 UUID
        if (!pdc.has(DEVOUR_UUID_KEY, PersistentDataType.STRING)) {
            pdc.set(DEVOUR_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        item.setItemMeta(meta);
        return true;
    }

    /**
     * 获取已吞噬的武器数量
     */
    public int getDevouredCount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer count = pdc.get(DEVOUR_COUNT_KEY, PersistentDataType.INTEGER);
        return count != null ? count : 0;
    }

    /**
     * 保存吞噬数据到物品 (兼容旧方法)
     */
    public void saveDevourData(ItemStack item, DevourData data) {
        // 新的存储方式在 devour 方法中直接处理
        // 这里只更新 UUID
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(DEVOUR_UUID_KEY, PersistentDataType.STRING, data.getItemUUID().toString());

        item.setItemMeta(meta);
    }

    /**
     * 获取物品ID（Display名称去除颜色）
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return null;
        }

        Component displayName = meta.displayName();
        String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
        return plainName.trim();
    }

    /**
     * 获取物品显示名称
     */
    public String getItemName(ItemStack item) {
        if (item == null) {
            return "未知";
        }

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                Component displayName = meta.displayName();
                return PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }

        String mythicType = getMythicType(item);
        if (mythicType != null) {
            return mythicType;
        }

        return item.getType().name();
    }

    /**
     * 检查物品是否已被吞噬
     */
    public boolean isDevoured(ItemStack devourWeapon, ItemStack targetWeapon) {
        String targetId = getItemId(targetWeapon);
        if (targetId == null || targetId.isEmpty()) {
            return false;
        }

        List<DevouredWeaponData> weapons = loadDevouredWeapons(devourWeapon);
        for (DevouredWeaponData weapon : weapons) {
            if (targetId.equals(weapon.getItemId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查物品ID是否已被吞噬
     */
    public boolean isDevoured(ItemStack devourWeapon, String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }

        List<DevouredWeaponData> weapons = loadDevouredWeapons(devourWeapon);
        for (DevouredWeaponData weapon : weapons) {
            if (itemId.equals(weapon.getItemId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 执行吞噬 - 每把武器占用一个槽位
     */
    public boolean devour(ItemStack devourWeapon, ItemStack targetWeapon, int slotIndex) {
        // 验证吞噬剑
        if (!isDevourWeapon(devourWeapon)) {
            return false;
        }

        // 验证目标武器
        if (!isValidWeapon(targetWeapon)) {
            return false;
        }

        // 获取物品信息
        String targetId = getItemId(targetWeapon);
        if (targetId == null || targetId.isEmpty()) {
            return false;
        }

        // 检查是否已吞噬
        if (isDevoured(devourWeapon, targetId)) {
            return false;
        }

        // 加载当前已吞噬的武器
        List<DevouredWeaponData> weapons = loadDevouredWeapons(devourWeapon);

        // 检查槽位是否已被占用
        for (DevouredWeaponData w : weapons) {
            if (w.getSlotIndex() == slotIndex) {
                return false;
            }
        }

        // 获取物品名称和Mythic类型
        String targetName = getItemName(targetWeapon);
        String mythicType = getMythicType(targetWeapon);

        // 创建被吞噬武器数据
        DevouredWeaponData weaponData = DevouredWeaponData.fromItemStack(
            targetWeapon, slotIndex, targetId, mythicType
        );

        if (weaponData == null) {
            return false;
        }

        // 解析并序列化属性
        Map<String, AttributeValue> targetAttrs = parser.parseWeaponAttributes(targetWeapon);
        if (!targetAttrs.isEmpty()) {
            weaponData.setSerializedAttributes(serializeAttributes(targetAttrs));
        }

        // 添加到列表
        weapons.add(weaponData);

        // 保存数据
        ItemStack finalWeapon = devourWeapon.clone();
        if (!saveDevouredWeapons(finalWeapon, weapons)) {
            return false;
        }

        // 更新 Lore
        updateWeaponLore(finalWeapon, weapons);

        // 将结果复制回原物品
        devourWeapon.setItemMeta(finalWeapon.getItemMeta());

        return true;
    }

    /**
     * 序列化属性
     */
    private String serializeAttributes(Map<String, AttributeValue> attrs) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue().getValue());
        }
        return sb.toString();
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

    /**
     * 更新武器Lore - 显示所有已吞噬武器的属性和槽位信息
     */
    private void updateWeaponLore(ItemStack item, List<DevouredWeaponData> weapons) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        // 计算所有被吞噬武器的属性总和
        Map<String, AttributeValue> totalAttrs = new HashMap<>();

        // 先解析原始武器的属性
        Map<String, AttributeValue> originalAttrs = parser.parseWeaponAttributes(item);
        totalAttrs.putAll(originalAttrs);

        // 累加所有被吞噬武器的属性
        for (DevouredWeaponData weapon : weapons) {
            if (weapon.getSerializedAttributes() != null) {
                Map<String, AttributeValue> weaponAttrs = deserializeAttributes(weapon.getSerializedAttributes());
                for (Map.Entry<String, AttributeValue> entry : weaponAttrs.entrySet()) {
                    totalAttrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
                }
            }
        }

        // 更新 Lore
        List<Component> newLore = new ArrayList<>();
        Set<String> updatedAttrs = new HashSet<>();

        // 移除旧的槽位信息（如果有）
        boolean inSlotSection = false;
        for (Component line : lore) {
            String strippedLine = PlainTextComponentSerializer.plainText().serialize(line);

            // 跳过槽位信息区域
            if (strippedLine.contains("吞噬槽位") && strippedLine.contains("－－－－")) {
                inSlotSection = true;
                continue;
            }
            if (inSlotSection && strippedLine.contains("－－－－")) {
                inSlotSection = false;
                continue;
            }
            if (inSlotSection) {
                continue;
            }

            // 更新属性值 - 使用与原始格式一致的纯文本格式
            boolean updated = false;
            for (String attrName : totalAttrs.keySet()) {
                if (strippedLine.contains(attrName + "：") || strippedLine.contains(attrName + ":")) {
                    AttributeValue value = totalAttrs.get(attrName);
                    int colonPos = Math.max(strippedLine.indexOf("："), strippedLine.indexOf(":"));
                    if (colonPos > 0) {
                        // 保持原始格式，不使用颜色代码，确保后续能正确解析
                        String prefix = strippedLine.substring(0, colonPos + 1);
                        newLore.add(mm.colorize("<dark_gray>" + prefix + value.format()));
                        updatedAttrs.add(attrName);
                        updated = true;
                    }
                    break;
                }
            }

            if (!updated) {
                newLore.add(line);
            }
        }

        // 添加新属性（吞噬剑原本没有的）
        for (Map.Entry<String, AttributeValue> entry : totalAttrs.entrySet()) {
            if (!updatedAttrs.contains(entry.getKey())) {
                int insertPos = findInsertPosition(newLore);
                if (insertPos >= 0) {
                    newLore.add(insertPos, mm.colorize("<dark_gray>" + entry.getKey() + "：" + entry.getValue().format()));
                } else {
                    newLore.add(mm.colorize("<dark_gray>" + entry.getKey() + "：" + entry.getValue().format()));
                }
            }
        }

        // 更新吞噬槽位显示 - 直接在原有槽位行上修改
        if (!weapons.isEmpty()) {
            // 按槽位索引排序
            weapons.sort(Comparator.comparingInt(DevouredWeaponData::getSlotIndex));

            for (DevouredWeaponData weapon : weapons) {
                String name = weapon.getDisplayName() != null ? weapon.getDisplayName() : weapon.getItemId();
                int slotIndex = weapon.getSlotIndex();

                // 查找并更新对应的槽位行
                boolean updated = false;
                for (int i = 0; i < newLore.size(); i++) {
                    Component line = newLore.get(i);
                    String stripped = PlainTextComponentSerializer.plainText().serialize(line);

                    // 匹配 "吞噬槽位X" 或 "[槽位 X]" 格式
                    if (stripped.contains("吞噬槽位" + slotIndex) ||
                        stripped.contains("[槽位 " + slotIndex + "]") ||
                        stripped.contains("槽位 " + slotIndex)) {
                        newLore.set(i, mm.colorize("<dark_gray>[<gray>槽位 " + slotIndex + "<dark_gray>] <green>已吞噬: <aqua>" + name));
                        updated = true;
                        break;
                    }
                }

                // 如果没有找到对应的槽位行，在Lore末尾添加
                if (!updated) {
                    newLore.add(mm.colorize("<dark_gray>[<gray>槽位 " + slotIndex + "<dark_gray>] <green>已吞噬: <aqua>" + name));
                }
            }
        }

        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * 找到插入位置（分隔线前）
     */
    private int findInsertPosition(List<Component> lore) {
        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            String stripped = PlainTextComponentSerializer.plainText().serialize(line);
            if (stripped.contains("吞噬槽位") || stripped.contains("－－－－")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 找到槽位信息插入位置（Lore末尾或特定位置）
     */
    private int findSlotInsertPosition(List<Component> lore) {
        // 默认在Lore末尾添加
        return lore.size();
    }

    /**
     * 更新槽位显示
     */
    public void updateSlotDisplay(ItemStack item, int slotIndex, String itemName) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String slotPrefix = "吞噬槽位" + slotIndex;

        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            String stripped = PlainTextComponentSerializer.plainText().serialize(line);

            if (stripped.contains(slotPrefix)) {
                lore.set(i, mm.colorize("<dark_red>[<gray> " + slotPrefix + " <dark_aqua>] <green>已吞噬: " + itemName));
                break;
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 验证是否为有效武器
     */
    public boolean isValidWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Material type = item.getType();
        return type.name().endsWith("_SWORD") || type.name().endsWith("_AXE");
    }

    /**
     * 获取解析器
     */
    public DevourParser getParser() {
        return parser;
    }
}
