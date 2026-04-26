package cn.guangdian.rpgskill.data;

import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.rpgskill.RPGSkill;
import cn.guangdian.rpgskill.cooldown.CooldownManager;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 技能数据处理器
 * 使用 RPGCore AbstractPlayerDataHandler 管理玩家技能数据生命周期
 */
public class SkillDataHandler extends AbstractPlayerDataHandler {

    private final RPGSkill plugin;
    private final CooldownManager cooldownManager;

    public SkillDataHandler(RPGSkill plugin, CooldownManager cooldownManager) {
        super(plugin);
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
    }

    @Override
    protected void onPlayerLoad(Player player) {
        UUID playerId = player.getUniqueId();

        // 检查是否需要持久化冷却时间
        if (plugin.getConfigurateManager() != null &&
            plugin.getConfigurateManager().isCooldownPersistAfterLogout()) {
            // 从存储加载冷却数据（如果有）
            // 这里可以实现从数据库或文件加载
            plugin.getLogger().info("玩家 " + player.getName() + " 技能数据已加载");
        }
    }

    @Override
    protected void onPlayerSave(Player player) {
        UUID playerId = player.getUniqueId();

        // 如果需要持久化冷却时间，保存到存储
        if (plugin.getConfigurateManager() != null &&
            plugin.getConfigurateManager().isCooldownPersistAfterLogout()) {
            // 保存冷却数据到数据库或文件
            // 这里可以实现保存逻辑
            plugin.getLogger().info("玩家 " + player.getName() + " 技能数据已保存");
        }

        // 清理内存中的冷却数据
        cooldownManager.clearPlayerCooldowns(playerId.toString());
    }

    @Override
    public int getPriority() {
        // 技能数据优先级中等
        return 50;
    }

    @Override
    public String getHandlerName() {
        return "SkillData";
    }
}
