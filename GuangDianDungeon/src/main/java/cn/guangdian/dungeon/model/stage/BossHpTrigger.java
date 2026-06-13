package cn.guangdian.dungeon.model.stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss 血量阈值触发器
 * 当 Boss 血量降至指定百分比时，执行生成和消息动作
 */
public class BossHpTrigger {
    private double hpPercent;          // 触发的血量百分比 (0-100)
    private List<MobSpawn> spawns;      // 触发时生成的怪物
    private String message;             // 触发时的广播消息
    private boolean triggered = false;  // 是否已触发

    public BossHpTrigger() {
        this.spawns = new ArrayList<>();
    }

    public BossHpTrigger(double hpPercent) {
        this.hpPercent = hpPercent;
        this.spawns = new ArrayList<>();
    }

    public double getHpPercent() { return hpPercent; }
    public void setHpPercent(double hpPercent) { this.hpPercent = hpPercent; }

    public List<MobSpawn> getSpawns() { return spawns; }
    public void setSpawns(List<MobSpawn> spawns) { this.spawns = spawns; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }

    /**
     * 检查当前血量是否触发该阈值
     */
    public boolean shouldTrigger(double currentHpPercent) {
        return !triggered && currentHpPercent <= hpPercent;
    }
}
