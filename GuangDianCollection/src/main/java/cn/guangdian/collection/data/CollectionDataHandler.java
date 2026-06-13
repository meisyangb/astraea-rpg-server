package cn.guangdian.collection.data;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.service.CollectionServiceImpl;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class CollectionDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianCollection plugin;
    
    public CollectionDataHandler(GuangDianCollection plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        // ✅ 缓存模式：玩家上线时预加载数据到缓存
        // getPlayerData 会自动加载并缓存
        CollectionServiceImpl service = (CollectionServiceImpl) plugin.getCollectionService();
        service.getPlayerData(player.getUniqueId());
        plugin.getLogger().fine("玩家数据已缓存: " + player.getName());
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        // ✅ 玩家退出时保存并清理缓存
        CollectionServiceImpl service = (CollectionServiceImpl) plugin.getCollectionService();
        service.saveAndClearCache(player.getUniqueId());
        plugin.getLogger().fine("玩家缓存已清理: " + player.getName());
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
