package cn.guangdian.mobs.skills;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.MobSkill;
import cn.guangdian.mobs.skills.condition.AltitudeCondition;
import cn.guangdian.mobs.skills.condition.BiomeCondition;
import cn.guangdian.mobs.skills.condition.HealthCondition;
import cn.guangdian.mobs.skills.condition.TargetWithinCondition;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.util.CooldownManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 技能执行器
 */
public class SkillExecutor {

    private final GuangDianMobs plugin;
    private final CooldownManager cooldownManager;

    public SkillExecutor(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.cooldownManager = CooldownManager.getInstance();
    }

    /**
     * 清理实体的冷却数据
     */
    public void clearCooldowns(UUID entityId) {
        cooldownManager.clearAllCooldowns(entityId);
    }

    /**
     * 执行技能
     */
    public boolean executeSkill(LivingEntity caster, MobSkill skill, LivingEntity target) {
        if (!checkCooldown(caster, skill)) {
            return false;
        }

        if (!checkConditions(caster, target, skill)) {
            return false;
        }

        if (ThreadLocalRandom.current().nextDouble() > skill.getChance()) {
            return false;
        }

        setCooldown(caster, skill);

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
        if (!executedSkills.add(skill.getId())) {
            return;
        }

        if (executedSkills.size() > 10) {
            return;
        }

        playEffects(caster, skill);

        switch (skill.getType()) {
            case DAMAGE -> executeDamageSkill(caster, skill, target);
            case HEAL -> executeHealSkill(caster, skill, target);
            case BUFF -> executeBuffSkill(caster, skill, target);
            case DEBUFF -> executeDebuffSkill(caster, skill, target);
            case SUMMON -> executeSummonSkill(caster, skill);
            case TELEPORT -> executeTeleportSkill(caster, skill, target);
            case PROJECTILE -> executeProjectileSkill(caster, skill, target);
        }

        for (String subSkillId : skill.getSubSkills()) {
            MobSkill subSkill = plugin.getSkillManager().getSkill(subSkillId);
            if (subSkill != null) {
                executeSkillInternal(caster, subSkill, target, executedSkills);
            }
        }

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
                case "biome" -> {
                    return new BiomeCondition(parts[1]).check(caster, target, skill);
                }
                case "altitude" -> {
                    String range = parts[1];
                    if (range.startsWith("<")) {
                        double max = Double.parseDouble(range.substring(1));
                        return new AltitudeCondition(Double.NEGATIVE_INFINITY, max).check(caster, target, skill);
                    } else if (range.startsWith(">")) {
                        double min = Double.parseDouble(range.substring(1));
                        return new AltitudeCondition(min, Double.POSITIVE_INFINITY).check(caster, target, skill);
                    } else if (range.contains("-")) {
                        String[] rangeParts = range.split("-");
                        double min = Double.parseDouble(rangeParts[0]);
                        double max = Double.parseDouble(rangeParts[1]);
                        return new AltitudeCondition(min, max).check(caster, target, skill);
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
            double offsetX = ThreadLocalRandom.current().nextDouble(-3, 3);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-3, 3);
            Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);

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

        Fireball fireball = caster.getWorld().spawn(eyeLoc, Fireball.class);
        fireball.setDirection(direction);
        fireball.setYield((float) skill.getDamage() / 10f);
    }

    /**
     * 获取技能目标
     */
    private Collection<LivingEntity> getTargets(LivingEntity caster, MobSkill skill, LivingEntity primaryTarget) {
        List<LivingEntity> targets = new ArrayList<>();

        MobSkill.TargetSelector selector = skill.getTargetSelector();
        String targetType = selector != null ? selector.getType() : skill.getTargetType();
        double range = selector != null ? selector.getRadius() : skill.getRange();

        switch (targetType.toUpperCase()) {
            case "SELF" -> targets.add(caster);
            case "TARGET" -> {
                if (primaryTarget != null) targets.add(primaryTarget);
            }
            case "AOE", "LIVINGINRADIUS" -> {
                targets.addAll(caster.getLocation().getNearbyLivingEntities(range,
                    entity -> entity != caster && isEnemy(caster, entity)));
            }
            case "RANDOM" -> {
                List<LivingEntity> nearby = new ArrayList<>(caster.getLocation().getNearbyLivingEntities(range,
                    entity -> entity != caster && isEnemy(caster, entity)));
                if (!nearby.isEmpty()) {
                    targets.add(nearby.get(ThreadLocalRandom.current().nextInt(nearby.size())));
                }
            }
            case "PLAYERSINRADIUS" -> {
                for (Entity entity : caster.getLocation().getNearbyEntities(range, range, range)) {
                    if (entity instanceof Player player && isEnemy(caster, player)) {
                        targets.add(player);
                    }
                }
            }
            case "MOBSINRADIUS" -> {
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
        if (caster instanceof Monster && target instanceof Player) return true;
        if (caster instanceof Player && target instanceof Monster) return true;
        return false;
    }

    /**
     * 播放技能效果
     */
    private void playEffects(LivingEntity caster, MobSkill skill) {
        Location loc = caster.getLocation();

        if (skill.getParticle() != null) {
            try {
                Particle particle = Particle.valueOf(skill.getParticle().toUpperCase());
                caster.getWorld().spawnParticle(particle, loc, 20, 1, 1, 1, 0.1);
            } catch (IllegalArgumentException ignored) {}
        }

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
        String cooldownKey = "skill_" + skill.getId();
        return !cooldownManager.isOnCooldown(entity.getUniqueId(), cooldownKey);
    }

    /**
     * 设置技能冷却
     */
    private void setCooldown(LivingEntity entity, MobSkill skill) {
        String cooldownKey = "skill_" + skill.getId();
        long cooldownMs = skill.getCooldown() * 50L;
        cooldownManager.setCooldown(entity.getUniqueId(), cooldownKey, cooldownMs);
    }
}
