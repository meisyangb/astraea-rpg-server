package cn.guangdian.mobs.model;

/**
 * 怪物选项配置
 * 对应 MythicMobs 的 Options 配置
 */
public class MobOptions {

    private boolean alwaysShowName = false;     // 始终显示名称
    private boolean preventOtherDrops = false;  // 阻止其他掉落
    private boolean preventSlimeSplit = false;  // 阻止史莱姆分裂
    private double movementSpeed = -1;          // 移动速度覆盖
    private double knockbackResistance = -1;    // 击退抗性
    private double maxCombatDistance = -1;      // 最大战斗距离
    private int size = -1;                      // 体型大小 (史莱姆等)
    private boolean showBossBar = false;        // 显示Boss血条
    private String bossBarColor = "RED";        // Boss血条颜色
    private String bossBarStyle = "SOLID";      // Boss血条样式

    // MythicMobs 新增支持
    private boolean preventSunBurn = false;     // 防止阳光燃烧
    private boolean invisible = false;          // 隐形
    private int noDamageTicks = 0;              // 无敌时间(tick)
    private boolean despawn = true;             // 是否自然消失

    // 伪装配置
    private DisguiseSettings disguise;          // 伪装设置

    public MobOptions() {
        this.disguise = new DisguiseSettings();
    }

    // Getters and Setters
    public boolean isAlwaysShowName() { return alwaysShowName; }
    public void setAlwaysShowName(boolean alwaysShowName) { this.alwaysShowName = alwaysShowName; }

    public boolean isPreventOtherDrops() { return preventOtherDrops; }
    public void setPreventOtherDrops(boolean preventOtherDrops) { this.preventOtherDrops = preventOtherDrops; }

    public boolean isPreventSlimeSplit() { return preventSlimeSplit; }
    public void setPreventSlimeSplit(boolean preventSlimeSplit) { this.preventSlimeSplit = preventSlimeSplit; }

    public double getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(double movementSpeed) { this.movementSpeed = movementSpeed; }

    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double knockbackResistance) { this.knockbackResistance = knockbackResistance; }

    public double getMaxCombatDistance() { return maxCombatDistance; }
    public void setMaxCombatDistance(double maxCombatDistance) { this.maxCombatDistance = maxCombatDistance; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isShowBossBar() { return showBossBar; }
    public void setShowBossBar(boolean showBossBar) { this.showBossBar = showBossBar; }

    public String getBossBarColor() { return bossBarColor; }
    public void setBossBarColor(String bossBarColor) { this.bossBarColor = bossBarColor; }

    public String getBossBarStyle() { return bossBarStyle; }
    public void setBossBarStyle(String bossBarStyle) { this.bossBarStyle = bossBarStyle; }

    // MythicMobs 新增 Getter/Setter
    public boolean isPreventSunBurn() { return preventSunBurn; }
    public void setPreventSunBurn(boolean preventSunBurn) { this.preventSunBurn = preventSunBurn; }

    public boolean isInvisible() { return invisible; }
    public void setInvisible(boolean invisible) { this.invisible = invisible; }

    public int getNoDamageTicks() { return noDamageTicks; }
    public void setNoDamageTicks(int noDamageTicks) { this.noDamageTicks = noDamageTicks; }

    public boolean isDespawn() { return despawn; }
    public void setDespawn(boolean despawn) { this.despawn = despawn; }

    public DisguiseSettings getDisguise() { return disguise; }
    public void setDisguise(DisguiseSettings disguise) { this.disguise = disguise; }

    /**
     * 伪装设置
     */
    public static class DisguiseSettings {
        private boolean enabled = false;        // 是否启用伪装
        private String type = "ZOMBIE";         // 伪装成的实体类型
        private String playerName = null;       // 伪装成玩家时的名称
        private String skin = null;             // 伪装成玩家时的皮肤

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getSkin() { return skin; }
        public void setSkin(String skin) { this.skin = skin; }
    }
}
