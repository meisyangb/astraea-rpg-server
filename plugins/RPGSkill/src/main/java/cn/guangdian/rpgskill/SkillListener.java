package cn.guangdian.rpgskill;

import cn.guangdian.rpgskill.service.SkillService;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/**
 * 技能事件监听器
 * 
 * <p>注意：玩家交互技能（右键/左键点击）由 RPGItems 处理，
 * 此监听器只处理战斗相关事件（ON_HIT, ON_DAMAGE_TAKEN）</p>
 */
public class SkillListener implements Listener {

    private final SkillService skillService;

    public SkillListener(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 实体被攻击事件 - 触发 ON_HIT 类型被动技能
     * 
     * <p>注意：此事件只处理技能定义中 trigger 为 ON_HIT 的技能，
     * 物品绑定的技能由 RPGItems 统一处理</p>
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();

        // 获取玩家当前激活的被动技能（从玩家数据中）
        // 注意：物品绑定的技能由 RPGItems 处理，这里只处理玩家学习的被动技能
        Optional<SkillDefinition> skillOpt = getPlayerPassiveSkill(player, TriggerType.ON_HIT);
        if (skillOpt.isEmpty()) return;

        SkillDefinition skill = skillOpt.get();

        // 检查触发类型
        if (skill.getTrigger() == TriggerType.ON_HIT &&
            event.getEntity() instanceof org.bukkit.entity.LivingEntity) {

            SkillContext context = SkillContext.builder(player)
                    .target((org.bukkit.entity.LivingEntity) event.getEntity())
                    .baseDamage(event.getDamage())
                    .build();

            skillService.executeSkill(skill.getId(), context);
        }
    }

    /**
     * 获取玩家的被动技能
     * 
     * <p>从玩家数据中获取当前激活的被动技能，
     * 不是从物品获取</p>
     */
    private Optional<SkillDefinition> getPlayerPassiveSkill(Player player, TriggerType triggerType) {
        // TODO: 从玩家数据系统获取玩家学习的被动技能
        // 目前返回空，因为被动技能系统还未实现
        return Optional.empty();
    }
}
