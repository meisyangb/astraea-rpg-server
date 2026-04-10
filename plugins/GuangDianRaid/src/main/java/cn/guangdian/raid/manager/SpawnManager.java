package cn.guangdian.raid.manager;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.instance.RaidInstance;
import cn.guangdian.raid.model.DifficultyScaling;
import cn.guangdian.raid.model.EnemySpawn;
import cn.guangdian.raid.model.EnemyWave;
import cn.guangdian.raid.model.RaidPhaseType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SpawnManager {

    private final GuangDianRaid plugin;
    private final Random random;
    private final boolean mythicMobsEnabled;

    public SpawnManager(GuangDianRaid plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.mythicMobsEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    public void spawnWavesForPhase(RaidInstance instance, RaidPhaseType phase) {
        for (EnemyWave wave : instance.getRaid().getEnemyWaves()) {
            if (shouldSpawnWave(wave, instance, phase)) {
                spawnWave(instance, wave);
            }
        }
    }

    private boolean shouldSpawnWave(EnemyWave wave, RaidInstance instance, RaidPhaseType phase) {
        return switch (wave.getTrigger()) {
            case PHASE_START -> true;
            case INTEL_COLLECT -> instance.getCollectedIntel() >= wave.getTriggerValue();
            case OBJECTIVE_COMPLETE -> false;
            case PLAYER_DEATH -> false;
            case TIME_ELAPSED -> false;
        };
    }

    private void spawnWave(RaidInstance instance, EnemyWave wave) {
        for (EnemySpawn spawn : wave.getSpawns()) {
            spawnEnemies(instance, spawn);
        }
    }

    private void spawnEnemies(RaidInstance instance, EnemySpawn spawn) {
        World world = instance.getWorld();
        if (world == null) return;

        DifficultyScaling difficulty = instance.getCurrentDifficulty();
        int count = spawn.getCount() + difficulty.getAdditionalMobs();

        for (int i = 0; i < count; i++) {
            Location loc = getSpawnLocation(instance, spawn);
            if (loc == null) continue;

            loc.setWorld(world);

            if (mythicMobsEnabled) {
                spawnMythicMob(instance, spawn.getMobType(), loc, difficulty);
            } else {
                spawnVanillaMob(instance, spawn.getMobType(), loc, difficulty);
            }
        }
    }

    private Location getSpawnLocation(RaidInstance instance, EnemySpawn spawn) {
        List<Location> locations = spawn.getLocations();
        if (locations.isEmpty()) {
            return instance.getRaid().getSpawnLocation();
        }
        return locations.get(random.nextInt(locations.size()));
    }

    private void spawnMythicMob(RaidInstance instance, String mobType, Location location, DifficultyScaling difficulty) {
        try {
            Object mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit")
                .getMethod("inst").invoke(null);
            Object apiHelper = mythicBukkit.getClass().getMethod("getAPIHelper").invoke(mythicBukkit);
            Object activeMob = apiHelper.getClass().getMethod("spawnMythicMob", String.class, Location.class)
                .invoke(apiHelper, mobType, location);
            
            if (activeMob != null) {
                Object entity = activeMob.getClass().getMethod("getEntity").invoke(activeMob);
                Object bukkitEntity = entity.getClass().getMethod("getBukkitEntity").invoke(entity);
                if (bukkitEntity instanceof LivingEntity living) {
                    applyDifficultyScaling(living, difficulty);
                }
                instance.addSpawnedEnemy(((Entity) bukkitEntity).getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("生成MythicMobs怪物失败: " + mobType + " - " + e.getMessage());
            spawnVanillaMob(instance, "ZOMBIE", location, difficulty);
        }
    }

    private void spawnVanillaMob(RaidInstance instance, String mobType, Location location, DifficultyScaling difficulty) {
        try {
            org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(mobType.toUpperCase());
            Entity entity = location.getWorld().spawnEntity(location, entityType);
            if (entity instanceof LivingEntity living) {
                applyDifficultyScaling(living, difficulty);
            }
            instance.addSpawnedEnemy(entity.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("生成原版怪物失败: " + mobType + " - " + e.getMessage());
        }
    }

    private void applyDifficultyScaling(LivingEntity entity, DifficultyScaling difficulty) {
        var healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double baseHealth = healthAttr.getBaseValue();
            healthAttr.setBaseValue(baseHealth * difficulty.getHealthMultiplier());
            entity.setHealth(healthAttr.getBaseValue());
        }

        var damageAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * difficulty.getDamageMultiplier());
        }
    }

    public void onMobDeath(LivingEntity mob) {
        for (RaidInstance instance : plugin.getInstanceManager().getAllInstances()) {
            if (instance.getSpawnedEnemies().contains(mob.getUniqueId())) {
                instance.getSpawnedEnemies().remove(mob.getUniqueId());
                break;
            }
        }
    }
}
