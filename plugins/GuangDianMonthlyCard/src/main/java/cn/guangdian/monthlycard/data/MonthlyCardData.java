package cn.guangdian.monthlycard.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MonthlyCardData {
    
    private final UUID playerId;
    private String cardType;
    private long activateTime;
    private long expireTime;
    private final Set<String> claimedDays;
    private int totalClaimedDays;
    private long lastClaimTime;
    private int consecutiveDays;  // 连续签到天数
    private int makeupCount;      // 补签次数
    
    public MonthlyCardData(UUID playerId) {
        this.playerId = playerId;
        this.cardType = "none";
        this.activateTime = 0;
        this.expireTime = 0;
        this.claimedDays = new HashSet<>();
        this.totalClaimedDays = 0;
        this.lastClaimTime = 0;
        this.consecutiveDays = 0;
        this.makeupCount = 0;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getCardType() {
        return cardType;
    }
    
    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
    
    public long getActivateTime() {
        return activateTime;
    }
    
    public void setActivateTime(long activateTime) {
        this.activateTime = activateTime;
    }
    
    public long getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
    
    public Set<String> getClaimedDays() {
        return claimedDays;
    }
    
    public int getTotalClaimedDays() {
        return totalClaimedDays;
    }
    
    public void setTotalClaimedDays(int totalClaimedDays) {
        this.totalClaimedDays = totalClaimedDays;
    }
    
    public long getLastClaimTime() {
        return lastClaimTime;
    }
    
    public void setLastClaimTime(long lastClaimTime) {
        this.lastClaimTime = lastClaimTime;
    }

    public int getConsecutiveDays() {
        return consecutiveDays;
    }

    public void setConsecutiveDays(int consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }

    public int getMakeupCount() {
        return makeupCount;
    }

    public void setMakeupCount(int makeupCount) {
        this.makeupCount = makeupCount;
    }
    
    public boolean hasActiveCard() {
        return !"none".equals(cardType) && System.currentTimeMillis() < expireTime;
    }

    /**
     * 检查月卡是否激活（别名）
     */
    public boolean isActive() {
        return hasActiveCard();
    }

    /**
     * 获取月卡类型ID（别名）
     */
    public String getCardTypeId() {
        return cardType;
    }

    /**
     * 获取当前天数
     */
    public int getCurrentDay() {
        return getDaysSinceActivation();
    }

    /**
     * 检查今日是否已领取
     */
    public boolean hasClaimedToday() {
        return !canClaimToday();
    }

    /**
     * 检查某天是否已领取
     */
    public boolean hasClaimedDay(int day) {
        // 计算那天的日期字符串
        if (activateTime == 0) return false;
        LocalDate activationDate = Instant.ofEpochMilli(activateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        LocalDate targetDate = activationDate.plusDays(day - 1);
        return claimedDays.contains(targetDate.toString());
    }
    
    public boolean isExpired() {
        return !"none".equals(cardType) && System.currentTimeMillis() >= expireTime;
    }
    
    public long getRemainingDays() {
        if (!hasActiveCard()) return 0;
        long remaining = expireTime - System.currentTimeMillis();
        return Math.max(0, remaining / (24 * 60 * 60 * 1000));
    }
    
    public int getRemainingDaysInt() {
        return (int) getRemainingDays();
    }
    
    public boolean canClaimToday() {
        if (!hasActiveCard()) return false;
        String todayKey = getTodayKey();
        return !claimedDays.contains(todayKey);
    }
    
    public void markClaimedToday() {
        String todayKey = getTodayKey();
        claimedDays.add(todayKey);
        totalClaimedDays++;
        lastClaimTime = System.currentTimeMillis();
    }
    
    private String getTodayKey() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return today.toString();
    }
    
    public int getDaysSinceActivation() {
        if (activateTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - activateTime;
        return (int) (elapsed / (24 * 60 * 60 * 1000)) + 1;
    }
    
    public void clear() {
        this.cardType = "none";
        this.activateTime = 0;
        this.expireTime = 0;
        this.claimedDays.clear();
        this.totalClaimedDays = 0;
        this.lastClaimTime = 0;
    }
    
    public static MonthlyCardData fromStorage(UUID playerId, String cardType, long activateTime, 
                                               long expireTime, Set<String> claimedDays, 
                                               int totalClaimedDays, long lastClaimTime) {
        MonthlyCardData data = new MonthlyCardData(playerId);
        data.setCardType(cardType);
        data.setActivateTime(activateTime);
        data.setExpireTime(expireTime);
        data.getClaimedDays().addAll(claimedDays);
        data.setTotalClaimedDays(totalClaimedDays);
        data.setLastClaimTime(lastClaimTime);
        return data;
    }
}
