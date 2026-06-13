package cn.guangdian.dungeon.model.stage;

import java.util.ArrayList;
import java.util.List;

public class MobSpawn {
    private String mobId;
    private String spawnPointId;
    private int amount;
    private int delaySeconds;
    private double radius;
    private boolean isBoss;
    private boolean healthBar;
    private boolean announceSkills;
    private int level;
    private List<BossHpTrigger> bossHpTriggers;

    public MobSpawn() {
        this.bossHpTriggers = new ArrayList<>();
    }

    public String getMobId() { return mobId; }
    public void setMobId(String mobId) { this.mobId = mobId; }

    public String getSpawnPointId() { return spawnPointId; }
    public void setSpawnPointId(String spawnPointId) { this.spawnPointId = spawnPointId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public boolean isBoss() { return isBoss; }
    public void setBoss(boolean boss) { isBoss = boss; }

    public boolean isHealthBar() { return healthBar; }
    public void setHealthBar(boolean healthBar) { this.healthBar = healthBar; }

    public boolean isAnnounceSkills() { return announceSkills; }
    public void setAnnounceSkills(boolean announceSkills) { this.announceSkills = announceSkills; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public List<BossHpTrigger> getBossHpTriggers() { return bossHpTriggers; }
    public void setBossHpTriggers(List<BossHpTrigger> bossHpTriggers) { this.bossHpTriggers = bossHpTriggers; }
}
