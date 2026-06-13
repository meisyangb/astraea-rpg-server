package cn.guangdian.armorstats.source;

import cn.guangdian.armorstats.data.PlayerStats;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 玩家属性应用器
 * 将计算后的属性应用到玩家
 * 
 * 修复：添加所有缺失的属性存储
 */
public class PlayerAttributeApplier {

    // PDC Keys - 存储玩家属性（用于战斗计算）
    private static final NamespacedKey KEY_ATTACK_MIN = new NamespacedKey("armorstats", "attack_min");
    private static final NamespacedKey KEY_ATTACK_MAX = new NamespacedKey("armorstats", "attack_max");
    private static final NamespacedKey KEY_DEFENSE_MIN = new NamespacedKey("armorstats", "defense_min");
    private static final NamespacedKey KEY_DEFENSE_MAX = new NamespacedKey("armorstats", "defense_max");
    private static final NamespacedKey KEY_CRIT_CHANCE = new NamespacedKey("armorstats", "crit_chance");
    private static final NamespacedKey KEY_CRIT_DAMAGE = new NamespacedKey("armorstats", "crit_damage");
    private static final NamespacedKey KEY_LIFESTEAL_CHANCE = new NamespacedKey("armorstats", "lifesteal_chance");
    private static final NamespacedKey KEY_LIFESTEAL_MULTIPLIER = new NamespacedKey("armorstats", "lifesteal_multiplier");
    private static final NamespacedKey KEY_DODGE_CHANCE = new NamespacedKey("armorstats", "dodge_chance");
    private static final NamespacedKey KEY_PARRY_CHANCE = new NamespacedKey("armorstats", "parry_chance");
    private static final NamespacedKey KEY_MOVE_SPEED = new NamespacedKey("armorstats", "move_speed");
    private static final NamespacedKey KEY_DAMAGE_REDUCTION = new NamespacedKey("armorstats", "damage_reduction");
    
    // PVP 属性
    private static final NamespacedKey KEY_PVP_ATTACK_MIN = new NamespacedKey("armorstats", "pvp_attack_min");
    private static final NamespacedKey KEY_PVP_ATTACK_MAX = new NamespacedKey("armorstats", "pvp_attack_max");
    private static final NamespacedKey KEY_PVP_DEFENSE_MIN = new NamespacedKey("armorstats", "pvp_defense_min");
    private static final NamespacedKey KEY_PVP_DEFENSE_MAX = new NamespacedKey("armorstats", "pvp_defense_max");
    
    // 暴击抵抗
    private static final NamespacedKey KEY_CRIT_RESIST = new NamespacedKey("armorstats", "crit_resist");
    private static final NamespacedKey KEY_CRIT_DAMAGE_RESIST = new NamespacedKey("armorstats", "crit_damage_resist");
    
    // 吸血抵抗
    private static final NamespacedKey KEY_LIFESTEAL_RESIST = new NamespacedKey("armorstats", "lifesteal_resist");
    
    // 护甲系统
    private static final NamespacedKey KEY_ARMOR = new NamespacedKey("armorstats", "armor");
    private static final NamespacedKey KEY_ARMOR_STRENGTH = new NamespacedKey("armorstats", "armor_strength");
    private static final NamespacedKey KEY_ARMOR_PENETRATION = new NamespacedKey("armorstats", "armor_penetration");
    private static final NamespacedKey KEY_DEFENSE_PENETRATION = new NamespacedKey("armorstats", "defense_penetration");
    
    // 伤害反弹
    private static final NamespacedKey KEY_DAMAGE_REFLECT = new NamespacedKey("armorstats", "damage_reflect");
    private static final NamespacedKey KEY_REFLECT_RATIO = new NamespacedKey("armorstats", "reflect_ratio");
    
    // 状态效果
    private static final NamespacedKey KEY_POISON_CHANCE = new NamespacedKey("armorstats", "poison_chance");
    private static final NamespacedKey KEY_FREEZE_CHANCE = new NamespacedKey("armorstats", "freeze_chance");
    private static final NamespacedKey KEY_BLIND_CHANCE = new NamespacedKey("armorstats", "blind_chance");
    private static final NamespacedKey KEY_BURN_CHANCE = new NamespacedKey("armorstats", "burn_chance");
    private static final NamespacedKey KEY_SCORCH_CHANCE = new NamespacedKey("armorstats", "scorch_chance");
    
