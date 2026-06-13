package cn.guangdian.battlepass.model;

import java.util.ArrayList;
import java.util.List;

public class BattlePassLevel {
    
    private int level;
    private int requiredExp;
    private BattlePassReward freeReward;
    private BattlePassReward premiumReward;
    
    public BattlePassLevel() {
    }
    
    public BattlePassLevel(int level, int requiredExp) {
        this.level = level;
        this.requiredExp = requiredExp;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getRequiredExp() {
        return requiredExp;
    }
    
    public void setRequiredExp(int requiredExp) {
        this.requiredExp = requiredExp;
    }
    
    public BattlePassReward getFreeReward() {
        return freeReward;
    }
    
    public void setFreeReward(BattlePassReward freeReward) {
        this.freeReward = freeReward;
    }
    
    public BattlePassReward getPremiumReward() {
        return premiumReward;
    }
    
    public void setPremiumReward(BattlePassReward premiumReward) {
        this.premiumReward = premiumReward;
    }
}
