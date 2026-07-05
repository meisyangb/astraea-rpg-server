package cn.guangdian.forge.model;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 锻造品质系统
 * <p>
 * 品质共 6 档，每档有独立的属性倍率：<br>
 * 粗糙 0.70x / 普通 1.00x / 优秀 1.15x / 精良 1.30x / 史诗 1.50x / 传说 1.80x
 * </p>
 *
 * <p>品质分公式：score = forgeLevel × 2 + random(0, 30)</p>
 */
public class ForgeQuality {

    // PDC Key（命名空间: guangdianforge）
    public static final NamespacedKey KEY_QUALITY_TIER = new NamespacedKey("guangdianforge", "quality_tier");
    public static final NamespacedKey KEY_QUALITY_MULTIPLIER = new NamespacedKey("guangdianforge", "quality_multiplier");
    public static final NamespacedKey KEY_QUALITY_STRENGTH = new NamespacedKey("guangdianforge", "quality_strength");

    private static final Random RANDOM = new Random();

    // ═══════════════════════════════════════════════════
    //  品质枚举
    // ═══════════════════════════════════════════════════
    public enum Tier {
        ROUGH(   0,   "粗糙", "gray",         0.70),
        NORMAL(  1,   "普通", "white",        1.00),
        EXCELLENT(2,  "优秀", "green",        1.15),
        SUPERIOR(3,  "精良", "blue",          1.30),
        EPIC(    4,  "史诗", "dark_purple",   1.50),
        LEGENDARY(5, "传说", "gold",          1.80);

        private final int id;
        private final String display;
        private final String color;
        private final double multiplier;

        Tier(int id, String display, String color, double multiplier) {
            this.id = id;
            this.display = display;
            this.color = color;
            this.multiplier = multiplier;
        }

        public int getId() { return id; }
        public String getDisplay() { return display; }
        public String getColor() { return color; }
        public double getMultiplier() { return multiplier; }

        /** MiniMessage 格式的颜色标签 */
        public String getColorTag() {
            return "<" + color + ">";
        }

        public static Tier fromId(int id) {
            for (Tier t : values()) {
                if (t.id == id) return t;
            }
            return NORMAL;
        }

        /** 根据品质分映射品质档位 */
        public static Tier fromScore(int score) {
            if (score <= 8)  return ROUGH;
            if (score <= 18) return NORMAL;
            if (score <= 28) return EXCELLENT;
            if (score <= 38) return SUPERIOR;
            if (score <= 46) return EPIC;
            return LEGENDARY;
        }
    }

    // ═══════════════════════════════════════════════════
    //  实例字段
    // ═══════════════════════════════════════════════════
    private final Tier tier;
    private final int strength; // 0-100，用于强度条显示

    public ForgeQuality(Tier tier, int strength) {
        this.tier = tier;
        this.strength = Math.max(0, Math.min(100, strength));
    }

    public Tier getTier() { return tier; }
    public double getMultiplier() { return tier.getMultiplier(); }
    public int getStrength() { return strength; }

    // ═══════════════════════════════════════════════════
    //  随机生成
    // ═══════════════════════════════════════════════════

    /**
     * 根据锻造等级随机生成品质
     * @param forgeLevel 玩家锻造等级
     * @return 随机品质
     */
    public static ForgeQuality generate(int forgeLevel) {
        int score = forgeLevel * 2 + RANDOM.nextInt(31); // 0~30 随机
        Tier tier = Tier.fromScore(score);

        // 强度 = (score / 最大可能分) * 100，再根据品质档位微调
        int maxPossible = Math.min(10, forgeLevel) * 2 + 30;
        int baseStrength = Math.min(100, score * 100 / maxPossible);

        // 在品质档位范围内微调 ±5
        int adjustedStrength = baseStrength + RANDOM.nextInt(11) - 5;
        adjustedStrength = Math.max(0, Math.min(100, adjustedStrength));

        return new ForgeQuality(tier, adjustedStrength);
    }

    // ═══════════════════════════════════════════════════
    //  写入 PDC
    // ═══════════════════════════════════════════════════

    /**
     * 将品质数据写入物品 PDC
     */
    public void writeToItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_QUALITY_TIER, PersistentDataType.INTEGER, tier.getId());
        pdc.set(KEY_QUALITY_MULTIPLIER, PersistentDataType.DOUBLE, tier.getMultiplier());
        pdc.set(KEY_QUALITY_STRENGTH, PersistentDataType.INTEGER, strength);
        item.setItemMeta(meta);
    }

    /**
     * 将品质信息追加到物品 Lore 末尾
     * 使用 MiniMessage 格式（兼容 RPGItems 的 Lore 格式）
     */
    public void appendLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<net.kyori.adventure.text.Component> lore = meta.hasLore()
            ? new ArrayList<>(meta.lore())
            : new ArrayList<>();

        String color = tier.getColorTag();
        String darkGray = "<dark_gray>";

        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<i:false>" + darkGray + "<strikethrough>━━━━━━━━━━━━━━━━━━━━</strikethrough>"));

        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<i:false><gold><bold>【锻造品质】</bold></gold>"));

        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<i:false><gold>品质: " + color + "<bold>" + tier.getDisplay() + "</bold></" + tier.getColor() + ">"
                + " <gray>(属性 ×" + String.format("%.2f", tier.getMultiplier()) + ")</gray>"));

        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<i:false><gold>强度: " + buildStrengthBar()));

        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<i:false>" + darkGray + "<strikethrough>━━━━━━━━━━━━━━━━━━━━</strikethrough>"));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    // ═══════════════════════════════════════════════════
    //  从 PDC 读取
    // ═══════════════════════════════════════════════════

    /**
     * 从物品 PDC 读取品质数据
     * @return 品质对象，若没有品质数据则返回 null
     */
    public static ForgeQuality readFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(KEY_QUALITY_TIER, PersistentDataType.INTEGER)) return null;

        Integer tierId = pdc.get(KEY_QUALITY_TIER, PersistentDataType.INTEGER);
        Integer strength = pdc.getOrDefault(KEY_QUALITY_STRENGTH, PersistentDataType.INTEGER, 50);

        if (tierId == null) return null;
        Tier tier = Tier.fromId(tierId);
        return new ForgeQuality(tier, strength);
    }

    /**
     * 检查物品是否已有锻造品质
     */
    public static boolean hasQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_QUALITY_TIER, PersistentDataType.INTEGER);
    }

    // ═══════════════════════════════════════════════════
    //  强度条
    // ═══════════════════════════════════════════════════

    private static final int BAR_LENGTH = 15;

    /**
     * 构建可视化强度条 [|||||||||||||||]
     */
    private String buildStrengthBar() {
        int filled = strength * BAR_LENGTH / 100;
        int empty = BAR_LENGTH - filled;

        StringBuilder sb = new StringBuilder();
        sb.append(tier.getColorTag()).append("[");
        for (int i = 0; i < filled; i++) sb.append("|");
        if (empty > 0) {
            sb.append("<dark_gray>");
            for (int i = 0; i < empty; i++) sb.append("|");
        }
        sb.append(tier.getColorTag()).append("]");
        sb.append("<gray> ").append(strength).append("/100");

        return sb.toString();
    }
}
