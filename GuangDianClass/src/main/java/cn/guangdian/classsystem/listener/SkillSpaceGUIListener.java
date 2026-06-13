package cn.guangdian.classsystem.listener;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.gui.SkillSpaceGUI;
import cn.guangdian.classsystem.manager.SkillSpaceManager;
import cn.guangdian.classsystem.model.PlayerSkillData;
import cn.guangdian.classsystem.model.SkillOrb;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 技能空间GUI交互监听器
 * 
 * 处理技能空间GUI的点击事件:
 * - 点击技能球: 查看详情或绑定到快捷栏
 * - 点击快捷栏槽位: 解绑技能
 */
public class SkillSpaceGUIListener implements Listener {

    private final GuangDianClass plugin;
    private final SkillSpaceManager skillSpaceManager;
    private final SkillSpaceGUI skillSpaceGUI;
    
    // 玩家选择绑定技能的状态 (UUID -> 技能ID)
    private final Map<UUID, String> pendingBindings = new HashMap<>();

    public SkillSpaceGUIListener(GuangDianClass plugin) {
        this.plugin = plugin;
        this.skillSpaceManager = plugin.getSkillSpaceManager();
        this.skillSpaceGUI = new SkillSpaceGUI(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否为技能空间GUI
        if (event.getInventory().getHolder() instanceof SkillSpaceGUI.SkillSpaceHolder) {
            handleSkillSpaceClick(event);
            return;
        }
        
        // 检查是否为技能详情GUI
        if (event.getInventory().getHolder() instanceof SkillDetailHolder) {
            handleSkillDetailClick(event);
            return;
        }
    }
    
    /**
     * 处理技能空间GUI点击
     */
    private void handleSkillSpaceClick(InventoryClickEvent event) {
        // 取消所有点击事件,防止物品移动
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 如果点击的是玩家背包,不做任何处理
        if (slot >= event.getInventory().getSize()) {
            return;
        }
        
        // 点击主动技能区 (第1-2行, 槽位 0-17)
        if (slot >= 0 && slot < 18) {
            handleSkillClick(player, slot, event.getCurrentItem(), event.getClick());
            return;
        }
        
        // 点击被动技能区 (第3-4行, 槽位 18-35)
        if (slot >= 18 && slot < 36) {
            handlePassiveSkillClick(player, event.getCurrentItem());
            return;
        }
        
        // 点击快捷栏绑定预览 (第5行, 槽位 36-44)
        if (slot >= 36 && slot < 45) {
            handleHotbarClick(player, slot - 36, event.getCurrentItem());
            return;
        }
    }
    
    /**
     * 处理技能详情GUI点击
     */
    private void handleSkillDetailClick(InventoryClickEvent event) {
        // 取消所有点击事件,防止物品移动
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 如果点击的是玩家背包,不做任何处理
        if (slot >= event.getInventory().getSize()) {
            return;
        }
        
        // 点击返回按钮 (槽位 11)
        if (slot == 11) {
            skillSpaceGUI.open(player);
            return;
        }
        
        // 点击绑定按钮 (槽位 15)
        if (slot == 15) {
            // 从GUI标题中提取技能名称
            String title = event.getView().getTitle();
            String skillName = title.replace("§6§l技能详情 - ", "");
            
            // 查找对应的技能
            PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
            for (SkillOrb skill : skillData.getSkillSpace().values()) {
                if (skill.getName().equals(skillName)) {
                    // 开始绑定流程
                    pendingBindings.put(player.getUniqueId(), skill.getSkillId());
                    player.closeInventory();
                    player.sendMessage(Component.text("§a请打开技能空间,然后点击快捷栏槽位以绑定技能 §e" + skill.getName()));
                    return;
                }
            }
        }
    }

    /**
     * 处理主动技能点击
     */
    private void handleSkillClick(Player player, int slot, ItemStack item, ClickType clickType) {
        if (item == null || item.getType() == Material.AIR) return;
        
        String skillId = skillSpaceManager.getSkillIdFromItem(item);
        if (skillId == null) return;
        
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        SkillOrb skill = skillData.getSkill(skillId);
        
        if (skill == null) return;
        
        // 检查是否解锁
        if (!skill.isUnlocked()) {
            player.sendMessage(Component.text("§c技能未解锁!"));
            return;
        }
        
        // 左键: 打开技能详情GUI
        if (clickType == ClickType.LEFT) {
            openSkillDetailGUI(player, skill);
            return;
        }
        
        // 右键: 开始绑定流程
        if (clickType == ClickType.RIGHT) {
            pendingBindings.put(player.getUniqueId(), skillId);
            player.sendMessage(Component.text("§a请点击快捷栏槽位以绑定技能 §e" + skill.getName()));
            return;
        }
        
        // Shift+左键: 快速绑定到第一个空闲槽位
        if (clickType == ClickType.SHIFT_LEFT) {
            bindToFirstEmptySlot(player, skillId);
            return;
        }
    }

    /**
     * 处理被动技能点击
     */
    private void handlePassiveSkillClick(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        String skillId = skillSpaceManager.getSkillIdFromItem(item);
        if (skillId == null) return;
        
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        SkillOrb skill = skillData.getSkill(skillId);
        
        if (skill == null) return;
        
        // 被动技能只能查看详情
        openSkillDetailGUI(player, skill);
    }

    /**
     * 处理快捷栏点击
     */
    private void handleHotbarClick(Player player, int hotbarSlot, ItemStack item) {
        UUID playerId = player.getUniqueId();
        
        // 检查是否有待绑定的技能
        if (pendingBindings.containsKey(playerId)) {
            String skillId = pendingBindings.remove(playerId);
            skillSpaceManager.bindSkillToHotbar(player, skillId, hotbarSlot);
            
            // 刷新GUI
            skillSpaceGUI.open(player);
            return;
        }
        
        // 解绑技能
        if (item != null && item.getType() != Material.AIR && 
            item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            skillSpaceManager.unbindSkillFromHotbar(player, hotbarSlot);
            
            // 刷新GUI
            skillSpaceGUI.open(player);
        }
    }

    /**
     * 打开技能详情GUI
     */
    private void openSkillDetailGUI(Player player, SkillOrb skill) {
        Inventory detailGUI = Bukkit.createInventory(
            new SkillDetailHolder(),
            27,
            Component.text("§6§l技能详情 - " + skill.getName())
        );
        
        // 技能信息物品
        ItemStack infoItem = createSkillInfoItem(skill);
        detailGUI.setItem(13, infoItem);
        
        // 如果是主动技能且已解锁,显示绑定按钮
        if (skill.isActive() && skill.isUnlocked()) {
            ItemStack bindButton = createBindButton(skill);
            detailGUI.setItem(15, bindButton);
        }
        
        // 返回按钮
        ItemStack backButton = createBackButton();
        detailGUI.setItem(11, backButton);
        
        player.openInventory(detailGUI);
    }
    
    /**
     * 技能详情GUI的Holder
     */
    public static class SkillDetailHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /**
     * 创建技能信息物品
     */
    private ItemStack createSkillInfoItem(SkillOrb skill) {
        ItemStack item = new ItemStack(skill.getMaterial());
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§6" + skill.getName())
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("§7类型: " + (skill.isActive() ? "§e主动技能" : "§b被动技能")));
        lore.add(Component.empty());
        
        if (skill.isActive()) {
            lore.add(Component.text("§7伤害倍率: §c" + skill.getDamageMult() + "x"));
            lore.add(Component.text("§7范围: §a" + skill.getRange() + " 格"));
            lore.add(Component.text("§7冷却: §b" + skill.getCooldown() + " 秒"));
            lore.add(Component.text("§7法力消耗: §d" + skill.getManaCost()));
        } else {
            lore.add(Component.text("§7触发概率: §a" + skill.getTriggerChance() + "%"));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("§f" + skill.getDescription()));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * 创建绑定按钮
     */
    private ItemStack createBindButton(SkillOrb skill) {
        ItemStack item = new ItemStack(Material.EMERALD);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§a§l绑定到快捷栏")
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        
        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("§7点击选择快捷栏槽位"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * 创建返回按钮
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§c§l返回技能空间")
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 绑定到第一个空闲槽位
     */
    private void bindToFirstEmptySlot(Player player, String skillId) {
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        
        for (int i = 0; i < 9; i++) {
            if (skillData.getHotbarBinding(i) == null) {
                skillSpaceManager.bindSkillToHotbar(player, skillId, i);
                return;
            }
        }
        
        player.sendMessage(Component.text("§c快捷栏已满!请先解绑一个技能."));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // 清理待绑定的技能状态，防止内存泄漏
        pendingBindings.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // 禁止在技能空间GUI和技能详情GUI中拖拽
        if (event.getInventory().getHolder() instanceof SkillSpaceGUI.SkillSpaceHolder ||
            event.getInventory().getHolder() instanceof SkillDetailHolder) {
            event.setCancelled(true);
        }
    }
}
