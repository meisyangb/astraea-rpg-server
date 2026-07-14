package cn.guangdian.socket.parser;

import cn.guangdian.rpgitems.attribute.CompoundAttributeCodec;
import cn.guangdian.rpgitems.attribute.RPGItemsKeys;
import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.model.AttributeValue;
import cn.guangdian.socket.model.GemData;
import cn.guangdian.socket.storage.GemStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 宝石解析器 - 重构版
 *
 * 核心原则：
 * 1. 槽位使用枚举 - 固定槽位，直接访问，O(1)复杂度
 * 2. Lore只用于展示 - 不参与任何属性计算
 * 3. 所有属性通过PDC - RPGItems写入，其他插件读取
 */
public class SocketParser {

    private static final Map<String, String> GEM_TYPE_TO_COLOR = new LinkedHashMap<>();

    static {
        GEM_TYPE_TO_COLOR.put("红宝石", "<dark_red>");
        GEM_TYPE_TO_COLOR.put("蓝宝石", "<dark_blue>");
        GEM_TYPE_TO_COLOR.put("绿宝石", "<dark_green>");
        GEM_TYPE_TO_COLOR.put("黄宝石", "<gold>");
        GEM_TYPE_TO_COLOR.put("紫宝石", "<dark_purple>");
    }

    /**
     * 初始化（保留配置加载，但不使用 Lore 解析）
     */
    public static void initialize(ConfigurationSection patternSection, ConfigurationSection gemSection) {
        // 不再需要初始化 Lore 解析模式
        // 所有数据都通过 PDC 读取
    }

    /**
     * 获取宝石颜色（用于 Lore 展示）
     */
    public static String getGemColor(String gemType) {
        if (gemType == null) return "<white>";
        for (Map.Entry<String, String> entry : GEM_TYPE_TO_COLOR.entrySet()) {
            if (gemType.contains(entry.getKey()) || entry.getKey().contains(gemType)) {
                return entry.getValue();
            }
        }
        return "<white>";
    }

    /**
     * 从装备 PDC 读取所有槽位的宝石类型（用于 GUI 展示）
     *
     * @param item 装备物品
     * @return 槽位类型列表（固定顺序，使用枚举）
     */
    public static List<String> parseSocketGems(ItemStack item) {
        List<String> sockets = new ArrayList<>();

        if (item == null || !item.hasItemMeta()) {
            return sockets;
        }

        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) {
            return sockets;
        }

        // 使用枚举直接遍历固定槽位
        for (cn.guangdian.socket.constant.SocketSlot slot : cn.guangdian.socket.constant.SocketSlot.VALUES) {
            String socketType = gemStorage.getSocketType(item, slot.getIndex());
            if (socketType != null && !socketType.isEmpty()) {
                sockets.add(socketType);
            }
        }

