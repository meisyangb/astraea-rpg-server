package cn.guangdian.mobs.ai;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.mobs.model.MobAI;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 怪物AI控制器
 * 管理怪物的AI行为和目标选择
 */
public class MobAIController {

    private final GuangDianMobs plugin;
    private final Map<UUID, MobAIState> aiStates = new ConcurrentHashMap<>();
    private long updateTaskId = -1;

    public MobAIController(GuangDianMobs plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    /**
     * 为实体应用AI
     */
    public void applyAI(LivingEntity entity, CustomMob template) {
        if (template.getAi() == null) return;

        MobAI ai = template.getAi();
        MobAIState state = new MobAIState(entity.getUniqueId(), ai);
        aiStates.put(entity.getUniqueId(), state);

        // 应用AI设置
        applyAISettings(entity, ai.getSettings());
    }

    /**
     * 应用AI设置
     */
    private void applyAISettings(LivingEntity entity, MobAI.AISettings settings) {
        // 设置追踪范围
        if (entity.getAttribute(org.bukkit.attribute.Attribute.FOLLOW_RANGE) != null) {
            entity.getAttribute(org.bukkit.attribute.Attribute.FOLLOW_RANGE)
                .setBaseValue(settings.getFollowRange());
        }

        // 设置移动速度
        if (entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED) != null) {
            double speed = settings.getWanderSpeed();
            entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        }

        // 应用特殊AI行为
        if (entity instanceof Mob mob) {
            // 清除默认AI
            mob.setAware(true);

            // 设置是否避水
            if (settings.isAvoidWater()) {
                // Paper API: 设置避水
                // 注意：这需要NMS或Paper特定API
            }

            // 设置是否避日
            if (settings.isAvoidSun() && entity instanceof Zombie zombie) {
                zombie.setShouldBurnInDay(true);
            }
        }
    }

    /**
     * 启动AI更新任务
     */
    private void startUpdateTask() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        updateTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            for (MobAIState state : aiStates.values()) {
                updateAI(state);
            }
        }, 20L, 20L); // 每秒更新一次
    }

    /**
     * 更新AI状态
     */
    private void updateAI(MobAIState state) {
        org.bukkit.entity.Entity entity = plugin.getServer().getEntity(state.getEntityId());
        if (!(entity instanceof LivingEntity living)) {
            aiStates.remove(state.getEntityId());
            return;
        }

        if (living.isDead()) {
            aiStates.remove(state.getEntityId());
            return;
        }

        MobAI ai = state.getAi();
        MobAI.AISettings settings = ai.getSettings();

        // 检查撤退逻辑
        if (settings.getRetreatHealthPercent() > 0) {
            double healthPercent = (living.getHealth() / living.getMaxHealth()) * 100;
            if (healthPercent <= settings.getRetreatHealthPercent()) {
                handleRetreat(living);
            }
        }

        // 更新目标选择
        updateTarget(living, ai);
    }

    /**
     * 处理撤退逻辑
     */
    private void handleRetreat(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;

        // 给予速度效果
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));

        // 寻找逃跑方向（远离目标）
        LivingEntity target = mob.getTarget();
        if (target != null) {
            Location entityLoc = entity.getLocation();
            Location targetLoc = target.getLocation();

            // 计算逃跑方向
            double dx = entityLoc.getX() - targetLoc.getX();
            double dz = entityLoc.getZ() - targetLoc.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 0) {
                dx /= distance;
                dz /= distance;

                // 设置逃跑目标位置
                Location fleeLoc = entityLoc.clone().add(dx * 10, 0, dz * 10);
                fleeLoc.setY(entityLoc.getWorld().getHighestBlockYAt(fleeLoc) + 1);

                // 使用 Paper 的 Pathfinder API 如果可用
                // 否则简单地向反方向移动
                if (mob.getPathfinder() != null) {
                    mob.getPathfinder().moveTo(fleeLoc, 1.5);
                }
            }
        }
    }

    /**
     * 更新目标选择
     */
    private void updateTarget(LivingEntity entity, MobAI ai) {
        if (!(entity instanceof Mob mob)) return;

        List<String> selectors = ai.getTargetSelectors();
        if (selectors.isEmpty()) return;

        // 获取当前目标
        LivingEntity currentTarget = mob.getTarget();

        // 根据选择器寻找新目标
        LivingEntity newTarget = findTargetBySelectors(entity, selectors, ai.getThreatSettings());

        // 如果找到新目标且不同，切换目标
        if (newTarget != null && newTarget != currentTarget) {
            mob.setTarget(newTarget);
        }
    }

    /**
     * 根据选择器寻找目标
     */
    private LivingEntity findTargetBySelectors(LivingEntity entity, List<String> selectors, MobAI.ThreatSettings threatSettings) {
        double threatRadius = threatSettings.getThreatRadius();

        for (String selector : selectors) {
            String[] parts = selector.split(" ");
            String type = parts[0].toLowerCase();

            switch (type) {
                case "players" -> {
                    // 寻找最近的玩家
                    Player nearest = null;
                    double minDistance = threatRadius;

                    for (Player player : entity.getLocation().getWorld().getPlayers()) {
                        if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                            player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

                        double distance = player.getLocation().distance(entity.getLocation());
                        if (distance <= minDistance) {
                            minDistance = distance;
                            nearest = player;
                        }
                    }
                    if (nearest != null) return nearest;
                }
                case "attacker" -> {
                    // 优先攻击最后攻击者（需要配合仇恨系统）
                    // 这里简化处理，实际应该查询仇恨表
                }
                case "highestthreat" -> {
                    // 优先攻击仇恨最高的目标
                    if (plugin.getAggroService() != null) {
                        return plugin.getAggroService().getTopAggroTarget(entity);
                    }
                }
                case "randomplayer" -> {
                    // 随机选择玩家
                    List<Player> players = new ArrayList<>(entity.getLocation().getWorld().getPlayers());
                    players.removeIf(p -> p.isDead() || p.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                        p.getGameMode() == org.bukkit.GameMode.SPECTATOR);
                    if (!players.isEmpty()) {
                        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
                    }
                }
            }
        }

        return null;
    }

    /**
     * 移除实体的AI
     */
    public void removeAI(UUID entityId) {
        aiStates.remove(entityId);
    }

    /**
     * 清理
     */
    public void cleanup() {
        if (updateTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(updateTaskId);
            }
        }
        aiStates.clear();
    }

    /**
     * AI状态类
     */
    private static class MobAIState {
        private final UUID entityId;
        private final MobAI ai;

        public MobAIState(UUID entityId, MobAI ai) {
            this.entityId = entityId;
            this.ai = ai;
        }

        public UUID getEntityId() { return entityId; }
        public MobAI getAi() { return ai; }
    }
}
