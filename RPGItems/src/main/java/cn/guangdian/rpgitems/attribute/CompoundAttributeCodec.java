package cn.guangdian.rpgitems.attribute;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.nio.ByteBuffer;

/**
 * 复合属性编解码器
 *
 * 将 48 个 double 属性序列化为单个 byte[]，只占 1 个 PDC key。
 *
 * 优化前：每个物品 15-20 个 PDC key-value 对（每个 key 约 100-200 字节 NBT 开销）
 * 优化后：每个物品 1 个 PDC key，384 字节 byte[]（48 × 8 bytes）
 *
 * 内存节省：50 玩家 × 5000 物品 × (15 key × 150 字节 - 384 字节) ≈ 50MB -> 10MB
 *
 * 二进制格式：48 个连续的 double（8 bytes each），共 384 bytes
 * 序号顺序固定，对应 ItemTemplate.Attributes 的字段顺序
 */
public final class CompoundAttributeCodec {

    /** 属性总数 */
    public static final int COUNT = 48;

    /** 序号常量 - 与 ItemTemplate.Attributes 字段顺序一致 */
    public static final int
        ATTACK_MIN = 0,          ATTACK_MAX = 1,
        DEFENSE_MIN = 2,         DEFENSE_MAX = 3,
        MAX_HEALTH = 4,          HEALTH_REGEN = 5,
        CRIT_CHANCE = 6,         CRIT_DAMAGE = 7,
        LIFESTEAL_CHANCE = 8,    LIFESTEAL_MULTIPLIER = 9,
        DODGE_CHANCE = 10,       PARRY_CHANCE = 11,
        MOVE_SPEED = 12,         DAMAGE_REDUCTION = 13,
        PVP_ATTACK_MIN = 14,     PVP_ATTACK_MAX = 15,
        PVP_DEFENSE_MIN = 16,    PVP_DEFENSE_MAX = 17,
        CRIT_RESIST = 18,        CRIT_DAMAGE_RESIST = 19,
        LIFESTEAL_RESIST = 20,
        ARMOR = 21,              ARMOR_STRENGTH = 22,
        ARMOR_PENETRATION = 23,  DEFENSE_PENETRATION = 24,
        DAMAGE_REFLECT = 25,     REFLECT_RATIO = 26,
        POISON_CHANCE = 27,      FREEZE_CHANCE = 28,
        BLIND_CHANCE = 29,       BURN_CHANCE = 30,
        SCORCH_CHANCE = 31,      IGNITE_CHANCE = 32,
        SLOW_CHANCE = 33,
        FIRE_RESIST = 34,        FALL_RESIST = 35,
        DROWNING_RESIST = 36,    POISON_RESIST = 37,
        WITHER_RESIST = 38,      LAVA_RESIST = 39,
        MAGIC_RESIST = 40,       EXPLOSION_RESIST = 41,
        PROJECTILE_RESIST = 42,
        KNOCKBACK_RESIST = 43,   EXP_BONUS = 44,
        HEALTH_REGEN_PERCENT = 45,
        DODGE_REFLECT_CHANCE = 46, DODGE_REFLECT_RATIO = 47;

    /** 复合属性的 PDC key */
    public static final NamespacedKey KEY_COMPOUND = new NamespacedKey("rpgitems", "attrs");

    /** 强化基础属性的 PDC key（保存强化前的原始值） */
    public static final NamespacedKey KEY_BASE_COMPOUND = new NamespacedKey("rpgitems", "base_attrs");

    /** byte[] 大小 */
    public static final int BYTE_SIZE = COUNT * 8; // 384

    private CompoundAttributeCodec() {}

    /**
     * 序列化 double[] 为 byte[]
     *
     * @param attrs 长度为 48 的 double 数组
     * @return 384 字节的 byte[]
     */
    public static byte[] serialize(double[] attrs) {
        ByteBuffer bb = ByteBuffer.allocate(BYTE_SIZE);
        for (int i = 0; i < COUNT; i++) {
            bb.putDouble(i < attrs.length ? attrs[i] : 0.0);
        }
        return bb.array();
    }

    /**
     * 反序列化 byte[] 为 double[]
     *
     * @param data PDC 中读取的 byte[]
     * @return 长度为 48 的 double 数组，如果 data 无效则返回全零数组
     */
    public static double[] deserialize(byte[] data) {
        double[] attrs = new double[COUNT];
        if (data == null || data.length < BYTE_SIZE) {
            return attrs;
        }
        ByteBuffer bb = ByteBuffer.wrap(data);
        for (int i = 0; i < COUNT; i++) {
            attrs[i] = bb.getDouble();
        }
        return attrs;
    }
}
