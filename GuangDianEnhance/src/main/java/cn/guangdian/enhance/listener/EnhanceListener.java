package cn.guangdian.enhance.listener;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.data.EnhanceResult;
import cn.guangdian.enhance.manager.EnhanceManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EnhanceListener implements Listener {

    private final GuangDianEnhance plugin;
    private final EnhanceManager enhanceManager;
    private final EnhanceConfig config;
    private final MiniMessageService miniMessage;

    public EnhanceListener(GuangDianEnhance plugin, EnhanceManager enhanceManager) {
        this.plugin = plugin;
        this.enhanceManager = enhanceManager;
        this.config = plugin.getEnhanceConfig();
        this.miniMessage = plugin.getMiniMessage();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        if (isEnhanceTriggerItem(offHand) && config.isEnhanceable(mainHand)) {
            event.setCancelled(true);
            
            EnhanceResult result = enhanceManager.enhance(player, mainHand);
            
            if (result == EnhanceResult.NOT_ENHANCEABLE) {
                player.sendMessage(miniMessage.colorize(
                    "<red>该物品无法强化"));
            }
        }
    }

    private boolean isEnhanceTriggerItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        return item.getType().name().contains("ANVIL");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        enhanceManager.removePlayer(event.getPlayer().getUniqueId());
    }
}
