package cn.guangdian.accessory.listener;

import cn.guangdian.accessory.GuangDianAccessory;
import cn.guangdian.accessory.event.AccessoryEquipEvent;
import cn.guangdian.accessory.model.Accessory;
import cn.guangdian.accessory.model.AccessorySlot;
import cn.guangdian.accessory.service.AccessoryServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.service.api.AttributeParseService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public class AccessoryStatsListener implements Listener {
    
    private final GuangDianAccessory plugin;
    
    public AccessoryStatsListener(GuangDianAccessory plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        updatePlayerAccessoryStats(player);
    }
    
    public void updatePlayerAccessoryStats(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            return;
        }
        
        AttributeParseService parseService = rpgCore.getServiceRegistry()
            .getService(AttributeParseService.class);
        
        if (parseService == null || !parseService.isAvailable()) {
            return;
        }
        
        Object accessoryStats = parseService.createEmptyAttributes();
        
        AccessoryServiceAdapter service = (AccessoryServiceAdapter) plugin.getAccessoryService();
        
        for (AccessorySlot slot : AccessorySlot.values()) {
            Optional<Accessory> equippedOpt = service.getEquippedAccessory(player, slot);
            if (equippedOpt.isPresent()) {
                Accessory equipped = equippedOpt.get();
                ItemStack item = equipped.getMythicItem(1);
                if (item != null) {
                    Map<String, Object> attrs = parseService.parseItemAttributes(item);
                    parseService.mergeAttributes(accessoryStats, attrs);
                }
            }
        }
        
        parseService.setExternalAccessoryStats(player, accessoryStats);
    }
}
