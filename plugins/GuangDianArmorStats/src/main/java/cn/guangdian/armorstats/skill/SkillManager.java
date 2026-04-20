package cn.guangdian.armorstats.skill;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.CombatLogManager;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 技能管理器
 * 
 * RPGCore 服务集成:
 * - SoundService: 使用 RPGCore 统一音效服务，本地实现作为降级
 * - MiniMessage: 使用 RPGCore 统一消息服务，本地实现作为降级
 */
public class SkillManager {

    // 技能伤害标记key
    public static final String SKILL_DAMAGE_KEY = "guangdian_skill_damage";
    public static final String SKILL_DAMAGE_VALUE = "skill";

    private final StatsManager statsManager;
    private final Map<String, Skill> skills = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private CombatLogManager combatLogManager;
    
    // RPGCore 服务引用
    private final SoundService soundService;
    private final MiniMessage miniMessage;

    public SkillManager(StatsManager statsManager) {
        this.statsManager = statsManager;
        // 获取 RPGCore 服务，优先使用统一服务，本地实现作为降级
        GuangDianArmorStats plugin = GuangDianArmorStats.getInstance();
        this.soundService = plugin.getSoundService();
        this.miniMessage = plugin.getMiniMessage().getMiniMessage();
        loadSkills();
    }

    public void setCombatLogManager(CombatLogManager combatLogManager) {
        this.combatLogManager = combatLogManager;
    }

    public void loadSkills() {
        var configManager = GuangDianArmorStats.getInstance().getConfigManager();
        var skillsConfig = configManager.getSkills();
        
        if (skillsConfig != null) {
            ConfigurationSection skillsSection = skillsConfig.getConfigurationSection("skills");
            if (skillsSection != null) {
                for (String skillName : skillsSection.getKeys(false)) {
                    ConfigurationSection skillConfig = skillsSection.getConfigurationSection(skillName);
                    if (skillConfig != null) {
                        String type = skillConfig.getString("type", "damage_trigger");
                        double triggerChance = skillConfig.getDouble("trigger_chance", 100);
                        double range = skillConfig.getDouble("range", 4.0);
                        double damageMult = skillConfig.getDouble("damage_mult", 1.0);
                        long cooldown = skillConfig.getLong("cooldown", 0);
                        String effect = skillConfig.getString("effect", "none");
                        int duration = skillConfig.getInt("duration", 0);
                        double healPercent = skillConfig.getDouble("heal_percent", 0);
                        double manaCost = skillConfig.getDouble("mana_cost", 0);
                        
                        List<String> statusEffects = new ArrayList<>();
                        if (skillConfig.contains("status_effects")) {
                            statusEffects = skillConfig.getStringList("status_effects");
                        }

                        boolean trueDamage = skillConfig.getBoolean("true_damage", false);
                        boolean pvpOnly = skillConfig.getBoolean("pvp_only", false);
                        boolean dot = skillConfig.getBoolean("dot", false);

                        Skill skill = new Skill(skillName, type, triggerChance, range, damageMult, 
                                              cooldown, effect, duration, statusEffects, healPercent, manaCost,
                                              trueDamage, pvpOnly, dot);
                        skills.put(skillName, skill);
                    }
                }
            }
        }
        GuangDianArmorStats.getInstance().getLogger().info("Loaded " + skills.size() + " skills: " + skills.keySet());
    }

    public Skill getSkill(String name) {
        return skills.get(name);
    }

    public Map<String, Skill> getSkills() {
        return skills;
    }

    public boolean tryTriggerPassiveSkill(Player attacker, LivingEntity target, String skillName, double damage) {
        Skill skill = getSkill(skillName);
        if (skill == null || !skill.isPassive()) return false;

        if (isOnCooldown(attacker.getUniqueId(), skillName)) {
            return false;
        }

        double roll = ThreadLocalRandom.current().nextDouble() * 100;
        if (roll > skill.getTriggerChance()) {
            return false;
        }

        // PVP限制检查
        if (target != null && skill.isPvpOnly() && !(target instanceof Player)) {
            return false;
        }

        setCooldown(attacker.getUniqueId(), skillName);

        // 真实伤害直接设置生命值
        if (skill.isTrueDamage()) {
            double trueDamageAmount = calculateTrueDamage(attacker, target, skill, damage);
            applyTrueDamage(attacker, target, trueDamageAmount);
        } else {
            triggerDamageSkill(attacker, skill, damage);
        }

        return true;
    }

