package cn.guangdian.armorstats.placeholder;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.ClassService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * 装备属性占位符扩展
 * 通过 RPGCore ServiceRegistry 获取 ClassService
 */
public class ArmorStatsPlaceholderExpansion extends PlaceholderExpansion {

    private final GuangDianArmorStats plugin;
    private ClassService classService;

    public ArmorStatsPlaceholderExpansion(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        initClassService();
    }

    /**
     * 初始化职业服务
     */
    private void initClassService() {
        // 从 RPGCore ServiceRegistry 获取服务
        if (RPGCore.getInstance() != null) {
            ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
            if (registry != null) {
                classService = registry.getOptionalService(ClassService.class).orElse(null);
            }
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdrpg";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuangDian";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || plugin.getStatsManager() == null) {
            return "0";
        }

        PlayerStats stats = plugin.getStatsManager().getPlayerStats(player);
        if (stats == null) {
            return "0";
        }

        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            // ========== 生命值系统 ==========
            case "health" -> formatValue(player.getHealth(), false);
            case "max_health", "total_max_health" -> formatValue(getPlayerMaxHealth(player), false);
            case "base_health" -> "20";
            case "equipment_health" -> formatValue(stats.getMaxHealth() - getClassHealth(player), false);
            case "class_health" -> formatValue(getClassHealth(player), false);
            case "attack" -> formatValue(plugin.getDamageManager().getTotalAttack(player), false);
            case "defense" -> formatValue(plugin.getDamageManager().getTotalDefense(player), false);
            case "min_attack" -> formatValue(stats.getMinAttack(), false);
            case "max_attack" -> formatValue(stats.getMaxAttack(), false);
            case "min_defense" -> formatValue(stats.getDefenseMin(), false);
            case "max_defense" -> formatValue(stats.getDefenseMax(), false);
            
            // ========== 暴击系统 ==========
            case "crit_chance", "critical_chance" -> formatValue(stats.getCritChancePercent(), false);
            case "crit_damage", "critical_damage" -> formatValue(150.0 + stats.getCritDamagePercent(), false);
            case "crit_resist" -> formatValue(stats.getCritResistPercent(), false);
            case "crit_damage_resist" -> formatValue(stats.getCritDamageResistPercent(), false);
            
            // ========== 吸血系统 ==========
            case "life_steal", "lifesteal" -> formatValue(stats.getLifestealPercent(), false);
            case "lifesteal_multiplier" -> formatValue(stats.getLifestealMultiplier(), false);
            case "lifesteal_resist" -> formatValue(stats.getLifestealResistPercent(), false);
            
            // ========== 回血系统 ==========
            case "health_regen" -> formatValue(stats.getHealthRegen(), false);
            case "health_regen_percent" -> formatValue(stats.getHealthRegenPercent(), false);
            
            // ========== 闪避/招架系统 ==========
            case "dodge" -> formatValue(stats.getDodgePercent(), false);
            case "parry" -> formatValue(stats.getParryPercent(), false);
            
            // ========== 反伤系统 ==========
            case "damage_reflect" -> formatValue(stats.getDamageReflectPercent(), false);
            case "reflect" -> formatValue(stats.getReflectPercent(), false);
            case "dodge_reflect" -> formatValue(stats.getDodgeReflectPercent(), false);
            case "dodge_reflect_ratio" -> formatValue(stats.getDodgeReflectRatio(), false);
            
            // ========== PVP属性 ==========
            case "pvp_attack" -> formatValue(stats.getPvpAttackAverage(), false);
            case "pvp_min_attack" -> formatValue(stats.getPvpMinAttack(), false);
            case "pvp_max_attack" -> formatValue(stats.getPvpMaxAttack(), false);
            case "pvp_defense" -> formatValue(stats.getPvpDefenseAverage(), false);
            case "pvp_min_defense" -> formatValue(stats.getPvpDefenseMin(), false);
            case "pvp_max_defense" -> formatValue(stats.getPvpDefenseMax(), false);
            
            // ========== 移动/攻速 ==========
            case "move_speed" -> formatValue(stats.getMoveSpeedPercent(), false);
            case "attack_speed" -> formatValue(getAttackSpeed(player), true);
            
            // ========== 状态效果 ==========
            case "poison" -> formatValue(stats.getPoisonPercent(), false);
            case "freeze" -> formatValue(stats.getFreezePercent(), false);
            case "blind" -> formatValue(stats.getBlindPercent(), false);
            case "burn" -> formatValue(stats.getBurnPercent(), false);
            case "scorch" -> formatValue(stats.getScorchPercent(), false);
            
            // ========== 经验加成 ==========
            case "exp_bonus" -> formatValue(stats.getExpBonusPercent(), false);
            
            // ========== 护甲与穿透系统 ==========
            case "armor" -> formatValue(stats.getArmorPercent(), false);
            case "armor_strength" -> formatValue(stats.getArmorStrength(), false);
            case "armor_penetration" -> formatValue(stats.getArmorPenetration(), false);
            case "defense_penetration" -> formatValue(stats.getDefensePenetration(), false);
            case "damage_reduction" -> formatValue(stats.getDamageReductionBonus(), false);
            
            // ========== 击退抗性 ==========
            case "knockback_resist" -> formatValue(stats.getKnockbackResistPercent(), false);
            
            // ========== 环境伤害抗性 ==========
            case "fire_resist" -> formatValue(stats.getFireResistPercent(), false);
            case "fall_resist" -> formatValue(stats.getFallResistPercent(), false);
            case "drowning_resist" -> formatValue(stats.getDrowningResistPercent(), false);
            case "poison_resist" -> formatValue(stats.getPoisonResistPercent(), false);
            case "wither_resist" -> formatValue(stats.getWitherResistPercent(), false);
            case "lava_resist" -> formatValue(stats.getLavaResistPercent(), false);
            case "magic_resist" -> formatValue(stats.getMagicResistPercent(), false);
            case "explosion_resist" -> formatValue(stats.getExplosionResistPercent(), false);
            case "projectile_resist" -> formatValue(stats.getProjectileResistPercent(), false);
            
            default -> null;
        };
    }

    private double getAttackSpeed(Player player) {
        if (player.getAttribute(Attribute.ATTACK_SPEED) == null) {
            return 0.0;
        }
        return player.getAttribute(Attribute.ATTACK_SPEED).getValue();
    }
    
    private double getPlayerMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            return attr.getValue();
        }
        return player.getMaxHealth();
    }

    /**
     * 获取职业生命值加成
     */
    private double getClassHealth(Player player) {
        if (classService == null) {
            return 0.0;
        }
        Map<String, Double> classStats = classService.getPlayerClassStats(player.getUniqueId());
        if (classStats == null) {
            return 0.0;
        }
        return classStats.getOrDefault("health", 0.0);
    }

    private String formatValue(double value, boolean keepDecimal) {
        if (keepDecimal) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.valueOf((int) Math.round(value));
    }
}
