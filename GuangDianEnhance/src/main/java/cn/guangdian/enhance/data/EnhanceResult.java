package cn.guangdian.enhance.data;

public enum EnhanceResult {
    
    SUCCESS(true, "强化成功!"),
    FAILED_NO_CHANGE(false, "强化失败，等级不变"),
    FAILED_DEGRADE(false, "强化失败，等级下降"),
    FAILED_DESTROY(false, "强化失败，装备已破碎"),
    NOT_ENHANCEABLE(false, "该物品无法强化"),
    INSUFFICIENT_MATERIAL(false, "材料不足"),
    INSUFFICIENT_MONEY(false, "金币不足"),
    MAX_LEVEL_REACHED(false, "已达到最高强化等级"),
    IN_COOLDOWN(false, "强化冷却中");

    private final boolean success;
    private final String defaultMessage;

    EnhanceResult(boolean success, String defaultMessage) {
        this.success = success;
        this.defaultMessage = defaultMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isFailed() {
        return !success;
    }

    public boolean isDestructive() {
        return this == FAILED_DESTROY;
    }

    public boolean isDegrade() {
        return this == FAILED_DEGRADE;
    }
}
