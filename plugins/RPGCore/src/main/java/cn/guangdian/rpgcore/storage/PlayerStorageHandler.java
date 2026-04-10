package cn.guangdian.rpgcore.storage;

import org.bukkit.entity.Player;
import java.util.UUID;

public interface PlayerStorageHandler {
    
    void save(UUID playerId, Object data);
    
    Object load(UUID playerId);
    
    void delete(UUID playerId);
    
    boolean exists(UUID playerId);
    
    String getHandlerName();
    
    int getPriority();
}
