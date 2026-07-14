package cn.guangdian.realm;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 内丹监听器
 * 
 * 防止内丹被移动、丢弃
 * 玩家登录/复活时自动检查内丹
 */
public class NeidanListener implements Listener {
    private final GuangDianRealm plugin;
    private final NeidanManager neidanManager;
    
    public NeidanListener(GuangDianRealm plugin) {
        this.plugin = plugin;
        this.neidanManager = new NeidanManager(plugin);
    }
    
    /**
     * 获取内丹管理器
     */
    public NeidanManager getNeidanManager() {
        return neidanManager;
    }
    
    /**
     * 玩家登录时检查并给予内丹
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟1秒检查，确保玩家数据已加载
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                if (!neidanManager.hasNeidan(player)) {
                    neidanManager.giveNeidan(player);
                    plugin.getLogger().info("已为玩家 " + player.getName() + " 赋予内丹");
                }
            }
        }.runTaskLater(plugin, 20L);
    }
    
    /**
     * 玩家复活时检查内丹
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                if (!neidanManager.hasNeidan(player)) {
                    neidanManager.giveNeidan(player);
                }
            }
        }.runTaskLater(plugin, 5L);
    }
    
    /**
     * 禁止丢弃内丹
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (neidanManager.isNeidanItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[境界] 内丹无法丢弃!");
        }
    }
    
    /**
     * 防止通过快捷键切换到内丹格子
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemHeld(PlayerItemHeldEvent event) {
        // 如果玩家试图切换到第9格（索引8），阻止
        if (event.getNewSlot() == 8) {
            // 检查目标格子是否是内丹
            Player player = event.getPlayer();
            ItemStack targetItem = player.getInventory().getItem(8);
            
            if (neidanManager.isNeidan(targetItem, player)) {
                // 允许查看，但不允许切换到手中
                // 这里我们允许切换，因为内丹是可以查看的
            }
        }
    }
    
    /**
     * 防止通过库存操作移动内丹
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory bottomInventory = event.getView().getBottomInventory();
        
        // 检查是否涉及第9格（索引8）
        int rawSlot = event.getRawSlot();
        int slot = event.getSlot();
        
        // 情况1：点击内丹格子（第9格）
        if (slot == 8 && event.getInventory() == player.getInventory()) {
            ItemStack clickedItem = event.getCurrentItem();
            
            if (neidanManager.isNeidan(clickedItem, player)) {
                // 禁止所有对内丹格子的操作
                if (event.getAction() == InventoryAction.PICKUP_ALL ||
                    event.getAction() == InventoryAction.PICKUP_SOME ||
                    event.getAction() == InventoryAction.PICKUP_HALF ||
                    event.getAction() == InventoryAction.PICKUP_ONE ||
                    event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    event.getAction() == InventoryAction.HOTBAR_SWAP ||
                    event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                    
                    event.setCancelled(true);
                    player.sendMessage("§c[境界] 内丹无法移动!");
                    return;
                }
            }
        }
        
        // 情况2：尝试将物品移到第9格
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack movedItem = event.getCurrentItem();
            if (movedItem != null && neidanManager.isNeidan(movedItem, player)) {
                // 内丹被尝试移动
                event.setCancelled(true);
                player.sendMessage("§c[境界] 内丹无法移动!");
                return;
            }
        }
        
        // 情况3：使用快捷键(1-9)将物品移到第9格
        if (event.getAction() == InventoryAction.HOTBAR_SWAP ||
            event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            
            // 检查是否涉及第9格（热键9对应索引8）
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton == 8) {
                // 试图用热键9交换
                ItemStack cursorItem = event.getCursor();
                ItemStack currentItem = event.getCurrentItem();
                
                // 如果目标位置有内丹
                if (neidanManager.isNeidan(currentItem, player)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[境界] 内丹无法移动!");
                    return;
                }
                
                // 如果手中拿着的是内丹
                if (cursorItem != null && neidanManager.isNeidan(cursorItem, player)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[境界] 内丹无法移动!");
                    return;
                }
            }
        }
        
        // 情况4：shift+点击内丹
        if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && neidanManager.isNeidan(clickedItem, player)) {
                event.setCancelled(true);
                player.sendMessage("§c[境界] 内丹无法移动!");
                return;
            }
        }
        
        // 情况5：从其他格子拖拽到第9格
        if (event.getAction() == InventoryAction.PLACE_SOME ||
            event.getAction() == InventoryAction.PLACE_ONE ||
            event.getAction() == InventoryAction.PLACE_ALL) {
            
            // 检查是否拖拽到了玩家物品栏的第9格
            if (slot == 8 && event.getInventory() == player.getInventory()) {
                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && neidanManager.isNeidan(cursorItem, player)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[境界] 内丹无法移动!");
                    return;
                }
            }
        }
    }
    
    /**
     * 防止通过拖拽移动内丹
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // 检查是否涉及第9格
        if (event.getInventorySlots().contains(8) || event.getRawSlots().contains(8)) {
            // 检查是否是内丹
            ItemStack draggedItem = event.getOldCursor();
            if (draggedItem != null && neidanManager.isNeidan(draggedItem, player)) {
                event.setCancelled(true);
                player.sendMessage("§c[境界] 内丹无法移动!");
                return;
            }
            
            // 检查目标格子是否已有内丹
            ItemStack targetItem = player.getInventory().getItem(8);
            if (neidanManager.isNeidan(targetItem, player)) {
                event.setCancelled(true);
                player.sendMessage("§c[境界] 内丹无法移动!");
                return;
            }
        }
    }
}