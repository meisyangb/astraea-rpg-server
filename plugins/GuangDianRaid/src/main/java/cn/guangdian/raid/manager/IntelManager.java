package cn.guangdian.raid.manager;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.Intel;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class IntelManager {

    private final GuangDianRaid plugin;

    public IntelManager(GuangDianRaid plugin) {
        this.plugin = plugin;
    }

    public boolean isIntelItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        for (Intel intel : plugin.getConfigManager().getAllRaids().stream()
                .flatMap(r -> r.getIntelItems().values().stream()).toList()) {
            if (intel.getMaterial() == item.getType()) {
                var meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    return meta.getDisplayName().equals(intel.getName());
                }
            }
        }
        return false;
    }

    public Optional<Intel> parseIntelItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        
        for (Intel intel : plugin.getConfigManager().getAllRaids().stream()
                .flatMap(r -> r.getIntelItems().values().stream()).toList()) {
            if (intel.getMaterial() == item.getType()) {
                var meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && 
                    meta.getDisplayName().equals(intel.getName())) {
                    return Optional.of(intel);
                }
            }
        }
        return Optional.empty();
    }

    public void handleIntelCollect(Player player, Item droppedItem) {
        Optional<RaidInstance> instanceOpt = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instanceOpt.isEmpty()) return;

        RaidInstance instance = instanceOpt.get();
        
        if (!instance.getDroppedItems().contains(droppedItem.getUniqueId())) return;

        ItemStack item = droppedItem.getItemStack();
        Optional<Intel> intelOpt = parseIntelItem(item);
        
        if (intelOpt.isPresent()) {
            Intel intel = intelOpt.get();
            instance.collectIntel(player, intel);
            droppedItem.remove();
            instance.getDroppedItems().remove(droppedItem.getUniqueId());
        }
    }
}
