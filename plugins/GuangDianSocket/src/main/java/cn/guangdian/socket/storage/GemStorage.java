package cn.guangdian.socket.storage;

import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.model.GemData;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 宝石数据存储管理器
 * 
 * 职责：独立管理所有宝石镶嵌数据
 * - 使用 PDC 存储宝石数据到装备
 * - 支持序列化/反序列化
 * - 提供数据查询接口
 */
public class GemStorage {

    private static final String GEM_DATA_KEY = "guangdian_socket_gems";
    private static final String SOCKET_COUNT_KEY = "guangdian_socket_count";
    private static final String SOCKET_TYPES_KEY = "guangdian_socket_types";

    private final GuangDianSocket plugin;
    private final NamespacedKey gemDataKey;
    private final NamespacedKey socketCountKey;
    private final NamespacedKey socketTypesKey;

    public GemStorage(GuangDianSocket plugin) {
        this.plugin = plugin;
        this.gemDataKey = new NamespacedKey(plugin, GEM_DATA_KEY);
        this.socketCountKey = new NamespacedKey(plugin, SOCKET_COUNT_KEY);
        this.socketTypesKey = new NamespacedKey(plugin, SOCKET_TYPES_KEY);
    }

    /**
     * 保存宝石镶嵌数据到装备
     * 
     * @param item 装备物品
     * @param gems 镶嵌的宝石列表
     * @return 是否保存成功
     */
    public boolean saveGems(ItemStack item, List<GemData> gems) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 保存宝石数量
        pdc.set(socketCountKey, PersistentDataType.INTEGER, gems.size());

        // 序列化宝石数据
        StringBuilder gemDataBuilder = new StringBuilder();
        StringBuilder typesBuilder = new StringBuilder();

        for (int i = 0; i < gems.size(); i++) {
            GemData gem = gems.get(i);

            if (gemDataBuilder.length() > 0) {
                gemDataBuilder.append("|");
            }
            // 格式: slot:index,type:gemType,id:itemId,amount:count
            gemDataBuilder.append("slot:").append(i)
                         .append(",type:").append(gem.getType())
                         .append(",id:").append(gem.getItemId())
                         .append(",amount:").append(gem.getAmount());

            if (typesBuilder.length() > 0) {
                typesBuilder.append(",");
            }
            typesBuilder.append(gem.getSocketType());
        }

        pdc.set(gemDataKey, PersistentDataType.STRING, gemDataBuilder.toString());
        pdc.set(socketTypesKey, PersistentDataType.STRING, typesBuilder.toString());

        item.setItemMeta(meta);
        return true;
    }

    /**
     * 从装备加载宝石镶嵌数据
     * 
     * @param item 装备物品
     * @return 宝石数据列表
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

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 检查是否有宝石数据
        if (!pdc.has(gemDataKey, PersistentDataType.STRING)) {
            return gems;
        }

        String gemDataStr = pdc.get(gemDataKey, PersistentDataType.STRING);
        if (gemDataStr == null || gemDataStr.isEmpty()) {
            return gems;
        }

        // 解析宝石数据
        String[] gemEntries = gemDataStr.split("\\|");
        for (String entry : gemEntries) {
            GemData gem = parseGemData(entry);
            if (gem != null) {
                gems.add(gem);
            }
        }

        return gems;
    }

    /**
     * 获取装备的宝石孔位数量
     * 
     * @param item 装备物品
     * @return 宝石孔位数量，如果没有则返回0
     */
    public int getSocketCount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer count = pdc.get(socketCountKey, PersistentDataType.INTEGER);
        return count != null ? count : 0;
    }

    /**
     * 获取装备的宝石孔位类型
     * 
     * @param item 装备物品
     * @return 孔位类型列表
     */
    public List<String> getSocketTypes(ItemStack item) {
        List<String> types = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return types;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return types;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typesStr = pdc.get(socketTypesKey, PersistentDataType.STRING);

        if (typesStr != null && !typesStr.isEmpty()) {
            types.addAll(Arrays.asList(typesStr.split(",")));
        }

        return types;
    }

    /**
     * 清除装备上的所有宝石数据
     * 
     * @param item 装备物品
     * @return 清除后的装备
     */
    public ItemStack clearGems(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(gemDataKey);
        pdc.remove(socketCountKey);
        pdc.remove(socketTypesKey);

        result.setItemMeta(meta);
        return result;
    }

    /**
     * 检查装备是否有宝石镶嵌
     * 
     * @param item 装备物品
     * @return 是否有镶嵌
     */
    public boolean hasGems(ItemStack item) {
        return getSocketCount(item) > 0;
    }

    /**
     * 解析单个宝石数据
     */
    private GemData parseGemData(String data) {
        try {
            GemData gem = new GemData();
            String[] pairs = data.split(",");

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length != 2) continue;

                String key = keyValue[0];
                String value = keyValue[1];

                switch (key) {
                    case "slot" -> gem.setSlotIndex(Integer.parseInt(value));
                    case "type" -> gem.setType(value);
                    case "id" -> gem.setItemId(value);
                    case "amount" -> gem.setAmount(Integer.parseInt(value));
                    case "socket" -> gem.setSocketType(value);
                }
            }

            return gem.isValid() ? gem : null;
        } catch (Exception e) {
            plugin.getLogger().warning("解析宝石数据失败: " + data);
            return null;
        }
    }
}
