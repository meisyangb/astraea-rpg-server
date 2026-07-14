package cn.guangdian.enhance.stone;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * 强化石类型枚举
 * 通过 RPGItems PDC (rpgitems:attrs 复合存储) 识别强化石
 * 所有石头都需放入 GUI 对应槽位
 */
public enum EnhanceStoneType {
    /** 基础强化材料, +1~+5 */
    QUENCH("淬炼石", Material.REDSTONE, "淬炼石",
           StoneEffect.MATERIAL, 0.0, "强化装备的必需材料"),

    /** 高级强化材料, +6~+10 */
    QUENCH_ADV("强化淬炼石", Material.REDSTONE_BLOCK, "强化淬炼石",
           StoneEffect.MATERIAL, 0.0, "高纯度强化材料"),

    /** 顶级强化材料, +11~+15 */
    QUENCH_SUP("极品淬炼石", Material.NETHERITE_SCRAP, "极品淬炼石",
           StoneEffect.MATERIAL, 0.0, "顶级强化材料，蕴含虚空之力"),

    /** 成功率+25% */
    LUCKY("幸运石", Material.LAPIS_LAZULI, "幸运石",
           StoneEffect.SUCCESS_BONUS, 0.25, "增加25%强化成功率"),

    /** 防止降级 */
    PROTECT("保护石", Material.AMETHYST_SHARD, "保护石",
           StoneEffect.PREVENT_DEGRADE, 0.0, "失败时防止装备降级"),

    /** 防止破碎 */
    SAFETY("安全石", Material.ECHO_SHARD, "安全石",
           StoneEffect.PREVENT_DESTROY, 0.0, "失败时防止装备破碎"),

    /** 100%成功 */
    GUARANTEE("必成石", Material.NETHER_STAR, "必成石",
           StoneEffect.GUARANTEE, 1.0, "100%强化成功");

    private final String stoneId;
    private final Material material;
    private final String displayName;
    private final StoneEffect effect;
    private final double value;
    private final String description;

    // RPGItems PDC key
    private static final NamespacedKey STONE_KEY = new NamespacedKey("rpgitems", "enhance_stone");

    EnhanceStoneType(String stoneId, Material material, String displayName,
                     StoneEffect effect, double value, String description) {
        this.stoneId = stoneId;
        this.material = material;
        this.displayName = displayName;
        this.effect = effect;
        this.value = value;
        this.description = description;
    }

    /** 通过 PDC 检测物品是否为对应强化石 */
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        try {
            // 直接从 rpgitems:enhance_stone 读取（RPGItems 已修复）
            String id = meta.getPersistentDataContainer().get(STONE_KEY, PersistentDataType.STRING);
            return stoneId.equals(id);
        } catch (Exception e) {
            return false;
        }
    }

    /** 通过 PDC 检测物品是哪种强化石 */
    public static Optional<EnhanceStoneType> detect(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        for (EnhanceStoneType t : values()) {
            if (t.matches(item)) return Optional.of(t);
        }
        return Optional.empty();
    }

    /** 是否为淬炼石类材料 */
    public boolean isMaterial() {
        return effect == StoneEffect.MATERIAL;
    }

    /** 应用效果到成功率 */
    public double apply(double baseRate) {
        return switch (effect) {
            case SUCCESS_BONUS -> Math.min(1.0, baseRate + value);
            case GUARANTEE -> 1.0;
            default -> baseRate;
        };
    }

    public String getStoneId() { return stoneId; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public StoneEffect getEffect() { return effect; }
    public double getValue() { return value; }
    public String getDescription() { return description; }
}
