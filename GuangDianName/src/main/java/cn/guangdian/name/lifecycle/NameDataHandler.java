package cn.guangdian.name.lifecycle;

import cn.guangdian.name.GuangDianName;
import cn.guangdian.name.HealthDisplay;
import cn.guangdian.name.TitleDisplay;
import cn.guangdian.name.TextDisplayManager;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class NameDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianName plugin;
    private final HealthDisplay healthDisplay;
    private final TitleDisplay titleDisplay;
    private final TextDisplayManager textDisplayManager;
    private long loadTaskId = -1;
    
    public NameDataHandler(GuangDianName plugin, HealthDisplay healthDisplay, 
                           TitleDisplay titleDisplay, TextDisplayManager textDisplayManager) {
        super(plugin);
        this.plugin = plugin;
        this.healthDisplay = healthDisplay;
        this.titleDisplay = titleDisplay;
        this.textDisplayManager = textDisplayManager;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            loadTaskId = rpgCore.getScheduler().runSyncLater(() -> {
                if (player.isOnline()) {
                    healthDisplay.initPlayer(player);
                    titleDisplay.initPlayer(player);
                    textDisplayManager.createTextDisplay(player);
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
        healthDisplay.cleanupPlayer(player);
        titleDisplay.cleanupPlayer(player);
        textDisplayManager.removeTextDisplay(player);
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
