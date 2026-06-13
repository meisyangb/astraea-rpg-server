package cn.guangdian.dungeon.model;

/**
 * 副本设置模型
 */
public class DungeonSettings {

    private final int maxPlayers;
    private final int minPlayers;
    private final int timeLimit;
    private final int cooldown;
    private final int maxDeaths;
    private final int reviveCooldown;
    private final int minLevel;
    private final int maxLevel;
    private final String permission;
    private final String iconMaterial;
    private final int recommendedLevel;

    public DungeonSettings(int maxPlayers, int minPlayers, int timeLimit, int cooldown,
                          int maxDeaths, int reviveCooldown, int minLevel, int maxLevel,
                          String permission, String iconMaterial, int recommendedLevel) {
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.timeLimit = timeLimit;
        this.cooldown = cooldown;
        this.maxDeaths = maxDeaths;
        this.reviveCooldown = reviveCooldown;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.permission = permission;
        this.iconMaterial = iconMaterial;
        this.recommendedLevel = recommendedLevel;
    }

    public int getMaxPlayers() { return maxPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public int getTimeLimit() { return timeLimit; }
    public int getCooldown() { return cooldown; }
    public int getMaxDeaths() { return maxDeaths; }
    public int getReviveCooldown() { return reviveCooldown; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getPermission() { return permission; }
    public String getIconMaterial() { return iconMaterial; }
    public int getRecommendedLevel() { return recommendedLevel; }
}
