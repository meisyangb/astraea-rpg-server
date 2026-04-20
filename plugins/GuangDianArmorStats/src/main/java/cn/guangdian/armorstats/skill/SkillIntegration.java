package cn.guangdian.armorstats.skill;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * RPGSkill 集成服务
 * <p>负责通过 RPGCore 服务注册表获取 RPGSkillAPI，执行装备技能</p>
 * <p>替代原有的内部 SkillManager，实现技能系统统一管理</p>
 */
public class SkillIntegration {

    private final GuangDianArmorStats plugin;
    private Object skillAPI;
    private boolean enabled = false;
    private boolean initialized = false;

    public SkillIntegration(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * 初始化 RPGSkill 集成
     * <p>通过 RPGCore 服务注册表获取 RPGSkillAPI</p>
     */
    private void initialize() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            plugin.getLogger().warning("RPGCore 未加载，技能集成功能不可用");
            return;
        }

        ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
        if (serviceRegistry == null) {
            plugin.getLogger().warning("ServiceRegistry 不可用，技能集成功能不可用");
            return;
        }

        tryInitialize(serviceRegistry);
    }

    /**
     * 尝试初始化技能集成
     */
    private void tryInitialize(ServiceRegistry serviceRegistry) {
        try {
            Optional<?> apiOpt = serviceRegistry.getOptionalService(
                Class.forName("cn.guangdian.rpgskill.api.RPGSkillAPI")
            );
            if (apiOpt.isPresent()) {
                this.skillAPI = apiOpt.get();
                this.enabled = true;
                this.initialized = true;
                plugin.getLogger().info("RPGSkill 集成已启用（通过 ServiceRegistry）- 装备技能将由 RPGSkill 统一管理");
            } else {
                this.initialized = false;
                plugin.getLogger().info("RPGSkill 服务未注册，将在稍后重试初始化");
            }
        } catch (ClassNotFoundException e) {
            this.initialized = true;
            plugin.getLogger().info("RPGSkillAPI 类不存在，装备技能功能不可用");
        }
    }

    /**
     * 尝试重新初始化（延迟初始化）
     * <p>当 RPGSkill 插件后加载时调用</p>
     */
    public void tryReinitialize() {
        if (enabled) return;
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;
        
        ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
        if (serviceRegistry == null) return;
        
        tryInitialize(serviceRegistry);
    }

    /**
     * 检查集成是否可用
     */
    public boolean isEnabled() {
        if (!enabled && !initialized) {
            tryReinitialize();
        }
        return enabled && skillAPI != null;
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
            plugin.getLogger().fine("技能系统未启用，无法执行技能: " + skillId);
            return false;
        }

        try {
            java.lang.reflect.Method getSkillMethod = skillAPI.getClass().getMethod("getSkill", String.class);
            Optional<?> skillOpt = (Optional<?>) getSkillMethod.invoke(skillAPI, skillId);

            if (skillOpt.isEmpty()) {
                plugin.getLogger().fine("技能不存在: " + skillId);
                return false;
            }

            if (!player.isOnline()) {
                return false;
            }

            Object skill = skillOpt.get();
            Class<?> skillContextClass = Class.forName("cn.guangdian.rpgskill.skill.SkillContext");
            java.lang.reflect.Method builderMethod = skillContextClass.getMethod("builder", Player.class);
            Object builder = builderMethod.invoke(null, player);
            java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
            Object context = buildMethod.invoke(builder);

            java.lang.reflect.Method executeMethod = skillAPI.getClass().getMethod("executeSkill", String.class, skillContextClass);
            Object result = executeMethod.invoke(skillAPI, skillId, context);

            java.lang.reflect.Method isSuccessMethod = result.getClass().getMethod("isSuccess");
            return (boolean) isSuccessMethod.invoke(result);

        } catch (Exception e) {
            plugin.getLogger().fine("执行技能失败: " + skillId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String skillId) {
        if (!isEnabled()) return false;

        try {
            java.lang.reflect.Method getSkillMethod = skillAPI.getClass().getMethod("getSkill", String.class);
            Optional<?> skillOpt = (Optional<?>) getSkillMethod.invoke(skillAPI, skillId);
            return skillOpt.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取技能冷却剩余时间（秒）
     */
    public long getCooldownRemaining(Player player, String skillId) {
        if (!isEnabled()) return 0;

        try {
            java.lang.reflect.Method getCooldownMethod = skillAPI.getClass().getMethod(
                "getCooldownRemaining", String.class, String.class
            );
            return (long) getCooldownMethod.invoke(skillAPI, player.getUniqueId().toString(), skillId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 检查技能是否在冷却中
     */
    public boolean isOnCooldown(Player player, String skillId) {
        if (!isEnabled()) return false;

        try {
            java.lang.reflect.Method isOnCooldownMethod = skillAPI.getClass().getMethod(
                "isOnCooldown", String.class, String.class
            );
            return (boolean) isOnCooldownMethod.invoke(skillAPI, player.getUniqueId().toString(), skillId);
        } catch (Exception e) {
            return false;
        }
    }
}
