package cn.guangdian.battlepass.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattlePassReward {
    
    private String rewardId;
    private String displayName;
    private Material icon;
    private int iconData;
    private List<String> lore;
    private List<ItemStack> items;
    private Map<String, Integer> commands;
    private int points;
    private int exp;
    private int money;
    
    public BattlePassReward() {
        this.items = new ArrayList<>();
        this.commands = new HashMap<>();
        this.lore = new ArrayList<>();
    }
    
    public String getRewardId() {
        return rewardId;
    }
    
    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    public int getIconData() {
        return iconData;
    }
    
    public void setIconData(int iconData) {
        this.iconData = iconData;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public void setItems(List<ItemStack> items) {
        this.items = items;
    }
    
    public void addItem(ItemStack item) {
        this.items.add(item);
    }
    
    public Map<String, Integer> getCommands() {
        return commands;
    }
    
    public void setCommands(Map<String, Integer> commands) {
        this.commands = commands;
    }
    
    public void addCommand(String command, int chance) {
        this.commands.put(command, chance);
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public int getExp() {
        return exp;
    }
    
    public void setExp(int exp) {
        this.exp = exp;
    }
    
    public int getMoney() {
        return money;
    }
    
    public void setMoney(int money) {
        this.money = money;
    }
}
