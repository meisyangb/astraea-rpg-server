package cn.guangdian.mobs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 怪物AI配置
 * 参考 MythicMobs 的 AI 系统
 */
public class MobAI {

    // AI 目标选择器（决定攻击谁）
    private List<String> targetSelectors;

    // AI 目标（决定做什么）
    private List<String> aiGoals;

    // AI 设置
    private AISettings settings;

    // 仇恨系统设置
    private ThreatSettings threatSettings;

    public MobAI() {
        this.targetSelectors = new ArrayList<>();
        this.aiGoals = new ArrayList<>();
        this.settings = new AISettings();
        this.threatSettings = new ThreatSettings();
    }

    // Getters and Setters
    public List<String> getTargetSelectors() { return targetSelectors; }
    public void setTargetSelectors(List<String> targetSelectors) { this.targetSelectors = targetSelectors; }

    public List<String> getAiGoals() { return aiGoals; }
    public void setAiGoals(List<String> aiGoals) { this.aiGoals = aiGoals; }

    public AISettings getSettings() { return settings; }
    public void setSettings(AISettings settings) { this.settings = settings; }

    public ThreatSettings getThreatSettings() { return threatSettings; }
    public void setThreatSettings(ThreatSettings threatSettings) { this.threatSettings = threatSettings; }

    /**
     * AI 基础设置
     */
    public static class AISettings {
        private boolean canSwim = true;           // 会游泳
        private boolean canBreakDoors = false;    // 会破门
        private boolean canOpenDoors = false;     // 会开门
        private boolean canPickUpItems = false;   // 会捡物品
        private boolean avoidWater = false;       // 避水
        private boolean avoidSun = false;         // 避日
        private boolean canFly = false;           // 会飞
        private boolean canClimb = false;         // 会爬
        private double followRange = 32.0;        // 追踪范围
        private double wanderSpeed = 1.0;         // 游荡速度
        private double attackSpeed = 1.0;         // 攻击速度
        private double retreatHealthPercent = 0.0; // 撤退血量百分比

        // Getters and Setters
        public boolean isCanSwim() { return canSwim; }
        public void setCanSwim(boolean canSwim) { this.canSwim = canSwim; }

        public boolean isCanBreakDoors() { return canBreakDoors; }
        public void setCanBreakDoors(boolean canBreakDoors) { this.canBreakDoors = canBreakDoors; }

        public boolean isCanOpenDoors() { return canOpenDoors; }
        public void setCanOpenDoors(boolean canOpenDoors) { this.canOpenDoors = canOpenDoors; }

        public boolean isCanPickUpItems() { return canPickUpItems; }
        public void setCanPickUpItems(boolean canPickUpItems) { this.canPickUpItems = canPickUpItems; }

        public boolean isAvoidWater() { return avoidWater; }
        public void setAvoidWater(boolean avoidWater) { this.avoidWater = avoidWater; }

        public boolean isAvoidSun() { return avoidSun; }
        public void setAvoidSun(boolean avoidSun) { this.avoidSun = avoidSun; }

        public boolean isCanFly() { return canFly; }
        public void setCanFly(boolean canFly) { this.canFly = canFly; }

        public boolean isCanClimb() { return canClimb; }
        public void setCanClimb(boolean canClimb) { this.canClimb = canClimb; }

        public double getFollowRange() { return followRange; }
        public void setFollowRange(double followRange) { this.followRange = followRange; }

        public double getWanderSpeed() { return wanderSpeed; }
        public void setWanderSpeed(double wanderSpeed) { this.wanderSpeed = wanderSpeed; }

        public double getAttackSpeed() { return attackSpeed; }
        public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }

        public double getRetreatHealthPercent() { return retreatHealthPercent; }
        public void setRetreatHealthPercent(double retreatHealthPercent) { this.retreatHealthPercent = retreatHealthPercent; }
    }

    /**
     * 仇恨系统设置
     */
    public static class ThreatSettings {
        private double threatRadius = 16.0;       // 仇恨范围
        private double threatDecayRate = 1.0;     // 仇恨衰减率
        private boolean useThreatTable = true;    // 使用仇恨表
        private boolean tauntImmune = false;      // 免疫嘲讽
        private boolean ignoreTargetsOutOfRange = true; // 忽略范围外目标
        private double targetSwitchThreshold = 1.2; // 目标切换阈值

        // Getters and Setters
        public double getThreatRadius() { return threatRadius; }
        public void setThreatRadius(double threatRadius) { this.threatRadius = threatRadius; }

        public double getThreatDecayRate() { return threatDecayRate; }
        public void setThreatDecayRate(double threatDecayRate) { this.threatDecayRate = threatDecayRate; }

        public boolean isUseThreatTable() { return useThreatTable; }
        public void setUseThreatTable(boolean useThreatTable) { this.useThreatTable = useThreatTable; }

        public boolean isTauntImmune() { return tauntImmune; }
        public void setTauntImmune(boolean tauntImmune) { this.tauntImmune = tauntImmune; }

        public boolean isIgnoreTargetsOutOfRange() { return ignoreTargetsOutOfRange; }
        public void setIgnoreTargetsOutOfRange(boolean ignoreTargetsOutOfRange) { this.ignoreTargetsOutOfRange = ignoreTargetsOutOfRange; }

        public double getTargetSwitchThreshold() { return targetSwitchThreshold; }
        public void setTargetSwitchThreshold(double targetSwitchThreshold) { this.targetSwitchThreshold = targetSwitchThreshold; }
    }
}
