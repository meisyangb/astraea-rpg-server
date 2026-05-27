package cn.guangdian.rpgcore.event.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 公会事件基类
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class RpgGuildEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    protected final String guildId;
    protected final String guildName;

    public RpgGuildEvent(String guildId, String guildName) {
        super(!Bukkit.isPrimaryThread());
        this.guildId = guildId;
        this.guildName = guildName;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * 公会创建事件
     */
    public static class Create extends RpgGuildEvent {
        private final Player creator;

        public Create(String guildId, String guildName, Player creator) {
            super(guildId, guildName);
            this.creator = creator;
        }

        public Player getCreator() {
            return creator;
        }
    }

    /**
     * 公会解散事件
     */
    public static class Disband extends RpgGuildEvent {
        private final Player disbander;

        public Disband(String guildId, String guildName, Player disbander) {
            super(guildId, guildName);
            this.disbander = disbander;
        }

        public Player getDisbander() {
            return disbander;
        }
    }

    /**
     * 玩家加入公会事件
     */
    public static class Join extends RpgGuildEvent {
        private final Player player;

        public Join(String guildId, String guildName, Player player) {
            super(guildId, guildName);
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }
    }

    /**
     * 玩家离开公会事件
     */
    public static class Leave extends RpgGuildEvent {
        private final Player player;
        private final LeaveReason reason;

        public enum LeaveReason {
            QUIT,
            KICK,
            DISBAND
        }

        public Leave(String guildId, String guildName, Player player, LeaveReason reason) {
            super(guildId, guildName);
            this.player = player;
            this.reason = reason;
        }

        public Player getPlayer() {
            return player;
        }

        public LeaveReason getReason() {
            return reason;
        }
    }
}
