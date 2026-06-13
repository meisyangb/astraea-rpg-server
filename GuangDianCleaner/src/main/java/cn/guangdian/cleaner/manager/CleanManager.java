package cn.guangdian.cleaner.manager;

import cn.guangdian.cleaner.GuangDianCleaner;
import cn.guangdian.cleaner.config.ConfigManager;
import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CleanManager {

    private final GuangDianCleaner plugin;
    private final ConfigManager configManager;

    private long autoCleanTaskId = -1;
    private long warningTaskId = -1;

    // 玩家掉落物追踪（用于保护刚掉落的物品）
    private final ConcurrentHashMap<UUID, Long> playerDropTimeMap = new ConcurrentHashMap<>();

    // 本次清理统计
    private final AtomicLong currentCleanItems = new AtomicLong(0);
    private final AtomicLong currentCleanEntities = new AtomicLong(0);

    // 是否正在清理
    private volatile boolean isCleaning = false;

    // ItemLabel 的 PDC Key，用于识别标签实体
    private final NamespacedKey itemLabelKey;

    public CleanManager(GuangDianCleaner plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemLabelKey = new NamespacedKey("guangdianitemlabel", "itemlabel_id");
    }

    public void startAutoCleanTask() {
        if (!configManager.isAutoCleanEnabled()) {
            return;
        }

        stopAutoCleanTask();

        int interval = configManager.getAutoCleanInterval();
        int warningTime = configManager.getWarningTime();

        long intervalTicks = interval * 20L;
        long warningTicks = warningTime * 20L;

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            plugin.getLogger().warning("RPGCore 未加载，无法启动自动清理任务");
            return;
        }

        autoCleanTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            performClean(true);
        }, intervalTicks, intervalTicks);

        if (warningTime > 0 && warningTime < interval) {
            long warningDelay = intervalTicks - warningTicks;
            warningTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
                broadcastWarning(warningTime);
            }, warningDelay, intervalTicks);
        }

        plugin.getLogger().info("自动清理任务已启动，间隔: " + interval + "秒");
    }

    public void stopAutoCleanTask() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            if (autoCleanTaskId != -1) {
                rpgCore.getScheduler().cancelTask(autoCleanTaskId);
                autoCleanTaskId = -1;
            }
            if (warningTaskId != -1) {
                rpgCore.getScheduler().cancelTask(warningTaskId);
                warningTaskId = -1;
            }
        }
    }

    /**
     * 重启自动清理任务
     */
    public void restartAutoCleanTask() {
        stopAutoCleanTask();
        startAutoCleanTask();
    }

    /**
     * 广播预警消息
     */
    private void broadcastWarning(int seconds) {
        Component message = configManager.getFormattedMessage(
            configManager.getMessagePrefix() + configManager.getMessageWarning(),
            "%time%", String.valueOf(seconds)
        );
        Bukkit.broadcast(message);
    }

    /**
     * 执行清理操作
     *
     * @param broadcast 是否广播清理结果
     * @return 清理的物品数量
     */
    public int performClean(boolean broadcast) {
        if (isCleaning) {
            return 0; // 防止重复清理
        }

        isCleaning = true;
        currentCleanItems.set(0);
        currentCleanEntities.set(0);

        // 收集需要清理的世界
        List<World> worldsToClean = getWorldsToClean();

        // Paper 1.21.4 要求实体操作必须在主线程执行
        // 整个清理流程在主线程执行以保证线程安全
        // 清理操作不是高频操作，性能影响可接受
        for (World world : worldsToClean) {
            cleanWorldItems(world);
        }

        long items = currentCleanItems.get();

        // 更新总统计
        configManager.addCleanStats(items, currentCleanEntities.get());

        if (broadcast && items > 0) {
            Component message = configManager.getFormattedMessage(
                configManager.getMessagePrefix() + configManager.getMessageCleaned(),
                "%count%", String.valueOf(items)
            );
            Bukkit.broadcast(message);
        }

        isCleaning = false;
        return (int) items;
    }

    /**
     * 清理指定世界的物品及其关联的 ItemLabel 标签实体
     * 注意：此方法必须在主线程调用，因为涉及实体操作
     */
    private void cleanWorldItems(World world) {
        // 获取所有掉落物实体（必须在主线程执行）
        Collection<Item> items = world.getEntitiesByClass(Item.class);

        // 收集被清理物品的 UUID，用于后续匹配标签实体
        Set<UUID> removedItemIds = new HashSet<>();

        for (Item item : items) {
            if (shouldCleanItem(item)) {
                if (item.isValid() && !item.isDead()) {
                    removedItemIds.add(item.getUniqueId());
                    item.remove();
                    currentCleanItems.incrementAndGet();
                    currentCleanEntities.incrementAndGet();
                }
            }
        }

        // 清理关联的 ItemLabel TextDisplay 标签实体
        if (!removedItemIds.isEmpty()) {
            cleanItemLabelEntities(world, removedItemIds);
        }
    }

    /**
     * 清理与已移除物品关联的 ItemLabel TextDisplay 标签
     * 通过 PDC 标记识别标签实体，匹配已清理物品的 UUID
     */
    private void cleanItemLabelEntities(World world, Set<UUID> removedItemIds) {
        for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
            String itemUuidStr = display.getPersistentDataContainer()
                .get(itemLabelKey, PersistentDataType.STRING);
            if (itemUuidStr == null) continue;

            try {
                UUID itemUuid = UUID.fromString(itemUuidStr);
                if (removedItemIds.contains(itemUuid)) {
                    display.remove();
                    currentCleanEntities.incrementAndGet();
                }
            } catch (IllegalArgumentException ignored) {
                // 无效的 UUID 格式，跳过
            }
        }
    }

    /**
     * 判断是否应该清理该物品
     */
    public boolean shouldCleanItem(Item item) {
        ItemStack itemStack = item.getItemStack();

        // 保护有名称的物品
        if (configManager.isProtectNamedItems() && itemStack.hasItemMeta()) {
            if (itemStack.getItemMeta().hasDisplayName()) {
                return false;
            }
        }

        // 保护玩家刚掉落的物品
        if (configManager.isProtectPlayerDrops()) {
            UUID thrower = item.getThrower();
            if (thrower != null) {
                Long dropTime = playerDropTimeMap.get(thrower);
                if (dropTime != null) {
                    long elapsed = System.currentTimeMillis() - dropTime;
                    if (elapsed < configManager.getProtectPlayerDropsTime() * 1000L) {
                        return false;
                    }
                }
            }
        }

        // 根据过滤模式判断
        return checkItemFilter(itemStack);
    }

    /**
     * 检查世界是否启用清理
     */
    public boolean isWorldEnabled(String worldName) {
        ConfigManager.WorldMode mode = configManager.getWorldMode();
        Set<String> worldList = configManager.getWorldList();

        switch (mode) {
            case ALL:
                return true;
            case WHITELIST:
                return worldList.contains(worldName);
            case BLACKLIST:
                return !worldList.contains(worldName);
            default:
                return true;
        }
    }

    public boolean isAutoCleanEnabled() {
        return autoCleanTaskId != -1;
    }

    /**
     * 检查物品过滤规则
     */
    private boolean checkItemFilter(ItemStack itemStack) {
        ConfigManager.FilterMode mode = configManager.getFilterMode();
        Set<org.bukkit.Material> itemFilter = configManager.getItemFilter();
        Set<String> nameFilter = configManager.getItemNameFilter();

        switch (mode) {
            case NONE:
                return true;

            case BLACKLIST:
                // 黑名单：只清理列表中的物品
                if (itemFilter.contains(itemStack.getType())) {
                    return true;
                }
                // 检查物品名称
                if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    String displayName = itemStack.getItemMeta().getDisplayName();
                    for (String name : nameFilter) {
                        if (displayName.contains(name)) {
                            return true;
                        }
                    }
                }
                return false;

            case WHITELIST:
                // 白名单：只清理不在列表中的物品
                if (itemFilter.contains(itemStack.getType())) {
                    return false;
                }
                // 检查物品名称
                if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    String displayName = itemStack.getItemMeta().getDisplayName();
                    for (String name : nameFilter) {
                        if (displayName.contains(name)) {
                            return false;
                        }
                    }
                }
                return true;

            default:
                return true;
        }
    }

    /**
     * 获取需要清理的世界列表
     */
    private List<World> getWorldsToClean() {
        ConfigManager.WorldMode mode = configManager.getWorldMode();
        Set<String> worldList = configManager.getWorldList();

        List<World> result = new ArrayList<>();

        switch (mode) {
            case ALL:
                result.addAll(Bukkit.getWorlds());
                break;

            case WHITELIST:
                for (String worldName : worldList) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        result.add(world);
                    }
                }
                break;

            case BLACKLIST:
                for (World world : Bukkit.getWorlds()) {
                    if (!worldList.contains(world.getName())) {
                        result.add(world);
                    }
                }
                break;
        }

        return result;
    }

    /**
     * 记录玩家掉落物品时间
     */
    public void recordPlayerDrop(UUID playerUuid) {
        playerDropTimeMap.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * 清理过期的掉落记录
     */
    public void cleanupOldRecords() {
        long expireTime = System.currentTimeMillis() - configManager.getProtectPlayerDropsTime() * 1000L * 2;
        playerDropTimeMap.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }

    /**
     * 获取本次清理统计
     */
    public long getCurrentCleanItems() {
        return currentCleanItems.get();
    }

    /**
     * 是否正在清理
     */
    public boolean isCleaning() {
        return isCleaning;
    }
}