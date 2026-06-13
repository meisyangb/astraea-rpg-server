package cn.guangdian.chain.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public interface ChainService {
    
    List<Block> findConnectedBlocks(Block startBlock, int maxBlocks);
    
    void processChainBreak(Player player, Block startBlock, List<Block> blocks);
    
    boolean isChainableBlock(Block block);
    
    boolean hasChainPermission(Player player, Block block);
    
    int getMaxChainBlocks(Player player);
}
