package cn.guangdian.chain.processor;

import cn.guangdian.chain.GuangDianChain;
import cn.guangdian.chain.config.ChainConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ChainProcessor {
    
    private final GuangDianChain plugin;
    private final ChainConfig config;
    
    private static final BlockFace[] FACES = {
        BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    
    public ChainProcessor(GuangDianChain plugin) {
        this.plugin = plugin;
        this.config = plugin.getChainConfig();
    }
    
    public List<Block> findConnectedBlocks(Block startBlock, int maxBlocks) {
        List<Block> connected = new ArrayList<>();
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        
        Material targetType = startBlock.getType();
        visited.add(startBlock.getLocation());
        queue.add(startBlock);
        
        while (!queue.isEmpty() && connected.size() < maxBlocks) {
            Block current = queue.poll();
            connected.add(current);
            
            for (BlockFace face : FACES) {
                Block relative = current.getRelative(face);
                Location loc = relative.getLocation();
                
                if (!visited.contains(loc) && relative.getType() == targetType) {
                    visited.add(loc);
                    queue.add(relative);
                }
            }
        }
        
        return connected;
    }
    
    public void processChainBreak(Player player, Block startBlock, List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        int durabilityCost = calculateDurabilityCost(blocks.size());
        
        for (Block block : blocks) {
            breakBlock(player, block, tool);
            playBreakEffect(block);
        }
        
        damageTool(tool, durabilityCost);
        
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }
    
    private void breakBlock(Player player, Block block, ItemStack tool) {
        if (config.shouldDropNaturally()) {
            block.breakNaturally(tool);
        } else {
            block.setType(Material.AIR);
        }
    }
    
    private void playBreakEffect(Block block) {
        if (config.shouldPlayParticles()) {
            block.getWorld().spawnParticle(
                Particle.BLOCK,
                block.getLocation().add(0.5, 0.5, 0.5),
                10,
                0.3, 0.3, 0.3,
                block.getBlockData()
            );
        }
    }
    
    private int calculateDurabilityCost(int blockCount) {
        int baseCost = config.getBaseDurabilityCost();
        double multiplier = config.getDurabilityMultiplier();
        return (int) Math.ceil(baseCost + blockCount * multiplier);
    }
    
    private void damageTool(ItemStack tool, int damage) {
        if (tool == null || tool.getType() == Material.AIR) {
            return;
        }
        
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int currentDamage = damageable.getDamage();
            int maxDurability = tool.getType().getMaxDurability();
            
            int newDamage = currentDamage + damage;
            if (newDamage >= maxDurability) {
                tool.setAmount(0);
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta(meta);
            }
        }
    }
    
    public boolean isChainableBlock(Block block) {
        Material type = block.getType();
        
        if (config.isOreChainEnabled() && isOre(type)) {
            return true;
        }
        
        if (config.isTreeChainEnabled() && isLog(type)) {
            return true;
        }
        
        return config.getCustomChainableBlocks().contains(type);
    }
    
    private boolean isOre(Material type) {
        return type.name().contains("_ORE") || 
               type == Material.RAW_IRON_BLOCK ||
               type == Material.RAW_COPPER_BLOCK ||
               type == Material.RAW_GOLD_BLOCK;
    }
    
    private boolean isLog(Material type) {
        return type.name().contains("_LOG") || 
               type.name().contains("_STEM") ||
               type.name().contains("_WOOD") ||
               type.name().contains("_HYPHAE");
    }
    
    public boolean hasChainPermission(Player player, Block block) {
        Material type = block.getType();
        
        if (isOre(type)) {
            return player.hasPermission("guangdian.chain.ore");
        }
        
        if (isLog(type)) {
            return player.hasPermission("guangdian.chain.tree");
        }
        
        return player.hasPermission("guangdian.chain.custom");
    }
    
    public int getMaxChainBlocks(Player player) {
        int base = config.getDefaultMaxBlocks();
        
        if (player.hasPermission("guangdian.chain.vip")) {
            return config.getVipMaxBlocks();
        }
        
        if (player.hasPermission("guangdian.chain.premium")) {
            return config.getPremiumMaxBlocks();
        }
        
        return base;
    }
}