    // 环境抗性
    private static final NamespacedKey KEY_FIRE_RESIST = new NamespacedKey("armorstats", "fire_resist");
    private static final NamespacedKey KEY_FALL_RESIST = new NamespacedKey("armorstats", "fall_resist");
    private static final NamespacedKey KEY_DROWNING_RESIST = new NamespacedKey("armorstats", "drowning_resist");
    private static final NamespacedKey KEY_POISON_RESIST = new NamespacedKey("armorstats", "poison_resist");
    private static final NamespacedKey KEY_WITHER_RESIST = new NamespacedKey("armorstats", "wither_resist");
    private static final NamespacedKey KEY_LAVA_RESIST = new NamespacedKey("armorstats", "lava_resist");
    private static final NamespacedKey KEY_MAGIC_RESIST = new NamespacedKey("armorstats", "magic_resist");
    private static final NamespacedKey KEY_EXPLOSION_RESIST = new NamespacedKey("armorstats", "explosion_resist");
    private static final NamespacedKey KEY_PROJECTILE_RESIST = new NamespacedKey("armorstats", "projectile_resist");
    
    // 其他属性
    private static final NamespacedKey KEY_KNOCKBACK_RESIST = new NamespacedKey("armorstats", "knockback_resist");
    private static final NamespacedKey KEY_EXP_BONUS = new NamespacedKey("armorstats", "exp_bonus");
    private static final NamespacedKey KEY_HEALTH_REGEN_PERCENT = new NamespacedKey("armorstats", "health_regen_percent");
    private static final NamespacedKey KEY_HEALTH_REGEN = new NamespacedKey("armorstats", "health_regen");
    private static final NamespacedKey KEY_DODGE_REFLECT_CHANCE = new NamespacedKey("armorstats", "dodge_reflect_chance");
    private static final NamespacedKey KEY_DODGE_REFLECT_RATIO = new NamespacedKey("armorstats", "dodge_reflect_ratio");

    /**
     * 应用属性到玩家
     * 
     * 注意：Minecraft 属性（生命、移动速度）由 SimpleAttributeApplier 处理
     * 这里只存储战斗属性到 PDC（用于战斗计算）
     * 
     * @param player 玩家
     * @param stats 最终属性统计
     */
    public void apply(Player player, PlayerStats stats) {
        // 只存储战斗属性到 PDC（用于战斗计算）
        // Minecraft 属性（生命、移动速度）由 SimpleAttributeApplier 处理
        storeCombatAttributes(player, stats);
    }

    /**
     * 存储战斗属性到 PDC
     */
    private void storeCombatAttributes(Player player, PlayerStats stats) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // 攻击属性
        pdc.set(KEY_ATTACK_MIN, PersistentDataType.DOUBLE, stats.getMinAttack());
        pdc.set(KEY_ATTACK_MAX, PersistentDataType.DOUBLE, stats.getMaxAttack());
        
        // 防御属性
        pdc.set(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE, stats.getDefenseMin());
        pdc.set(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE, stats.getDefenseMax());
        
        // 暴击属性
        pdc.set(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE, stats.getCritChancePercent());
        pdc.set(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE, stats.getCritDamagePercent());
        
        // 生命偷取
        pdc.set(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE, stats.getLifestealPercent());
        pdc.set(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE, stats.getLifestealMultiplier());
        
        // 闪避与格挡
        pdc.set(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE, stats.getDodgePercent());
        pdc.set(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE, stats.getParryPercent());
        
        // 移动速度（用于显示）
        pdc.set(KEY_MOVE_SPEED, PersistentDataType.DOUBLE, stats.getMoveSpeedPercent());
        
        // 减伤
        pdc.set(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE, stats.getDamageReductionBonus());
        
        // PVP 属性
        pdc.set(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE, stats.getPvpMinAttack());
        pdc.set(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE, stats.getPvpMaxAttack());
        pdc.set(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE, stats.getPvpDefenseMin());
        pdc.set(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE, stats.getPvpDefenseMax());
        
        // 暴击抵抗
        pdc.set(KEY_CRIT_RESIST, PersistentDataType.DOUBLE, stats.getCritResistPercent());
        pdc.set(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE, stats.getCritDamageResistPercent());
        
        // 吸血抵抗
        pdc.set(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE, stats.getLifestealResistPercent());
        
        // 护甲系统
        pdc.set(KEY_ARMOR, PersistentDataType.DOUBLE, stats.getArmorPercent());
        pdc.set(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE, stats.getArmorStrength());
        pdc.set(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE, stats.getArmorPenetration());
        pdc.set(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE, stats.getDefensePenetration());
        
        // 伤害反弹
        pdc.set(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE, stats.getDamageReflectPercent());
        pdc.set(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE, stats.getReflectPercent());
        
