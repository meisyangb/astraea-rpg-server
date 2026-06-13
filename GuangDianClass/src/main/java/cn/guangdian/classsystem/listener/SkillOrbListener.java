package cn.guangdian.classsystem.listener;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.gui.SkillSpaceGUI;
import cn.guangdian.classsystem.manager.SkillSpaceManager;
import cn.guangdian.classsystem.model.PlayerClassData;
import cn.guangdian.classsystem.model.PlayerSkillData;
import cn.guangdian.classsystem.model.SkillOrb;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * 技能球监听器
 * 
 * 监听玩家使用技能球的事件:
 * - 右键: 使用技能
 * - Shift+右键: 打开技能空间GUI
 */
public class SkillOrbListener implements Listener {

    private final GuangDianClass plugin;
    private final SkillSpaceManager skillSpaceManager;
    private final SkillSpaceGUI skillSpaceGUI;

    public SkillOrbListener(GuangDianClass plugin) {
        this.plugin = plugin;
        this.skillSpaceManager = plugin.getSkillSpaceManager();
        this.skillSpaceGUI = new SkillSpaceGUI(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理主手事件
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // 只处理右键事件
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // 检查是否为技能球
        String skillId = skillSpaceManager.getSkillIdFromItem(item);
        if (skillId == null) return;
        
        // 取消原始事件
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Shift+右键: 打开技能空间GUI
        if (player.isSneaking()) {
            skillSpaceGUI.open(player);
            return;
        }
        
        // 右键: 使用技能
        useSkill(player, skillId);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 只处理主手事件
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // 检查是否为技能球
        String skillId = skillSpaceManager.getSkillIdFromItem(item);
        if (skillId == null) return;
        
        // 取消原始事件
        event.setCancelled(true);
        
        // Shift+右键: 打开技能空间GUI
        if (player.isSneaking()) {
            skillSpaceGUI.open(player);
            return;
        }
        
        // 右键实体: 使用技能攻击目标
        Entity target = event.getRightClicked();
        useSkillOnTarget(player, skillId, target);
    }

    /**
     * 使用技能
     */
    private void useSkill(org.bukkit.entity.Player player, String skillId) {
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        SkillOrb skill = skillData.getSkill(skillId);
        
        if (skill == null) {
            player.sendMessage(Component.text("§c§l[错误] §f技能不存在!"));
            return;
        }
        
        // 检查是否解锁
        if (!skill.isUnlocked()) {
            player.sendMessage(Component.text("§c§l[错误] §f技能 §e" + skill.getName() + " §f未解锁!"));
            player.sendMessage(Component.text("§7提示: 提升职业等级可以解锁更多技能"));
            return;
        }
        
        // 检查是否为主动技能
        if (skill.isPassive()) {
            player.sendMessage(Component.text("§c§l[错误] §f被动技能 §b" + skill.getName() + " §f无法手动使用!"));
            player.sendMessage(Component.text("§7提示: 被动技能会自动触发效果"));
            return;
        }
        
        // 检查冷却
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), "skill_" + skillId)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(
                player.getUniqueId(), "skill_" + skillId);
            long remainingSeconds = remaining / 1000;
            player.sendMessage(Component.text("§c§l[冷却中] §f技能 §e" + skill.getName() + " §f正在冷却!"));
            player.sendMessage(Component.text("§7剩余时间: §c" + remainingSeconds + " §7秒"));
            return;
        }
        
        // 检查法力消耗
        if (skill.getManaCost() > 0) {
            if (!plugin.getManaManager().hasEnoughMana(player, skill.getManaCost())) {
                double current = plugin.getManaManager().getCurrentMana(player);
                player.sendMessage(Component.text("§c§l[魔力不足] §f魔力值不足!"));
                player.sendMessage(Component.text("§7需要: §d" + skill.getManaCost() + 
                    " §7当前: §b" + String.format("%.1f", current)));
                return;
            }
        }
        
        // 触发技能（无目标）
        triggerSkill(player, skill, null);
        
        // 消耗魔力值
        if (skill.getManaCost() > 0) {
            plugin.getManaManager().consumeMana(player, skill.getManaCost());
        }
        
        // 设置冷却
        plugin.getCooldownManager().setCooldown(
            player.getUniqueId(), 
            "skill_" + skillId, 
            skill.getCooldown() * 1000L
        );
    }
    
    /**
     * 对目标使用技能
     */
    private void useSkillOnTarget(Player player, String skillId, Entity target) {
        PlayerSkillData skillData = skillSpaceManager.getPlayerSkillData(player);
        SkillOrb skill = skillData.getSkill(skillId);
        
        if (skill == null) {
            player.sendMessage(Component.text("§c§l[错误] §f技能不存在!"));
            return;
        }
        
        // 检查是否解锁
        if (!skill.isUnlocked()) {
            player.sendMessage(Component.text("§c§l[错误] §f技能 §e" + skill.getName() + " §f未解锁!"));
            return;
        }
        
        // 检查是否为主动技能
        if (skill.isPassive()) {
            player.sendMessage(Component.text("§c§l[错误] §f被动技能无法手动使用!"));
            return;
        }
        
        // 检查冷却
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), "skill_" + skillId)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(
                player.getUniqueId(), "skill_" + skillId);
            player.sendMessage(Component.text("§c技能冷却中,剩余 §e" + (remaining / 1000) + " §c秒!"));
            return;
        }
        
        // 检查法力消耗
        if (skill.getManaCost() > 0) {
            if (!plugin.getManaManager().hasEnoughMana(player, skill.getManaCost())) {
                double current = plugin.getManaManager().getCurrentMana(player);
                player.sendMessage(Component.text("§c§l[魔力不足] §f魔力值不足!"));
                player.sendMessage(Component.text("§7需要: §d" + skill.getManaCost() + 
                    " §7当前: §b" + String.format("%.1f", current)));
                return;
            }
        }
        
        // 触发技能（有目标）
        triggerSkill(player, skill, target);
        
        // 消耗魔力值
        if (skill.getManaCost() > 0) {
            plugin.getManaManager().consumeMana(player, skill.getManaCost());
        }
        
        // 设置冷却
        plugin.getCooldownManager().setCooldown(
            player.getUniqueId(), 
            "skill_" + skillId, 
            skill.getCooldown() * 1000L
        );
    }

    /**
     * 触发技能效果
     */
    private void triggerSkill(org.bukkit.entity.Player player, SkillOrb skill, Entity target) {
        // 发送详细的技能使用信息
        player.sendMessage(Component.text("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Component.text("§e§l技能触发 §6» §f" + skill.getName()));
        player.sendMessage(Component.text("§7类型: §b" + (skill.isActive() ? "主动技能" : "被动技能")));
        player.sendMessage(Component.text("§7伤害倍率: §c" + String.format("%.1f", skill.getDamageMult()) + "x"));
        player.sendMessage(Component.text("§7范围: §a" + String.format("%.1f", skill.getRange()) + " 格"));
        player.sendMessage(Component.text("§7冷却时间: §e" + skill.getCooldown() + " 秒"));
        if (skill.getManaCost() > 0) {
            player.sendMessage(Component.text("§7法力消耗: §d" + skill.getManaCost()));
        }
        player.sendMessage(Component.text("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // 计算基础伤害
        double baseDamage = calculateBaseDamage(player);
        double finalDamage = baseDamage * skill.getDamageMult();
        
        // 使用技能效果执行器执行技能效果
        plugin.getSkillEffectExecutor().executeSkillEffect(player, skill, target, finalDamage);
    }
    
    /**
     * 计算玩家基础伤害
     */
    private double calculateBaseDamage(Player player) {
        // 获取玩家职业数据
        PlayerClassData classData = plugin.getPlayerData(player);
        if (classData == null) {
            return 10.0; // 默认伤害
        }
        
        // 获取力量属性
        int strength = classData.getAllocatedAttributes().getOrDefault("strength", 0);
        
        // 基础伤害 = 10 + 力量 * 0.5
        return 10.0 + strength * 0.5;
    }
}
