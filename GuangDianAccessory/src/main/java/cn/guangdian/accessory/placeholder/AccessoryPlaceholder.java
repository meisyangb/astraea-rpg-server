package cn.guangdian.accessory.placeholder;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.api.AccessoryService;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AccessoryPlaceholder extends PlaceholderExpansion {
    
    private final GuangDianAccessory plugin;
    
    public AccessoryPlaceholder(GuangDianAccessory plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "gdaccessory";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Astraea RPG Team";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }
        
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }
        
        AccessoryService service = plugin.getAccessoryService();
        if (service == null) {
            return "";
        }
        
        String identifier = params.toLowerCase();
        
        if (identifier.equals("badge")) {
            return service.getEquippedAccessory(onlinePlayer, AccessorySlot.BADGE)
                .map(Accessory::getName)
                .orElse("无");
        }
        
        if (identifier.equals("medal")) {
            return service.getEquippedAccessory(onlinePlayer, AccessorySlot.MEDAL)
                .map(Accessory::getName)
                .orElse("无");
        }
        
        if (identifier.equals("relic")) {
            return service.getEquippedAccessory(onlinePlayer, AccessorySlot.RELIC)
                .map(Accessory::getName)
                .orElse("无");
        }
        
        if (identifier.startsWith("attribute_")) {
            String attributeName = identifier.substring("attribute_".length());
            Map<String, Double> attributes = service.getTotalAttributes(onlinePlayer);
            Double value = attributes.get(attributeName);
            return value != null ? String.format("%.1f", value) : "0";
        }
        
        if (identifier.equals("total_count")) {
            int count = 0;
            for (AccessorySlot slot : AccessorySlot.values()) {
                if (service.getEquippedAccessory(onlinePlayer, slot).isPresent()) {
                    count++;
                }
            }
            return String.valueOf(count);
        }
        
        return null;
    }
    
    public void unregister() {
        me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(this);
    }
}
