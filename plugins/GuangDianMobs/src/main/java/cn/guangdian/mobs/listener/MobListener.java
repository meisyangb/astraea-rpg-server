package cn.guangdian.mobs.listener;

import cn.guangdian.aggro.api.AggroService;
import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.mobs.model.MobOptions;
import cn.guangdian.mobs.model.MobSkill;
import cn.guangdian.mobs.skills.SkillExecutor;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 怪物监听器
 */
public class MobListener implements Listener {

    private final GuangDianMobs plugin;
    private final SkillExecutor skillExecutor;

    public MobListener(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.skillExecutor = new SkillExecutor(plugin);
    }

    /**
     * 怪物生成事件
     */
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        // 可以在这里处理自然生成的怪物替换
    }

    /**
     * 实体受到伤害事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 处理怪物攻击玩家
        if (event.getDamager() instanceof LivingEntity damager &&
            event.getEntity() instanceof Player target) {

            String mobId = plugin.getMobManager().getMobIdFromEntity(damager);
            if (mobId == null) return;

            CustomMob mobTemplate = plugin.getMobManager().getMobTemplate(mobId);
            if (mobTemplate == null) return;

            // 更新仇恨值
            AggroService aggroService = plugin.getAggroService();
            if (aggroService != null) {
                aggroService.addAggro(damager, target, event.getDamage());
            }

            // 更新Boss血条
            plugin.getBossBarManager().updateBossBar(damager);

            // 尝试触发技能
            tryTriggerSkills(damager, target, mobTemplate);
        }

        // 处理玩家攻击怪物
        if (event.getEntity() instanceof LivingEntity target &&
            event.getDamager() instanceof Player player) {

            String mobId = plugin.getMobManager().getMobIdFromEntity(target);
            if (mobId == null) return;

            CustomMob mobTemplate = plugin.getMobManager().getMobTemplate(mobId);
            if (mobTemplate == null) return;

            // 更新仇恨值
            AggroService aggroService = plugin.getAggroService();
            if (aggroService != null) {
                aggroService.addAggro(target, player, event.getDamage());
            }

            // 更新Boss血条
            plugin.getBossBarManager().updateBossBar(target);

            // 显示Boss血条给玩家
            if (mobTemplate.getOptions().isShowBossBar()) {
                plugin.getBossBarManager().showToPlayer(target, player);
            }

            // 被攻击时有几率触发技能
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                tryTriggerSkills(target, player, mobTemplate);
            }
        }
    }

    /**
     * 怪物死亡事件
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        String mobId = plugin.getMobManager().getMobIdFromEntity(entity);
        if (mobId == null) return;

        CustomMob mobTemplate = plugin.getMobManager().getMobTemplate(mobId);
        if (mobTemplate == null) return;

        Player killer = entity.getKiller();
        MobOptions options = mobTemplate.getOptions();

        // 清除原版掉落
        if (options.isPreventOtherDrops()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // 处理自定义掉落
        if (mobTemplate.getDropTable() != null) {
            plugin.getDropManager().handleMobDeath(entity, mobTemplate.getDropTable(), killer);
        }

        // 处理命令掉落
        handleCommandDrops(entity, killer, mobTemplate);

        // 发送击杀消息
        if (killer != null && !mobTemplate.getKillMessages().isEmpty()) {
            sendKillMessage(killer, entity, mobTemplate);
        }

        // 清理仇恨
        AggroService aggroService = plugin.getAggroService();
        if (aggroService != null) {
            aggroService.clearAggro(entity);
        }

        // 移除Boss血条
        plugin.getBossBarManager().removeBossBar(entity);

        // 通知刷新点管理器
        plugin.getSpawnPointManager().onMobDeath(entity.getUniqueId());

        // 清理技能冷却数据，防止内存泄漏
        skillExecutor.clearCooldowns(entity.getUniqueId());
    }

    /**
     * 处理命令掉落
     */
    private void handleCommandDrops(LivingEntity entity, Player killer, CustomMob mobTemplate) {
        List<CustomMob.CommandDrop> commandDrops = mobTemplate.getCommandDrops();
        if (commandDrops.isEmpty()) return;

        for (CustomMob.CommandDrop drop : commandDrops) {
            if (ThreadLocalRandom.current().nextDouble() > drop.getChance()) continue;

            String command = drop.getCommand();
            String target = drop.getTarget();

            // 替换变量
            command = command.replace("<target.name>", killer != null ? killer.getName() : "Unknown")
                           .replace("<mob.name>", mobTemplate.getDisplayName())
                           .replace("<mob.uuid>", entity.getUniqueId().toString());

            // 确定执行目标
            Player targetPlayer = null;
            if ("@trigger".equals(target) || "@killer".equals(target)) {
                targetPlayer = killer;
            } else if ("@self".equals(target)) {
                // 对怪物自身执行命令
                command = command.replace("<target.name>", entity.getName());
            }

            if (targetPlayer != null) {
                command = command.replace("<target.name>", targetPlayer.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            } else if ("@self".equals(target)) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
        }
    }

    /**
     * 发送击杀消息
     */
    private void sendKillMessage(Player killer, LivingEntity entity, CustomMob mobTemplate) {
        List<String> messages = mobTemplate.getKillMessages();
        if (messages.isEmpty()) return;

        // 随机选择一条消息
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));

        // 替换变量
        message = message.replace("<target.name>", killer.getName())
                        .replace("<mob.name>", mobTemplate.getDisplayName())
                        .replace("<mob.hp>", String.valueOf((int) entity.getHealth()))
                        .replace("<mob.mhp>", String.valueOf((int) entity.getMaxHealth()));

        // 转换颜色代码并发送
        MiniMessageService mm = MiniMessageService.getInstance();
        killer.sendMessage(mm.colorize(message));
    }

    /**
     * 玩家移动事件 - 距离仇恨
     * 使用节流机制，每5tick处理一次
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 只处理实际移动（位置变化超过0.1格）
        if (event.getFrom().distance(event.getTo()) < 0.1) return;

        Player player = event.getPlayer();

        // 节流：每5tick处理一次（使用玩家元数据存储上次处理时间）
        long currentTick = System.currentTimeMillis();
        Long lastProcess = player.getMetadata("gdmm_last_aggro_tick").stream()
            .findFirst()
            .map(m -> (Long) m.value())
            .orElse(0L);

        if (currentTick - lastProcess < 250) { // 250ms = 5tick
            return;
        }

        // 更新上次处理时间
        player.setMetadata("gdmm_last_aggro_tick", new org.bukkit.metadata.FixedMetadataValue(plugin, currentTick));

        AggroService aggroService = plugin.getAggroService();
        if (aggroService == null) return;

        // 检查附近的自定义怪物
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof LivingEntity living) {
                String mobId = plugin.getMobManager().getMobIdFromEntity(living);
                if (mobId != null) {
                    double distance = living.getLocation().distance(player.getLocation());
                    aggroService.addAggro(living, player, 0.1 * (10 - distance));
                }
            }
        }
    }

    /**
     * 处理阳光燃烧 - 防止阳光燃烧
     */
    @EventHandler
    public void onEntityCombust(org.bukkit.event.entity.EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        String mobId = plugin.getMobManager().getMobIdFromEntity(entity);
        if (mobId == null) return;

        CustomMob mobTemplate = plugin.getMobManager().getMobTemplate(mobId);
        if (mobTemplate == null) return;

        // 如果设置了防止阳光燃烧，取消事件
        if (mobTemplate.getOptions().isPreventSunBurn()) {
            event.setCancelled(true);
        }
    }

    /**
     * 尝试触发技能
     */
    private void tryTriggerSkills(LivingEntity mob, Player target, CustomMob mobTemplate) {
        List<String> skillIds = mobTemplate.getSkills();
        if (skillIds == null || skillIds.isEmpty()) return;

        // 随机选择一个技能触发
        for (String skillId : skillIds) {
            MobSkill skill = plugin.getSkillManager().getSkill(skillId);
            if (skill == null) continue;

            // 执行技能
            skillExecutor.executeSkill(mob, skill, target);
        }
    }
}
