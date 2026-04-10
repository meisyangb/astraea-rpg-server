package cn.guangdian.battlepass.listener;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.hook.MythicMobsHook;
import cn.guangdian.battlepass.model.BattlePassTask;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class BattlePassListener implements Listener {
    
    private final GuangDianBattlePass plugin;
    
    public BattlePassListener(GuangDianBattlePass plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            if (bp != null) {
                bp.setTaskProgress("login", bp.getTaskProgress("login") + 1);
            }
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null) return;
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(killer.getUniqueId());
            if (bp == null) return;
            
            if (entity instanceof Player) {
                bp.setTaskProgress("kill_player", bp.getTaskProgress("kill_player") + 1);
                checkTaskCompletion(killer, bp, "kill_player");
            } else {
                bp.setTaskProgress("kill_mob", bp.getTaskProgress("kill_mob") + 1);
                checkTaskCompletion(killer, bp, "kill_mob");
            }
            
            plugin.getExpTriggerManager().processKill(killer, entity);
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            if (bp == null) return;
            
            bp.setTaskProgress("break_block", bp.getTaskProgress("break_block") + 1);
            checkTaskCompletion(player, bp, "break_block");
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            if (bp == null) return;
            
            bp.setTaskProgress("place_block", bp.getTaskProgress("place_block") + 1);
            checkTaskCompletion(player, bp, "place_block");
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getRecipe().getResult();
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            if (bp == null) return;
            
            bp.setTaskProgress("craft_item", bp.getTaskProgress("craft_item") + 1);
            checkTaskCompletion(player, bp, "craft_item");
            
            plugin.getExpTriggerManager().processItemObtain(player, item);
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        
        plugin.getScheduler().runAsync(() -> {
            PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
            if (bp == null) return;
            
            bp.setTaskProgress("enchant_item", bp.getTaskProgress("enchant_item") + 1);
            checkTaskCompletion(player, bp, "enchant_item");
            
            plugin.getExpTriggerManager().processItemObtain(player, item);
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.getScheduler().runAsync(() -> {
                PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
                if (bp == null) return;
                
                bp.setTaskProgress("fishing", bp.getTaskProgress("fishing") + 1);
                checkTaskCompletion(player, bp, "fishing");
            });
        }
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        
        plugin.getScheduler().runAsync(() -> {
            plugin.getExpTriggerManager().processItemObtain(player, item);
        });
    }
    
    private void checkTaskCompletion(Player player, PlayerBattlePass bp, String taskType) {
        BattlePassTask task = plugin.getRewardManager().getTask(taskType);
        if (task == null) return;
        
        int progress = bp.getTaskProgress(taskType);
        if (progress >= task.getRequiredAmount()) {
            plugin.getBattlePassManager().addExp(player.getUniqueId(), task.getExpReward());
            player.sendMessage("§a完成任务: " + task.getTaskName() + " §7(+" + task.getExpReward() + " 经验)");
            bp.setTaskProgress(taskType, 0);
        }
    }
}