    public boolean tryTriggerPassiveSkill(Player attacker, String skillName, double damage) {
        return tryTriggerPassiveSkill(attacker, null, skillName, damage);
    }

    public boolean triggerActiveSkill(Player attacker, String skillName) {
        Skill skill = getSkill(skillName);
        if (skill == null || !skill.isActive()) return false;

        if (isOnCooldown(attacker.getUniqueId(), skillName)) {
            long remaining = getCooldownRemaining(attacker.getUniqueId(), skillName);
            String message = GuangDianArmorStats.getInstance().getConfig()
                .getString("messages.skill_cooldown", "<red>技能 %skill% 冷却中,剩余 %time% 秒!")
                .replace("%skill%", skill.getName())
                .replace("%time%", String.valueOf(remaining));
            attacker.sendMessage(miniMessage.deserialize(message));
            return false;
        }

        setCooldown(attacker.getUniqueId(), skillName);

        PlayerStats stats = statsManager.getPlayerStats(attacker);
        double attackDamage = stats.getAttackAverage();
        
        triggerDamageSkill(attacker, skill, attackDamage);
        return true;
    }

    private void triggerDamageSkill(Player attacker, Skill skill, double baseDamage) {
        Location loc = attacker.getLocation();
        double range = skill.getRange();

        List<Entity> nearbyEntities = new ArrayList<>(attacker.getWorld().getNearbyEntities(loc, range, range, range));

        double damage = baseDamage * skill.getDamageMultiplier();

        playEffect(attacker, skill, loc);

        // 判断是否是闪电类技能
        boolean isLightningSkill = skill.getEffect() != null && skill.getEffect().toLowerCase().contains("lightning");

        int hitCount = 0;
        for (Entity entity : nearbyEntities) {
            if (entity == attacker) continue;
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity target = (LivingEntity) entity;

            double distance = loc.distance(entity.getLocation());
            if (distance <= range) {
                // 闪电类技能 - 召唤闪电劈向目标
                if (isLightningSkill) {
                    Location targetLoc = target.getLocation();
                    // 召唤视觉闪电效果
                    target.getWorld().strikeLightningEffect(targetLoc);
                    // 播放雷声 - 使用 RPGCore SoundService
                    soundService.playSound(targetLoc, "ENTITY_LIGHTNING_BOLT_THUNDER", 1.0f, 1.0f);
                }

                // 设置技能伤害标记
                attacker.setMetadata(SKILL_DAMAGE_KEY, new FixedMetadataValue(GuangDianArmorStats.getInstance(), damage));
                
                try {
                    target.damage(damage, attacker);
                    hitCount++;

                    // 战斗日志 - 技能伤害
                    if (combatLogManager != null) {
                        combatLogManager.logSkillDamage(attacker, skill.getName(), target, damage);
                    }

                    if (skill.hasStatusEffects()) {
                        applyStatusEffects(target, skill);
                    }
                    
                    // DOT持续伤害
                    if (skill.isDot() && skill.getDuration() > 0) {
                        applyDotDamage(attacker, target, skill, damage);
                    }
                } finally {
                    // 清除标记
                    attacker.removeMetadata(SKILL_DAMAGE_KEY, GuangDianArmorStats.getInstance());
                }
            }
        }

        if (skill.getHealPercent() > 0) {
            double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healAmount = maxHealth * (skill.getHealPercent() / 100.0);
            double newHealth = Math.min(attacker.getHealth() + healAmount, maxHealth);
            attacker.setHealth(newHealth);

            // 战斗日志 - 技能治疗
            if (combatLogManager != null) {
                combatLogManager.logSkillHeal(attacker, skill.getName(), healAmount);
            }
        }

        String message = GuangDianArmorStats.getInstance().getConfig()
            .getString("messages.skill_triggered", "<red>技能 %skill% 触发!")
            .replace("%skill%", skill.getName());
        attacker.sendMessage(miniMessage.deserialize(message));
    }

