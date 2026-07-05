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
 * 怪物生成桥接层 — GuangDianMobs v2.0
 * <p>通过反射获取 MobSpawner + AIController，直接调用新 API。</p>
 */
public class MobBridge {

    private final GuangDianDungeon plugin;
    private final Map<UUID, String> spawnedBosses;
    private boolean mobPluginEnabled;
    private Object guangDianMobsPlugin;
    private Object mobSpawner;
    private Object aiController;

    public MobBridge(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.spawnedBosses = new ConcurrentHashMap<>();
        checkPlugins();
    }

    private void checkPlugins() {
        mobPluginEnabled = Bukkit.getPluginManager().isPluginEnabled("GuangDianMobs");
        if (mobPluginEnabled) {
            guangDianMobsPlugin = Bukkit.getPluginManager().getPlugin("GuangDianMobs");
            plugin.getLogger().info("已连接 GuangDianMobs v2.0");
        } else {
            plugin.getLogger().warning("GuangDianMobs 未启用");
        }
    }

    public boolean isMobPluginEnabled() {
        return mobPluginEnabled;
    }

    /**
     * 生成自定义怪物 — 使用新 API
     */
    public LivingEntity spawnMob(String mobId, Location loc) {
        if (!mobPluginEnabled || guangDianMobsPlugin == null) {
            plugin.getLogger().warning("GuangDianMobs 未启用");
            return null;
        }

        try {
            // 缓存反射引用
            if (mobSpawner == null) {
                mobSpawner = guangDianMobsPlugin.getClass()
                    .getMethod("getMobSpawner").invoke(guangDianMobsPlugin);
            }
            if (aiController == null) {
                aiController = guangDianMobsPlugin.getClass()
                    .getMethod("getAIController").invoke(guangDianMobsPlugin);
            }

            // 获取模板
            @SuppressWarnings("unchecked")
            Map<String, Object> templates = (Map<String, Object>) guangDianMobsPlugin.getClass()
                .getMethod("getMobTemplates").invoke(guangDianMobsPlugin);

            Object template = templates.get(mobId);
            if (template == null) {
                plugin.getLogger().warning("怪物模板不存在: " + mobId);
                return null;
            }

            // 生成: spawner.spawn(template, loc)
            Object entity = mobSpawner.getClass()
                .getMethod("spawn", template.getClass(), Location.class)
                .invoke(mobSpawner, template, loc);

            if (entity instanceof LivingEntity living) {
                // 附加 AI: aiController.attach(living, template)
                aiController.getClass()
                    .getMethod("attach", LivingEntity.class, template.getClass())
                    .invoke(aiController, living, template);

                plugin.getLogger().info("[DEBUG] GuangDianMobs 生成成功: " + mobId);
                return living;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("GuangDianMobs 生成失败: " + mobId + " - " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) e.printStackTrace();
        }
        return null;
    }

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

    public void removeBoss(UUID entityId) { spawnedBosses.remove(entityId); }

    public void clearSessionBosses(String sessionId) {
        spawnedBosses.values().removeIf(sessionId::equals);
    }

    public Map<UUID, String> getSpawnedBosses() { return spawnedBosses; }
}
