package cn.guangdian.classsystem.skill;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.enums.*;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 技能管理器 - 使用枚举简化技能系统
 * 
 * 设计说明：
 * - 使用 Skill 枚举直接定义所有技能，无需配置文件加载
 * - GuangDianClass 定义技能配置
 * - GuangDianArmorStats 执行技能效果（可选）
 * - 本类作为适配器，连接两个插件
 */
public class SkillManager {
    
    private final GuangDianClass plugin;
    
    // GuangDianArmorStats 技能执行器引用（可选）
    private Object armorStatsSkillManager;
    
    public SkillManager(GuangDianClass plugin) {
        this.plugin = plugin;
        hookArmorStats();
        plugin.getLogger().info("技能管理器已初始化，使用枚举定义技能");
    }
    
    /**
     * 钩子 GuangDianArmorStats 技能执行系统（可选）
     */
    private void hookArmorStats() {
        var armorStatsPlugin = Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (armorStatsPlugin != null && armorStatsPlugin.isEnabled()) {
            try {
                var getSkillManagerMethod = armorStatsPlugin.getClass().getMethod("getSkillManager");
                armorStatsSkillManager = getSkillManagerMethod.invoke(armorStatsPlugin);
                plugin.getLogger().info("已成功钩子 GuangDianArmorStats 技能执行系统");
            } catch (Exception e) {
                plugin.getLogger().warning("无法钩子 GuangDianArmorStats: " + e.getMessage());
                armorStatsSkillManager = null;
            }
        } else {
            plugin.getLogger().info("GuangDianArmorStats 未启用，使用本地技能执行");
            armorStatsSkillManager = null;
        }
    }
    
