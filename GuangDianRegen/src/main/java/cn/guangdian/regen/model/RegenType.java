package cn.guangdian.regen.model;

/**
 * 区域类型枚举
 */
public enum RegenType {
    MINE("矿场"),
    FOREST("林场"),
    FARM("农场");

    private final String displayName;

    RegenType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RegenType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINE;
        }
    }
}
