package cn.guangdian.classsystem.placeholder;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.classsystem.api.ClassService;
import cn.guangdian.classsystem.manager.AttributeManager;
import cn.guangdian.classsystem.manager.ClassManager;
import cn.guangdian.classsystem.manager.ExpManager;
import cn.guangdian.classsystem.model.AttributeType;
import cn.guangdian.classsystem.model.GameClass;
import cn.guangdian.classsystem.model.PlayerClassData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClassPlaceholder extends PlaceholderExpansion {
    
    private final GuangDianClass plugin;
    private final ClassService classService;
    private final ClassManager classManager;
    private final ExpManager expManager;
    private final AttributeManager attributeManager;
    
    public ClassPlaceholder(GuangDianClass plugin, ClassService classService,
                           ClassManager classManager, ExpManager expManager, AttributeManager attributeManager) {
        this.plugin = plugin;
        this.classService = classService;
        this.classManager = classManager;
        this.expManager = expManager;
        this.attributeManager = attributeManager;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "gdclass";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Astraea RPG Team";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        String identifier = params.toLowerCase();
        
        return switch (identifier) {
            case "name", "class" -> classService.getPlayerClassName(player.getUniqueId());
            case "tier", "阶位" -> String.valueOf(classService.getPlayerTier(player.getUniqueId()));
            case "exp" -> String.valueOf(classService.getPlayerExp(player.getUniqueId()));
            case "advancement", "转职" -> {
                int level = classService.getPlayerAdvancementLevel(player.getUniqueId());
                yield switch (level) {
                    case 1 -> "一转";
                    case 2 -> "二转";
                    case 3 -> "三转";
                    case 4 -> "神级";
                    default -> "未转职";
                };
            }
            case "advancement_level" -> String.valueOf(classService.getPlayerAdvancementLevel(player.getUniqueId()));
            case "total_exp" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                yield data != null ? String.valueOf(data.getTotalExp()) : "0";
            }
            case "exp_progress" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                double progress = expManager.getExpProgress(data);
                yield String.format("%.2f", progress * 100);
            }
            case "exp_required" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                long required = classManager.getExpRequiredForNextTier(data.getTier());
                yield String.valueOf(required);
            }
            case "next_tier" -> {
                int tier = classService.getPlayerTier(player.getUniqueId());
                int maxTier = plugin.getConfig().getInt("settings.max-tier", 9);
                yield tier < maxTier ? String.valueOf(tier + 1) : "MAX";
            }
            case "max_tier" -> String.valueOf(plugin.getConfig().getInt("settings.max-tier", 9));
            case "stats_health" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                GameClass gameClass = classManager.getClass(data.getClassId());
                if (gameClass == null) yield "0";
                yield String.valueOf(gameClass.getStats().getOrDefault("health", 0.0).intValue());
            }
            case "stats_attack" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                GameClass gameClass = classManager.getClass(data.getClassId());
                if (gameClass == null) yield "0";
                yield String.valueOf(gameClass.getStats().getOrDefault("attack", 0.0).intValue());
            }
            case "stats_defense" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                GameClass gameClass = classManager.getClass(data.getClassId());
                if (gameClass == null) yield "0";
                yield String.valueOf(gameClass.getStats().getOrDefault("defense", 0.0).intValue());
            }
            case "stats_mana" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "0";
                GameClass gameClass = classManager.getClass(data.getClassId());
                if (gameClass == null) yield "0";
                yield String.valueOf(gameClass.getStats().getOrDefault("mana", 0.0).intValue());
            }
            case "can_advance" -> {
                PlayerClassData data = classService.getPlayerData(player.getUniqueId());
                if (data == null) yield "false";
                yield expManager.canAdvance(data) ? "true" : "false";
            }
            case "attr_points", "attr_available" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAvailablePoints(player.getPlayer()));
                }
                yield "0";
            }
            case "attr_used" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getTotalAllocatedPoints(player.getPlayer()));
                }
                yield "0";
            }
            case "attr_strength" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAllocatedPoints(player.getPlayer(), AttributeType.STRENGTH));
                }
                yield "0";
            }
            case "attr_vitality" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAllocatedPoints(player.getPlayer(), AttributeType.VITALITY));
                }
                yield "0";
            }
            case "attr_agility" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAllocatedPoints(player.getPlayer(), AttributeType.AGILITY));
                }
                yield "0";
            }
            case "attr_intelligence" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAllocatedPoints(player.getPlayer(), AttributeType.INTELLIGENCE));
                }
                yield "0";
            }
            case "attr_luck" -> {
                if (player.isOnline()) {
                    yield String.valueOf(attributeManager.getAllocatedPoints(player.getPlayer(), AttributeType.LUCK));
                }
                yield "0";
            }
            case "attr_bonus_health" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.valueOf((int) bonus.health);
                }
                yield "0";
            }
            case "attr_bonus_attack" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.format("%.1f", bonus.attack);
                }
                yield "0";
            }
            case "attr_bonus_defense" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.format("%.1f", bonus.defense);
                }
                yield "0";
            }
            case "attr_bonus_crit_chance" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.format("%.1f", bonus.critChance);
                }
                yield "0";
            }
            case "attr_bonus_crit_damage" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.format("%.1f", bonus.critDamage);
                }
                yield "0";
            }
            case "attr_bonus_mana" -> {
                if (player.isOnline()) {
                    AttributeManager.AttributeBonus bonus = attributeManager.calculateTotalBonus(player.getPlayer());
                    yield String.valueOf((int) bonus.mana);
                }
                yield "0";
            }
            default -> null;
        };
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return onRequest(player, params);
    }
}
