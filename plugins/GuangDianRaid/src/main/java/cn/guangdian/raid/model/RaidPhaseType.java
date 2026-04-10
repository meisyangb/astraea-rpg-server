package cn.guangdian.raid.model;

public enum RaidPhaseType {
    WAITING("等待中"),
    INFILTRATION("潜入阶段"),
    SEARCH("搜索阶段"),
    COMBAT("战斗阶段"),
    EXTRACTION("撤离阶段"),
    COMPLETED("已完成"),
    FAILED("失败");

    private final String displayName;

    RaidPhaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
