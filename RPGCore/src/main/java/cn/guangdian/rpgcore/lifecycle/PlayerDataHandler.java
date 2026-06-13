package cn.guangdian.rpgcore.lifecycle;

import org.bukkit.entity.Player;
import java.util.UUID;

public interface PlayerDataHandler {
    
    void onLoad(PlayerDataLoadEvent event);
    
    void onSave(PlayerDataSaveEvent event);
    
    int getPriority();
    
    String getHandlerName();
    
    default boolean shouldLoad(Player player) {
        return true;
    }
    
    default boolean shouldSave(Player player) {
        return true;
    }
}
