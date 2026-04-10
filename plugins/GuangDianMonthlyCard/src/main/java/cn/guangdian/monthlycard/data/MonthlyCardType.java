package cn.guangdian.monthlycard.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthlyCardType {
    
    private final String id;
    private String displayName;
    private String description;
    private int durationDays;
    private long price;
    private String currencyType;
    private List<DailyReward> dailyRewards;
    private List<ItemStack> instantRewards;
    private Map<String, Object> extraData;
    
    public MonthlyCardType(String id) {
        this.id = id;
        this.displayName = id;
        this.description = "";
        this.durationDays = 30;
        this.price = 0;
        this.currencyType = "points";
        this.dailyRewards = new ArrayList<>();
        this.instantRewards = new ArrayList<>();
        this.extraData = new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getDurationDays() {
        return durationDays;
    }
    
    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }
    
    public long getPrice() {
        return price;
    }
    
    public void setPrice(long price) {
        this.price = price;
    }
    
    public String getCurrencyType() {
        return currencyType;
    }
    
    public void setCurrencyType(String currencyType) {
        this.currencyType = currencyType;
    }
    
    public List<DailyReward> getDailyRewards() {
        return dailyRewards;
    }
    
    public void setDailyRewards(List<DailyReward> dailyRewards) {
        this.dailyRewards = dailyRewards;
    }
    
    public List<ItemStack> getInstantRewards() {
        return instantRewards;
    }
    
    public void setInstantRewards(List<ItemStack> instantRewards) {
        this.instantRewards = instantRewards;
    }
    
    public Map<String, Object> getExtraData() {
        return extraData;
    }
    
    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }
    
    public DailyReward getRewardForDay(int day) {
        if (day < 1 || day > dailyRewards.size()) {
            return dailyRewards.isEmpty() ? null : dailyRewards.get(dailyRewards.size() - 1);
        }
        return dailyRewards.get(day - 1);
    }
    
    public static MonthlyCardType fromConfig(String id, ConfigurationSection section) {
        MonthlyCardType type = new MonthlyCardType(id);
        type.setDisplayName(section.getString("display-name", id));
        type.setDescription(section.getString("description", ""));
        type.setDurationDays(section.getInt("duration-days", 30));
        type.setPrice(section.getLong("price", 0));
        type.setCurrencyType(section.getString("currency-type", "points"));
        
        ConfigurationSection rewardsSection = section.getConfigurationSection("daily-rewards");
        if (rewardsSection != null) {
            for (String dayKey : rewardsSection.getKeys(false)) {
                ConfigurationSection daySection = rewardsSection.getConfigurationSection(dayKey);
                if (daySection != null) {
                    DailyReward reward = DailyReward.fromConfig(daySection);
                    type.getDailyRewards().add(reward);
                }
            }
        }
        
        return type;
    }
}
