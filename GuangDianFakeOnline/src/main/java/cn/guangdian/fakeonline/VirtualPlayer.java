package cn.guangdian.fakeonline;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.util.UUID;

/**
 * 虚拟玩家数据类
 */
public class VirtualPlayer {
    
    private final UUID uuid;
    private final String name;
    private final WrappedGameProfile profile;
    private final EnumWrappers.PlayerInfoAction action;
    private final EnumWrappers.NativeGameMode gameMode;
    private int ping;
    
    public VirtualPlayer(UUID uuid, String name, WrappedGameProfile profile, 
                        EnumWrappers.PlayerInfoAction action,
                        EnumWrappers.NativeGameMode gameMode, int ping) {
        this.uuid = uuid;
        this.name = name;
        this.profile = profile;
        this.action = action;
        this.gameMode = gameMode;
        this.ping = ping;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public WrappedGameProfile getProfile() {
        return profile;
    }
    
    public EnumWrappers.PlayerInfoAction getAction() {
        return action;
    }
    
    public EnumWrappers.NativeGameMode getGameMode() {
        return gameMode;
    }
    
    public int getPing() {
        return ping;
    }
    
    public void setPing(int ping) {
        this.ping = ping;
    }
}