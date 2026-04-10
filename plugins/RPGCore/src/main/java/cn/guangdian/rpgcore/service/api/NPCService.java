package cn.guangdian.rpgcore.service.api;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NPCService {

    List<String> getNPCIds();

    boolean npcExists(String npcId);

    Optional<Map<String, Object>> getNPCInfo(String npcId);

    boolean createNPC(String npcId, String displayName, Location location, String skinPlayerName);

    boolean deleteNPC(String npcId);

    boolean moveNPC(String npcId, Location location);

    boolean updateName(String npcId, String displayName);

    boolean updateSkin(String npcId, String skinPlayerName);

    void reloadNPCs();
}