    private void applyStatusEffects(LivingEntity target, Skill skill) {
        int duration = skill.getDuration() > 0 ? skill.getDuration() * 20 : 60;
        
        for (String effectName : skill.getStatusEffects()) {
            PotionEffectType type = getPotionEffectType(effectName.toLowerCase());
            if (type != null) {
                target.addPotionEffect(new PotionEffect(type, duration, 0));
            }
        }
    }

    private double calculateTrueDamage(Player attacker, LivingEntity target, Skill skill, double baseDamage) {
        double damage = baseDamage * skill.getDamageMultiplier();
        
        if (skill.getHealPercent() > 0) {
            double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            damage = maxHealth * (skill.getHealPercent() / 100.0);
        }
        
        return damage;
    }

    private void applyTrueDamage(Player attacker, LivingEntity target, double damage) {
        if (target instanceof Player) {
            Player player = (Player) target;
            double currentHealth = player.getHealth();
            double newHealth = Math.max(0, currentHealth - damage);
            player.setHealth(newHealth);
        } else {
            target.setHealth(Math.max(0, target.getHealth() - damage));
        }
        
        if (combatLogManager != null) {
            combatLogManager.logSkillDamage(attacker, "真实伤害", target, damage);
        }
        
        Location loc = target.getLocation();
        // 使用 RPGCore SoundService
        soundService.playSound(loc, "ENTITY_WITHER_HURT", 1.0f, 1.0f);
    }