    /**
     * 触发主动技能
     */
    public boolean triggerActiveSkill(Player player, String skillId) {
        Skill skill = Skill.fromId(skillId);
        if (skill == null) {
            player.sendMessage(Component.text("技能不存在！").color(NamedTextColor.RED));
            return false;
        }
        
        if (!skill.isActive()) {
            player.sendMessage(Component.text("这不是主动技能！").color(NamedTextColor.RED));
            return false;
        }
        
        // 检查玩家是否拥有该技能
        if (!hasSkill(player, skill)) {
            player.sendMessage(Component.text("你还没有解锁这个技能！").color(NamedTextColor.RED));
            return false;
        }
        
        // 检查冷却
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), skillId)) {
            int remaining = plugin.getCooldownManager().getRemainingCooldownSeconds(player.getUniqueId(), skillId);
            player.sendMessage(Component.text("技能冷却中，剩余 " + remaining + " 秒").color(NamedTextColor.RED));
            return false;
        }
        
        // 检查魔力
        if (plugin.getManaManager().getMana(player) < skill.getManaCost()) {
            player.sendMessage(Component.text("魔力不足！").color(NamedTextColor.RED));
            return false;
        }
        
        // 消耗魔力
        plugin.getManaManager().consumeMana(player, skill.getManaCost());
        
        // 设置冷却
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), skillId, skill.getCooldown());
        
        // 执行技能
        executeSkill(player, skill);
        
        player.sendMessage(Component.text("使用技能: " + skill.getName()).color(NamedTextColor.GOLD));
        return true;
    }
    
    /**
     * 执行技能效果
     */
    private void executeSkill(Player player, Skill skill) {
        if (armorStatsSkillManager != null) {
            tryExecuteArmorStatsSkill(player, skill);
        } else {
            plugin.getSkillEffectExecutor().executeSkill(player, skill);
        }
    }
    
    /**
     * 尝试调用 ArmorStats 技能执行系统
     */
    private void tryExecuteArmorStatsSkill(Player player, Skill skill) {
        if (armorStatsSkillManager == null) return;
        
        try {
            var skillClass = Class.forName("cn.guangdian.armorstats.skill.Skill");
            var triggerMethod = armorStatsSkillManager.getClass().getMethod(
                "triggerSkill", Player.class, String.class, double.class
            );
            triggerMethod.invoke(armorStatsSkillManager, player, skill.getId(), skill.getDamageMult());
        } catch (Exception e) {
            plugin.getLogger().warning("调用 ArmorStats 技能执行失败: " + e.getMessage());
            plugin.getSkillEffectExecutor().executeSkill(player, skill);
        }
    }
    
    /**
     * 触发被动技能
     */
    public boolean triggerPassiveSkill(Player player, Skill skill) {
        if (skill == null || !skill.isPassive()) return false;
        
        // 检查玩家是否拥有该技能
        if (!hasSkill(player, skill)) return false;
        
        // 执行被动技能效果
        if (armorStatsSkillManager != null) {
            tryTriggerArmorStatsPassiveSkill(player, skill);
        } else {
            plugin.getSkillEffectExecutor().executePassiveSkill(player, skill);
        }
        
        return true;
    }
    
    /**
     * 尝试调用 ArmorStats 被动技能触发
     */
    private void tryTriggerArmorStatsPassiveSkill(Player player, Skill skill) {
        if (armorStatsSkillManager == null) return;
        
        try {
            var triggerMethod = armorStatsSkillManager.getClass().getMethod(
                "triggerPassiveSkill", Player.class, String.class
            );
            triggerMethod.invoke(armorStatsSkillManager, player, skill.getId());
        } catch (Exception e) {
            plugin.getSkillEffectExecutor().executePassiveSkill(player, skill);
        }
    }
    
    /**
     * 检查玩家是否拥有技能
     */
    public boolean hasSkill(Player player, Skill skill) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return false;
        
        GameClass gameClass = plugin.getClassManager().getClass(data.getClassId());
        if (gameClass == null) return false;
        
        // 检查途径匹配
        Pathway playerPathway = Pathway.fromId(gameClass.getPathway());
        if (playerPathway != skill.getPathway()) return false;
        
        // 检查序列（玩家序列 <= 技能序列表示已解锁，序列越低越强）
        Sequence playerSequence = Sequence.fromLevel(gameClass.getSequence());
        if (playerSequence.getLevel() > skill.getSequence().getLevel()) return false;
        
        // 检查分支（如果有分支要求）
        Branch playerBranch = Branch.fromId(gameClass.getBranch());
        if (skill.getBranch() != Branch.NONE && playerBranch != skill.getBranch()) {
            // 如果技能有分支要求，但玩家分支不匹配，检查是否是基础技能
            if (skill.getBranch() != Branch.NONE) return false;
        }
        
        return true;
    }
    
    /**
     * 获取玩家解锁的技能列表（只有当前职业的技能）
     */
    public Skill[] getUnlockedSkills(Player player) {
        PlayerClassData data = plugin.getPlayerData(player);
        if (data == null) return new Skill[0];
        
        GameClass gameClass = plugin.getClassManager().getClass(data.getClassId());
        if (gameClass == null) return new Skill[0];
        
        // 只获取当前职业的途径技能
        Pathway pathway = Pathway.fromId(gameClass.getPathway());
        if (pathway == null) return new Skill[0];
        
        Sequence sequence = Sequence.fromLevel(gameClass.getSequence());
        Branch branch = Branch.fromId(gameClass.getBranch());
        
        // 获取途径的所有技能
        Skill[] pathwaySkills = Skill.getSkillsByPathway(pathway);
        
        // 筛选已解锁的技能（只包含当前职业的技能）
        int count = 0;
        for (Skill skill : pathwaySkills) {
            // 序列 <= 技能序列表示已解锁（序列越低越强）
            if (sequence.getLevel() <= skill.getSequence().getLevel()) {
                // 检查分支匹配
                if (skill.getBranch() == Branch.NONE || skill.getBranch() == branch) {
                    count++;
                }
            }
        }
        
        Skill[] unlocked = new Skill[count];
        int index = 0;
        for (Skill skill : pathwaySkills) {
            if (sequence.getLevel() <= skill.getSequence().getLevel()) {
                if (skill.getBranch() == Branch.NONE || skill.getBranch() == branch) {
                    unlocked[index++] = skill;
                }
            }
        }
        
        return unlocked;
    }
    
    /**
     * 发送技能列表给玩家
     */
    public void sendSkillList(Player player) {
        Skill[] skills = getUnlockedSkills(player);
        
        if (skills.length == 0) {
            player.sendMessage(Component.text("你还没有解锁任何技能！").color(NamedTextColor.RED));
            return;
        }
        
        PlayerClassData classData = plugin.getPlayerData(player);
        GameClass gameClass = classData != null ? plugin.getClassManager().getClass(classData.getClassId()) : null;
        
        if (gameClass != null) {
            player.sendMessage(Component.text("========== " + gameClass.getPathwayName() + "途径技能 ==========")
                .color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("当前序列: " + gameClass.getSequenceName())
                .color(NamedTextColor.YELLOW));
        }
        
        for (Skill skill : skills) {
            String typeIcon = skill.isPassive() ? "[被动]" : "[主动]";
            player.sendMessage(Component.text(typeIcon + " " + skill.getName())
                .color(NamedTextColor.YELLOW)
                .append(Component.text(" - " + skill.getSequence().getName())
                    .color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));
    }
    
    /**
     * 获取技能定义（通过枚举）
     */
    public Skill getSkill(String skillId) {
        return Skill.fromId(skillId);
    }
    
    /**
     * 重新加载
     */
    public void reload() {
        hookArmorStats();
    }
}