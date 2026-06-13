package cn.guangdian.npc.api;

import cn.guangdian.npc.model.NPCData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public interface NPCAPI {

    Optional<NPCData> getNPC(String id);

    Collection<NPCData> getAllNPCs();

    int getNPCCount();

    NPCData createNPC(String id, Location location, String menuId);

    boolean removeNPC(String id);

    boolean exists(String id);

    void openMenu(Player player, String menuId);

    void openNPCMenu(Player player, String npcId);

    void teleportToNPC(Player player, String npcId);

    void setNPCEnabled(String id, boolean enabled);

    void setNPCMenu(String id, String menuId);

    void setNPCDisplayName(String id, String displayName);

    void setNPCCommands(String id, java.util.List<String> commands);

    void addNPCCommand(String id, String command);
}
