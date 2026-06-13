package cn.guangdian.classsystem.model;

/**
 * 转职等级枚举，统一管理等级对应的倍率和名称
 */
public enum AdvancementLevel {
    BASE(0, "基础", 1.0),
    FIRST(1, "一转", 1.2),
    SECOND(2, "二转", 1.5),
    THIRD(3, "三转", 2.0),
    DIVINE(4, "神级", 3.0);

    public final int level;
    public final String name;
    public final double multiplier;

    AdvancementLevel(int level, String name, double multiplier) {
        this.level = level;
        this.name = name;
        this.multiplier = multiplier;
    }

    private static final AdvancementLevel[] BY_LEVEL = new AdvancementLevel[5];
    static {
        for (AdvancementLevel al : values()) {
            BY_LEVEL[al.level] = al;
        }
    }

    public static AdvancementLevel fromLevel(int level) {
        if (level >= 0 && level < BY_LEVEL.length) return BY_LEVEL[level];
        return BASE;
    }

    public static double getMultiplier(int level) {
        return fromLevel(level).multiplier;
    }

    public static String getName(int level) {
        return fromLevel(level).name;
    }
}
