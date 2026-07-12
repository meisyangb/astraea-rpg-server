package cn.guangdian.enhance.util;

import org.bukkit.NamespacedKey;

import java.nio.ByteBuffer;

/**
 * 复合属性编解码器
 *
 * 将 48 个 double 属性序列化为单个 byte[]，只占 1 个 PDC key。
 * 序号顺序与 RPGItems ItemTemplate.Attributes 字段顺序一致。
 */
public final class CompoundAttributeCodec {

    public static final int COUNT = 48;

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

    public static final NamespacedKey KEY_COMPOUND = new NamespacedKey("rpgitems", "attrs");
    public static final NamespacedKey KEY_BASE_COMPOUND = new NamespacedKey("rpgitems", "base_attrs");

    public static final int BYTE_SIZE = COUNT * 8;

    private CompoundAttributeCodec() {}

    public static byte[] serialize(double[] attrs) {
        ByteBuffer bb = ByteBuffer.allocate(BYTE_SIZE);
        for (int i = 0; i < COUNT; i++) {
            bb.putDouble(i < attrs.length ? attrs[i] : 0.0);
        }
        return bb.array();
    }

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
