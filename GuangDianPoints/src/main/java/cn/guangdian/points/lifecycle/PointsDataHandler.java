package cn.guangdian.points.lifecycle;

import cn.guangdian.points.GuangDianPoints;
import cn.guangdian.points.storage.DatabaseStorage;
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

        // 如果使用数据库存储，数据已经在插件启动时加载
        // 这里只需要确保玩家有默认余额
        plugin.getBalances().putIfAbsent(playerId, plugin.getDefaultBalance());
    }

    @Override
    protected void onPlayerSave(Player player) {
        UUID playerId = player.getUniqueId();

        // 保存玩家数据到数据库
        DatabaseStorage storage = plugin.getDatabaseStorage();
        if (storage != null && storage.isEnabled()) {
            storage.savePlayerSync(playerId);
        }

        // 清理玩家锁
        if (plugin.getLockManager() != null) {
            plugin.getLockManager().cleanup(playerId);
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
