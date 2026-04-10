package cn.guangdian.raid.api;

import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.RaidPhaseType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RaidEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public static class RaidStartEvent extends RaidEvent {
        private final RaidInstance instance;

        public RaidStartEvent(RaidInstance instance) {
            this.instance = instance;
        }

        public RaidInstance getInstance() { return instance; }
    }

    public static class RaidPhaseChangeEvent extends RaidEvent {
        private final RaidInstance instance;
        private final RaidPhaseType oldPhase;
        private final RaidPhaseType newPhase;

        public RaidPhaseChangeEvent(RaidInstance instance, RaidPhaseType oldPhase, RaidPhaseType newPhase) {
            this.instance = instance;
            this.oldPhase = oldPhase;
            this.newPhase = newPhase;
        }

        public RaidInstance getInstance() { return instance; }
        public RaidPhaseType getOldPhase() { return oldPhase; }
        public RaidPhaseType getNewPhase() { return newPhase; }
    }

    public static class RaidCompleteEvent extends RaidEvent {
        private final RaidInstance instance;
        private final boolean success;

        public RaidCompleteEvent(RaidInstance instance, boolean success) {
            this.instance = instance;
            this.success = success;
        }

        public RaidInstance getInstance() { return instance; }
        public boolean isSuccess() { return success; }
    }

    public static class PlayerExtractionEvent extends RaidEvent {
        private final Player player;
        private final RaidInstance instance;

        public PlayerExtractionEvent(Player player, RaidInstance instance) {
            this.player = player;
            this.instance = instance;
        }

        public Player getPlayer() { return player; }
        public RaidInstance getInstance() { return instance; }
    }

    public static class IntelCollectEvent extends RaidEvent {
        private final Player player;
        private final RaidInstance instance;
        private final String intelId;
        private final int value;

        public IntelCollectEvent(Player player, RaidInstance instance, String intelId, int value) {
            this.player = player;
            this.instance = instance;
            this.intelId = intelId;
            this.value = value;
        }

        public Player getPlayer() { return player; }
        public RaidInstance getInstance() { return instance; }
        public String getIntelId() { return intelId; }
        public int getValue() { return value; }
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
