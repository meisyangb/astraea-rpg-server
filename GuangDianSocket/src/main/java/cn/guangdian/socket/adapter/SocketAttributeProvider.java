package cn.guangdian.socket.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ItemAttributeProvider;
import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.model.AttributeValue;
import cn.guangdian.socket.model.GemData;
import cn.guangdian.socket.parser.SocketParser;
import cn.guangdian.socket.storage.GemStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 宝石属性提供者
 * 
 * 职责：为 RPGCore 提供宝石镶嵌属性
 * 实现 ItemAttributeProvider 接口，供属性系统查询
 */
public class SocketAttributeProvider implements ItemAttributeProvider {

    private final GuangDianSocket plugin;
    private final GemStorage gemStorage;

    public SocketAttributeProvider(GuangDianSocket plugin) {
        this.plugin = plugin;
        this.gemStorage = plugin.getGemStorage();
    }

    @Override
    public String getProviderName() {
        return "GuangDianSocket";
    }

    @Override
    public Map<String, Double> getItemAttributes(ItemStack item, Player player) {
        Map<String, Double> attributes = new HashMap<>();

        if (item == null || !item.hasItemMeta()) {
            return attributes;
        }

        // 从存储中加载宝石数据
        List<GemData> gems = gemStorage.loadGems(item);

        // 聚合所有宝石属性
        for (GemData gem : gems) {
            Map<String, AttributeValue> gemAttrs = SocketParser.parseGemAttributesById(gem.getItemId());
            for (Map.Entry<String, AttributeValue> entry : gemAttrs.entrySet()) {
                double value = entry.getValue().getValue();
                attributes.merge(entry.getKey(), value, Double::sum);
            }
        }

        return attributes;
    }

    @Override
    public List<String> getAttributeLore(ItemStack item) {
        List<String> lore = new ArrayList<>();

        if (item == null || !gemStorage.hasGems(item)) {
            return lore;
        }

        List<GemData> gems = gemStorage.loadGems(item);
        if (gems.isEmpty()) {
            return lore;
        }

        lore.add("<gold>==== 镶嵌属性 ====");

        // 聚合属性
        Map<String, AttributeValue> totalAttrs = new HashMap<>();
        for (GemData gem : gems) {
            Map<String, AttributeValue> gemAttrs = SocketParser.parseGemAttributesById(gem.getItemId());
            for (Map.Entry<String, AttributeValue> entry : gemAttrs.entrySet()) {
                totalAttrs.merge(entry.getKey(), entry.getValue(), AttributeValue::merge);
            }
        }

        // 格式化属性
        for (Map.Entry<String, AttributeValue> entry : totalAttrs.entrySet()) {
            lore.add(formatAttributeLine(entry.getKey(), entry.getValue()));
        }

        lore.add("<gold>═══════════════");

        return lore;
    }

    @Override
    public boolean hasAttributes(ItemStack item) {
        return item != null && gemStorage.hasGems(item);
    }

    /**
     * 注册到 RPGCore
     */
    public void register() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getItemAttributeManager().registerProvider(this);
            plugin.getLogger().info("宝石属性提供者已注册到 RPGCore");
        }
    }

    /**
     * 从 RPGCore 注销
     */
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getItemAttributeManager().unregisterProvider(getProviderName());
            plugin.getLogger().info("宝石属性提供者已从 RPGCore 注销");
        }
    }

    private String formatAttributeLine(String attributeName, AttributeValue value) {
        String color = getAttributeColor(attributeName);
        return color + attributeName + ": <aqua>+" + formatValue(attributeName, value);
    }

    private String getAttributeColor(String attrName) {
        return switch (attrName) {
            case "攻击力" -> "<red>";
            case "生命上限" -> "<green>";
            case "防御力" -> "<yellow>";
            case "闪避" -> "<dark_purple>";
            case "暴击几率", "暴击伤害" -> "<aqua>";
            default -> "<white>";
        };
    }

    private String formatValue(String attrName, AttributeValue value) {
        if (value instanceof AttributeValue.RangeValue range) {
            return formatNumber(range.getMin()) + "-" + formatNumber(range.getMax());
        }
        String result = formatNumber(value.getValue());
        if (attrName.contains("几率") || attrName.contains("概率") || attrName.contains("率")) {
            result += "%";
        }
        return result;
    }

    private String formatNumber(double num) {
        if (num == (long) num) {
            return String.valueOf((long) num);
        }
        return String.valueOf(num);
    }
}
