package cn.guangdian.rpgskill.service;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgskill.RPGSkill;
import cn.guangdian.rpgskill.cooldown.CooldownManager;
import cn.guangdian.rpgskill.registry.SkillRegistry;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * 技能服务
 * 核心服务类，处理技能执行逻辑
 */
public class SkillService {

    private final RPGSkill plugin;
    private final SkillRegistry skillRegistry;
    private final CooldownManager cooldownManager;

    public SkillService(RPGSkill plugin, SkillRegistry skillRegistry, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.skillRegistry = skillRegistry;
        this.cooldownManager = cooldownManager;
    }

    /**
     * 获取技能定义
     */
    public Optional<SkillDefinition> getSkill(String skillId) {
        return skillRegistry.getSkill(skillId);
    }

    /**
     * 执行技能
     */
    public SkillResult executeSkill(String skillId, SkillContext context) {
        Optional<SkillDefinition> skillOpt = skillRegistry.getSkill(skillId);
        if (skillOpt.isEmpty()) {
            return SkillResult.failure("技能不存在: " + skillId);
        }

        SkillDefinition skill = skillOpt.get();
        Player player = context.getCaster();
        String playerId = player.getUniqueId().toString();

        // 检查冷却
        if (cooldownManager.isOnCooldown(playerId, skillId, skill.getCooldown())) {
            long remaining = cooldownManager.getCooldownRemaining(playerId, skillId, skill.getCooldown());
            sendCooldownMessage(player, skill.getName(), remaining);
            return SkillResult.cooldown(remaining);
        }

        // 检查法力（预留接口）
        // TODO: 集成法力系统

        // 执行技能
        if (skill.getExecutor() == null) {
            return SkillResult.failure("技能执行器未配置: " + skillId);
        }
        SkillResult result = skill.getExecutor().execute(skill, context);

        // 设置冷却
        if (result.isSuccess() && skill.getCooldown() > 0) {
            cooldownManager.setCooldown(playerId, skillId);
        }

        // 发送成功消息
        if (result.isSuccess() && result.getMessage() != null) {
            MiniMessageService mm = MiniMessageService.getInstance();
            player.sendMessage(mm.colorize("<green>" + result.getMessage()));
        }

        return result;
    }

    /**
     * 检查是否可以执行技能
     */
    public boolean canExecute(Player player, String skillId) {
        Optional<SkillDefinition> skillOpt = skillRegistry.getSkill(skillId);
        if (skillOpt.isEmpty()) return false;

        SkillDefinition skill = skillOpt.get();
        String playerId = player.getUniqueId().toString();

        return !cooldownManager.isOnCooldown(playerId, skillId, skill.getCooldown());
    }

    /**
     * 检查技能是否在冷却中
     */
    public boolean isOnCooldown(String playerId, String skillId) {
        Optional<SkillDefinition> skillOpt = skillRegistry.getSkill(skillId);
        if (skillOpt.isEmpty()) return false;

        SkillDefinition skill = skillOpt.get();
        return cooldownManager.isOnCooldown(playerId, skillId, skill.getCooldown());
    }

    /**
     * 获取剩余冷却时间
     */
    public long getCooldownRemaining(String playerId, String skillId) {
        Optional<SkillDefinition> skillOpt = skillRegistry.getSkill(skillId);
        if (skillOpt.isEmpty()) return 0;

        SkillDefinition skill = skillOpt.get();
        return cooldownManager.getCooldownRemaining(playerId, skillId, skill.getCooldown());
    }

    /**
     * 设置技能冷却
     */
    public void setCooldown(String playerId, String skillId, long seconds) {
        cooldownManager.setCooldown(playerId, skillId);
    }

    /**
     * 清除玩家冷却
     */
    public void clearCooldowns(String playerId) {
        cooldownManager.clearPlayerCooldowns(playerId);
    }

    private void sendCooldownMessage(Player player, String skillName, long remaining) {
        MiniMessageService mm = MiniMessageService.getInstance();
        String message = String.format("<red>技能 %s 冷却中，剩余 %d 秒", skillName, remaining);
        player.sendMessage(mm.colorize(message));
    }
}
