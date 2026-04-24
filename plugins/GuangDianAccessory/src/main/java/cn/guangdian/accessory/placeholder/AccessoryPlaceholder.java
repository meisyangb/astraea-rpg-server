package cn.guangdian.accessory.placeholder;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;

public class AccessoryPlaceholder {
    
    private final GuangDianAccessory plugin;
    
    public AccessoryPlaceholder(GuangDianAccessory plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdaccessory", (player, params) -> {
            if (player == null || !player.isOnline()) return "";
            
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) return "";
            
            AccessoryService service1 = plugin.getAccessoryService();
            if (service1 == null) return "";
            
            String identifier = params.toLowerCase();
            
            if (identifier.equals("badge")) {
                return service1.getEquippedAccessory(onlinePlayer, AccessorySlot.BADGE)
                    .map(Accessory::getName)
                    .orElse("无");
            }
            
            if (identifier.equals("medal")) {
                return service1.getEquippedAccessory(onlinePlayer, AccessorySlot.MEDAL)
                    .map(Accessory::getName)
                    .orElse("无");
            }
            
            if (identifier.equals("relic")) {
                return service1.getEquippedAccessory(onlinePlayer, AccessorySlot.RELIC)
                    .map(Accessory::getName)
                    .orElse("无");
            }
            
            if (identifier.startsWith("attribute_")) {
                String attributeName = identifier.substring("attribute_".length());
                Map<String, Double> attributes = service1.getTotalAttributes(onlinePlayer);
                Double value = attributes.get(attributeName);
                return value != null ? String.format("%.1f", value) : "0";
            }
            
            if (identifier.equals("total_count")) {
                int count = 0;
                for (AccessorySlot slot : AccessorySlot.values()) {
                    if (service1.getEquippedAccessory(onlinePlayer, slot).isPresent()) {
                        count++;
                    }
                }
                return String.valueOf(count);
            }
            
            return null;
        });
    }
    
    public void unregister() {
    }
}
