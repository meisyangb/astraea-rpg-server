package cn.guangdian.mobhealth;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LibsDisguisesHook {

    private final GuangDianMobHealth plugin;
    private boolean libsDisguisesEnabled = false;
    private Object disguisesManager;
    private Method getDisguiseMethod;
    private Method setNameVisibleMethod;
    private Method setNameMethod;

    // 记录我们隐藏过名字的实体，用于恢复
    private final Map<UUID, Boolean> originalNameVisible = new ConcurrentHashMap<>();

    public LibsDisguisesHook(GuangDianMobHealth plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        Plugin libsPlugin = Bukkit.getPluginManager().getPlugin("LibsDisguises");
        if (libsPlugin == null || !libsPlugin.isEnabled()) {
            plugin.getLogger().info("LibsDisguises 未安装，跳过兼容挂钩");
            return;
        }

        try {
            // 获取 LibsDisguises 的 DisguiseAPI
            Class<?> disguiseAPIClass = Class.forName("me.libraryaddict.disguise.DisguiseAPI");

            // 获取 getDisguise 方法
            getDisguiseMethod = disguiseAPIClass.getMethod("getDisguise", org.bukkit.entity.Entity.class);

            libsDisguisesEnabled = true;
            plugin.getLogger().info("已成功挂钩 LibsDisguises");
        } catch (Exception e) {
            plugin.getLogger().warning("挂钩 LibsDisguises 失败: " + e.getMessage());
            libsDisguisesEnabled = false;
        }
    }

    /**
     * 检查实体是否有 LibsDisguises 伪装
     */
    public boolean hasDisguise(LivingEntity entity) {
        if (!libsDisguisesEnabled || entity == null) return false;

        try {
            Object disguise = getDisguiseMethod.invoke(null, entity);
            return disguise != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 隐藏 LibsDisguises 伪装的名字显示
     */
    public void hideDisguiseName(LivingEntity entity) {
        if (!libsDisguisesEnabled || entity == null) return;

        try {
            Object disguise = getDisguiseMethod.invoke(null, entity);
            if (disguise == null) return;

            // 获取当前名字显示状态
            Class<?> disguiseClass = disguise.getClass();

            // 尝试调用 isNameVisible 方法记录原始状态
            try {
                Method isNameVisibleMethod = disguiseClass.getMethod("isNameVisible");
                Boolean wasVisible = (Boolean) isNameVisibleMethod.invoke(disguise);
                originalNameVisible.put(entity.getUniqueId(), wasVisible);
            } catch (NoSuchMethodException ignored) {}

            // 调用 setNameVisible(false) 隐藏名字
            try {
                Method setNameVisible = disguiseClass.getMethod("setNameVisible", boolean.class);
                setNameVisible.invoke(disguise, false);
                plugin.debug("隐藏 LibsDisguises 伪装名字: " + entity.getName());
            } catch (NoSuchMethodException e) {
                plugin.debug("LibsDisguises 不支持 setNameVisible 方法");
            }

            // 尝试调用 setDynamicName(false) 禁用动态名字
            try {
                Method setDynamicName = disguiseClass.getMethod("setDynamicName", boolean.class);
                setDynamicName.invoke(disguise, false);
            } catch (NoSuchMethodException ignored) {}

        } catch (Exception e) {
            plugin.debug("隐藏 LibsDisguises 名字失败: " + e.getMessage());
        }
    }

    /**
     * 恢复 LibsDisguises 伪装的名字显示
     */
    public void restoreDisguiseName(LivingEntity entity) {
        if (!libsDisguisesEnabled || entity == null) return;

        UUID entityId = entity.getUniqueId();
        Boolean wasVisible = originalNameVisible.remove(entityId);
        if (wasVisible == null) return;

        try {
            Object disguise = getDisguiseMethod.invoke(null, entity);
            if (disguise == null) return;

            Class<?> disguiseClass = disguise.getClass();
            Method setNameVisible = disguiseClass.getMethod("setNameVisible", boolean.class);
            setNameVisible.invoke(disguise, wasVisible);
            plugin.debug("恢复 LibsDisguises 伪装名字: " + entity.getName());

        } catch (Exception e) {
            plugin.debug("恢复 LibsDisguises 名字失败: " + e.getMessage());
        }
    }

    public boolean isLibsDisguisesEnabled() {
        return libsDisguisesEnabled;
    }
}
