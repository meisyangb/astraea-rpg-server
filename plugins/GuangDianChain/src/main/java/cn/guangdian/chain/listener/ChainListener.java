package cn.guangdian.chain.listener;

import cn.guangdian.chain.GuangDianChain;
import cn.guangdian.chain.api.ChainService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChainListener implements Listener {
    
    private final GuangDianChain plugin;
    private final ChainService chainService;
    private final Set<UUID> processingPlayers;
    
    public ChainListener(GuangDianChain plugin) {
        this.plugin = plugin;
        this.chainService = plugin.getChainService();
        this.processingPlayers = new HashSet<>();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (processingPlayers.contains(player.getUniqueId())) {
            return;
        }
        
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isProperTool(tool, event.getBlock())) {
            return;
        }
        
        if (!player.isSneaking()) {
            return;
        }
        
        Block block = event.getBlock();
        
        if (!chainService.isChainableBlock(block)) {
            return;
        }
        
        if (!chainService.hasChainPermission(player, block)) {
            return;
        }
        
        int maxBlocks = chainService.getMaxChainBlocks(player);
        List<Block> connectedBlocks = chainService.findConnectedBlocks(block, maxBlocks);
        
        if (connectedBlocks.size() <= 1) {
            return;
        }
        
        processingPlayers.add(player.getUniqueId());
        
        try {
            connectedBlocks.remove(block);
            
            chainService.processChainBreak(player, block, connectedBlocks);
        } finally {
            processingPlayers.remove(player.getUniqueId());
        }
    }
    
    private boolean isProperTool(ItemStack tool, Block block) {
        Material blockType = block.getType();
        Material toolType = tool.getType();
        
        if (isOre(blockType)) {
            return isPickaxe(toolType);
        }
        
        if (isLog(blockType)) {
            return isAxe(toolType);
        }
        
        return true;
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
    
    private boolean isPickaxe(Material type) {
        return type.name().endsWith("_PICKAXE");
    }
    
    private boolean isAxe(Material type) {
        return type.name().endsWith("_AXE");
    }
}
