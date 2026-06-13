package cn.guangdian.killaura.model;

public enum TargetStrategy {

    NEAREST("nearest", "最近目标"),
    LOWEST_HEALTH("lowest_health", "最低血量"),
    HIGHEST_AGGRO("highest_aggro", "最高仇恨");

    private final String key;
    private final String displayName;

    TargetStrategy(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TargetStrategy fromKey(String key) {
        for (TargetStrategy strategy : values()) {
            if (strategy.key.equalsIgnoreCase(key)) {
                return strategy;
            }
        }
        return NEAREST;
    }
}
