package cn.guangdian.dungeon.model;

public enum PartyState {
    CREATED("已创建"),
    FORMING("组建中"),
    READY("已准备"),
    IN_DUNGEON("副本中"),
    DISBANDED("已解散");

    private final String displayName;

    PartyState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
