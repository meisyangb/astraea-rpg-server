package cn.guangdian.npc.model;

public enum NPCType {
    SHOP("商店", "<green>[商店]", "EMERALD"),
    QUEST("任务", "<yellow>[任务]", "BOOK"),
    TELEPORT("传送", "<aqua>[传送]", "ENDER_PEARL"),
    BANK("银行", "<gold>[银行]", "GOLD_INGOT"),
    GUILD("公会", "<light_purple>[公会]", "DIAMOND"),
    TRAINER("训练师", "<red>[训练师]", "ENCHANTED_BOOK"),
    REPAIR("修理", "<gray>[修理]", "ANVIL"),
    IDENTIFY("鉴定", "<dark_purple>[鉴定]", "ENCHANTING_TABLE"),
    GENERAL("通用", "<white>", "PLAYER_HEAD");

    private final String displayName;
    private final String prefix;
    private final String iconMaterial;

    NPCType(String displayName, String prefix, String iconMaterial) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.iconMaterial = iconMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public static NPCType fromString(String name) {
        if (name == null) {
            return GENERAL;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
