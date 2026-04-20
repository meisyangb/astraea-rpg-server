package cn.guangdian.quest.integration;

import cn.guangdian.quest.GuangDianQuest;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

/**
 * 插件集成管理器
 * 负责集成 GuangDianMobs 和 RPGItems
 */
public class PluginIntegration {

    private final GuangDianQuest plugin;

    // GuangDianMobs
    private Object guangDianMobs;
    private Method getMobIdFromEntityMethod;
    private boolean guangDianMobsAvailable = false;

    // RPGItems
    private Object rpgItemsAPI;
    private Method getItemIdMethod;
    private Method isRPGItemMethod;
    private boolean rpgItemsAvailable = false;

    public PluginIntegration(GuangDianQuest plugin) {
        this.plugin = plugin;
        initGuangDianMobs();
        initRPGItems();
    }

    /**
     * 初始化 GuangDianMobs 集成
     */
    private void initGuangDianMobs() {
        if (!Bukkit.getPluginManager().isPluginEnabled("GuangDianMobs")) {
            plugin.getLogger().info("GuangDianMobs 未加载，跳过集成");
            return;
        }

        try {
            guangDianMobs = Bukkit.getPluginManager().getPlugin("GuangDianMobs");
            if (guangDianMobs == null) {
                plugin.getLogger().warning("GuangDianMobs 插件实例获取失败");
                return;
            }

            // 获取 MobManager
            Method getMobManagerMethod = guangDianMobs.getClass().getMethod("getMobManager");
            Object mobManager = getMobManagerMethod.invoke(guangDianMobs);

            if (mobManager != null) {
                getMobIdFromEntityMethod = mobManager.getClass().getMethod("getMobIdFromEntity", LivingEntity.class);
                guangDianMobsAvailable = true;
                plugin.getLogger().info("GuangDianMobs 集成已启用 - 支持自定义怪物击杀任务");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "GuangDianMobs 集成初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化 RPGItems 集成
     */
    private void initRPGItems() {
        if (!Bukkit.getPluginManager().isPluginEnabled("RPGItems")) {
            plugin.getLogger().info("RPGItems 未加载，跳过集成");
            return;
        }

        try {
            // 获取 RPGItems 插件实例
            Object rpgItemsPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
            if (rpgItemsPlugin == null) {
                plugin.getLogger().warning("RPGItems 插件实例获取失败");
                return;
            }

            // 获取 API
            Method getApiMethod = rpgItemsPlugin.getClass().getMethod("getAPI");
            rpgItemsAPI = getApiMethod.invoke(rpgItemsPlugin);

            if (rpgItemsAPI != null) {
                getItemIdMethod = rpgItemsAPI.getClass().getMethod("getItemId", ItemStack.class);
                isRPGItemMethod = rpgItemsAPI.getClass().getMethod("isRPGItem", ItemStack.class);
                rpgItemsAvailable = true;
                plugin.getLogger().info("RPGItems 集成已启用 - 支持自定义物品收集任务");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "RPGItems 集成初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 GuangDianMobs 怪物ID
     */
    public Optional<String> getGuangDianMobId(LivingEntity entity) {
        if (!guangDianMobsAvailable || getMobIdFromEntityMethod == null) {
            return Optional.empty();
        }

        try {
            Object result = getMobIdFromEntityMethod.invoke(
                guangDianMobs.getClass().getMethod("getMobManager").invoke(guangDianMobs),
                entity
            );
            return Optional.ofNullable((String) result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 检查是否是 GuangDianMobs 怪物
     */
    public boolean isGuangDianMob(LivingEntity entity) {
        return getGuangDianMobId(entity).isPresent();
    }

    /**
     * 获取 RPGItems 物品ID
     */
    public Optional<String> getRPGItemId(ItemStack item) {
        if (!rpgItemsAvailable || getItemIdMethod == null || item == null) {
            return Optional.empty();
        }

        try {
            Object result = getItemIdMethod.invoke(rpgItemsAPI, item);
            return (Optional<String>) result;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 检查是否是 RPGItems 物品
     */
    public boolean isRPGItem(ItemStack item) {
        if (!rpgItemsAvailable || isRPGItemMethod == null || item == null) {
            return false;
        }

        try {
            Object result = isRPGItemMethod.invoke(rpgItemsAPI, item);
            return (boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否支持 GuangDianMobs
     */
    public boolean isGuangDianMobsAvailable() {
        return guangDianMobsAvailable;
    }

    /**
     * 是否支持 RPGItems
     */
    public boolean isRPGItemsAvailable() {
        return rpgItemsAvailable;
    }
}
