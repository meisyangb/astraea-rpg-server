package cn.guangdian.quest.lifecycle;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class QuestDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianQuest plugin;
    
    public QuestDataHandler(GuangDianQuest plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        
        if (data.needsDailyReset()) {
            java.util.Set<String> dailyIds = plugin.getQuestManager().getDailyQuestIds();
            data.resetDaily(dailyIds);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        plugin.savePlayerOnQuit(player);
    }
    
    @Override
    public int getPriority() {
        return 150;
    }
    
    @Override
    public String getHandlerName() {
        return "Quest";
    }
}