    private void applyDotDamage(Player attacker, LivingEntity target, Skill skill, double baseDamage) {
        int tickDuration = skill.getDuration() * 20;
        int tickInterval = 20;
        double damagePerTick = baseDamage * skill.getDamageMultiplier() * 0.1;
        
        long[] taskId = { -1 };
        int[] ticksElapsed = { 0 };
        
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            taskId[0] = rpgCore.getScheduler().runSyncRepeating(() -> {
                if (!target.isValid() || ticksElapsed[0] >= tickDuration) {
                    if (rpgCore != null) {
                        rpgCore.getScheduler().cancelTask(taskId[0]);
                    }
                    return;
                }
            
            if (ticksElapsed[0] % tickInterval == 0) {
                double currentHealth = target.getHealth();
                double newHealth = Math.max(1, currentHealth - damagePerTick);
                target.setHealth(newHealth);
                
                if (combatLogManager != null) {
                    combatLogManager.logSkillDamage(attacker, skill.getName() + "(DOT)", target, damagePerTick);
                }
                
                // 使用 RPGCore SoundService
                soundService.playSound(target.getLocation(), "ENTITY_WITHER_HURT", 0.5f, 1.0f);
            }
            
            ticksElapsed[0] += 2;
        }, 0L, 2L);
        }
    }

    private PotionEffectType getPotionEffectType(String name) {
        return switch (name) {
            case "poison", "中毒" -> PotionEffectType.POISON;
            case "slowness", "缓慢", "冰冻" -> PotionEffectType.SLOWNESS;
            case "blindness", "致盲" -> PotionEffectType.BLINDNESS;
            case "weakness", "虚弱" -> PotionEffectType.WEAKNESS;
            case "wither", "凋零" -> PotionEffectType.WITHER;
            case "fire_resistance", "防火" -> PotionEffectType.FIRE_RESISTANCE;
            case "speed", "速度" -> PotionEffectType.SPEED;
            case "strength", "力量" -> PotionEffectType.STRENGTH;
            case "regeneration", "生命恢复" -> PotionEffectType.REGENERATION;
            case "invisibility", "隐身" -> PotionEffectType.INVISIBILITY;
            case "night_vision", "夜视" -> PotionEffectType.NIGHT_VISION;
            case "jump", "跳跃" -> PotionEffectType.JUMP_BOOST;
            case "haste", "急迫" -> PotionEffectType.HASTE;
            case "resistance", "抗性" -> PotionEffectType.RESISTANCE;
            case "absorption", "伤害吸收" -> PotionEffectType.ABSORPTION;
            case "saturation", "饱和" -> PotionEffectType.SATURATION;
            case "glowing", "发光" -> PotionEffectType.GLOWING;
            case "levitation", "漂浮" -> PotionEffectType.LEVITATION;
            case "luck", "幸运" -> PotionEffectType.LUCK;
            case "bad_luck", "霉运" -> PotionEffectType.UNLUCK;
            case "slow_falling", "缓降" -> PotionEffectType.SLOW_FALLING;
            case "conduit_power", "潮涌能量" -> PotionEffectType.CONDUIT_POWER;
            case "dolphins_grace", "海豚的恩惠" -> PotionEffectType.DOLPHINS_GRACE;
            case "bad_omen", "不祥之兆" -> PotionEffectType.BAD_OMEN;
            case "hero_of_the_village", "村庄英雄" -> PotionEffectType.HERO_OF_THE_VILLAGE;
            case "darkness", "黑暗" -> PotionEffectType.DARKNESS;
            case "mining_fatigue", "挖掘疲劳" -> PotionEffectType.MINING_FATIGUE;
            case "nausea", "反胃" -> PotionEffectType.NAUSEA;
            case "hunger", "饥饿" -> PotionEffectType.HUNGER;
            default -> null;
        };
    }

    private void playEffect(Player attacker, Skill skill, Location loc) {
        String effect = skill.getEffect();
        
        switch (effect) {
            case "fire":
                attacker.getWorld().spawnParticle(Particle.FLAME, loc, 50, 2, 1, 2, 0.1);
                soundService.playSound(loc, "ENTITY_BLAZE_SHOOT", 1.0f, 1.0f);
                break;
            case "lightning":
                attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 100, 3, 2, 3, 0.2);
                soundService.playSound(loc, "ENTITY_LIGHTNING_BOLT_THUNDER", 1.0f, 1.0f);
                break;
            case "ice":
                attacker.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 50, 2, 1, 2, 0.1);
                soundService.playSound(loc, "BLOCK_GLASS_BREAK", 1.0f, 1.0f);
                break;
            case "poison":
                attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 30, 2, 1, 2, 0.1);
                soundService.playSound(loc, "ENTITY_SPIDER_AMBIENT", 1.0f, 1.0f);
                break;
            case "heal":
                attacker.getWorld().spawnParticle(Particle.HEART, loc.add(0, 2, 0), 20, 1, 1, 1, 0.1);
                soundService.playSound(loc, "BLOCK_BEACON_POWER_SELECT", 1.0f, 1.0f);
                break;
            case "explosion":
                attacker.getWorld().spawnParticle(Particle.EXPLOSION, loc, 10, 2, 1, 2, 0.1);
                soundService.playSound(loc, "ENTITY_GENERIC_EXPLODE", 1.0f, 1.0f);
                break;
            case "magic":
                attacker.getWorld().spawnParticle(Particle.ENCHANT, loc, 50, 2, 1, 2, 0.1);
                soundService.playSound(loc, "BLOCK_ENCHANTMENT_TABLE_USE", 1.0f, 1.0f);
                break;
            default:
                attacker.getWorld().spawnParticle(Particle.FLAME, loc, 20, 2, 1, 2);
                break;
        }
    }

    public boolean isOnCooldown(UUID playerId, String skillName) {
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return false;

        Long lastUse = cooldowns.get(skillName);
        if (lastUse == null) return false;

        Skill skill = getSkill(skillName);
        if (skill == null) return false;

        long cooldownMillis = skill.getCooldown() * 1000;
        return System.currentTimeMillis() - lastUse < cooldownMillis;
    }

    public void setCooldown(UUID playerId, String skillName) {
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        cooldowns.put(skillName, System.currentTimeMillis());
    }

    public long getCooldownRemaining(UUID playerId, String skillName) {
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return 0;

        Long lastUse = cooldowns.get(skillName);
        if (lastUse == null) return 0;

        Skill skill = getSkill(skillName);
        if (skill == null) return 0;

        long cooldownMillis = skill.getCooldown() * 1000;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining / 1000);
    }

    public void clearCooldowns(UUID playerId) {
        playerCooldowns.remove(playerId);
    }
}