        // 状态效果
        pdc.set(KEY_POISON_CHANCE, PersistentDataType.DOUBLE, stats.getPoisonPercent());
        pdc.set(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE, stats.getFreezePercent());
        pdc.set(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE, stats.getBlindPercent());
        pdc.set(KEY_BURN_CHANCE, PersistentDataType.DOUBLE, stats.getBurnPercent());
        pdc.set(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE, stats.getScorchPercent());
        
        // 环境抗性
        pdc.set(KEY_FIRE_RESIST, PersistentDataType.DOUBLE, stats.getFireResistPercent());
        pdc.set(KEY_FALL_RESIST, PersistentDataType.DOUBLE, stats.getFallResistPercent());
        pdc.set(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE, stats.getDrowningResistPercent());
        pdc.set(KEY_POISON_RESIST, PersistentDataType.DOUBLE, stats.getPoisonResistPercent());
        pdc.set(KEY_WITHER_RESIST, PersistentDataType.DOUBLE, stats.getWitherResistPercent());
        pdc.set(KEY_LAVA_RESIST, PersistentDataType.DOUBLE, stats.getLavaResistPercent());
        pdc.set(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE, stats.getMagicResistPercent());
        pdc.set(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE, stats.getExplosionResistPercent());
        pdc.set(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE, stats.getProjectileResistPercent());
        
