package cn.guangdian.monthlycard.data;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyReward {
    
    private long points;
    private double money;
    private List<ItemStack> items;
    private List<String> commands;
    private List<String> messages;
    private Map<String, Object> extraRewards;
    
    public DailyReward() {
        this.points = 0;
        this.money = 0;
        this.items = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.extraRewards = new HashMap<>();
    }
    
    public long getPoints() {
        return points;
    }
    
    public void setPoints(long points) {
        this.points = points;
    }
    
    public double getMoney() {
        return money;
    }
    
    public void setMoney(double money) {
        this.money = money;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public void setItems(List<ItemStack> items) {
        this.items = items;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    
    public List<String> getMessages() {
        return messages;
    }
    
    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
    
    public Map<String, Object> getExtraRewards() {
        return extraRewards;
    }
    
    public void setExtraRewards(Map<String, Object> extraRewards) {
        this.extraRewards = extraRewards;
    }
    
    public boolean hasAnyReward() {
        return points > 0 || money > 0 || !items.isEmpty() || !commands.isEmpty();
    }

    /**
     * 获取奖励描述
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (points > 0) {
            sb.append(points).append(" 点券 ");
        }
        if (money > 0) {
            sb.append(money).append(" 游戏币 ");
        }
        if (!items.isEmpty()) {
            sb.append(items.size()).append(" 个物品 ");
        }
        if (sb.length() == 0) {
            return "无奖励";
        }
        return sb.toString().trim();
    }
    
    public static DailyReward fromConfig(ConfigurationSection section) {
        DailyReward reward = new DailyReward();
        reward.setPoints(section.getLong("points", 0));
        reward.setMoney(section.getDouble("money", 0));
        
        List<?> itemsList = section.getList("items");
        if (itemsList != null) {
            for (Object obj : itemsList) {
                if (obj instanceof ItemStack) {
                    reward.getItems().add((ItemStack) obj);
                }
            }
        }
        
        reward.setCommands(section.getStringList("commands"));
        reward.setMessages(section.getStringList("messages"));
        
        return reward;
    }
    
    public static DailyReward simple(long points) {
        DailyReward reward = new DailyReward();
        reward.setPoints(points);
        return reward;
    }
    
    public static DailyReward simpleWithMoney(long points, double money) {
        DailyReward reward = new DailyReward();
        reward.setPoints(points);
        reward.setMoney(money);
        return reward;
    }
}
