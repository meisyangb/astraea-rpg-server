package cn.guangdian.name.lifecycle;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.name.NameDisplayManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class NameDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianName plugin;
    private final NameDisplayManager displayManager;
    private long loadTaskId = -1;
    
    public NameDataHandler(GuangDianName plugin, NameDisplayManager displayManager) {
        super(plugin);
        this.plugin = plugin;
        this.displayManager = displayManager;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            loadTaskId = rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    displayManager.initPlayer(player);
                }
            }, 50L);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        // 取消加载任务
        if (loadTaskId >= 0) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(loadTaskId);
            }
            loadTaskId = -1;
        }
        displayManager.removeAllDisplays(player);
    }
    
    @Override
    public int getPriority() {
        return 300;
    }
    
    @Override
    public String getHandlerName() {
        return "Name";
    }
}