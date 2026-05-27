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

    public MobOptions() {}

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
}
