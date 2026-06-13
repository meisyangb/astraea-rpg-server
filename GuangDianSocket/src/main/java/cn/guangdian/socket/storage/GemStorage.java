package cn.guangdian.socket.storage;

import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.constant.SocketSlot;
import cn.guangdian.socket.model.GemData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝石存储管理器 - 使用 PDC 存储方案 + 槽位枚举
 *
 * 存储格式：在装备的 PDC 中存储宝石ID
 * Key: rpgitems:gem_0, rpgitems:gem_1, ... (由 SocketSlot 枚举定义)
 * Value: RPGItems物品ID
 *
 * 优势：
 * - 使用枚举直接访问，无遍历
 * - 读取速度快，直接从PDC获取
 * - 不依赖Lore格式，更可靠
 */
public class GemStorage {

    private final GuangDianSocket plugin;

    public GemStorage(GuangDianSocket plugin) {
        this.plugin = plugin;
    }

    /**
     * 保存宝石数据到装备的 PDC 中（单个槽位操作）
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引 (0-4)
     * @param gemId 宝石ID
     * @return 是否保存成功
     */
    public boolean saveGem(ItemStack item, int slotIndex, String gemId) {
        if (item == null) {
            return false;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(slot.getGemKey(), PersistentDataType.STRING, gemId != null ? gemId : "");
        item.setItemMeta(meta);

        return true;
    }

    /**
     * 保存 Lore 行索引到 PDC（用于快速定位）
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引 (0-4)
     * @param loreIndex Lore 行索引
     * @return 是否保存成功
     */
    public boolean saveLoreIndex(ItemStack item, int slotIndex, int loreIndex) {
        if (item == null) {
            return false;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(slot.getLoreIndexKey(), PersistentDataType.INTEGER, loreIndex);
        item.setItemMeta(meta);

        return true;
    }

    /**
     * 获取 Lore 行索引
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引 (0-4)
     * @return Lore 行索引，未找到返回 -1
     */
    public int getLoreIndex(ItemStack item, int slotIndex) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return -1;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(slot.getLoreIndexKey(), PersistentDataType.INTEGER)) {
            return -1;
        }

        return pdc.get(slot.getLoreIndexKey(), PersistentDataType.INTEGER);
    }

    /**
     * 从指定槽位读取宝石ID
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引 (0-4)
     * @return 宝石ID，如果为空返回null
     */
    public String getGem(ItemStack item, int slotIndex) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(slot.getGemKey(), PersistentDataType.STRING)) {
            return null;
        }

        String gemId = pdc.get(slot.getGemKey(), PersistentDataType.STRING);
        if (gemId == null || gemId.isEmpty()) {
            return null;
        }

        return gemId;
    }

    /**
     * 获取指定槽位的槽位类型
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引 (0-4)
     * @return 槽位类型（红宝石、蓝宝石等）
     */
    public String getSocketType(ItemStack item, int slotIndex) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(slot.getSocketKey(), PersistentDataType.STRING)) {
            return null;
        }

        return pdc.get(slot.getSocketKey(), PersistentDataType.STRING);
    }

    /**
     * 从装备的 PDC 中加载所有已镶嵌的宝石数据
     *
     * @param item 目标装备
     * @return 宝石数据列表（只包含非空槽位）
     */
    public List<GemData> loadGems(ItemStack item) {
        List<GemData> gems = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return gems;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return gems;
        }

        // 使用枚举直接遍历固定槽位
        for (SocketSlot slot : SocketSlot.VALUES) {
            String gemId = getGem(item, slot.getIndex());
            if (gemId != null && !gemId.isEmpty()) {
                GemData gem = new GemData();
                gem.setSlotIndex(slot.getIndex());
                gem.setItemId(gemId);
                gem.setDisplayName(gemId);
                gem.setRPGItem(true);
                gem.setAmount(1);

                // 获取槽位类型
                String socketType = getSocketType(item, slot.getIndex());
                gem.setType(socketType);
                gem.setSocketType(socketType);

                gems.add(gem);
            }
        }

        return gems;
    }

    /**
     * 获取所有槽位的类型列表（固定顺序）
     *
     * @param item 目标装备
     * @return 槽位类型列表
     */
    public List<String> getSocketTypes(ItemStack item) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < SocketSlot.size(); i++) {
            String type = getSocketType(item, i);
            types.add(type != null ? type : "");
        }
        return types;
    }

    /**
     * 清除指定槽位的宝石
     *
     * @param item 目标装备
     * @param slotIndex 槽位索引
     * @return 清除后的装备
     */
    public ItemStack clearGem(ItemStack item, int slotIndex) {
        if (item == null) {
            return item;
        }

        SocketSlot slot = SocketSlot.fromIndex(slotIndex);
        if (slot == null) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(slot.getGemKey(), PersistentDataType.STRING, "");
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 清除所有宝石数据
     *
     * @param item 目标装备
     * @return 清除后的装备
     */
    public ItemStack clearAllGems(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // 使用枚举清除所有槽位
        for (SocketSlot slot : SocketSlot.VALUES) {
            pdc.set(slot.getGemKey(), PersistentDataType.STRING, "");
        }

        result.setItemMeta(meta);
        return result;
    }

    /**
     * 获取已镶嵌的宝石数量
     */
    public int getGemCount(ItemStack item) {
        return loadGems(item).size();
    }

    /**
     * 检查物品是否有宝石
     */
    public boolean hasGems(ItemStack item) {
        return getGemCount(item) > 0;
    }

    /**
     * 获取槽位数量
     */
    public int getSlotCount() {
        return SocketSlot.size();
    }
}