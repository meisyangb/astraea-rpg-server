package cn.guangdian.killaura.manager;

import cn.guangdian.killaura.GuangDianKillAura;
import cn.guangdian.killaura.config.KillAuraConfig;
import cn.guangdian.killaura.model.TargetStrategy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetSelector {

    private final GuangDianKillAura plugin;

    public TargetSelector(GuangDianKillAura plugin) {
        this.plugin = plugin;
    }

    public LivingEntity selectTarget(Player player, TargetStrategy strategy) {
        KillAuraConfig config = plugin.getKillAuraConfig();
        List<LivingEntity> candidates = findCandidates(player, config);

        if (candidates.isEmpty()) {
            return null;
        }

        return switch (strategy) {
            case NEAREST -> selectNearest(player, candidates);
            case LOWEST_HEALTH -> selectLowestHealth(candidates);
            case HIGHEST_AGGRO -> selectHighestAggro(player, candidates);
        };
    }

    private List<LivingEntity> findCandidates(Player player, KillAuraConfig config) {
        double range = config.getMaxAttackRange();
        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!isValidTarget(player, living, config)) {
                continue;
            }
            candidates.add(living);
            if (candidates.size() >= config.getMaxTargetsPerScan()) {
                break;
            }
        }

        return candidates;
    }

    private boolean isValidTarget(Player player, LivingEntity entity, KillAuraConfig config) {
        if (entity.equals(player)) {
            return false;
        }

        if (entity.isDead()) {
            return false;
        }

        if (entity instanceof Player targetPlayer) {
            if (!config.isAttackPlayers()) {
                return false;
            }
            if (targetPlayer.hasPermission("killaura.bypass")) {
                return false;
            }
            if (!player.canSee(targetPlayer)) {
                return false;
            }
        }

        if (isMonster(entity) && !config.isAttackMonsters()) {
            return false;
        }

        if (isAnimal(entity) && !config.isAttackAnimals()) {
            return false;
        }

        if (config.isRequireLineOfSight() && !player.hasLineOfSight(entity)) {
            return false;
        }

        double distance = player.getLocation().distance(entity.getLocation());
        if (distance > config.getMaxAttackRange()) {
            return false;
        }

        return true;
    }

    private boolean isMonster(LivingEntity entity) {
        return entity instanceof org.bukkit.entity.Monster
            || entity instanceof org.bukkit.entity.Slime
            || entity instanceof org.bukkit.entity.Phantom
            || entity instanceof org.bukkit.entity.EnderDragon
            || entity instanceof org.bukkit.entity.Wither;
    }

    private boolean isAnimal(LivingEntity entity) {
        return entity instanceof org.bukkit.entity.Animals
            || entity instanceof org.bukkit.entity.WaterMob
            || entity instanceof org.bukkit.entity.Golem;
    }

    private LivingEntity selectNearest(Player player, List<LivingEntity> candidates) {
        return candidates.stream()
            .min(Comparator.comparingDouble(e -> player.getLocation().distance(e.getLocation())))
            .orElse(null);
    }

    private LivingEntity selectLowestHealth(List<LivingEntity> candidates) {
        return candidates.stream()
            .min(Comparator.comparingDouble(LivingEntity::getHealth))
            .orElse(null);
    }

    private LivingEntity selectHighestAggro(Player player, List<LivingEntity> candidates) {
        return candidates.stream()
            .max(Comparator.comparingDouble(e -> getAggroValue(player, e)))
            .orElse(null);
    }

    private double getAggroValue(Player player, LivingEntity entity) {
        if (plugin.getRPGCore() == null) {
            return 0;
        }

        try {
            var serviceOpt = plugin.getRPGCore().getServiceRegistry()
                .getOptionalService(Class.forName("cn.guangdian.aggro.api.AggroService"));
            if (serviceOpt.isPresent()) {
                Object service = serviceOpt.get();
                var method = service.getClass().getMethod("getAggro", LivingEntity.class, Player.class);
                Object result = method.invoke(service, entity, player);
                if (result instanceof Double d) {
                    return d;
                }
            }
        } catch (Exception ignored) {
        }

        return 0;
    }
}
