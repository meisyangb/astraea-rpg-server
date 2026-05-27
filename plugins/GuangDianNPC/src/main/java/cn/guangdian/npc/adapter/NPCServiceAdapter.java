package cn.guangdian.npc.adapter;

import cn.guangdian.npc.GuangDianNPC;
import cn.guangdian.npc.manager.NPCManager;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.NPCService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class NPCServiceAdapter implements NPCService {

    private final GuangDianNPC plugin;
    private final boolean useRPGCore;
    private EventBus eventBus;
    private AsyncExecutor asyncExecutor;

    public NPCServiceAdapter(GuangDianNPC plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.eventBus = rpgCore.getEventBus();
                this.asyncExecutor = rpgCore.getAsyncExecutor();

                registry.registerService(NPCService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: NPCService");

                plugin.getNpcManager().setAsyncExecutor(asyncExecutor);
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public List<String> getNPCIds() {
        return new ArrayList<>(plugin.getNpcManager().getNPCs().keySet());
    }

    @Override
    public boolean npcExists(String npcId) {
        return plugin.getNpcManager().getNPC(npcId) != null;
    }

    @Override
    public Optional<Map<String, Object>> getNPCInfo(String npcId) {
        NPCData npc = plugin.getNpcManager().getNPC(npcId);
        if (npc == null) {
            return Optional.empty();
        }
        return Optional.of(npc.serialize());
    }

    @Override
    public boolean createNPC(String npcId, String displayName, Location location, String skinPlayerName) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        String lowerId = npcId.toLowerCase();
        if (plugin.getNpcManager().getNPC(lowerId) != null) {
            return false;
        }

        NPCData npc = new NPCData(lowerId, displayName != null ? displayName : "<yellow>" + npcId, location, "main");
        if (skinPlayerName != null) {
            npc.setSkinName(skinPlayerName);
        }
        plugin.getNpcManager().getNPCs().put(lowerId, npc);
        plugin.getNpcManager().spawnNPC(npc);
        plugin.getNpcManager().save();
        return true;
    }

    @Override
    public boolean deleteNPC(String npcId) {
        return plugin.getNpcManager().removeNPC(npcId);
    }

    @Override
    public boolean moveNPC(String npcId, Location location) {
        NPCData npc = plugin.getNpcManager().getNPC(npcId);
        if (npc == null || location == null) {
            return false;
        }
        npc.setLocation(location);
        plugin.getNpcManager().respawnNPC(npc);
        plugin.getNpcManager().save();
        return true;
    }

    @Override
    public boolean updateName(String npcId, String displayName) {
        NPCData npc = plugin.getNpcManager().getNPC(npcId);
        if (npc == null) {
            return false;
        }
        npc.setDisplayName(displayName);
        plugin.getNpcManager().respawnNPC(npc);
        plugin.getNpcManager().save();
        return true;
    }

    @Override
    public boolean updateSkin(String npcId, String skinPlayerName) {
        NPCData npc = plugin.getNpcManager().getNPC(npcId);
        if (npc == null) {
            return false;
        }
        npc.setSkinName(skinPlayerName);
        plugin.getNpcManager().save();
        return true;
    }

    @Override
    public void reloadNPCs() {
        plugin.getNpcManager().reload();
    }

    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(NPCService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: NPCService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }
}
