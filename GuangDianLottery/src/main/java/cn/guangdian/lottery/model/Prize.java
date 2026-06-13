package cn.guangdian.lottery.model;

import org.bukkit.Material;
import java.util.List;

public class Prize {
    
    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final int amount;
    private final double weight;
    private final double chance;
    private final boolean rare;
    private final String rarityColor;
    private final List<String> commands;
    private final List<String> messages;
    private final String mythicMobsItem;
    
    public Prize(String id, String displayName, Material material, int customModelData,
                 int amount, double weight, double chance, boolean rare, String rarityColor,
                 List<String> commands, List<String> messages, String mythicMobsItem) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.amount = amount;
        this.weight = weight;
        this.chance = chance;
        this.rare = rare;
        this.rarityColor = rarityColor;
        this.commands = commands;
        this.messages = messages;
        this.mythicMobsItem = mythicMobsItem;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public int getAmount() { return amount; }
    public double getWeight() { return weight; }
    public double getChance() { return chance; }
    public boolean isRare() { return rare; }
    public String getRarityColor() { return rarityColor; }
    public List<String> getCommands() { return commands; }
    public List<String> getMessages() { return messages; }
    public String getMythicMobsItem() { return mythicMobsItem; }
}
