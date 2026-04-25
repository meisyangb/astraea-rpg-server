package cn.guangdian.npc.papi;

import cn.guangdian.npc.GuangDianNPC;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

import java.util.Collection;

public class NPCPlaceholders {

    private final GuangDianNPC plugin;

    public NPCPlaceholders(GuangDianNPC plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdnpc", (player, params) -> {
            if (plugin.getNpcManager() == null) {
                return "";
            }

            String lowerParams = params.toLowerCase();

            if (lowerParams.equals("count") || lowerParams.equals("数量")) {
                return String.valueOf(plugin.getNpcManager().getNPCCount());
            }

            if (lowerParams.equals("list") || lowerParams.equals("列表")) {
                Collection<NPCData> npcs = plugin.getNpcManager().getAllNPCs();
                if (npcs.isEmpty()) {
                    return "无";
                }
                StringBuilder sb = new StringBuilder();
                for (NPCData npc : npcs) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(npc.getId());
                }
                return sb.toString();
            }

            if (lowerParams.startsWith("name_") || lowerParams.startsWith("名称_")) {
                String npcId = lowerParams.substring(lowerParams.indexOf("_") + 1);
                NPCData npc = plugin.getNpcManager().getNPC(npcId);
                return npc != null ? npc.getDisplayName() : "";
            }

            if (lowerParams.startsWith("menu_") || lowerParams.startsWith("菜单_")) {
                String npcId = lowerParams.substring(lowerParams.indexOf("_") + 1);
                NPCData npc = plugin.getNpcManager().getNPC(npcId);
                return npc != null ? npc.getMenuId() : "";
            }

            if (lowerParams.startsWith("world_") || lowerParams.startsWith("世界_")) {
                String npcId = lowerParams.substring(lowerParams.indexOf("_") + 1);
                NPCData npc = plugin.getNpcManager().getNPC(npcId);
                return npc != null ? npc.getWorldName() : "";
            }

            if (lowerParams.startsWith("enabled_") || lowerParams.startsWith("启用_")) {
                String npcId = lowerParams.substring(lowerParams.indexOf("_") + 1);
                NPCData npc = plugin.getNpcManager().getNPC(npcId);
                return npc != null ? (npc.isEnabled() ? "true" : "false") : "false";
            }

            if (lowerParams.startsWith("exists_") || lowerParams.startsWith("存在_")) {
                String npcId = lowerParams.substring(lowerParams.indexOf("_") + 1);
                return plugin.getNpcManager().getNPC(npcId) != null ? "true" : "false";
            }

            return null;
        });
    }

    public void unregister() {
    }
}
