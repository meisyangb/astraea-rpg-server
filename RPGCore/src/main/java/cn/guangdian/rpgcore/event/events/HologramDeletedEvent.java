package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;

public class HologramDeletedEvent extends CoreEvent {

    private final String hologramName;

    public HologramDeletedEvent(String hologramName) {
        super(false);
        this.hologramName = hologramName;
    }

    public String getHologramName() {
        return hologramName;
    }
}
