package cn.guangdian.classsystem.listener;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.SkillSpaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 技能球保护监听器
 * 
 * 防止玩家移动、丢弃、交换快捷栏中的技能球
 */
public class SkillOrbProtectionListener implements Listener {

    private final GuangDianClass plugin;
    private final SkillSpaceManager skillSpaceManager;

    public SkillOrbProtectionListener(GuangDianClass plugin) {
        this.plugin = plugin;
        this.skillSpaceManager = plugin.getSkillSpaceManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.isCancelled()) return;

        // 检查是否在技能空间GUI或技能详情GUI中
        if (event.getInventory().getHolder() instanceof cn.guangdian.classsystem.gui.SkillSpaceGUI.SkillSpaceHolder ||
            event.getInventory().getHolder() instanceof cn.guangdian.classsystem.listener.SkillSpaceGUIListener.SkillDetailHolder) {
            return; // 这些GUI已经有保护了
        }

        // 只在玩家自己的背包界面中保护技能球（非自定义GUI界面）
        // 检查玩家当前打开的顶部界面是否为自定义GUI
        org.bukkit.inventory.Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getHolder() != null && !(topInventory.getHolder() instanceof org.bukkit.entity.Player)) {
            // 玩家正在查看自定义GUI（非玩家自身背包），不干预
            return;
        }

        // 检查点击的物品是否为技能球
        ItemStack clickedItem = event.getCurrentItem();
        if (isSkillOrb(clickedItem)) {
            // 阻止移动技能球
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("§c技能球无法移动!请通过技能空间GUI管理技能."));
            return;
        }

        // 检查光标上的物品是否为技能球（玩家试图放置技能球）
        ItemStack cursorItem = event.getCursor();
        if (isSkillOrb(cursorItem)) {
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("§c技能球无法移动!请通过技能空间GUI管理技能."));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.isCancelled()) return;

        // 检查是否在技能空间GUI或技能详情GUI中
        if (event.getInventory().getHolder() instanceof cn.guangdian.classsystem.gui.SkillSpaceGUI.SkillSpaceHolder ||
            event.getInventory().getHolder() instanceof cn.guangdian.classsystem.listener.SkillSpaceGUIListener.SkillDetailHolder) {
            return;
        }

        // 只在玩家自己的背包界面中保护技能球
        org.bukkit.inventory.Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getHolder() != null && !(topInventory.getHolder() instanceof org.bukkit.entity.Player)) {
            return;
        }

        // 检查拖拽的物品是否为技能球
        ItemStack draggedItem = event.getOldCursor();
        if (isSkillOrb(draggedItem)) {
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("§c技能球无法拖拽!请通过技能空间GUI管理技能."));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // 检查丢弃的物品是否为技能球
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isSkillOrb(droppedItem)) {
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("§c技能球无法丢弃!请通过技能空间GUI管理技能."));
            return;
        }
    }

    /**
     * 检查物品是否为技能球
     */
    private boolean isSkillOrb(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        String skillId = skillSpaceManager.getSkillIdFromItem(item);
        return skillId != null;
    }
}
