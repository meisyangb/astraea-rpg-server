package cn.guangdian.cavefu.permission;

/**
 * 洞府权限等级
 */
public enum PermissionType {
    OWNER("洞主", 3),
    MEMBER("成员", 2),
    VISITOR("访客", 1);

    private final String displayName;
    private final int priority;

    PermissionType(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAtLeast(PermissionType other) {
        return this.priority >= other.priority;
    }

    public boolean isHigherThan(PermissionType other) {
        return this.priority > other.priority;
    }

    public static PermissionType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (Exception e) {
            return VISITOR;
        }
    }
}