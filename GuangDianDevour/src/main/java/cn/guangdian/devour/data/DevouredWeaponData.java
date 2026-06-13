package cn.guangdian.devour.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 被吞噬武器数据模型
 * 参考镶嵌插件 GemData 设计，每把被吞噬的武器作为一个整体存储
 *
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class DevouredWeaponData {

    /** 槽位索引 (1-based) */
    private int slotIndex;

    /** 物品ID (Display名称的纯文本) */
    private String itemId;

    /** 物品显示名称 */
    private String displayName;

    /** 物品材质 */
    private String material;

    /** MythicMobs 类型 */
    private String mythicType;

    /** 是否为 MythicMobs 物品 */
    private boolean isMythicItem;

    /** 物品Lore (序列化) */
    private String serializedLore;

    /** 物品属性 (序列化为字符串) */
    private String serializedAttributes;

    public DevouredWeaponData() {
        this.slotIndex = -1;
        this.isMythicItem = false;
    }

    public DevouredWeaponData(int slotIndex, String itemId, String displayName) {
        this.slotIndex = slotIndex;
        this.itemId = itemId;
        this.displayName = displayName;
        this.isMythicItem = false;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getMythicType() {
        return mythicType;
    }

    public void setMythicType(String mythicType) {
        this.mythicType = mythicType;
    }

    public boolean isMythicItem() {
        return isMythicItem;
    }

    public void setMythicItem(boolean mythicItem) {
        isMythicItem = mythicItem;
    }

    public String getSerializedLore() {
        return serializedLore;
    }

    public void setSerializedLore(String serializedLore) {
        this.serializedLore = serializedLore;
    }

    public String getSerializedAttributes() {
        return serializedAttributes;
    }

    public void setSerializedAttributes(String serializedAttributes) {
        this.serializedAttributes = serializedAttributes;
    }

    /**
     * 验证数据是否有效
     */
    public boolean isValid() {
        return slotIndex >= 1 && itemId != null && !itemId.isEmpty();
    }

    /**
     * 从 ItemStack 创建数据
     */
    public static DevouredWeaponData fromItemStack(ItemStack item, int slotIndex, String itemId, String mythicType) {
        if (item == null || itemId == null || itemId.isEmpty()) {
            return null;
        }

        DevouredWeaponData data = new DevouredWeaponData();
        data.setSlotIndex(slotIndex);
        data.setItemId(itemId);
        data.setMaterial(item.getType().name());
        data.setMythicType(mythicType);
        data.setMythicItem(mythicType != null && !mythicType.isEmpty());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName());
                data.setDisplayName(plainName);
            }

            if (meta.hasLore() && meta.lore() != null) {
                StringBuilder sb = new StringBuilder();
                for (net.kyori.adventure.text.Component line : meta.lore()) {
                    if (sb.length() > 0) sb.append("||");
                    String lineText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                    sb.append(lineText.replace("\n", "\\n"));
                }
                data.setSerializedLore(sb.toString());
            }
        }

        return data.isValid() ? data : null;
    }

    /**
     * 序列化为存储字符串
     * 格式: slot:1,id:武器名,display:显示名,material:DIAMOND_SWORD,mythic:类型,mythicItem:true,lore:序列化Lore,attrs:序列化属性
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("slot:").append(slotIndex);
        sb.append(",id:").append(escapeSpecialChars(itemId != null ? itemId : ""));
        sb.append(",display:").append(escapeSpecialChars(displayName != null ? displayName : ""));
        sb.append(",material:").append(material != null ? material : "");
        sb.append(",mythic:").append(escapeSpecialChars(mythicType != null ? mythicType : ""));
        sb.append(",mythicItem:").append(isMythicItem);
        sb.append(",lore:").append(escapeSpecialChars(serializedLore != null ? serializedLore : ""));
        sb.append(",attrs:").append(escapeSpecialChars(serializedAttributes != null ? serializedAttributes : ""));
        return sb.toString();
    }

    /**
     * 从存储字符串反序列化
     */
    public static DevouredWeaponData deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            DevouredWeaponData weapon = new DevouredWeaponData();
            String[] pairs = data.split(",");

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length != 2) continue;

                String key = keyValue[0];
                String value = unescapeSpecialChars(keyValue[1]);

                switch (key) {
                    case "slot" -> weapon.setSlotIndex(Integer.parseInt(value));
                    case "id" -> weapon.setItemId(value);
                    case "display" -> weapon.setDisplayName(value);
                    case "material" -> weapon.setMaterial(value);
                    case "mythic" -> weapon.setMythicType(value);
                    case "mythicItem" -> weapon.setMythicItem(Boolean.parseBoolean(value));
                    case "lore" -> weapon.setSerializedLore(value);
                    case "attrs" -> weapon.setSerializedAttributes(value);
                }
            }

            return weapon.isValid() ? weapon : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeSpecialChars(String str) {
        if (str == null) return "";
        return str.replace(",", "\\c").replace("|", "\\p").replace(":", "\\o");
    }

    private static String unescapeSpecialChars(String str) {
        if (str == null) return "";
        return str.replace("\\c", ",").replace("\\p", "|").replace("\\o", ":");
    }

    @Override
    public String toString() {
        return "DevouredWeaponData{" +
                "slot=" + slotIndex +
                ", id='" + itemId + '\'' +
                ", display='" + displayName + '\'' +
                ", mythic='" + mythicType + '\'' +
                '}';
    }
}
