package cn.guangdian.aggro.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class MythicMobsHook {

    private static final Logger logger = Logger.getLogger("GuangDianAggro");

    private boolean enabled = false;
    private Object mythicBukkit;
    private Method getMobManagerMethod;
    private Method getActiveMobMethod;
    private Method getMobTypeMethod;
    private Method setThreatTableMethod;
    private Method getThreatTableMethod;
    private Method getThreatMethod;
    private Method setTargetMethod;
    private Plugin mythicMobsPlugin;

    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled()) {
            Bukkit.getLogger().info("[GuangDianAggro] MythicMobs 未安装，跳过集成");
            return;
        }

        this.mythicMobsPlugin = plugin;

        try {
            // 获取 MythicBukkit 实例
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            mythicBukkit = instMethod.invoke(null);

            // 获取 MobManager 方法
            getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
            Object mobManager = getMobManagerMethod.invoke(mythicBukkit);

            // 获取 getActiveMob(UUID) 方法
            getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", UUID.class);

            // 获取 ActiveMob 类和方法
            Class<?> activeMobClass = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
            getMobTypeMethod = findMethod(activeMobClass, "getMobType");
            setThreatTableMethod = findMethod(activeMobClass, "setThreatTableEnabled", boolean.class);
            getThreatTableMethod = findMethod(activeMobClass, "getThreatTable");
            setTargetMethod = findMethod(activeMobClass, "setTarget", org.bukkit.entity.LivingEntity.class);

            enabled = true;
            Bukkit.getLogger().info("[GuangDianAggro] MythicMobs 集成已启用");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[GuangDianAggro] MythicMobs 集成失败: " + e.getMessage());
            enabled = false;
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (!enabled || entity == null || getActiveMobMethod == null) return false;

        try {
            Object mobManager = getMobManagerMethod.invoke(mythicBukkit);
            Object result = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
            if (result instanceof Optional) {
                return ((Optional<?>) result).isPresent();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private Object getActiveMob(LivingEntity entity) {
        if (!enabled || entity == null || getActiveMobMethod == null) return null;

        try {
            Object mobManager = getMobManagerMethod.invoke(mythicBukkit);
            Object result = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
            if (result instanceof Optional) {
                return ((Optional<?>) result).orElse(null);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public String getMythicMobType(LivingEntity entity) {
        if (!enabled || entity == null || getMobTypeMethod == null) return null;

        try {
            Object activeMob = getActiveMob(entity);
            if (activeMob != null) {
                Object mobType = getMobTypeMethod.invoke(activeMob);
                return mobType != null ? mobType.toString() : null;
            }
        } catch (Exception e) {
        }

        return null;
    }

    public void setThreatTable(LivingEntity entity, boolean enabled) {
        if (!this.enabled || entity == null || setThreatTableMethod == null) return;

        try {
            Object activeMob = getActiveMob(entity);
            if (activeMob != null) {
                setThreatTableMethod.invoke(activeMob, enabled);
            }
        } catch (Exception e) {
        }
    }

    public double getMythicMobThreat(LivingEntity entity, org.bukkit.entity.Player player) {
        if (!enabled || entity == null || player == null || getThreatTableMethod == null) return 0;

        try {
            Object activeMob = getActiveMob(entity);
            if (activeMob != null) {
                Object threatTable = getThreatTableMethod.invoke(activeMob);

                if (threatTable != null && getThreatMethod == null) {
                    // 延迟获取 getThreat 方法
                    getThreatMethod = findMethod(threatTable.getClass(), "getThreat", org.bukkit.entity.Entity.class);
                }

                if (threatTable != null && getThreatMethod != null) {
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
        if (!enabled || entity == null || target == null || setTargetMethod == null) return;

        try {
            Object activeMob = getActiveMob(entity);
            if (activeMob != null) {
                setTargetMethod.invoke(activeMob, target);
            }
        } catch (Exception e) {
        }
    }
}
