package cn.guangdian.rpgitems.integration;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgskill.api.RPGSkillAPI;
import cn.guangdian.rpgskill.skill.SkillContext;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillResult;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * RPGSkill 集成服务
 * <p>负责通过 RPGCore 服务注册表获取 RPGSkillAPI，并执行技能</p>
 */
public class RPGSkillIntegration {

    private final RPGItems plugin;
    private RPGSkillAPI skillAPI;
    private boolean enabled = false;
    private final MiniMessageService miniMessage;

    public RPGSkillIntegration(RPGItems plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
        initialize();
    }

    /**
     * 初始化 RPGSkill 集成
     * <p>通过 RPGCore 服务注册表获取 RPGSkillAPI</p>
     */
    private void initialize() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            plugin.getLogger().warning("RPGCore 未加载，RPGSkill 集成不可用");
            return;
        }

        // 通过服务注册表获取 RPGSkillAPI（使用 Optional 避免异常）
        ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
        if (serviceRegistry == null) {
            plugin.getLogger().warning("ServiceRegistry 不可用，RPGSkill 集成不可用");
            return;
        }

        Optional<RPGSkillAPI> skillAPIOpt = serviceRegistry.getOptionalService(RPGSkillAPI.class);
        if (skillAPIOpt.isEmpty()) {
            plugin.getLogger().info("RPGSkill 服务未注册，技能绑定功能不可用");
            return;
        }

        this.skillAPI = skillAPIOpt.get();
        this.enabled = true;
        plugin.getLogger().info("RPGSkill 集成已启用（通过服务注册表）");
    }

    /**
     * 检查集成是否可用
     */
    public boolean isEnabled() {
        return enabled && skillAPI != null;
    }

    /**
     * 获取技能定义
     */
    public Optional<SkillDefinition> getSkill(String skillId) {
        if (!isEnabled()) return Optional.empty();
        return skillAPI.getSkill(skillId);
    }

    /**
     * 执行技能
     *
     * @param player  执行技能的玩家
     * @param skillId 技能ID
     * @return 是否执行成功
     */
    public boolean executeSkill(Player player, String skillId) {
        if (!isEnabled()) {
            player.sendMessage(miniMessage.red("技能系统未启用"));
            return false;
        }

        Optional<SkillDefinition> skillOpt = skillAPI.getSkill(skillId);
        if (skillOpt.isEmpty()) {
            player.sendMessage(miniMessage.red("技能不存在: " + skillId));
            return false;
        }

        // 检查冷却
        String playerId = player.getUniqueId().toString();
        if (skillAPI.isOnCooldown(playerId, skillId)) {
            long remaining = skillAPI.getCooldownRemaining(playerId, skillId);
            player.sendMessage(miniMessage.yellow("技能冷却中，剩余 " + remaining + " 秒"));
            return false;
        }

        // 检查玩家是否在线
        if (!player.isOnline()) {
            return false;
        }

        // 创建技能上下文并执行
        // 注意：冷却由 SkillService 自动管理，不需要在这里设置
        SkillContext context = SkillContext.builder(player).build();
        SkillResult result = skillAPI.executeSkill(skillId, context);

        if (result.isSuccess()) {
            return true;
        } else {
            player.sendMessage(miniMessage.red("技能执行失败: " + result.getMessage()));
            return false;
        }
    }
}
