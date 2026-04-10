package cn.guangdian.npc.model;

public enum NPCType {
    SHOP("商店", "§a[商店]", "EMERALD"),
    QUEST("任务", "§e[任务]", "BOOK"),
    TELEPORT("传送", "§b[传送]", "ENDER_PEARL"),
    BANK("银行", "§6[银行]", "GOLD_INGOT"),
    GUILD("公会", "§d[公会]", "DIAMOND"),
    TRAINER("训练师", "§c[训练师]", "ENCHANTED_BOOK"),
    REPAIR("修理", "§7[修理]", "ANVIL"),
    IDENTIFY("鉴定", "§5[鉴定]", "ENCHANTING_TABLE"),
    GENERAL("通用", "§f", "PLAYER_HEAD");

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
