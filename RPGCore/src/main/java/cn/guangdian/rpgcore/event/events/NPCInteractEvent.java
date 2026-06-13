package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class NPCInteractEvent extends CoreEvent {

    private final String npcId;
    private final String npcName;
    private final Player player;
    private final Location location;

    public NPCInteractEvent(String npcId, String npcName, Player player, Location location) {
        super(false);
        this.npcId = npcId;
        this.npcName = npcName;
        this.player = player;
        this.location = location;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }
}
