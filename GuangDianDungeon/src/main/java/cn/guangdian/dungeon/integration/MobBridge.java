package cn.guangdian.dungeon.integration;

import cn.guangdian.dungeon.GuangDianDungeon;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 怪物生成桥接层 — MythicMobs 5.x 直接 API 集成
 */
public class MobBridge {

    private final GuangDianDungeon plugin;
    private final Map<UUID, String> spawnedBosses;
    private boolean mythicMobsEnabled;
    private BukkitAPIHelper apiHelper;

    public MobBridge(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.spawnedBosses = new ConcurrentHashMap<>();
        checkPlugins();
    }

    private void checkPlugins() {
        try {
            mythicMobsEnabled = MythicBukkit.inst() != null;
            if (mythicMobsEnabled) {
                apiHelper = MythicBukkit.inst().getAPIHelper();
                plugin.getLogger().info("已连接 MythicMobs " + MythicBukkit.inst().getVersion());
            }
        } catch (Exception e) {
            mythicMobsEnabled = false;
            plugin.getLogger().warning("MythicMobs 未启用: " + e.getMessage());
        }
    }

    public boolean isMobPluginEnabled() {
        return mythicMobsEnabled;
    }

    /**
     * 生成 MythicMobs 怪物
     */
    public LivingEntity spawnMob(String mobId, Location loc) {
        if (!mythicMobsEnabled) {
            plugin.getLogger().warning("MythicMobs 未启用，无法生成: " + mobId);
            return null;
        }

        try {
            Optional<MythicMob> mobOpt = MythicBukkit.inst().getMobManager().getMythicMob(mobId);
            if (mobOpt.isEmpty()) {
                plugin.getLogger().warning("MythicMobs 怪物模板不存在: " + mobId);
                return null;
            }

            MythicMob mob = mobOpt.get();
            // 使用 BukkitAdapter 转换 Location -> AbstractLocation
            AbstractLocation abstractLoc = BukkitAdapter.adapt(loc);
            ActiveMob activeMob = mob.spawn(abstractLoc, 1.0);
            if (activeMob == null) {
                plugin.getLogger().warning("MythicMobs 生成失败: " + mobId);
                return null;
            }

            // 使用 BukkitAdapter 获取 Bukkit Entity
            AbstractEntity abstractEntity = activeMob.getEntity();
            Entity entity = abstractEntity.getBukkitEntity();
            if (entity instanceof LivingEntity living) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[DEBUG] MythicMobs 生成成功: " + mobId);
                }
                return living;
            }

            plugin.getLogger().warning("MythicMobs 生成的实体不是 LivingEntity: " + mobId);
            return null;

        } catch (Exception e) {
            plugin.getLogger().warning("MythicMobs 生成异常: " + mobId + " - " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // ========== PDC 标记 ==========

    public void tagEntityForSession(Entity entity, String sessionId) {
        if (entity == null || sessionId == null) return;
        entity.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "dungeon_session"),
            PersistentDataType.STRING, sessionId);
    }

    public void tagEntityAsBoss(Entity entity, String bossType) {
        if (entity == null) return;
        if (bossType != null) {
            entity.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "boss_id"),
                PersistentDataType.STRING, bossType);
        }
        spawnedBosses.put(entity.getUniqueId(), bossType);
    }

    public String getSessionIdFromEntity(Entity entity) {
        if (entity == null) return null;
        return entity.getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "dungeon_session"), PersistentDataType.STRING);
    }

    public boolean isDungeonBoss(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer()
            .has(new NamespacedKey(plugin, "boss_id"), PersistentDataType.STRING);
    }

    /**
     * 检测实体是否为 MythicMobs 怪物
     */
    public boolean isMythicMob(Entity entity) {
        if (!mythicMobsEnabled || apiHelper == null) return false;
        return apiHelper.isMythicMob(entity);
    }

    /**
     * 获取 MythicMobs 怪物的内部名称
     */
    public String getMythicMobName(Entity entity) {
        if (!mythicMobsEnabled || apiHelper == null) return null;
        ActiveMob activeMob = apiHelper.getMythicMobInstance(entity);
        return activeMob != null ? activeMob.getType().getInternalName() : null;
    }

    public void removeBoss(UUID entityId) {
        spawnedBosses.remove(entityId);
    }

    public void clearSessionBosses(String sessionId) {
        spawnedBosses.values().removeIf(sessionId::equals);
    }

    public Map<UUID, String> getSpawnedBosses() {
        return spawnedBosses;
    }
}