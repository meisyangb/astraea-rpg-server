package cn.guangdian.points.lifecycle;

import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.rpgcore.lifecycle.PlayerDataLoadEvent;
import cn.guangdian.rpgcore.lifecycle.PlayerDataSaveEvent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PointsDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianPoints plugin;
    
    public PointsDataHandler(GuangDianPoints plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();
        if (!plugin.getBalances().containsKey(playerId)) {
            plugin.getBalances().put(playerId, plugin.getDefaultBalance());
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        if (plugin.getLockManager() != null) {
            plugin.getLockManager().cleanup(player.getUniqueId());
        }
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "Points";
    }
}
