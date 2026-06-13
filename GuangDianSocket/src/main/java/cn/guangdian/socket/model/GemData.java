package cn.guangdian.socket.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 宝石数据模型
 * 
 * 职责：封装单个宝石的所有数据
 */
public class GemData {

    private int slotIndex;
    private String type;
    private String socketType;
    private String itemId;
    private String displayName;
    private String material;
    private String serializedLore;
    private int amount;
    private boolean isRPGItem;

    public GemData() {
        this.slotIndex = -1;
        this.amount = 1;
        this.isRPGItem = false;
    }

    public GemData(int slotIndex, String type, String socketType, String itemId, int amount) {
        this.slotIndex = slotIndex;
        this.type = type;
        this.socketType = socketType;
        this.itemId = itemId;
        this.amount = amount;
        this.isRPGItem = false;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSocketType() {
        return socketType;
    }

    public void setSocketType(String socketType) {
        this.socketType = socketType;
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

    public String getSerializedLore() {
        return serializedLore;
    }

    public void setSerializedLore(String serializedLore) {
        this.serializedLore = serializedLore;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    public boolean isRPGItem() {
        return isRPGItem;
    }

    public void setRPGItem(boolean rpgItem) {
        isRPGItem = rpgItem;
    }

    public boolean isValid() {
        return slotIndex >= 0 && 
               type != null && !type.isEmpty() && 
               itemId != null && !itemId.isEmpty();
    }

    public ItemStack toItemStack() {
        // 使用 itemId 从 RPGItems 获取物品
        if (itemId != null && !itemId.isEmpty()) {
            cn.guangdian.socket.hook.RPGItemsHook rpgItemsHook = cn.guangdian.socket.hook.RPGItemsHook.getInstance();
            if (rpgItemsHook != null && rpgItemsHook.isEnabled()) {
                ItemStack rpgItem = rpgItemsHook.getRPGItem(itemId);
                if (rpgItem != null) {
                    rpgItem.setAmount(amount);
                    return rpgItem;
                }
            }
        }

        // 回退：使用 material 创建物品
        ItemStack item;

        if (material != null && !material.isEmpty()) {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                item = new ItemStack(mat, amount);
            } catch (IllegalArgumentException e) {
                item = new ItemStack(Material.DIAMOND, amount);
            }
        } else {
            item = new ItemStack(Material.DIAMOND, amount);
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }

            if (serializedLore != null && !serializedLore.isEmpty()) {
                java.util.List<String> lore = new java.util.ArrayList<>();
                String[] lines = serializedLore.split("\\|\\|");
                for (String line : lines) {
                    lore.add(line.replace("\\n", "\n"));
                }
                meta.setLore(lore);
            }

            // 宝石默认无法破坏
            meta.setUnbreakable(true);
            // 隐藏属性，与原始宝石一致
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static GemData fromItemStack(ItemStack item, int slotIndex, String gemType) {
        if (item == null) return null;
        
        GemData data = new GemData();
        data.setSlotIndex(slotIndex);
        data.setType(gemType);
        data.setSocketType(gemType);
        data.setAmount(item.getAmount());
        data.setMaterial(item.getType().name());
        
        // 检测 RPGItems 物品
        cn.guangdian.socket.hook.RPGItemsHook rpgItemsHook = cn.guangdian.socket.hook.RPGItemsHook.getInstance();
        if (rpgItemsHook != null && rpgItemsHook.isEnabled()) {
            String rpgItemId = rpgItemsHook.getRPGItemId(item);
            if (rpgItemId != null && !rpgItemId.isEmpty()) {
                data.setRPGItem(true); // 标记为可拆卸物品
                data.setItemId(rpgItemId);
                if (item.hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta.hasDisplayName()) {
                        data.setDisplayName(meta.getDisplayName());
                    }
                    if (meta.hasLore() && meta.getLore() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (String line : meta.getLore()) {
                            if (sb.length() > 0) sb.append("||");
                            sb.append(line.replace("\n", "\\n"));
                        }
                        data.setSerializedLore(sb.toString());
                    }
                }
                return data.isValid() ? data : null;
            }
        }
        
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            
            if (meta.hasDisplayName()) {
                data.setDisplayName(meta.getDisplayName());
            }
            
            if (meta.hasLore() && meta.getLore() != null) {
                StringBuilder sb = new StringBuilder();
                for (String line : meta.getLore()) {
                    if (sb.length() > 0) sb.append("||");
                    sb.append(line.replace("\n", "\\n"));
                }
                data.setSerializedLore(sb.toString());
            }
            
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
            data.setItemId(plainName != null && !plainName.isEmpty() ? plainName : item.getType().name());
        } else {
            data.setItemId(item.getType().name());
        }
        
        return data.isValid() ? data : null;
    }

    public GemData copy() {
        GemData copy = new GemData();
        copy.slotIndex = this.slotIndex;
        copy.type = this.type;
        copy.socketType = this.socketType;
        copy.itemId = this.itemId;
        copy.displayName = this.displayName;
        copy.material = this.material;
        copy.serializedLore = this.serializedLore;
        copy.amount = this.amount;
        copy.isRPGItem = this.isRPGItem;
        return copy;
    }

    @Override
    public String toString() {
        return "GemData{" +
                "slot=" + slotIndex +
                ", type='" + type + '\'' +
                ", socket='" + socketType + '\'' +
                ", id='" + itemId + '\'' +
                ", material='" + material + '\'' +
                ", amount=" + amount +
                '}';
    }
}
