package cn.guangdian.classsystem.gui;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.manager.SkillSpaceManager;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.classsystem.model.PlayerSkillData;
import cn.guangdian.classsystem.model.SkillOrb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能空间GUI
 * 
 * 显示玩家的所有技能,包括主动技能和被动技能
 */
public class SkillSpaceGUI {

    private final GuangDianClass plugin;
    private final SkillSpaceManager skillSpaceManager;

    public SkillSpaceGUI(GuangDianClass plugin) {
        this.plugin = plugin;
        this.skillSpaceManager = plugin.getSkillSpaceManager();
    }

    /**
     * 打开技能空间GUI
     */
    public void open(Player player) {
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        
        // 创建GUI
        Inventory gui = Bukkit.createInventory(
            new SkillSpaceHolder(),
            54,
            Component.text("§6§l技能空间")
        );
        
        // 填充主动技能区 (第1-2行, 槽位 0-17)
        fillActiveSkills(gui, skillData);
        
        // 填充被动技能区 (第3-4行, 槽位 18-35)
        fillPassiveSkills(gui, skillData);
        
        // 填充快捷栏绑定预览 (第5行, 槽位 36-44)
        fillHotbarPreview(gui, skillData);
        
        // 填充信息区 (第6行, 槽位 45-53)
        fillInfoArea(gui, player, skillData);
        
        player.openInventory(gui);
    }

    /**
     * 填充主动技能区 - 只显示已解锁的技能
     */
    private void fillActiveSkills(Inventory gui, PlayerSkillData skillData) {
        int slot = 0;
        
        // 只显示已解锁的主动技能
        for (SkillOrb skill : skillData.getSkillSpace().values()) {
            if (skill.isActive() && skill.isUnlocked()) {
                ItemStack orb = skillSpaceManager.createSkillOrb(skill);
                gui.setItem(slot, orb);
                slot++;
                
                if (slot >= 18) break; // 前2行
            }
        }
        
        // 填充空槽位
        while (slot < 18) {
            gui.setItem(slot, createEmptySlot("主动技能"));
            slot++;
        }
    }

    /**
     * 填充被动技能区 - 只显示已解锁的技能
     */
    private void fillPassiveSkills(Inventory gui, PlayerSkillData skillData) {
        int slot = 18;
        
        // 只显示已解锁的被动技能
        for (SkillOrb skill : skillData.getSkillSpace().values()) {
            if (skill.isPassive() && skill.isUnlocked()) {
                ItemStack orb = skillSpaceManager.createPassiveSkillOrb(skill);
                gui.setItem(slot, orb);
                slot++;
                
                if (slot >= 36) break; // 第3-4行
            }
        }
        
        // 填充空槽位
        while (slot < 36) {
            gui.setItem(slot, createEmptySlot("被动技能"));
            slot++;
        }
    }

    /**
     * 填充快捷栏绑定预览
     */
    private void fillHotbarPreview(Inventory gui, PlayerSkillData skillData) {
        for (int i = 0; i < 9; i++) {
            String skillId = skillData.getHotbarBinding(i);
            
            if (skillId != null) {
                SkillOrb skill = skillData.getSkill(skillId);
                if (skill != null && skill.isUnlocked()) {
                    gui.setItem(36 + i, skillSpaceManager.createSkillOrb(skill));
                } else {
                    gui.setItem(36 + i, createEmptyHotbarSlot(i + 1));
                }
            } else {
                gui.setItem(36 + i, createEmptyHotbarSlot(i + 1));
            }
        }
    }

    /**
     * 填充信息区
     */
    private void fillInfoArea(Inventory gui, Player player, PlayerSkillData skillData) {
        // 获取玩家职业信息
        PlayerClassData classData = plugin.getPlayerData(player);
        String className = "无职业";
        int tier = 0;
        
        if (classData != null) {
            className = classData.getClassId();
            tier = classData.getTier();
        }
        
        // 创建信息物品
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        
        meta.displayName(Component.text("§6§l技能信息")
            .decorate(TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7职业: §e" + className));
        lore.add(Component.text("§7等级: §6Lv." + tier));
        lore.add(Component.empty());
        lore.add(Component.text("§7已解锁主动技能: §a" + skillData.getUnlockedActiveSkillCount()));
        lore.add(Component.text("§7已解锁被动技能: §a" + skillData.getUnlockedPassiveSkillCount()));
        lore.add(Component.empty());
        lore.add(Component.text("§e左键 §7查看技能详情"));
        lore.add(Component.text("§e右键 §7绑定到快捷栏"));
        
        meta.lore(lore);
        info.setItemMeta(meta);
        
        gui.setItem(49, info);
        
        // 填充装饰物品
        for (int i = 45; i < 54; i++) {
            if (i != 49) {
                gui.setItem(i, createDecorationItem());
            }
        }
    }

    /**
     * 创建空槽位
     */
    private ItemStack createEmptySlot(String type) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§7空槽位 - " + type));
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 创建空快捷栏槽位
     */
    private ItemStack createEmptyHotbarSlot(int slot) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§7快捷栏 §6" + slot + " §7- 空闲"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§e点击 §7绑定技能到此槽位"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * 创建装饰物品
     */
    private ItemStack createDecorationItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * 技能空间GUI持有者
     */
    public static class SkillSpaceHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
