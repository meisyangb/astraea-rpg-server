package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;

public class WorldDeletedEvent extends CoreEvent {

    private final String worldName;

    public WorldDeletedEvent(String worldName) {
        super(false);
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }
}
