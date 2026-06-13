package cn.guangdian.battlepass.manager;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.BattlePassReward;
import cn.guangdian.battlepass.model.BattlePassTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class RewardManager {
    
    private final GuangDianBattlePass plugin;
    private final Map<String, BattlePassReward> rewards;
    private final Map<String, BattlePassTask> tasks;
    private File rewardsFile;
    private File tasksFile;
    private YamlConfiguration rewardsConfig;
    private YamlConfiguration tasksConfig;
    
    public RewardManager(GuangDianBattlePass plugin) {
        this.plugin = plugin;
        this.rewards = new HashMap<>();
        this.tasks = new HashMap<>();
        loadRewards();
        loadTasks();
    }
    
    private void loadRewards() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        
        ConfigurationSection rewardsSection = rewardsConfig.getConfigurationSection("rewards");
        if (rewardsSection == null) return;
        
        for (String key : rewardsSection.getKeys(false)) {
            ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
            if (rewardSection == null) continue;
            
            BattlePassReward reward = loadReward(rewardSection);
            rewards.put(reward.getRewardId(), reward);
        }
        
        plugin.getLogger().info("已加载 " + rewards.size() + " 个奖励");
    }
    
    private BattlePassReward loadReward(ConfigurationSection section) {
        BattlePassReward reward = new BattlePassReward();
        reward.setRewardId(section.getString("id"));
        reward.setDisplayName(section.getString("name", "未命名奖励"));
        reward.setIcon(Material.matchMaterial(section.getString("icon", "DIAMOND")));
        reward.setPoints(section.getInt("points", 0));
        reward.setExp(section.getInt("exp", 0));
        reward.setMoney(section.getInt("money", 0));
        
        List<String> lore = section.getStringList("lore");
        reward.setLore(lore);
        
        return reward;
    }
    
    private void loadTasks() {
        tasksFile = new File(plugin.getDataFolder(), "tasks.yml");
        if (!tasksFile.exists()) {
            plugin.saveResource("tasks.yml", false);
        }
        tasksConfig = YamlConfiguration.loadConfiguration(tasksFile);
        
        ConfigurationSection tasksSection = tasksConfig.getConfigurationSection("tasks");
        if (tasksSection == null) return;
        
        for (String key : tasksSection.getKeys(false)) {
            ConfigurationSection taskSection = tasksSection.getConfigurationSection(key);
            if (taskSection == null) continue;
            
            BattlePassTask task = loadTask(taskSection);
            tasks.put(task.getTaskId(), task);
        }
        
        plugin.getLogger().info("已加载 " + tasks.size() + " 个任务");
    }
    
    private BattlePassTask loadTask(ConfigurationSection section) {
        BattlePassTask task = new BattlePassTask();
        task.setTaskId(section.getString("id"));
        task.setTaskName(section.getString("name", "未命名任务"));
        task.setTaskType(BattlePassTask.TaskType.valueOf(section.getString("type", "CUSTOM")));
        task.setRequiredAmount(section.getInt("amount", 1));
        task.setExpReward(section.getInt("exp", 100));
        task.setDescription(section.getString("description", ""));
        task.setDaily(section.getBoolean("daily", false));
        task.setWeekly(section.getBoolean("weekly", false));
        task.setTarget(section.getString("target", ""));
        
        return task;
    }
    
    public boolean giveReward(Player player, BattlePassReward reward) {
        if (reward == null) return false;
        
        try {
            for (ItemStack item : reward.getItems()) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
            
            if (reward.getPoints() > 0) {
                plugin.givePoints(player, reward.getPoints());
            }
            
            if (reward.getMoney() > 0) {
                plugin.giveMoney(player, reward.getMoney());
            }
            
            for (Map.Entry<String, Integer> entry : reward.getCommands().entrySet()) {
                String command = entry.getKey().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("给予奖励失败: " + e.getMessage());
            return false;
        }
    }
    
    public BattlePassReward getReward(String rewardId) {
        return rewards.get(rewardId);
    }
    
    public BattlePassTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    public Collection<BattlePassTask> getAllTasks() {
        return tasks.values();
    }
    
    public List<BattlePassTask> getDailyTasks() {
        List<BattlePassTask> dailyTasks = new ArrayList<>();
        for (BattlePassTask task : tasks.values()) {
            if (task.isDaily()) {
                dailyTasks.add(task);
            }
        }
        return dailyTasks;
    }
    
    public List<BattlePassTask> getWeeklyTasks() {
        List<BattlePassTask> weeklyTasks = new ArrayList<>();
        for (BattlePassTask task : tasks.values()) {
            if (task.isWeekly()) {
                weeklyTasks.add(task);
            }
        }
        return weeklyTasks;
    }
    
    public void reload() {
        rewards.clear();
        tasks.clear();
        loadRewards();
        loadTasks();
    }
}
