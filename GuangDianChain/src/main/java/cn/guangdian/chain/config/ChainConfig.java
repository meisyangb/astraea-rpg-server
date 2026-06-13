package cn.guangdian.chain.config;

import cn.guangdian.chain.GuangDianChain;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ChainConfig {
    
    private final GuangDianChain plugin;
    
    private boolean oreChainEnabled;
    private boolean treeChainEnabled;
    private int defaultMaxBlocks;
    private int vipMaxBlocks;
    private int premiumMaxBlocks;
    private int baseDurabilityCost;
    private double durabilityMultiplier;
    private boolean dropNaturally;
    private boolean playParticles;
    private List<Material> customChainableBlocks;
    
    public ChainConfig(GuangDianChain plugin) {
        this.plugin = plugin;
        this.customChainableBlocks = new ArrayList<>();
    }
    
    public void load() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        
        oreChainEnabled = config.getBoolean("chain.ore.enabled", true);
        treeChainEnabled = config.getBoolean("chain.tree.enabled", true);
        
        defaultMaxBlocks = config.getInt("limits.default", 32);
        vipMaxBlocks = config.getInt("limits.vip", 64);
        premiumMaxBlocks = config.getInt("limits.premium", 128);
        
        baseDurabilityCost = config.getInt("durability.base-cost", 1);
        durabilityMultiplier = config.getDouble("durability.multiplier", 0.1);
        
        dropNaturally = config.getBoolean("options.drop-naturally", true);
        playParticles = config.getBoolean("options.particles", true);
        
        loadCustomBlocks(config);
    }
    
    private void loadCustomBlocks(FileConfiguration config) {
        List<String> blockNames = config.getStringList("custom-blocks");
        for (String name : blockNames) {
            try {
                Material material = Material.valueOf(name.toUpperCase());
                customChainableBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的方块类型: " + name);
            }
        }
    }
    
    public boolean isOreChainEnabled() {
        return oreChainEnabled;
    }
    
    public boolean isTreeChainEnabled() {
        return treeChainEnabled;
    }
    
    public int getDefaultMaxBlocks() {
        return defaultMaxBlocks;
    }
    
    public int getVipMaxBlocks() {
        return vipMaxBlocks;
    }
    
    public int getPremiumMaxBlocks() {
        return premiumMaxBlocks;
    }
    
    public int getBaseDurabilityCost() {
        return baseDurabilityCost;
    }
    
    public double getDurabilityMultiplier() {
        return durabilityMultiplier;
    }
    
    public boolean shouldDropNaturally() {
        return dropNaturally;
    }
    
    public boolean shouldPlayParticles() {
        return playParticles;
    }
    
    public List<Material> getCustomChainableBlocks() {
        return customChainableBlocks;
    }
}
