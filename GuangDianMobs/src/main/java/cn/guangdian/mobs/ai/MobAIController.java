package cn.guangdian.mobs.ai;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.mobs.model.MobAI;
import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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
    private final Map<UUID, Long> lastGoalExecution = new ConcurrentHashMap<>();
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
        MobAIState state = new MobAIState(entity.getUniqueId(), ai, template);
        aiStates.put(entity.getUniqueId(), state);

        applyAISettings(entity, ai.getSettings());
    }

    /**
     * 应用AI设置
     */
    private void applyAISettings(LivingEntity entity, MobAI.AISettings settings) {
        if (entity.getAttribute(org.bukkit.attribute.Attribute.FOLLOW_RANGE) != null) {
            entity.getAttribute(org.bukkit.attribute.Attribute.FOLLOW_RANGE)
                .setBaseValue(settings.getFollowRange());
        }

        if (entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED) != null) {
            double speed = settings.getWanderSpeed();
            entity.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        }

        if (entity instanceof Mob mob) {
            mob.setAware(true);

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
        }, 20L, 20L);
    }

    /**
     * 更新AI状态
     */
    private void updateAI(MobAIState state) {
        Entity entity = plugin.getServer().getEntity(state.getEntityId());
        if (!(entity instanceof LivingEntity living)) {
            aiStates.remove(state.getEntityId());
            lastGoalExecution.remove(state.getEntityId());
            return;
        }

        if (living.isDead()) {
            aiStates.remove(state.getEntityId());
            lastGoalExecution.remove(state.getEntityId());
            return;
        }

        MobAI ai = state.getAi();
        MobAI.AISettings settings = ai.getSettings();

        if (settings.getRetreatHealthPercent() > 0) {
            double healthPercent = (living.getHealth() / living.getMaxHealth()) * 100;
            if (healthPercent <= settings.getRetreatHealthPercent()) {
                handleRetreat(living, settings);
                return;
            }
        }

        updateTarget(living, ai);

        executeAIGoals(living, ai.getAiGoals(), state.getTemplate());
    }

    /**
     * 执行AI目标
     */
    private void executeAIGoals(LivingEntity entity, List<String> goals, CustomMob template) {
        if (goals == null || goals.isEmpty()) return;
        if (!(entity instanceof Mob mob)) return;

        UUID entityId = entity.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastExec = lastGoalExecution.getOrDefault(entityId, 0L);

        if (currentTime - lastExec < 3000) return;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        for (String goal : goals) {
            String[] parts = goal.split(" ");
            String goalType = parts[0].toLowerCase();

            boolean executed = switch (goalType) {
                case "attack" -> executeAttackGoal(mob, target);
                case "summon" -> executeSummonGoal(entity, template, parts);
                case "flee" -> executeFleeGoal(entity, target);
                case "teleport" -> executeTeleportGoal(entity, target);
                case "buff" -> executeBuffGoal(entity, parts);
                default -> false;
            };

            if (executed) {
                lastGoalExecution.put(entityId, currentTime);
                break;
            }
        }
    }

    /**
     * 执行攻击目标
     */
    private boolean executeAttackGoal(Mob mob, LivingEntity target) {
        if (mob.getTarget() == target) return false;
        mob.setTarget(target);
        return true;
    }

    /**
     * 执行召唤目标
     */
    private boolean executeSummonGoal(LivingEntity entity, CustomMob template, String[] parts) {
        if (template == null) return false;

        int count = 1;
        if (parts.length > 1) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        count = Math.min(count, 5);

        Location loc = entity.getLocation();
        for (int i = 0; i < count; i++) {
            double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
            Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);

            String mobId = template.getId();
            plugin.getMobManager().spawnMob(mobId, spawnLoc);
        }

        return true;
    }

    /**
     * 执行逃跑目标
     */
    private boolean executeFleeGoal(LivingEntity entity, LivingEntity target) {
        if (target == null) return false;

        Location entityLoc = entity.getLocation();
        Location targetLoc = target.getLocation();

        double dx = entityLoc.getX() - targetLoc.getX();
        double dz = entityLoc.getZ() - targetLoc.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance <= 0) return false;

        dx /= distance;
        dz /= distance;

        Location fleeLoc = entityLoc.clone().add(dx * 8, 0, dz * 8);
        fleeLoc.setY(entityLoc.getWorld().getHighestBlockYAt(fleeLoc) + 1);

        if (entity instanceof Mob mob && mob.getPathfinder() != null) {
            mob.getPathfinder().moveTo(fleeLoc, 1.5);
        }

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        return true;
    }

    /**
     * 执行传送目标
     */
    private boolean executeTeleportGoal(LivingEntity entity, LivingEntity target) {
        if (target == null) return false;

        if (ThreadLocalRandom.current().nextDouble() > 0.3) return false;

        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().multiply(-2);
        Location teleportLoc = targetLoc.clone().add(direction);

        if (teleportLoc.getBlock().getType().isSolid()) {
            teleportLoc.setY(teleportLoc.getY() + 1);
        }

        entity.teleport(teleportLoc);
        return true;
    }

    /**
     * 执行增益目标
     */
    private boolean executeBuffGoal(LivingEntity entity, String[] parts) {
        if (parts.length < 2) return false;

        String effectName = parts[1].toUpperCase();
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type == null) return false;

        int duration = 200;
        int amplifier = 1;

        if (parts.length > 2) {
            try {
                duration = Integer.parseInt(parts[2]) * 20;
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 3) {
            try {
                amplifier = Integer.parseInt(parts[3]) - 1;
            } catch (NumberFormatException ignored) {}
        }

        entity.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }

    /**
     * 处理撤退逻辑
     */
    private void handleRetreat(LivingEntity entity, MobAI.AISettings settings) {
        if (!(entity instanceof Mob mob)) return;

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));

        LivingEntity target = mob.getTarget();
        if (target != null) {
            Location entityLoc = entity.getLocation();
            Location targetLoc = target.getLocation();

            double dx = entityLoc.getX() - targetLoc.getX();
            double dz = entityLoc.getZ() - targetLoc.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 0) {
                dx /= distance;
                dz /= distance;

                Location fleeLoc = entityLoc.clone().add(dx * 15, 0, dz * 15);
                fleeLoc.setY(entityLoc.getWorld().getHighestBlockYAt(fleeLoc) + 1);

                if (mob.getPathfinder() != null) {
                    mob.getPathfinder().moveTo(fleeLoc, 2.0);
                }
            }

            mob.setTarget(null);
        }
    }

    /**
     * 更新目标选择
     */
    private void updateTarget(LivingEntity entity, MobAI ai) {
        if (!(entity instanceof Mob mob)) return;

        List<String> selectors = ai.getTargetSelectors();
        if (selectors.isEmpty()) return;

        LivingEntity currentTarget = mob.getTarget();

        LivingEntity newTarget = findTargetBySelectors(entity, selectors, ai.getThreatSettings());

        if (newTarget != null && newTarget != currentTarget) {
            mob.setTarget(newTarget);
        }
    }

    /**
     * 根据选择器寻找目标
     */
    private LivingEntity findTargetBySelectors(LivingEntity entity, List<String> selectors, MobAI.ThreatSettings threatSettings) {
        double threatRadius = threatSettings.getThreatRadius();
        Location entityLoc = entity.getLocation();

        Collection<Entity> nearbyEntities = entityLoc.getWorld()
            .getNearbyEntities(entityLoc, threatRadius, threatRadius, threatRadius);

        for (String selector : selectors) {
            String[] parts = selector.split(" ");
            String type = parts[0].toLowerCase();

            LivingEntity target = switch (type) {
                case "players" -> findNearestPlayer(entity, nearbyEntities, threatRadius);
                case "attacker" -> findHighestThreat(entity);
                case "highestthreat" -> findHighestThreat(entity);
                case "randomplayer" -> findRandomPlayer(entity, nearbyEntities, threatRadius);
                default -> null;
            };

            if (target != null) return target;
        }

        return null;
    }

    /**
     * 寻找最近的玩家
     */
    private LivingEntity findNearestPlayer(LivingEntity entity, Collection<Entity> nearbyEntities, double maxDistance) {
        Player nearest = null;
        double minDistance = maxDistance;

        for (Entity e : nearbyEntities) {
            if (!(e instanceof Player player)) continue;
            if (!isValidTarget(player)) continue;

            double distance = player.getLocation().distanceSquared(entity.getLocation());
            if (distance < minDistance * minDistance) {
                minDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    /**
     * 寻找仇恨最高的目标
     */
    private LivingEntity findHighestThreat(LivingEntity entity) {
        if (plugin.getAggroService() == null) return null;

        try {
            return plugin.getAggroService().getTopAggroTarget(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 随机选择玩家
     */
    private LivingEntity findRandomPlayer(LivingEntity entity, Collection<Entity> nearbyEntities, double maxDistance) {
        List<Player> validPlayers = new ArrayList<>();

        for (Entity e : nearbyEntities) {
            if (!(e instanceof Player player)) continue;
            if (!isValidTarget(player)) continue;

            if (player.getLocation().distanceSquared(entity.getLocation()) <= maxDistance * maxDistance) {
                validPlayers.add(player);
            }
        }

        if (validPlayers.isEmpty()) return null;

        return validPlayers.get(ThreadLocalRandom.current().nextInt(validPlayers.size()));
    }

    /**
     * 检查是否是有效的目标
     */
    private boolean isValidTarget(Player player) {
        if (player.isDead()) return false;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return false;
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return false;
        return true;
    }

    /**
     * 移除实体的AI
     */
    public void removeAI(UUID entityId) {
        aiStates.remove(entityId);
        lastGoalExecution.remove(entityId);
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
        lastGoalExecution.clear();
    }

    /**
     * AI状态类
     */
    private static class MobAIState {
        private final UUID entityId;
        private final MobAI ai;
        private final CustomMob template;

        public MobAIState(UUID entityId, MobAI ai, CustomMob template) {
            this.entityId = entityId;
            this.ai = ai;
            this.template = template;
        }

        public UUID getEntityId() { return entityId; }
        public MobAI getAi() { return ai; }
        public CustomMob getTemplate() { return template; }
    }
}
