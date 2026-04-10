package cn.guangdian.aggro.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class MythicMobsHook {

    private boolean enabled = false;
    private Object mythicMukkit;
    private Method getMythicMobInstanceMethod;
    private Method getMobTypeMethod;
    private Plugin mythicMobsPlugin;

    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianAggro] MythicMobs 未安装，跳过集成");
            return;
        }

        this.mythicMobsPlugin = plugin;

        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            mythicMukkit = instMethod.invoke(null);

            Class<?> mobManagerClass = Class.forName("io.lumine.mythic.bukkit.BukkitMobManager");
            Method getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
            Object mobManager = getMobManagerMethod.invoke(mythicMukkit);

            getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", LivingEntity.class);

            Class<?> activeMobClass = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
            getMobTypeMethod = activeMobClass.getMethod("getMobType");

            enabled = true;
            Bukkit.getLogger().info("[GuangDianAggro] MythicMobs 集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianAggro] MythicMobs 集成失败: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (!enabled || entity == null) return false;

        try {
            Object activeMob = getMythicMobInstanceMethod.invoke(
                mythicMukkit.getClass().getMethod("getMobManager").invoke(mythicMukkit),
                entity
            );
            return activeMob != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMythicMobType(LivingEntity entity) {
        if (!enabled || entity == null) return null;

        try {
            Object mobManager = mythicMukkit.getClass().getMethod("getMobManager").invoke(mythicMukkit);
            Object activeMob = getMythicMobInstanceMethod.invoke(mobManager, entity);

            if (activeMob != null) {
                return (String) getMobTypeMethod.invoke(activeMob);
            }
        } catch (Exception e) {
        }

        return null;
    }

    public void setThreatTable(LivingEntity entity, boolean enabled) {
        if (!this.enabled || entity == null) return;

        try {
            Object mobManager = mythicMukkit.getClass().getMethod("getMobManager").invoke(mythicMukkit);
            Object activeMob = getMythicMobInstanceMethod.invoke(mobManager, entity);

            if (activeMob != null) {
                Class<?> activeMobClass = activeMob.getClass();
                Method setThreatTableMethod = activeMobClass.getMethod("setThreatTableEnabled", boolean.class);
                setThreatTableMethod.invoke(activeMob, enabled);
            }
        } catch (Exception e) {
        }
    }

    public double getMythicMobThreat(LivingEntity entity, org.bukkit.entity.Player player) {
        if (!enabled || entity == null || player == null) return 0;

        try {
            Object mobManager = mythicMukkit.getClass().getMethod("getMobManager").invoke(mythicMukkit);
            Object activeMob = getMythicMobInstanceMethod.invoke(mobManager, entity);

            if (activeMob != null) {
                Class<?> activeMobClass = activeMob.getClass();
                Method getThreatTableMethod = activeMobClass.getMethod("getThreatTable");
                Object threatTable = getThreatTableMethod.invoke(activeMob);

                if (threatTable != null) {
                    Method getThreatMethod = threatTable.getClass().getMethod("getThreat", org.bukkit.entity.Entity.class);
                    Object threat = getThreatMethod.invoke(threatTable, player);
                    if (threat instanceof Number) {
                        return ((Number) threat).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
        }

        return 0;
    }

    public void setMythicMobTarget(LivingEntity entity, org.bukkit.entity.Player target) {
        if (!enabled || entity == null || target == null) return;

        try {
            Object mobManager = mythicMukkit.getClass().getMethod("getMobManager").invoke(mythicMukkit);
            Object activeMob = getMythicMobInstanceMethod.invoke(mobManager, entity);

            if (activeMob != null) {
                Class<?> activeMobClass = activeMob.getClass();
                Method setTargetMethod = activeMobClass.getMethod("setTarget", org.bukkit.entity.LivingEntity.class);
                setTargetMethod.invoke(activeMob, target);
            }
        } catch (Exception e) {
        }
    }
}
