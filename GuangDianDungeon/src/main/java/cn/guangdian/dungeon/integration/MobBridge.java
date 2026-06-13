package cn.guangdian.dungeon.integration;

import cn.guangdian.dungeon.GuangDianDungeon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 怪物生成桥接层
 * 通过 RPGCore ServiceRegistry 获取 GuangDianMobs 的 MobManager 服务（编译时安全）
 * 同时保留反射回退以兼容旧版本
 */
public class MobBridge {

    private static final String MOB_MANAGER_SERVICE_KEY = "cn.guangdian.mobs.manager.MobManager";

    private final GuangDianDungeon plugin;
    private final Map<UUID, String> spawnedBosses;
    private boolean mobPluginEnabled;
    private Object guangDianMobsPlugin;

    public MobBridge(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.spawnedBosses = new ConcurrentHashMap<>();
        this.mobPluginEnabled = false;

        checkPlugins();
    }

    private void checkPlugins() {
        mobPluginEnabled = Bukkit.getPluginManager().isPluginEnabled("GuangDianMobs");

        if (mobPluginEnabled) {
            guangDianMobsPlugin = Bukkit.getPluginManager().getPlugin("GuangDianMobs");
            plugin.getLogger().info("使用 GuangDianMobs 怪物系统");
        } else {
            plugin.getLogger().warning("GuangDianMobs 未启用，怪物生成功能将不可用");
        }
    }

    public boolean isMobPluginEnabled() {
        return mobPluginEnabled;
    }

    /**
     * 生成自定义怪物
     * 优先使用 ServiceRegistry 获取服务，回退到反射调用
     */
    public LivingEntity spawnMob(String mobId, Location loc) {
        if (!mobPluginEnabled) {
            plugin.getLogger().warning("GuangDianMobs 未启用");
            return null;
        }

        // 优先尝试通过 ServiceRegistry 获取
        LivingEntity entity = spawnViaServiceRegistry(mobId, loc);
        if (entity != null) return entity;

        // 回退到反射调用
        return spawnViaReflection(mobId, loc);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LivingEntity spawnViaServiceRegistry(String mobId, Location loc) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore == null || rpgCore.getServiceRegistry() == null) return null;

            // 使用 raw type 避免泛型限制
            java.util.Optional service = (java.util.Optional)
                rpgCore.getServiceRegistry().getService(Class.forName(MOB_MANAGER_SERVICE_KEY));
            if (service == null || service.isEmpty()) return null;

            Object mobManager = service.get();
            Object entity = mobManager.getClass()
                .getMethod("spawnMob", String.class, Location.class)
                .invoke(mobManager, mobId, loc);

            if (entity instanceof LivingEntity) {
                plugin.getLogger().info("[DEBUG] GuangDianMobs 生成成功(ServiceRegistry): " + mobId);
                return (LivingEntity) entity;
            }
        } catch (Exception e) {
            // ServiceRegistry 方式不可用，回退到反射
        }
        return null;
    }

    private LivingEntity spawnViaReflection(String mobId, Location loc) {
        if (guangDianMobsPlugin == null) return null;

        try {
            Class<?> pluginClass = guangDianMobsPlugin.getClass();
            Object mobManager = pluginClass.getMethod("getMobManager").invoke(guangDianMobsPlugin);

            Class<?> mobManagerClass = mobManager.getClass();
            Object entity = mobManagerClass.getMethod("spawnMob", String.class, Location.class)
                .invoke(mobManager, mobId, loc);

            if (entity instanceof LivingEntity) {
                plugin.getLogger().info("[DEBUG] GuangDianMobs 生成成功(Reflection): " + mobId);
                return (LivingEntity) entity;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("GuangDianMobs生成失败: " + mobId + " - " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 标记实体所属的副本会话（通过 PDC）
     */
    public void tagEntityForSession(Entity entity, String sessionId) {
        if (entity == null || sessionId == null) return;
        entity.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "dungeon_session"),
            PersistentDataType.STRING,
            sessionId
        );
    }

    /**
     * 标记实体为 Boss
     */
    public void tagEntityAsBoss(Entity entity, String bossType) {
        if (entity == null) return;
        if (bossType != null) {
            entity.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "boss_id"),
                PersistentDataType.STRING,
                bossType
            );
        }
        spawnedBosses.put(entity.getUniqueId(), bossType);
    }

    /**
     * 从实体读取副本会话 ID
     */
    public String getSessionIdFromEntity(Entity entity) {
        if (entity == null) return null;
        var container = entity.getPersistentDataContainer();
        return container.get(new NamespacedKey(plugin, "dungeon_session"), PersistentDataType.STRING);
    }

    /**
     * 检查实体是否是副本 Boss
     */
    public boolean isDungeonBoss(Entity entity) {
        if (entity == null) return false;
        var container = entity.getPersistentDataContainer();
        return container.has(new NamespacedKey(plugin, "boss_id"), PersistentDataType.STRING);
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
