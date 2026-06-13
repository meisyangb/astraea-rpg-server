package cn.guangdian.gift.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 礼包领取条件
 */
public class GiftConditions {

    // 最大领取次数 (-1 表示无限)
    private int maxClaims = -1;

    // 需要的权限
    private String permission = "";

    // 需要的最低等级
    private int minLevel = 0;

    // 需要消耗的物品 (格式: 物品名:数量)
    private List<String> costItems = new ArrayList<>();

    // 需要消耗的金币
    private int costMoney = 0;

    // 冷却时间 (秒)
    private long cooldown = 0;

    public int getMaxClaims() {
        return maxClaims;
    }

    public void setMaxClaims(int maxClaims) {
        this.maxClaims = maxClaims;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public List<String> getCostItems() {
        return costItems;
    }

    public void setCostItems(List<String> costItems) {
        this.costItems = costItems;
    }

    public int getCostMoney() {
        return costMoney;
    }

    public void setCostMoney(int costMoney) {
        this.costMoney = costMoney;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
}
