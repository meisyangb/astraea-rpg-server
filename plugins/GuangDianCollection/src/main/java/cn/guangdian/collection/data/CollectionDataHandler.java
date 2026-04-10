package cn.guangdian.collection.data;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class CollectionDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    
    public CollectionDataHandler(GuangDianCollection plugin, CollectionService collectionService) {
        super(plugin);
        this.plugin = plugin;
        this.collectionService = collectionService;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        collectionService.getPlayerData(player);
        plugin.getLogger().info("已加载玩家图鉴数据: " + player.getName());
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        collectionService.savePlayerData(player.getUniqueId());
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "CollectionData";
    }
}
