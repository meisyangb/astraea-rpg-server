package cn.guangdian.mobs.skills;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.MobSkill;
import cn.guangdian.mobs.skills.condition.*;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 技能执行器
 */
public class SkillExecutor {

    private final GuangDianMobs plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public SkillExecutor(GuangDianMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * 清理实体的冷却数据
     */
    public void clearCooldowns(UUID entityId) {
        cooldowns.remove(entityId);
    }

    /**
     * 执行技能
     */
    public boolean executeSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        if (!checkCooldown(caster, skill)) {
            return false;
        }

        // 检查条件
        if (!checkConditions(caster, target, skill)) {
            return false;
        }

        // 检查触发几率
        if (ThreadLocalRandom.current().nextDouble() > skill.getChance()) {
            return false;
        }

        // 设置冷却
        setCooldown(caster, skill);

        // 处理延迟执行
        int delay = skill.getDelay();
        if (delay > 0) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSyncLater(() -> {
                    executeSkillInternal(caster, skill, target);
                }, delay);
            }
        } else {
            executeSkillInternal(caster, skill, target);
        }

        return true;
    }

    /**
     * 内部执行技能
     */
    private void executeSkillInternal(LivingEntity caster, MobSkill skill, LivingEntity target) {
        executeSkillInternal(caster, skill, target, new HashSet<>());
    }

    /**
     * 内部执行技能（带递归检测）
     */
    private void executeSkillInternal(LivingEntity caster, MobSkill skill, LivingEntity target, Set<String> executedSkills) {
        // 防止无限递归
        if (!executedSkills.add(skill.getId())) {
            return;
        }

        // 限制递归深度
        if (executedSkills.size() > 10) {
            return;
        }

        // 播放效果
        playEffects(caster, skill);

        // 执行技能逻辑
        switch (skill.getType()) {
            case DAMAGE -> executeDamageSkill(caster, skill, target);
            case HEAL -> executeHealSkill(caster, skill, target);
            case BUFF -> executeBuffSkill(caster, skill, target);
            case DEBUFF -> executeDebuffSkill(caster, skill, target);
            case SUMMON -> executeSummonSkill(caster, skill);
            case TELEPORT -> executeTeleportSkill(caster, skill, target);
            case PROJECTILE -> executeProjectileSkill(caster, skill, target);
        }

        // 执行子技能（元技能）
        for (String subSkillId : skill.getSubSkills()) {
            MobSkill subSkill = plugin.getSkillManager().getSkill(subSkillId);
            if (subSkill != null) {
                executeSkillInternal(caster, subSkill, target, executedSkills);
            }
        }

        // 发送消息 (使用 MiniMessage 解析)
        if (skill.getMessage() != null && target instanceof Player player) {
            MiniMessageService mm = MiniMessageService.getInstance();
            player.sendMessage(mm.colorize(skill.getMessage()));
        }
    }

    /**
     * 检查技能条件
     */
    private boolean checkConditions(LivingEntity caster, LivingEntity target, MobSkill skill) {
        for (String conditionStr : skill.getConditions()) {
            if (!parseAndCheckCondition(conditionStr, caster, target, skill)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析并检查条件
     */
    private boolean parseAndCheckCondition(String condition, LivingEntity caster, LivingEntity target, MobSkill skill) {
        // 格式: targetwithin 25 或 health <50
        String[] parts = condition.split(" ");
        if (parts.length < 2) return true;

        String type = parts[0].toLowerCase();

        try {
            switch (type) {
                case "targetwithin" -> {
                    double distance = Double.parseDouble(parts[1]);
                    return new TargetWithinCondition(distance).check(caster, target, skill);
                }
                case "health" -> {
                    // 格式: health <50 或 health 20-50
                    String range = parts[1];
                    if (range.startsWith("<")) {
                        double max = Double.parseDouble(range.substring(1));
                        return new HealthCondition(0, max).check(caster, target, skill);
                    } else if (range.contains("-")) {
                        String[] rangeParts = range.split("-");
                        double min = Double.parseDouble(rangeParts[0]);
                        double max = Double.parseDouble(rangeParts[1]);
                        return new HealthCondition(min, max).check(caster, target, skill);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return true;
        }

        return true;
    }

    /**
     * 执行伤害技能
     */
    private void executeDamageSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        Collection<LivingEntity> targets = getTargets(caster, skill, target);

        for (LivingEntity entity : targets) {
            entity.damage(skill.getDamage(), caster);
        }
    }

    /**
     * 执行治疗技能
     */
    private void executeHealSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        Collection<LivingEntity> targets = getTargets(caster, skill, target);

        for (LivingEntity entity : targets) {
            double newHealth = Math.min(entity.getHealth() + skill.getHealAmount(), entity.getMaxHealth());
            entity.setHealth(newHealth);
        }
    }

    /**
     * 执行增益技能
     */
    private void executeBuffSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        Collection<LivingEntity> targets = getTargets(caster, skill, target);

        for (String effect : skill.getEffects()) {
            PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
            if (type == null) continue;

            for (LivingEntity entity : targets) {
                entity.addPotionEffect(new PotionEffect(type, 200, 1));
            }
        }
    }

    /**
     * 执行减益技能
     */
    private void executeDebuffSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        Collection<LivingEntity> targets = getTargets(caster, skill, target);

        for (String effect : skill.getEffects()) {
            PotionEffectType type = PotionEffectType.getByName(effect.toUpperCase());
            if (type == null) continue;

            for (LivingEntity entity : targets) {
                entity.addPotionEffect(new PotionEffect(type, 200, 1));
            }
        }
    }

    /**
     * 执行召唤技能
     */
    private void executeSummonSkill(LivingEntity caster, MobSkill skill) {
        Location loc = caster.getLocation();
        int count = skill.getEffects().isEmpty() ? 1 : Integer.parseInt(skill.getEffects().get(0));

        for (int i = 0; i < count; i++) {
            // 在周围随机位置召唤
            double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
            Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);

            // 召唤同类型怪物
            if (caster instanceof Zombie) {
                caster.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            } else if (caster instanceof Skeleton) {
                caster.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
            }
        }
    }

    /**
     * 执行传送技能
     */
    private void executeTeleportSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        if (target != null) {
            // 传送到目标身后
            Location targetLoc = target.getLocation();
            Vector direction = targetLoc.getDirection().multiply(-2);
            Location teleportLoc = targetLoc.clone().add(direction);
            caster.teleport(teleportLoc);
        }
    }

    /**
     * 执行弹射物技能
     */
    private void executeProjectileSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        Location eyeLoc = caster.getEyeLocation();
        Vector direction;

        if (target != null) {
            direction = target.getEyeLocation().toVector().subtract(eyeLoc.toVector()).normalize();
        } else {
            direction = eyeLoc.getDirection();
        }

        // 发射火球
        Fireball fireball = caster.getWorld().spawn(eyeLoc, Fireball.class);
        fireball.setDirection(direction);
        fireball.setYield((float) skill.getDamage() / 10f);
    }

    /**
     * 获取技能目标
     */
    private Collection<LivingEntity> getTargets(LivingEntity caster, MobSkill skill, LivingEntity primaryTarget) {
        List<LivingEntity> targets = new ArrayList<>();

        // 使用新的目标选择器
        MobSkill.TargetSelector selector = skill.getTargetSelector();
        String targetType = selector != null ? selector.getType() : skill.getTargetType();
        double range = selector != null ? selector.getRadius() : skill.getRange();

        switch (targetType.toUpperCase()) {
            case "SELF" -> targets.add(caster);
            case "TARGET" -> {
                if (primaryTarget != null) targets.add(primaryTarget);
            }
            case "AOE", "LIVINGINRADIUS" -> {
                // 范围目标 - 所有活着的生物
                targets.addAll(caster.getLocation().getNearbyLivingEntities(range,
                    entity -> entity != caster && isEnemy(caster, entity)));
            }
            case "RANDOM" -> {
                // 随机目标
                List<LivingEntity> nearby = new ArrayList<>(caster.getLocation().getNearbyLivingEntities(range,
                    entity -> entity != caster && isEnemy(caster, entity)));
                if (!nearby.isEmpty()) {
                    targets.add(nearby.get(ThreadLocalRandom.current().nextInt(nearby.size())));
                }
            }
            case "PLAYERSINRADIUS" -> {
                // 范围内所有玩家
                for (Entity entity : caster.getLocation().getNearbyEntities(range, range, range)) {
                    if (entity instanceof Player player && isEnemy(caster, player)) {
                        targets.add(player);
                    }
                }
            }
            case "MOBSINRADIUS" -> {
                // 范围内所有怪物
                for (Entity entity : caster.getLocation().getNearbyEntities(range, range, range)) {
                    if (entity instanceof Monster monster && monster != caster) {
                        targets.add(monster);
                    }
                }
            }
        }

        return targets;
    }

    /**
     * 判断是否是敌人
     */
    private boolean isEnemy(LivingEntity caster, LivingEntity target) {
        // 简化判断：怪物攻击玩家，玩家攻击怪物
        if (caster instanceof Monster && target instanceof Player) return true;
        if (caster instanceof Player && target instanceof Monster) return true;
        return false;
    }

    /**
     * 播放技能效果
     */
    private void playEffects(LivingEntity caster, MobSkill skill) {
        Location loc = caster.getLocation();

        // 播放粒子效果
        if (skill.getParticle() != null) {
            try {
                Particle particle = Particle.valueOf(skill.getParticle().toUpperCase());
                caster.getWorld().spawnParticle(particle, loc, 20, 1, 1, 1, 0.1);
            } catch (IllegalArgumentException ignored) {}
        }

        // 播放音效 (Paper 1.21.6+ 使用 Registry)
        if (skill.getSound() != null) {
            try {
                NamespacedKey soundKey = NamespacedKey.minecraft(skill.getSound().toLowerCase());
                Sound sound = Registry.SOUNDS.get(soundKey);
                if (sound != null) {
                    caster.getWorld().playSound(loc, sound, 1.0f, 1.0f);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /**
     * 检查技能冷却
     */
    private boolean checkCooldown(LivingEntity entity, MobSkill skill) {
        UUID uuid = entity.getUniqueId();
        String skillId = skill.getId();

        Map<String, Long> entityCooldowns = cooldowns.getOrDefault(uuid, new HashMap<>());
        Long lastUse = entityCooldowns.get(skillId);

        if (lastUse == null) return true;

        long currentTime = System.currentTimeMillis();
        long cooldownMs = skill.getCooldown() * 50L; // tick to ms

        return currentTime - lastUse >= cooldownMs;
    }

    /**
     * 设置技能冷却
     */
    private void setCooldown(LivingEntity entity, MobSkill skill) {
        UUID uuid = entity.getUniqueId();
        String skillId = skill.getId();

        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                 .put(skillId, System.currentTimeMillis());
    }
}
