package cn.guangdian.signin.config;

import cn.guangdian.signin.GuangDianSignIn;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    
    private final GuangDianSignIn plugin;
    private final Map<Integer, RewardConfig> rewards;
    
    public ConfigManager(GuangDianSignIn plugin) {
        this.plugin = plugin;
        this.rewards = new HashMap<>();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        rewards.clear();
        
        if (config.contains("rewards")) {
            for (String key : config.getConfigurationSection("rewards").getKeys(false)) {
                try {
                    int day = Integer.parseInt(key);
                    RewardConfig reward = new RewardConfig(day);
                    
                    List<String> commands = config.getStringList("rewards." + key + ".commands");
                    for (String cmd : commands) {
                        reward.addCommand(cmd);
                    }
                    
                    String message = config.getString("rewards." + key + ".message", "");
                    reward.setMessage(message);
                    
                    rewards.put(day, reward);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("无效的奖励天数: " + key);
                }
            }
        }
    }
    
    public RewardConfig getReward(int consecutiveDays) {
        if (rewards.containsKey(consecutiveDays)) {
            return rewards.get(consecutiveDays);
        }
        
        int maxDay = 0;
        for (int day : rewards.keySet()) {
            if (day <= consecutiveDays && day > maxDay) {
                maxDay = day;
            }
        }
        
        return rewards.get(maxDay);
    }
    
    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
