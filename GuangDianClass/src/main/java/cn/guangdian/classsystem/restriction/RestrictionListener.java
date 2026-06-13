package cn.guangdian.classsystem.restriction;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.classsystem.restriction.ClassRestrictionManager.EquipCheckResult;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RestrictionListener implements Listener {
    
    private final GuangDianClass plugin;
    private final ClassRestrictionManager restrictionManager;
    private ExternalServiceIntegration externalServices;
    
    public RestrictionListener(GuangDianClass plugin, ClassRestrictionManager restrictionManager) {
        this.plugin = plugin;
        this.restrictionManager = restrictionManager;
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.externalServices = rpgCore.getExternalServices();
        }
    }
    
    /**
     * 监听右键使用物品（原有逻辑）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() == Material.AIR) return;
        
        // 检查 RPGItems 装备限制
        EquipCheckResult result = restrictionManager.canEquipItem(player, item);
        if (!result.isAllowed()) {
            event.setCancelled(true);
            sendDenyMessage(player, result);
            return;
        }
        
        // 检查原有 Material 限制
        if (!restrictionManager.canUseItem(player, item)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("你的职业无法使用此物品！").color(NamedTextColor.RED));
        }
    }
    
    /**
     * 监听背包点击事件 - 拦截装备穿戴
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        
        // 情况1: 玩家将物品放入装备槽位
        if (isArmorSlot(event.getRawSlot())) {
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                EquipCheckResult result = restrictionManager.canEquipItem(player, cursorItem);
                if (!result.isAllowed()) {
                    event.setCancelled(true);
                    sendDenyMessage(player, result);
                    return;
                }
            }
        }
        
        // 情况2: 玩家按 Shift+点击装备自动穿戴
        if (event.isShiftClick() && currentItem != null && currentItem.getType() != Material.AIR) {
            if (isArmorMaterial(currentItem.getType()) && !isArmorSlot(event.getRawSlot())) {
                EquipCheckResult result = restrictionManager.canEquipItem(player, currentItem);
                if (!result.isAllowed()) {
                    event.setCancelled(true);
                    sendDenyMessage(player, result);
                    return;
                }
            }
        }
        
        // 情况3: 玩家将武器放入主手槽位 (快捷栏第0-8格，但主手是手持的)
        // 这里检查副手槽位 (slot 45)
        if (event.getRawSlot() == 45) {
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                EquipCheckResult result = restrictionManager.canEquipItem(player, cursorItem);
                if (!result.isAllowed()) {
                    event.setCancelled(true);
                    sendDenyMessage(player, result);
                }
            }
        }
    }
    
    /**
     * 发送拒绝消息
     */
    private void sendDenyMessage(Player player, EquipCheckResult result) {
        if ("class".equals(result.getReason())) {
            PlayerClassData data = plugin.getPlayerData(player);
            String currentClass = "无";
            if (data != null && data.getClassId() != null) {
                var gameClass = plugin.getClassManager().getClass(data.getClassId());
                if (gameClass != null) {
                    currentClass = gameClass.getName();
                }
            }
            player.sendMessage(
                Component.text("无法装备！需要职业: " + result.getRequiredClass() + "，你的职业: " + currentClass)
                    .color(NamedTextColor.RED)
            );
        } else if ("level".equals(result.getReason())) {
            PlayerClassData data = plugin.getPlayerData(player);
            int currentTier = data != null ? data.getTier() : 0;
            player.sendMessage(
                Component.text("无法装备！需要阶位: " + result.getRequiredLevel() + "阶，当前阶位: " + currentTier + "阶")
                    .color(NamedTextColor.RED)
            );
        }
    }
    
    /**
     * 检查是否是盔甲装备槽位
     * 创造模式/背包界面中: 5=头盔, 6=胸甲, 7=护腿, 8=战靴
     */
    private boolean isArmorSlot(int rawSlot) {
        return rawSlot >= 5 && rawSlot <= 8;
    }
    
    /**
     * 检查物品是否是盔甲类材料
     */
    private boolean isArmorMaterial(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") 
            || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