        return sockets;
    }

    /**
     * 加载宝石数据（从 PDC）
     */
    public static List<GemData> loadGemData(ItemStack item) {
        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) return new ArrayList<>();
        return gemStorage.loadGems(item);
    }

    /**
     * 判断是否是宝石（从 PDC 读取）
     *
     * @param item 物品
     * @return 是否是宝石
     */
    public static boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 方式1: 检查 PDC 标记
        if (pdc.has(RPGItemsKeys.IS_GEM, PersistentDataType.BYTE)) {
            byte value = pdc.get(RPGItemsKeys.IS_GEM, PersistentDataType.BYTE);
            return value == 1;
        }

        // 方式2: 检查是否有宝石类型
        if (pdc.has(RPGItemsKeys.GEM_TYPE, PersistentDataType.STRING)) {
            return true;
        }

        // 方式3: 检查是否是 RPGItems 物品且有属性
        String itemId = getGemId(item);
        if (itemId != null && !itemId.isEmpty()) {
            // 尝试读取属性，如果有属性则认为是宝石
            Map<String, AttributeValue> attrs = parseGemAttributesById(itemId);
            return !attrs.isEmpty();
        }

        return false;
    }

    /**
     * 获取宝石类型（从 PDC 读取）
     *
     * @param item 物品
     * @return 宝石类型
     */
    public static String getGemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "未知";
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "未知";
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 从 PDC 读取宝石类型
        if (pdc.has(RPGItemsKeys.GEM_TYPE, PersistentDataType.STRING)) {
            return pdc.get(RPGItemsKeys.GEM_TYPE, PersistentDataType.STRING);
        }

        // 回退：从名称推断（仅用于展示）
        if (meta.hasDisplayName()) {
            String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            for (String gemType : GEM_TYPE_TO_COLOR.keySet()) {
                if (displayName.contains(gemType)) {
                    return gemType;
                }
            }
        }

        return "未知";
    }

    /**
     * 检查宝石是否兼容槽位
     */
    public static boolean canSocket(String socketType, String gemType) {
        if (socketType == null || gemType == null) return false;
        return socketType.equals(gemType);
    }

    /**
     * 检查宝石是否兼容槽位
     */
    public static boolean isGemCompatible(String socketType, String gemType) {
        return canSocket(socketType, gemType);
    }

    /**
     * 获取宝石ID（从 PDC 读取）
     *
     * @param item 物品
     * @return RPGItems 物品ID
     */
    public static String getGemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey keyId = new NamespacedKey("rpgitems", "id");

        if (pdc.has(keyId, PersistentDataType.STRING)) {
            return pdc.get(keyId, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * 获取已镶嵌的宝石物品列表（从 PDC 读取）
     */
    public static List<ItemStack> getStoredInlaidGems(ItemStack item) {
        List<ItemStack> gems = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return gems;

        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) return gems;

        List<GemData> gemDataList = gemStorage.loadGems(item);
        for (GemData gemData : gemDataList) {
            ItemStack gemItem = gemData.toItemStack();
            if (gemItem != null) {
                gems.add(gemItem);
            }
        }

        return gems;
    }

    /**
     * 应用镶嵌（使用槽位索引，避免错位）
     *
     * @param item 装备
     * @param gemsBySlot 宝石映射（槽位索引 -> 宝石物品）
     * @param attributes 属性映射（不再使用，保留参数兼容性）
     */
    public static void applyInlayBySlot(ItemStack item, Map<Integer, ItemStack> gemsBySlot, Map<String, AttributeValue> attributes) {
        if (item == null || gemsBySlot == null || gemsBySlot.isEmpty()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) return;

        // 1. 保存宝石ID到 PDC（使用枚举直接访问，无循环）
        for (cn.guangdian.socket.constant.SocketSlot slot : cn.guangdian.socket.constant.SocketSlot.VALUES) {
            int slotIndex = slot.getIndex();
            ItemStack gem = gemsBySlot.get(slotIndex);

            if (gem != null) {
                String gemId = getGemId(gem);
                if (gemId != null) {
                    gemStorage.saveGem(item, slotIndex, gemId);
                }
            }
        }

        // 2. 更新 Lore 展示（使用枚举直接访问，无循环嵌套）
        updateLoreForDisplayBySlot(item, gemsBySlot);
    }

    /**
     * 更新 Lore 展示（使用枚举直接访问，无循环嵌套）
     *
     * @param item 装备
     * @param gemsBySlot 宝石映射（槽位索引 -> 宝石物品）
     */
    private static void updateLoreForDisplayBySlot(ItemStack item, Map<Integer, ItemStack> gemsBySlot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> loreComponents = meta.lore();
        if (loreComponents == null) {
            loreComponents = new ArrayList<>();
        }

        // 转换为 MiniMessage 格式
        List<String> legacyLore = new ArrayList<>();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        for (Component comp : loreComponents) {
            String miniMsg = mm.serialize(comp);
            legacyLore.add(miniMsg);
        }

        // 初始化 Lore 行索引（首次镶嵌时）
        initializeLoreIndexes(item, legacyLore);

        // 使用枚举直接访问每个槽位，更新对应的 Lore 行
        legacyLore = updateLoreBySlotDirect(item, legacyLore, gemsBySlot);

        // 转换回 Component 列表
        List<Component> newLoreComponents = new ArrayList<>();
        for (String line : legacyLore) {
            newLoreComponents.add(mm.deserialize(line));
        }

        meta.lore(newLoreComponents);
        item.setItemMeta(meta);
    }

    /**
     * 初始化 Lore 行索引（首次镶嵌时，扫描并保存每个槽位的行索引）
     *
     * @param item 装备
     * @param lore Lore 列表
     */
    private static void initializeLoreIndexes(ItemStack item, List<String> lore) {
        if (lore == null) return;

        // 检查是否已经初始化过
        if (cn.guangdian.socket.GuangDianSocket.getInstance().getGemStorage().getLoreIndex(item, 0) >= 0) {
            return; // 已经初始化过，跳过
        }

        int currentSlotIndex = 0;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String plain = stripLegacyColor(line);

            // 遇到"已镶嵌"或"可镶嵌"行，记录行索引
            if (plain.contains("已镶嵌") || plain.contains("可镶嵌")) {
                cn.guangdian.socket.GuangDianSocket.getInstance().getGemStorage().saveLoreIndex(item, currentSlotIndex, i);
                currentSlotIndex++;
            }
        }
    }

    /**
     * 使用枚举直接访问槽位，更新对应的 Lore 行
     *
     * @param item 装备（用于读取 Lore 行索引）
     * @param lore Lore 列表
     * @param gemsBySlot 宝石映射（槽位索引 -> 宝石物品）
     * @return 更新后的 Lore 列表
     */
    private static List<String> updateLoreBySlotDirect(ItemStack item, List<String> lore, Map<Integer, ItemStack> gemsBySlot) {
        if (lore == null) return new ArrayList<>();

        List<String> newLore = new ArrayList<>(lore);

        // 使用枚举直接访问每个槽位
        for (cn.guangdian.socket.constant.SocketSlot slot : cn.guangdian.socket.constant.SocketSlot.VALUES) {
            int slotIndex = slot.getIndex();
            ItemStack gem = gemsBySlot.get(slotIndex);

            if (gem != null && gem.hasItemMeta()) {
                // 从 PDC 直接读取 Lore 行索引
                int loreIndex = cn.guangdian.socket.GuangDianSocket.getInstance().getGemStorage().getLoreIndex(item, slotIndex);

                if (loreIndex >= 0 && loreIndex < newLore.size()) {
                    String gemName = getGemDisplayName(gem);
                    String gemType = getGemType(gem);
                    String gemColor = getGemColor(gemType);

                    // 从 PDC 读取属性用于展示
                    Map<String, AttributeValue> gemAttrs = parseGemAttributes(gem);
                    String attrStr = formatAttributesSingleLine(gemAttrs);

                    String inlayLine = "<!italic><dark_aqua>[<!italic><green>已镶嵌 <!italic>" + gemColor + gemName + "<!italic><dark_aqua>]<!italic>" + attrStr;
                    newLore.set(loreIndex, inlayLine);  // 直接替换，无需遍历
                }
            }
        }

        return newLore;
    }

    /**
     * 应用镶嵌（只操作 PDC，Lore 仅用于展示）
     *
     * @param item 装备
     * @param gems 宝石列表
     * @param attributes 属性映射（不再使用，保留参数兼容性）
     */
    public static void applyInlay(ItemStack item, List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        if (item == null || gems == null || gems.isEmpty()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) return;

        // 1. 保存宝石ID到 PDC
        for (int i = 0; i < gems.size(); i++) {
            ItemStack gem = gems.get(i);
            if (gem != null) {
                String gemId = getGemId(gem);
                if (gemId != null) {
                    gemStorage.saveGem(item, i, gemId);
                }
            }
        }

        // 2. 更新 Lore 展示（仅用于展示，不参与属性计算）
        // 注意：updateLoreForDisplay 内部会调用 item.setItemMeta()，所以这里不需要再设置
        updateLoreForDisplay(item, gems);
    }

    /**
     * 更新 Lore 展示（仅用于展示，不参与属性计算）
     *
     * @param item 装备
     * @param gems 宝石列表（包含所有已镶嵌的宝石）
     */
    private static void updateLoreForDisplay(ItemStack item, List<ItemStack> gems) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> loreComponents = meta.lore();
        if (loreComponents == null) {
            loreComponents = new ArrayList<>();
        }

        // 转换为 MiniMessage 格式
        List<String> legacyLore = new ArrayList<>();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        for (Component comp : loreComponents) {
            String miniMsg = mm.serialize(comp);
            legacyLore.add(miniMsg);
        }

        // 清除所有旧的"已镶嵌"行，避免重复
        legacyLore = clearOldInlayLines(legacyLore);

        // 替换 Lore 中的可镶嵌为已镶嵌（不再保留旧的"已镶嵌"行，因为已经清除了）
        legacyLore = replaceSocketWithInlayInternal(legacyLore, gems);

        // 转换回 Component 列表
        List<Component> newLoreComponents = new ArrayList<>();
        for (String line : legacyLore) {
            newLoreComponents.add(mm.deserialize(line));
        }

        meta.lore(newLoreComponents);
        item.setItemMeta(meta);
    }

    /**
     * 清除所有旧的"已镶嵌"行
     * 避免重复镶嵌时出现描述重复的问题
     *
     * @param lore Lore 列表
     * @return 清理后的 Lore 列表
     */
    private static List<String> clearOldInlayLines(List<String> lore) {
        if (lore == null) return new ArrayList<>();

        List<String> cleanedLore = new ArrayList<>();
        for (String line : lore) {
            String plain = stripLegacyColor(line);
            // 移除所有"已镶嵌"行
            if (plain.contains("已镶嵌")) {
                continue;
            }
            cleanedLore.add(line);
        }

        return cleanedLore;
    }

    /**
     * 清除镶嵌（只操作 PDC，并更新 Lore 展示）
     *
     * @param item 装备
     * @return 清除后的装备
     */
    public static ItemStack clearInlay(ItemStack item) {
        if (item == null) return item;

        GemStorage gemStorage = getGemStorage();
        if (gemStorage == null) return item;

        // 1. 先读取所有槽位类型（在清除之前）
        List<String> socketTypes = new ArrayList<>();
        for (cn.guangdian.socket.constant.SocketSlot slot : cn.guangdian.socket.constant.SocketSlot.VALUES) {
            String socketType = gemStorage.getSocketType(item, slot.getIndex());
            socketTypes.add(socketType != null ? socketType : "");
        }

        // 2. 清除 PDC 中的宝石数据
        ItemStack clearedItem = gemStorage.clearAllGems(item);

        // 3. 更新 Lore 展示（将"已镶嵌"改回"可镶嵌"）
        updateLoreAfterRemoval(clearedItem, socketTypes);

        return clearedItem;
    }

    /**
     * 拆卸后更新 Lore 展示（将"已镶嵌"改回"可镶嵌"）
     *
     * @param item 装备
     * @param socketTypes 槽位类型列表（从 PDC 读取）
     */
    private static void updateLoreAfterRemoval(ItemStack item, List<String> socketTypes) {
        if (item == null || !item.hasItemMeta()) {
            System.out.println("[SocketParser] updateLoreAfterRemoval: 物品为空或没有 meta");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            System.out.println("[SocketParser] updateLoreAfterRemoval: meta 为空");
            return;
        }

        List<Component> loreComponents = meta.lore();
        if (loreComponents == null) {
            System.out.println("[SocketParser] updateLoreAfterRemoval: lore 为空");
            return;
        }

        System.out.println("[SocketParser] updateLoreAfterRemoval: 开始处理 Lore，共 " + loreComponents.size() + " 行");
        System.out.println("[SocketParser] updateLoreAfterRemoval: 槽位类型: " + socketTypes);

        // 转换为 MiniMessage 格式
        List<String> legacyLore = new ArrayList<>();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        for (Component comp : loreComponents) {
            String miniMsg = mm.serialize(comp);
            legacyLore.add(miniMsg);
            System.out.println("[SocketParser] updateLoreAfterRemoval: 原始行: " + miniMsg);
        }

        // 将"已镶嵌"改回"可镶嵌"
        List<String> newLore = new ArrayList<>();
        int socketIndex = 0;
        int replacedCount = 0;
        for (String line : legacyLore) {
            String plain = stripLegacyColor(line);

            // 检测已镶嵌行，改回可镶嵌
            if (plain.contains("已镶嵌")) {
                System.out.println("[SocketParser] updateLoreAfterRemoval: 找到已镶嵌行: " + plain);
                
                // 从槽位类型列表中获取对应的槽位类型
                if (socketIndex < socketTypes.size()) {
                    String socketType = socketTypes.get(socketIndex);
                    if (socketType != null && !socketType.isEmpty()) {
                        String socketColor = getGemColor(socketType);
                        String newLine = "<!italic><dark_aqua>[<!italic><gray>可镶嵌<!italic>" + socketColor + socketType + "<!italic><dark_aqua>]";
                        newLore.add(newLine);
                        socketIndex++;
                        replacedCount++;
                        System.out.println("[SocketParser] updateLoreAfterRemoval: 替换为: " + newLine);
                        continue;
                    } else {
                        System.out.println("[SocketParser] updateLoreAfterRemoval: 槽位类型为空，跳过");
                        socketIndex++;
                    }
                } else {
                    System.out.println("[SocketParser] updateLoreAfterRemoval: 槽位索引超出范围");
                }
            }

            newLore.add(line);
        }

        System.out.println("[SocketParser] updateLoreAfterRemoval: 共替换 " + replacedCount + " 行");

        // 转换回 Component 列表
        List<Component> newLoreComponents = new ArrayList<>();
        for (String line : newLore) {
            newLoreComponents.add(mm.deserialize(line));
        }

        meta.lore(newLoreComponents);
        item.setItemMeta(meta);
        System.out.println("[SocketParser] updateLoreAfterRemoval: Lore 更新完成");
    }

    /**
     * 从已镶嵌行提取宝石类型
     *
     * @param line Lore 行
     * @return 宝石类型（如：红宝石、蓝宝石等）
     */
    private static String extractSocketTypeFromInlayLine(String line) {
        // 尝试从颜色映射中匹配
        for (String gemType : GEM_TYPE_TO_COLOR.keySet()) {
            if (line.contains(gemType)) {
                return gemType;
            }
        }

        // 如果没有匹配到，尝试从行中提取
        // 格式：[已镶嵌 宝石名]
        if (line.contains("已镶嵌")) {
            // 移除颜色标签
            String cleaned = line.replaceAll("<[^>]+>", "");
            // 提取宝石类型
            if (cleaned.contains("红宝石")) return "红宝石";
            if (cleaned.contains("蓝宝石")) return "蓝宝石";
            if (cleaned.contains("绿宝石")) return "绿宝石";
            if (cleaned.contains("黄宝石")) return "黄宝石";
            if (cleaned.contains("紫宝石")) return "紫宝石";
        }

        return null;
    }

    /**
     * 从宝石物品读取属性（使用 PDC）
     *
     * @param gem 宝石物品
     * @return 属性映射
     */
    public static Map<String, AttributeValue> parseGemAttributes(ItemStack gem) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        if (gem == null || !gem.hasItemMeta()) return attributes;

        ItemMeta meta = gem.getItemMeta();
        if (meta == null) return attributes;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 优先读取复合存储格式
        byte[] compoundData = pdc.get(
            CompoundAttributeCodec.KEY_COMPOUND,
            PersistentDataType.BYTE_ARRAY
        );
        if (compoundData != null) {
            return parseGemAttributesFromCompound(compoundData);
        }
        // 回退到旧格式（向后兼容）

        // 定义范围属性（min/max 配对）
        String[][] rangeAttributes = {
            {"rpgitems:attack_min", "rpgitems:attack_max", "攻击力"},
            {"rpgitems:defense_min", "rpgitems:defense_max", "防御力"},
            {"rpgitems:pvp_attack_min", "rpgitems:pvp_attack_max", "PVP攻击力"},
            {"rpgitems:pvp_defense_min", "rpgitems:pvp_defense_max", "PVP防御力"}
        };

        // 处理范围属性
        for (String[] rangeAttr : rangeAttributes) {
            String minKeyStr = rangeAttr[0];
            String maxKeyStr = rangeAttr[1];
            String attrName = rangeAttr[2];

            NamespacedKey minKey = NamespacedKey.fromString(minKeyStr);
            NamespacedKey maxKey = NamespacedKey.fromString(maxKeyStr);

            double minValue = 0;
            double maxValue = 0;
            boolean hasMin = minKey != null && pdc.has(minKey, PersistentDataType.DOUBLE);
            boolean hasMax = maxKey != null && pdc.has(maxKey, PersistentDataType.DOUBLE);

            if (hasMin) {
                minValue = pdc.get(minKey, PersistentDataType.DOUBLE);
            }
            if (hasMax) {
                maxValue = pdc.get(maxKey, PersistentDataType.DOUBLE);
            }

            if (hasMin || hasMax) {
                if (hasMin && hasMax) {
                    attributes.put(attrName, AttributeValue.range(minValue, maxValue));
                } else if (hasMin) {
                    attributes.put(attrName, AttributeValue.of(minValue));
                } else {
                    attributes.put(attrName, AttributeValue.of(maxValue));
                }
            }
        }

        // 定义单值属性
        String[][] singleAttributes = {
            {"rpgitems:max_health", "生命上限"},
            {"rpgitems:health_regen", "生命回复"},
            {"rpgitems:crit_chance", "暴击几率"},
            {"rpgitems:crit_damage", "暴击伤害"},
            {"rpgitems:lifesteal_chance", "吸血几率"},
            {"rpgitems:lifesteal_multiplier", "吸血倍率"},
            {"rpgitems:dodge_chance", "闪避"},
            {"rpgitems:parry_chance", "招架"},
            {"rpgitems:move_speed", "移动速度"},
            {"rpgitems:damage_reduction", "减伤"},
            {"rpgitems:crit_resist", "暴击抵抗"},
            {"rpgitems:crit_damage_resist", "暴伤抵抗"},
            {"rpgitems:lifesteal_resist", "吸血抵抗"},
            {"rpgitems:armor", "护甲"},
            {"rpgitems:armor_strength", "护甲强度"},
            {"rpgitems:armor_penetration", "护甲穿透"},
            {"rpgitems:defense_penetration", "防御穿透"},
            {"rpgitems:damage_reflect", "伤害反弹"},
            {"rpgitems:reflect_ratio", "反弹比例"},
            {"rpgitems:poison_chance", "中毒几率"},
            {"rpgitems:freeze_chance", "冰冻几率"},
            {"rpgitems:blind_chance", "致盲几率"},
            {"rpgitems:burn_chance", "燃烧几率"},
            {"rpgitems:scorch_chance", "灼烧几率"},
            {"rpgitems:fire_resist", "火焰抗性"},
            {"rpgitems:fall_resist", "摔落抗性"},
            {"rpgitems:drowning_resist", "溺水抗性"},
            {"rpgitems:poison_resist", "中毒抗性"},
            {"rpgitems:wither_resist", "凋零抗性"},
            {"rpgitems:lava_resist", "岩浆抗性"},
            {"rpgitems:magic_resist", "魔法抗性"},
            {"rpgitems:explosion_resist", "爆炸抗性"},
            {"rpgitems:projectile_resist", "弹射物抗性"},
            {"rpgitems:knockback_resist", "击退抗性"},
            {"rpgitems:exp_bonus", "经验加成"},
            {"rpgitems:health_regen_percent", "生命恢复百分比"},
            {"rpgitems:dodge_reflect_chance", "闪避反弹几率"},
            {"rpgitems:dodge_reflect_ratio", "闪避反弹比例"}
        };

        // 处理单值属性
        for (String[] singleAttr : singleAttributes) {
            String keyStr = singleAttr[0];
            String attrName = singleAttr[1];

            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key != null && pdc.has(key, PersistentDataType.DOUBLE)) {
                double value = pdc.get(key, PersistentDataType.DOUBLE);
                if (value > 0) {
                    attributes.put(attrName, AttributeValue.of(value));
                }
            }
        }

        return attributes;
    }

    /**
     * 从复合 byte[] 读取宝石属性
     */
    private static Map<String, AttributeValue> parseGemAttributesFromCompound(byte[] data) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        double[] v = CompoundAttributeCodec.deserialize(data);

        // 范围属性
        double atkMin = v[CompoundAttributeCodec.ATTACK_MIN];
        double atkMax = v[CompoundAttributeCodec.ATTACK_MAX];
        if (atkMin > 0 || atkMax > 0) {
            if (atkMax == 0) atkMax = atkMin;
            attributes.put("攻击力", AttributeValue.range(atkMin, atkMax));
        }

        double defMin = v[CompoundAttributeCodec.DEFENSE_MIN];
        double defMax = v[CompoundAttributeCodec.DEFENSE_MAX];
        if (defMin > 0 || defMax > 0) {
            if (defMax == 0) defMax = defMin;
            attributes.put("防御力", AttributeValue.range(defMin, defMax));
        }

        double pvpAtkMin = v[CompoundAttributeCodec.PVP_ATTACK_MIN];
        double pvpAtkMax = v[CompoundAttributeCodec.PVP_ATTACK_MAX];
        if (pvpAtkMin > 0 || pvpAtkMax > 0) {
            if (pvpAtkMax == 0) pvpAtkMax = pvpAtkMin;
            attributes.put("PVP攻击力", AttributeValue.range(pvpAtkMin, pvpAtkMax));
        }

        double pvpDefMin = v[CompoundAttributeCodec.PVP_DEFENSE_MIN];
        double pvpDefMax = v[CompoundAttributeCodec.PVP_DEFENSE_MAX];
        if (pvpDefMin > 0 || pvpDefMax > 0) {
            if (pvpDefMax == 0) pvpDefMax = pvpDefMin;
            attributes.put("PVP防御力", AttributeValue.range(pvpDefMin, pvpDefMax));
        }

        // 单值属性
        putIfPositive(attributes, "生命上限", v[CompoundAttributeCodec.MAX_HEALTH], false);
        putIfPositive(attributes, "生命回复", v[CompoundAttributeCodec.HEALTH_REGEN], false);
        putIfPositive(attributes, "暴击几率", v[CompoundAttributeCodec.CRIT_CHANCE], true);
        putIfPositive(attributes, "暴击伤害", v[CompoundAttributeCodec.CRIT_DAMAGE], true);
        putIfPositive(attributes, "吸血几率", v[CompoundAttributeCodec.LIFESTEAL_CHANCE], true);
        putIfPositive(attributes, "吸血倍率", v[CompoundAttributeCodec.LIFESTEAL_MULTIPLIER], false);
        putIfPositive(attributes, "闪避", v[CompoundAttributeCodec.DODGE_CHANCE], true);
        putIfPositive(attributes, "招架", v[CompoundAttributeCodec.PARRY_CHANCE], true);
        putIfPositive(attributes, "移动速度", v[CompoundAttributeCodec.MOVE_SPEED], true);
        putIfPositive(attributes, "减伤", v[CompoundAttributeCodec.DAMAGE_REDUCTION], true);
        putIfPositive(attributes, "暴击抵抗", v[CompoundAttributeCodec.CRIT_RESIST], true);
        putIfPositive(attributes, "暴伤抵抗", v[CompoundAttributeCodec.CRIT_DAMAGE_RESIST], true);
        putIfPositive(attributes, "吸血抵抗", v[CompoundAttributeCodec.LIFESTEAL_RESIST], true);
        putIfPositive(attributes, "护甲值", v[CompoundAttributeCodec.ARMOR], false);
        putIfPositive(attributes, "护甲强度", v[CompoundAttributeCodec.ARMOR_STRENGTH], false);
        putIfPositive(attributes, "护甲穿透", v[CompoundAttributeCodec.ARMOR_PENETRATION], false);
        putIfPositive(attributes, "防御穿透", v[CompoundAttributeCodec.DEFENSE_PENETRATION], false);
        putIfPositive(attributes, "伤害反弹", v[CompoundAttributeCodec.DAMAGE_REFLECT], false);
        putIfPositive(attributes, "反伤比例", v[CompoundAttributeCodec.REFLECT_RATIO], true);
        putIfPositive(attributes, "中毒", v[CompoundAttributeCodec.POISON_CHANCE], true);
        putIfPositive(attributes, "冰冻", v[CompoundAttributeCodec.FREEZE_CHANCE], true);
        putIfPositive(attributes, "致盲", v[CompoundAttributeCodec.BLIND_CHANCE], true);
        putIfPositive(attributes, "燃烧", v[CompoundAttributeCodec.BURN_CHANCE], true);
        putIfPositive(attributes, "灼烧", v[CompoundAttributeCodec.SCORCH_CHANCE], true);
        putIfPositive(attributes, "火焰抗性", v[CompoundAttributeCodec.FIRE_RESIST], true);
        putIfPositive(attributes, "摔落抗性", v[CompoundAttributeCodec.FALL_RESIST], true);
        putIfPositive(attributes, "溺水抗性", v[CompoundAttributeCodec.DROWNING_RESIST], true);
        putIfPositive(attributes, "中毒抗性", v[CompoundAttributeCodec.POISON_RESIST], true);
        putIfPositive(attributes, "凋零抗性", v[CompoundAttributeCodec.WITHER_RESIST], true);
        putIfPositive(attributes, "岩浆抗性", v[CompoundAttributeCodec.LAVA_RESIST], true);
        putIfPositive(attributes, "魔法抗性", v[CompoundAttributeCodec.MAGIC_RESIST], true);
        putIfPositive(attributes, "爆炸抗性", v[CompoundAttributeCodec.EXPLOSION_RESIST], true);
        putIfPositive(attributes, "弹射物抗性", v[CompoundAttributeCodec.PROJECTILE_RESIST], true);
        putIfPositive(attributes, "击退抗性", v[CompoundAttributeCodec.KNOCKBACK_RESIST], true);
        putIfPositive(attributes, "经验加成", v[CompoundAttributeCodec.EXP_BONUS], true);
        putIfPositive(attributes, "生命恢复", v[CompoundAttributeCodec.HEALTH_REGEN_PERCENT], true);
        putIfPositive(attributes, "躲避反伤", v[CompoundAttributeCodec.DODGE_REFLECT_CHANCE], true);
        putIfPositive(attributes, "躲避反弹比例", v[CompoundAttributeCodec.DODGE_REFLECT_RATIO], true);

        return attributes;
    }

    private static void putIfPositive(Map<String, AttributeValue> attributes, String name, double value, boolean isPercent) {
        if (value > 0) {
            attributes.put(name, isPercent ? AttributeValue.ofPercent(value) : AttributeValue.of(value));
        }
    }

    /**
     * 根据物品ID从RPGItems获取宝石属性
     *
     * @param itemId RPGItems物品ID
     * @return 属性映射
     */
    public static Map<String, AttributeValue> parseGemAttributesById(String itemId) {
        Map<String, AttributeValue> attributes = new HashMap<>();

        if (itemId == null || itemId.isEmpty()) {
            return attributes;
        }

        // 尝试从RPGItems获取物品
        org.bukkit.plugin.Plugin rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
        if (rpgItemsPlugin == null || !rpgItemsPlugin.isEnabled()) {
            return attributes;
        }

        try {
            // 使用RPGItems的ItemService创建物品
            cn.guangdian.rpgitems.RPGItems rpgItems = (cn.guangdian.rpgitems.RPGItems) rpgItemsPlugin;
            cn.guangdian.rpgitems.service.ItemService itemService = rpgItems.getItemService();

            if (itemService == null) {
                return attributes;
            }

            // 创建物品实例
            java.util.Optional<org.bukkit.inventory.ItemStack> itemOpt = itemService.createItem(itemId);
            if (itemOpt.isEmpty()) {
                return attributes;
            }

            // 从物品实例解析属性
            org.bukkit.inventory.ItemStack gemItem = itemOpt.get();
            return parseGemAttributes(gemItem);

        } catch (Exception e) {
            // 如果出错，返回空Map
            return attributes;
        }
    }

    /**
     * 替换 Lore 中的可镶嵌为已镶嵌（仅用于展示）
     *
     * @param lore Lore 列表
     * @param gems 宝石列表
     * @param attributes 属性映射（不再使用，保留参数兼容性）
     * @return 新的 Lore 列表
     */
    public static List<String> replaceSocketWithInlay(List<String> lore, List<ItemStack> gems, Map<String, AttributeValue> attributes) {
        if (lore == null) return new ArrayList<>();

        List<String> newLore = new ArrayList<>();
        int gemIndex = 0;

        for (String line : lore) {
            String plain = stripLegacyColor(line);

            // 保留已存在的已镶嵌行（用于预览场景）
            if (plain.contains("已镶嵌")) {
                newLore.add(line);
                continue;
            }

            // 替换可镶嵌为已镶嵌
            if (plain.contains("可镶嵌") && gemIndex < gems.size()) {
                ItemStack gem = gems.get(gemIndex);
                if (gem != null && gem.hasItemMeta()) {
                    String gemName = getGemDisplayName(gem);
                    String gemType = getGemType(gem);
                    String gemColor = getGemColor(gemType);

                    // 从 PDC 读取属性用于展示
                    Map<String, AttributeValue> gemAttrs = parseGemAttributes(gem);
                    String attrStr = formatAttributesSingleLine(gemAttrs);

                    String inlayLine = "<!italic><dark_aqua>[<!italic><green>已镶嵌 <!italic>" + gemColor + gemName + "<!italic><dark_aqua>]<!italic>" + attrStr;
                    newLore.add(inlayLine);

                    gemIndex++;
                    continue;
                }
            }

            newLore.add(line);
        }

        return newLore;
    }

    /**
     * 替换 Lore 中的可镶嵌为已镶嵌（内部方法，不保留旧的"已镶嵌"行）
     * 用于实际镶嵌场景，所有宝石信息都从参数中获取
     *
     * @param lore Lore 列表
     * @param gems 宝石列表（包含所有已镶嵌的宝石）
     * @return 新的 Lore 列表
     */
    private static List<String> replaceSocketWithInlayInternal(List<String> lore, List<ItemStack> gems) {
        if (lore == null) return new ArrayList<>();

        List<String> newLore = new ArrayList<>();
        int gemIndex = 0;

        for (String line : lore) {
            String plain = stripLegacyColor(line);

            // 替换可镶嵌为已镶嵌
            if (plain.contains("可镶嵌") && gemIndex < gems.size()) {
                ItemStack gem = gems.get(gemIndex);
                if (gem != null && gem.hasItemMeta()) {
                    String gemName = getGemDisplayName(gem);
                    String gemType = getGemType(gem);
                    String gemColor = getGemColor(gemType);

                    // 从 PDC 读取属性用于展示
                    Map<String, AttributeValue> gemAttrs = parseGemAttributes(gem);
                    String attrStr = formatAttributesSingleLine(gemAttrs);

                    String inlayLine = "<!italic><dark_aqua>[<!italic><green>已镶嵌 <!italic>" + gemColor + gemName + "<!italic><dark_aqua>]<!italic>" + attrStr;
                    newLore.add(inlayLine);

                    gemIndex++;
                    continue;
                }
            }

            newLore.add(line);
        }

        return newLore;
    }

    /**
     * 获取宝石显示名称
     */
    public static String getGemDisplayName(ItemStack gem) {
        if (gem == null || !gem.hasItemMeta()) return "未知宝石";
        ItemMeta meta = gem.getItemMeta();
        if (meta == null) return "未知宝石";

        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        if (displayName == null || displayName.isEmpty()) {
            return gem.getType().name();
        }
        return displayName;
    }

    /**
     * 格式化属性为单行字符串（用于 Lore 展示）
     */
    private static String formatAttributesSingleLine(Map<String, AttributeValue> attrs) {
        if (attrs == null || attrs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
            sb.append(" <!italic><gray>").append(entry.getKey()).append(": <!italic><aqua>+");
            AttributeValue value = entry.getValue();
            if (value instanceof AttributeValue.RangeValue range) {
                sb.append(formatNumber(range.getMin())).append("-").append(formatNumber(range.getMax()));
            } else {
                sb.append(formatNumber(value.getValue()));
            }
            if (value.isPercentage()) {
                sb.append("%");
            }
        }
        return sb.toString();
    }

    /**
     * 去除旧版颜色代码
     */
    private static String stripLegacyColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    /**
     * 格式化数字
     */
    private static String formatNumber(double num) {
        if (num == (long) num) {
            return String.valueOf((long) num);
        }
        return String.valueOf(num);
    }

    /**
     * 获取 GemStorage 实例
     */
    private static GemStorage getGemStorage() {
        GuangDianSocket plugin = (GuangDianSocket) Bukkit.getPluginManager().getPlugin("GuangDianSocket");
        if (plugin == null) return null;
        return plugin.getGemStorage();
    }
}
