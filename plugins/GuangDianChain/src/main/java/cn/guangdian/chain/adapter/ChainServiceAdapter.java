package cn.guangdian.chain.adapter;

import cn.guangdian.chain.GuangDianChain;
import cn.guangdian.chain.api.ChainService;
import cn.guangdian.chain.processor.ChainProcessor;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class ChainServiceAdapter implements ChainService {
    
    private final GuangDianChain plugin;
    private final ChainProcessor processor;
    
    public ChainServiceAdapter(GuangDianChain plugin) {
        this.plugin = plugin;
        this.processor = new ChainProcessor(plugin);
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(ChainService.class, this);
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(ChainService.class);
        }
    }
    
    @Override
    public List<Block> findConnectedBlocks(Block startBlock, int maxBlocks) {
        return processor.findConnectedBlocks(startBlock, maxBlocks);
    }
    
    @Override
    public void processChainBreak(Player player, Block startBlock, List<Block> blocks) {
        processor.processChainBreak(player, startBlock, blocks);
    }
    
    @Override
    public boolean isChainableBlock(Block block) {
        return processor.isChainableBlock(block);
    }
    
    @Override
    public boolean hasChainPermission(Player player, Block block) {
        return processor.hasChainPermission(player, block);
    }
    
    @Override
    public int getMaxChainBlocks(Player player) {
        return processor.getMaxChainBlocks(player);
    }
}