        // 其他属性
        pdc.set(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE, stats.getKnockbackResistPercent());
        pdc.set(KEY_EXP_BONUS, PersistentDataType.DOUBLE, stats.getExpBonusPercent());
        pdc.set(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE, stats.getHealthRegenPercent());
        pdc.set(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE, stats.getHealthRegen());
        pdc.set(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE, stats.getDodgeReflectPercent());
        pdc.set(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE, stats.getDodgeReflectRatio());
    }

    /**
     * 从玩家 PDC 读取属性
     */
    public PlayerStats readFromPlayer(Player player) {
        PlayerStats stats = new PlayerStats();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // 攻击属性
        double attackMin = pdc.getOrDefault(KEY_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0);
        double attackMax = pdc.getOrDefault(KEY_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0);
        if (attackMin > 0 || attackMax > 0) {
            stats.setMinAttack(attackMin);
            stats.setMaxAttack(attackMax);
        }
        
        // 防御属性
        double defenseMin = pdc.getOrDefault(KEY_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0);
        double defenseMax = pdc.getOrDefault(KEY_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0);
        if (defenseMin > 0 || defenseMax > 0) {
            stats.setDefenseMin(defenseMin);
            stats.setDefenseMax(defenseMax);
        }
        
        // 暴击属性
        double critChance = pdc.getOrDefault(KEY_CRIT_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double critDamage = pdc.getOrDefault(KEY_CRIT_DAMAGE, PersistentDataType.DOUBLE, 0.0);
        if (critChance > 0) stats.setCritChancePercent(critChance);
        if (critDamage > 0) stats.setCritDamagePercent(critDamage);
        
        // 生命偷取
        double lifestealChance = pdc.getOrDefault(KEY_LIFESTEAL_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double lifestealMultiplier = pdc.getOrDefault(KEY_LIFESTEAL_MULTIPLIER, PersistentDataType.DOUBLE, 0.0);
        if (lifestealChance > 0) stats.setLifestealPercent(lifestealChance);
        if (lifestealMultiplier > 0) stats.setLifestealMultiplier(lifestealMultiplier);
        
        // 闪避与格挡
        double dodgeChance = pdc.getOrDefault(KEY_DODGE_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double parryChance = pdc.getOrDefault(KEY_PARRY_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (dodgeChance > 0) stats.setDodgePercent(dodgeChance);
        if (parryChance > 0) stats.setParryPercent(parryChance);
        
        // 移动速度
        double moveSpeed = pdc.getOrDefault(KEY_MOVE_SPEED, PersistentDataType.DOUBLE, 0.0);
        if (moveSpeed > 0) stats.setMoveSpeedPercent(moveSpeed);
        
        // 减伤
        double damageReduction = pdc.getOrDefault(KEY_DAMAGE_REDUCTION, PersistentDataType.DOUBLE, 0.0);
        if (damageReduction > 0) stats.setDamageReductionBonus(damageReduction);
        
        // PVP 属性
        double pvpAttackMin = pdc.getOrDefault(KEY_PVP_ATTACK_MIN, PersistentDataType.DOUBLE, 0.0);
        double pvpAttackMax = pdc.getOrDefault(KEY_PVP_ATTACK_MAX, PersistentDataType.DOUBLE, 0.0);
        if (pvpAttackMin > 0 || pvpAttackMax > 0) {
            stats.setPvpMinAttack(pvpAttackMin);
            stats.setPvpMaxAttack(pvpAttackMax);
        }
        double pvpDefenseMin = pdc.getOrDefault(KEY_PVP_DEFENSE_MIN, PersistentDataType.DOUBLE, 0.0);
        double pvpDefenseMax = pdc.getOrDefault(KEY_PVP_DEFENSE_MAX, PersistentDataType.DOUBLE, 0.0);
        if (pvpDefenseMin > 0 || pvpDefenseMax > 0) {
            stats.setPvpDefenseMin(pvpDefenseMin);
            stats.setPvpDefenseMax(pvpDefenseMax);
        }
        
        // 暴击抵抗
        double critResist = pdc.getOrDefault(KEY_CRIT_RESIST, PersistentDataType.DOUBLE, 0.0);
        double critDamageResist = pdc.getOrDefault(KEY_CRIT_DAMAGE_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (critResist > 0) stats.setCritResistPercent(critResist);
        if (critDamageResist > 0) stats.setCritDamageResistPercent(critDamageResist);
        
        // 吸血抵抗
        double lifestealResist = pdc.getOrDefault(KEY_LIFESTEAL_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (lifestealResist > 0) stats.setLifestealResistPercent(lifestealResist);
        
        // 护甲系统
        double armor = pdc.getOrDefault(KEY_ARMOR, PersistentDataType.DOUBLE, 0.0);
        double armorStrength = pdc.getOrDefault(KEY_ARMOR_STRENGTH, PersistentDataType.DOUBLE, 0.0);
        double armorPenetration = pdc.getOrDefault(KEY_ARMOR_PENETRATION, PersistentDataType.DOUBLE, 0.0);
        double defensePenetration = pdc.getOrDefault(KEY_DEFENSE_PENETRATION, PersistentDataType.DOUBLE, 0.0);
        if (armor > 0) stats.setArmorPercent(armor);
        if (armorStrength > 0) stats.setArmorStrength(armorStrength);
        if (armorPenetration > 0) stats.setArmorPenetration(armorPenetration);
        if (defensePenetration > 0) stats.setDefensePenetration(defensePenetration);
        
        // 伤害反弹
        double damageReflect = pdc.getOrDefault(KEY_DAMAGE_REFLECT, PersistentDataType.DOUBLE, 0.0);
        double reflectRatio = pdc.getOrDefault(KEY_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0);
        if (damageReflect > 0) stats.setDamageReflectPercent(damageReflect);
        if (reflectRatio > 0) stats.setReflectPercent(reflectRatio);
        
        // 状态效果
        double poisonChance = pdc.getOrDefault(KEY_POISON_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double freezeChance = pdc.getOrDefault(KEY_FREEZE_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double blindChance = pdc.getOrDefault(KEY_BLIND_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double burnChance = pdc.getOrDefault(KEY_BURN_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double scorchChance = pdc.getOrDefault(KEY_SCORCH_CHANCE, PersistentDataType.DOUBLE, 0.0);
        if (poisonChance > 0) stats.setPoisonPercent(poisonChance);
        if (freezeChance > 0) stats.setFreezePercent(freezeChance);
        if (blindChance > 0) stats.setBlindPercent(blindChance);
        if (burnChance > 0) stats.setBurnPercent(burnChance);
        if (scorchChance > 0) stats.setScorchPercent(scorchChance);
        
        // 环境抗性
        double fireResist = pdc.getOrDefault(KEY_FIRE_RESIST, PersistentDataType.DOUBLE, 0.0);
        double fallResist = pdc.getOrDefault(KEY_FALL_RESIST, PersistentDataType.DOUBLE, 0.0);
        double drowningResist = pdc.getOrDefault(KEY_DROWNING_RESIST, PersistentDataType.DOUBLE, 0.0);
        double poisonResist = pdc.getOrDefault(KEY_POISON_RESIST, PersistentDataType.DOUBLE, 0.0);
        double witherResist = pdc.getOrDefault(KEY_WITHER_RESIST, PersistentDataType.DOUBLE, 0.0);
        double lavaResist = pdc.getOrDefault(KEY_LAVA_RESIST, PersistentDataType.DOUBLE, 0.0);
        double magicResist = pdc.getOrDefault(KEY_MAGIC_RESIST, PersistentDataType.DOUBLE, 0.0);
        double explosionResist = pdc.getOrDefault(KEY_EXPLOSION_RESIST, PersistentDataType.DOUBLE, 0.0);
        double projectileResist = pdc.getOrDefault(KEY_PROJECTILE_RESIST, PersistentDataType.DOUBLE, 0.0);
        if (fireResist > 0) stats.setFireResistPercent(fireResist);
        if (fallResist > 0) stats.setFallResistPercent(fallResist);
        if (drowningResist > 0) stats.setDrowningResistPercent(drowningResist);
        if (poisonResist > 0) stats.setPoisonResistPercent(poisonResist);
        if (witherResist > 0) stats.setWitherResistPercent(witherResist);
        if (lavaResist > 0) stats.setLavaResistPercent(lavaResist);
        if (magicResist > 0) stats.setMagicResistPercent(magicResist);
        if (explosionResist > 0) stats.setExplosionResistPercent(explosionResist);
        if (projectileResist > 0) stats.setProjectileResistPercent(projectileResist);
        
        // 其他属性
        double knockbackResist = pdc.getOrDefault(KEY_KNOCKBACK_RESIST, PersistentDataType.DOUBLE, 0.0);
        double expBonus = pdc.getOrDefault(KEY_EXP_BONUS, PersistentDataType.DOUBLE, 0.0);
        double healthRegenPercent = pdc.getOrDefault(KEY_HEALTH_REGEN_PERCENT, PersistentDataType.DOUBLE, 0.0);
        double healthRegen = pdc.getOrDefault(KEY_HEALTH_REGEN, PersistentDataType.DOUBLE, 0.0);
        double dodgeReflectChance = pdc.getOrDefault(KEY_DODGE_REFLECT_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double dodgeReflectRatio = pdc.getOrDefault(KEY_DODGE_REFLECT_RATIO, PersistentDataType.DOUBLE, 0.0);
        if (knockbackResist > 0) stats.setKnockbackResistPercent(knockbackResist);
        if (expBonus > 0) stats.setExpBonusPercent(expBonus);
        if (healthRegenPercent > 0) stats.setHealthRegenPercent(healthRegenPercent);
        if (healthRegen > 0) stats.setHealthRegen(healthRegen);
        if (dodgeReflectChance > 0) stats.setDodgeReflectPercent(dodgeReflectChance);
        if (dodgeReflectRatio > 0) stats.setDodgeReflectRatio(dodgeReflectRatio);
        
        return stats;
    }

    /**
     * 清除玩家 PDC 中所有战斗属性
     */
    public void clearCombatAttributes(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey[] allKeys = {
            KEY_ATTACK_MIN, KEY_ATTACK_MAX, KEY_DEFENSE_MIN, KEY_DEFENSE_MAX,
            KEY_CRIT_CHANCE, KEY_CRIT_DAMAGE, KEY_LIFESTEAL_CHANCE, KEY_LIFESTEAL_MULTIPLIER,
            KEY_DODGE_CHANCE, KEY_PARRY_CHANCE, KEY_MOVE_SPEED, KEY_DAMAGE_REDUCTION,
            KEY_PVP_ATTACK_MIN, KEY_PVP_ATTACK_MAX, KEY_PVP_DEFENSE_MIN, KEY_PVP_DEFENSE_MAX,
            KEY_CRIT_RESIST, KEY_CRIT_DAMAGE_RESIST, KEY_LIFESTEAL_RESIST,
            KEY_ARMOR, KEY_ARMOR_STRENGTH, KEY_ARMOR_PENETRATION, KEY_DEFENSE_PENETRATION,
            KEY_DAMAGE_REFLECT, KEY_REFLECT_RATIO,
            KEY_POISON_CHANCE, KEY_FREEZE_CHANCE, KEY_BLIND_CHANCE, KEY_BURN_CHANCE, KEY_SCORCH_CHANCE,
            KEY_FIRE_RESIST, KEY_FALL_RESIST, KEY_DROWNING_RESIST, KEY_POISON_RESIST,
            KEY_WITHER_RESIST, KEY_LAVA_RESIST, KEY_MAGIC_RESIST, KEY_EXPLOSION_RESIST, KEY_PROJECTILE_RESIST,
            KEY_KNOCKBACK_RESIST, KEY_EXP_BONUS, KEY_HEALTH_REGEN, KEY_HEALTH_REGEN_PERCENT,
            KEY_DODGE_REFLECT_CHANCE, KEY_DODGE_REFLECT_RATIO
        };
        for (NamespacedKey key : allKeys) {
            pdc.remove(key);
        }
    }
}
