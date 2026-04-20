package cn.guangdian.rpgskill.api;

import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillResult;
import cn.guangdian.rpgskill.service.SkillService;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * RPGSkill 对外 API
 * 其他插件通过此 API 与技能系统交互
 */
public class RPGSkillAPI {

    private final SkillService skillService;

    public RPGSkillAPI(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 获取技能定义
     *
     * @param skillId 技能ID
     * @return 技能定义
     */
    public Optional<SkillDefinition> getSkill(String skillId) {
        return skillService.getSkill(skillId);
    }

    /**
     * 执行技能
     *
     * @param skillId 技能ID
     * @param context 技能上下文
     * @return 执行结果
     */
    public SkillResult executeSkill(String skillId, SkillContext context) {
        return skillService.executeSkill(skillId, context);
    }

    /**
     * 检查技能是否在冷却中
     *
     * @param playerId 玩家UUID
     * @param skillId  技能ID
     * @return 是否在冷却中
     */
    public boolean isOnCooldown(String playerId, String skillId) {
        return skillService.isOnCooldown(playerId, skillId);
    }

    /**
     * 获取剩余冷却时间（秒）
     *
     * @param playerId 玩家UUID
     * @param skillId  技能ID
     * @return 剩余冷却时间（秒）
     */
    public long getCooldownRemaining(String playerId, String skillId) {
        return skillService.getCooldownRemaining(playerId, skillId);
    }

    /**
     * 设置技能冷却
     *
     * @param playerId 玩家UUID
     * @param skillId  技能ID
     * @param seconds  冷却时间（秒）
     */
    public void setCooldown(String playerId, String skillId, long seconds) {
        skillService.setCooldown(playerId, skillId, seconds);
    }

    /**
     * 清除玩家的技能冷却
     *
     * @param playerId 玩家UUID
     */
    public void clearCooldowns(String playerId) {
        skillService.clearCooldowns(playerId);
    }

    /**
     * 检查玩家是否可以执行技能
     *
     * @param player  玩家
     * @param skillId 技能ID
     * @return 是否可以执行
     */
    public boolean canExecute(Player player, String skillId) {
        return skillService.canExecute(player, skillId);
    }
}
