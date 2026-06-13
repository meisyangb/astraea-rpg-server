package cn.guangdian.mobhealth;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;

public class MythicMobsHook {

    private final GuangDianMobHealth plugin;
    private boolean mythicMobsEnabled = false;
    private Object mobManager;
    private Method getActiveMobMethod;

    public MythicMobsHook(GuangDianMobHealth plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        Plugin mmPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mmPlugin == null || !mmPlugin.isEnabled()) {
            plugin.getLogger().info("MythicMobs 未安装，将只支持原版怪物");
            return;
        }

        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkit = instMethod.invoke(null);
            
            Method getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
            mobManager = getMobManagerMethod.invoke(mythicBukkit);
            
            getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", java.util.UUID.class);
            
            mythicMobsEnabled = true;
            plugin.getLogger().info("已成功挂钩 MythicMobs");
        } catch (Exception e) {
            plugin.getLogger().warning("挂钩 MythicMobs 失败: " + e.getMessage());
            mythicMobsEnabled = false;
        }
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (!mythicMobsEnabled || entity == null) return false;
        
        try {
            Object result = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
            if (result instanceof Optional) {
                return ((Optional<?>) result).isPresent();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String getMythicMobName(LivingEntity entity) {
        if (!mythicMobsEnabled || entity == null) return null;
        
        try {
            Object result = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
            if (result instanceof Optional) {
                Optional<?> mobOpt = (Optional<?>) result;
                if (mobOpt.isPresent()) {
                    Object mob = mobOpt.get();
                    
                    try {
                        Method getDisplayNameMethod = mob.getClass().getMethod("getDisplayName");
                        Object displayName = getDisplayNameMethod.invoke(mob);
                        if (displayName != null && !displayName.toString().isEmpty()) {
                            return displayName.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                    
                    try {
                        Method getNameMethod = mob.getClass().getMethod("getName");
                        Object name = getNameMethod.invoke(mob);
                        if (name != null && !name.toString().isEmpty()) {
                            return name.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                    
                    try {
                        Method getMobTypeMethod = mob.getClass().getMethod("getMobType");
                        Object mobType = getMobTypeMethod.invoke(mob);
                        if (mobType != null && !mobType.toString().isEmpty()) {
                            return mobType.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.debug("获取MythicMobs名称失败: " + e.getMessage());
        }
        
        return null;
    }

    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }
}
