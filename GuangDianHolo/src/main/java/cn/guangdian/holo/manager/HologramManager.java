package cn.guangdian.holo.manager;

import cn.guangdian.rpgcore.event.events.HologramCreatedEvent;
import cn.guangdian.rpgcore.event.events.HologramDeletedEvent;
import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.model.Hologram;
import cn.guangdian.holo.storage.ConfigManager;
import cn.guangdian.rpgcore.RPGCore;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {

    private final GuangDianHolo plugin;
    private final ConfigManager configManager;
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final Set<String> loadedWorlds = ConcurrentHashMap.newKeySet();
    private long updateTaskId = -1;
    private final Object saveLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");

    // 玩家距离检测相关
    private boolean playerProximityEnabled = true;
    private double playerProximityDistance = 50.0;
    private long proximityCheckTaskId = -1;

    public HologramManager(GuangDianHolo plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        loadProximityConfig();
    }

    /**
     * 加载玩家距离检测配置
     */
    public void loadProximityConfig() {
        this.playerProximityEnabled = configManager.isPlayerProximityEnabled();
        this.playerProximityDistance = configManager.getPlayerProximityDistance();
    }

    /**
     * 加载所有全息图（只加载世界已存在的）
     */
    public void loadHolograms() {
        holograms.clear();
        loadedWorlds.clear();
        
        Map<String, Hologram> loaded = configManager.loadHolograms();
        for (Hologram holo : loaded.values()) {
            holograms.put(holo.getName(), holo);
            
            // 启用距离检测时不立即生成实体，由 checkPlayerProximity 按需生成
            if (!playerProximityEnabled) {
                spawnHologram(holo);
            }
            
            // 记录已加载的世界
            if (holo.getWorld() != null) {
                loadedWorlds.add(holo.getWorld().getName().toLowerCase());
            }
            
            if (plugin.getCacheProvider() != null) {
                plugin.getCacheProvider().put("holo:" + holo.getName(), holo);
            }
        }
        
        plugin.getLogger().info("已加载 " + holograms.size() + " 个全息图" + 
            (playerProximityEnabled ? "（距离检测模式，仅附近有玩家时显示）" : ""));
        
        // 检查是否有未加载的全息图（世界未加载）
        int pendingCount = configManager.getHologramDataCache().size() - holograms.size();
        if (pendingCount > 0) {
            plugin.getLogger().info("有 " + pendingCount + " 个全息图等待世界加载...");
        }
    }
    
    /**
     * 当世界加载时调用，加载该世界的全息图
     */
    public void loadHologramsForWorld(World world) {
        if (world == null) return;
        
        String worldName = world.getName();
        if (loadedWorlds.contains(worldName.toLowerCase())) {
            return; // 已经加载过了
        }
        
        Map<String, ConfigManager.HologramData> worldHolograms = 
            configManager.getHologramDataForWorld(worldName);
        
        int loadedCount = 0;
        for (ConfigManager.HologramData data : worldHolograms.values()) {
            // 检查是否已经加载
            if (holograms.containsKey(data.name)) {
                continue;
            }
            
            Hologram hologram = configManager.createHologramFromData(data);
            if (hologram != null) {
                holograms.put(data.name, hologram);
                
                // 启用距离检测时不立即生成实体，由 checkPlayerProximity 按需生成
                if (!playerProximityEnabled) {
                    spawnHologram(hologram);
                }
                
                loadedCount++;
                
                if (plugin.getCacheProvider() != null) {
                    plugin.getCacheProvider().put("holo:" + data.name, hologram);
                }
            }
        }
        
        if (loadedCount > 0) {
            loadedWorlds.add(worldName.toLowerCase());
            plugin.getLogger().info("世界 '" + worldName + "' 加载了 " + loadedCount + " 个全息图");
        }
    }
    
    /**
     * 当世界卸载时调用，清理该世界的全息图
     */
    public void unloadHologramsForWorld(World world) {
        if (world == null) return;
        
        String worldName = world.getName();
        List<String> toRemove = new ArrayList<>();
        
        for (Hologram holo : holograms.values()) {
            if (holo.getWorld() != null && holo.getWorld().equals(world)) {
                despawnHologram(holo);
                toRemove.add(holo.getName());
                
                if (plugin.getCacheProvider() != null) {
                    plugin.getCacheProvider().invalidate("holo:" + holo.getName());
                }
            }
        }
        
        for (String name : toRemove) {
            holograms.remove(name);
        }
        
        loadedWorlds.remove(worldName.toLowerCase());
        
        if (!toRemove.isEmpty()) {
            plugin.getLogger().info("世界 '" + worldName + "' 卸载了 " + toRemove.size() + " 个全息图");
        }
    }

    public void reloadHolograms() {
        for (Hologram holo : holograms.values()) {
            despawnHologram(holo);
        }
        loadedWorlds.clear();

        // 重新加载距离检测配置
        loadProximityConfig();

        loadHolograms();

        // 重启距离检测任务
        startProximityCheckTask();
    }

    public void saveHolograms() {
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    for (Hologram holo : holograms.values()) {
                        if (holo.isPersistent()) {
                            configManager.saveHologram(holo);
                        }
                    }
                }
            });
        } else {
            synchronized (saveLock) {
                for (Hologram holo : holograms.values()) {
                    if (holo.isPersistent()) {
                        configManager.saveHologram(holo);
                    }
                }
            }
        }
    }

    public void startUpdateTask() {
        int interval = configManager.getUpdateInterval();
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            updateTaskId = rpgCore.getScheduler().runSyncRepeating(this::updateHolograms, interval, interval);
        }

        // 启动玩家距离检测任务
        startProximityCheckTask();
    }

    public void stopUpdateTask() {
        if (updateTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(updateTaskId);
            }
            updateTaskId = -1;
        }

        // 停止玩家距离检测任务
        stopProximityCheckTask();
    }

    /**
     * 启动玩家距离检测任务
     */
    public void startProximityCheckTask() {
        stopProximityCheckTask();

        if (!playerProximityEnabled) return;

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            // 每 20 ticks (1秒) 检查一次玩家距离
            proximityCheckTaskId = rpgCore.getScheduler().runSyncRepeating(this::checkPlayerProximity, 20L, 20L);
            plugin.getLogger().info("玩家距离检测已启用，检测范围: " + playerProximityDistance + " 格");
        }
    }

    /**
     * 停止玩家距离检测任务
     */
    public void stopProximityCheckTask() {
        if (proximityCheckTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(proximityCheckTaskId);
            }
            proximityCheckTaskId = -1;
        }
    }

    /**
     * 检查玩家距离，动态显示/隐藏全息图
     * 使用 World.getNearbyPlayers 高效检测附近玩家
     */
    private void checkPlayerProximity() {
        if (!playerProximityEnabled) return;

        double distSq = playerProximityDistance * playerProximityDistance;

        for (Hologram holo : holograms.values()) {
            World world = holo.getWorld();
            if (world == null) continue;

            Location holoLoc = holo.getLocation();
            if (holoLoc == null) continue;

            // 使用 getNearbyPlayers 高效查找附近玩家
            boolean hasNearbyPlayer = !world.getNearbyPlayers(holoLoc, playerProximityDistance, playerProximityDistance, playerProximityDistance,
                p -> p.getLocation().distanceSquared(holoLoc) <= distSq
            ).isEmpty();

            // 根据玩家距离决定显示或隐藏
            boolean currentlySpawned = !holo.getEntityIds().isEmpty() || !holo.getIconEntityIds().isEmpty();

            if (hasNearbyPlayer && !currentlySpawned) {
                // 有玩家 nearby 且未生成，则生成
                spawnHologram(holo);
            } else if (!hasNearbyPlayer && currentlySpawned) {
                // 没有玩家 nearby 且已生成，则销毁
                despawnHologram(holo);
            }
        }
    }

    private void updateHolograms() {
        for (Hologram holo : holograms.values()) {
            if (!holo.isVisible()) continue;
            
            // 跳过未生成实体的全息图（距离检测模式下可能未 spawn）
            if (holo.getEntityIds().isEmpty() && holo.getIconEntityIds().isEmpty()) continue;
            
            // 如果有动态内容，更新显示
            if (holo.hasDynamicContent()) {
                updateHologramDisplay(holo);
            }
        }
    }

    public Hologram createHologram(String name, Location location) {
        if (holograms.containsKey(name)) {
            return null;
        }

        Hologram holo = new Hologram(name, location);
        holo.setLineHeight(configManager.getDefaultLineHeight());
        holo.setViewDistance(configManager.getVisibilityDistance());

        holograms.put(name, holo);

        // 启用距离检测时，仅当附近有玩家才生成实体
        if (!playerProximityEnabled || hasNearbyPlayer(location)) {
            spawnHologram(holo);
        }

        saveHologramAsync(holo);

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().put("holo:" + name, holo);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new HologramCreatedEvent(name, location, null));
        }

        return holo;
    }

    /**
     * 检查指定位置附近是否有玩家
     */
    private boolean hasNearbyPlayer(Location location) {
        if (location == null || location.getWorld() == null) return false;
        World world = location.getWorld();
        double distSq = playerProximityDistance * playerProximityDistance;
        return !world.getNearbyPlayers(location, playerProximityDistance, playerProximityDistance, playerProximityDistance,
            p -> p.getLocation().distanceSquared(location) <= distSq
        ).isEmpty();
    }

    public boolean deleteHologram(String name) {
        Hologram holo = holograms.remove(name);
        if (holo == null) {
            return false;
        }

        despawnHologram(holo);
        
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> configManager.deleteHologram(name));
        } else {
            configManager.deleteHologram(name);
        }

        if (plugin.getCacheProvider() != null) {
            plugin.getCacheProvider().invalidate("holo:" + name);
        }

        if (plugin.getEventBus() != null) {
            plugin.getEventBus().publish(new HologramDeletedEvent(name));
        }

        return true;
    }

    public void spawnHologram(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        despawnHologram(holo);

        List<Integer> entityIds = new ArrayList<>();
        List<Integer> iconEntityIds = new ArrayList<>();
        List<String> lines = holo.getLines();

        for (int i = 0; i < lines.size(); i++) {
            Hologram.LineType lineType = holo.getLineType(i);
            
            if (lineType == Hologram.LineType.ICON) {
                // 生成物品显示
                spawnIconDisplay(holo, i, world, iconEntityIds);
            } else {
                // 生成文本显示
                spawnTextDisplay(holo, i, world, entityIds);
            }
        }

        holo.setEntityIds(entityIds);
        holo.setIconEntityIds(iconEntityIds);
    }

    private void spawnTextDisplay(Hologram holo, int lineIndex, World world, List<Integer> entityIds) {
        Location lineLoc = holo.getLineLocation(lineIndex);
        if (lineLoc == null) return;

        TextDisplay display = (TextDisplay) world.spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
        
        display.setCustomNameVisible(false);
        display.setPersistent(false);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.setSeeThrough(true);
        display.setShadowed(false);
        display.setViewRange(holo.getViewDistance());
        
        String line = holo.getDisplayText(lineIndex);
        if (line != null) {
            Component text = parseTextWithPlaceholders(line, null);
            display.text(text);
        }

        entityIds.add(display.getEntityId());
    }

    private void spawnIconDisplay(Hologram holo, int lineIndex, World world, List<Integer> iconEntityIds) {
        Location iconLoc = holo.getIconLocation(lineIndex);
        if (iconLoc == null) return;

        ItemStack item = holo.getIconItem(lineIndex);
        if (item == null) return;

        ItemDisplay display = (ItemDisplay) world.spawnEntity(iconLoc, EntityType.ITEM_DISPLAY);
        
        display.setItemStack(item);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setPersistent(false);
        display.setViewRange(holo.getViewDistance());
        
        // 设置显示大小
        display.setTransformation(new org.bukkit.util.Transformation(
            new org.joml.Vector3f(-0.25f, -0.25f, -0.25f), // 缩放
            new org.joml.Quaternionf(), // 旋转左
            new org.joml.Vector3f(0.5f, 0.5f, 0.5f), // 缩放
            new org.joml.Quaternionf()  // 旋转右
        ));

        iconEntityIds.add(display.getEntityId());
    }

    public void despawnHologram(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        // 移除文本实体
        for (int entityId : holo.getEntityIds()) {
            Entity entity = world.getEntities().stream()
                .filter(e -> e.getEntityId() == entityId)
                .findFirst()
                .orElse(null);
            if (entity != null) {
                entity.remove();
            }
        }
        
        // 移除图标实体
        for (int entityId : holo.getIconEntityIds()) {
            Entity entity = world.getEntities().stream()
                .filter(e -> e.getEntityId() == entityId)
                .findFirst()
                .orElse(null);
            if (entity != null) {
                entity.remove();
            }
        }
        
        holo.setEntityIds(new ArrayList<>());
        holo.setIconEntityIds(new ArrayList<>());
    }

    public void updateHologramDisplay(Hologram holo) {
        World world = holo.getWorld();
        if (world == null) return;

        List<String> lines = holo.getLines();
        List<Integer> currentIds = holo.getEntityIds();
        List<Integer> currentIconIds = holo.getIconEntityIds();

        // 更新文本行
        int textLineIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (holo.getLineType(i) == Hologram.LineType.ICON) {
                continue; // 跳过ICON行
            }
            
            if (textLineIndex < currentIds.size()) {
                int entityId = currentIds.get(textLineIndex);
                Entity entity = world.getEntities().stream()
                    .filter(e -> e.getEntityId() == entityId)
                    .findFirst()
                    .orElse(null);
                
                if (entity instanceof TextDisplay display) {
                    String line = holo.getDisplayText(i);
                    if (line != null) {
                        Component text = parseTextWithPlaceholders(line, null);
                        display.text(text);
                    }
                }
                textLineIndex++;
            }
        }
    }

    public void respawnHologram(Hologram holo) {
        despawnHologram(holo);
        spawnHologram(holo);
    }

    private Component parseText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (text.contains("<") && text.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception e) {
                plugin.getLogger().fine("MiniMessage parse failed, using legacy: " + e.getMessage());
            }
        }

        if (text.contains("&") || text.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        return Component.text(text);
    }

    private Component parseTextWithPlaceholders(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 解析 PlaceholderAPI 占位符
        String processedText = text;
        if (text.contains("{") && text.contains("}")) {
            processedText = parseCustomPlaceholders(text);
            
            // 如果有 PlaceholderAPI，解析 PAPI 占位符
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && player != null) {
                processedText = PlaceholderAPI.setPlaceholders(player, processedText);
            }
        }

        // 解析 MiniMessage
        if (processedText.contains("<") && processedText.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(processedText);
            } catch (Exception e) {
                plugin.getLogger().fine("MiniMessage parse failed: " + e.getMessage());
            }
        }

        // 降级到传统颜色代码
        if (processedText.contains("&") || processedText.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(processedText);
        }

        return Component.text(processedText);
    }

    private String parseCustomPlaceholders(String text) {
        Matcher matcher = placeholderPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getCustomPlaceholderValue(placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private String getCustomPlaceholderValue(String placeholder) {
        // 处理 {online: world} 格式的占位符
        if (placeholder.startsWith("online:")) {
            String worldName = placeholder.substring(7).trim();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return String.valueOf(world.getPlayerCount());
            }
            return "0";
        }
        
        // 处理 {displayname} 占位符
        if (placeholder.equals("displayname")) {
            return "{displayname}"; // 保持原样，等玩家靠近时再解析
        }
        
        // 其他占位符保持原样
        return "{" + placeholder + "}";
    }

    public void saveHologramAsync(Hologram holo) {
        if (!holo.isPersistent()) return;
        
        if (plugin.getAsyncExecutor() != null) {
            plugin.getAsyncExecutor().execute(() -> {
                synchronized (saveLock) {
                    configManager.saveHologram(holo);
                }
            });
        } else {
            configManager.saveHologram(holo);
        }
    }

    public void removeAllHolograms() {
        for (Hologram holo : holograms.values()) {
            despawnHologram(holo);
        }
        holograms.clear();
        loadedWorlds.clear();
    }

    public Hologram getHologram(String name) {
        if (plugin.getCacheProvider() != null) {
            Hologram cached = plugin.getCacheProvider().get("holo:" + name, Hologram.class);
            if (cached != null) {
                return cached;
            }
        }
        return holograms.get(name);
    }

    public Collection<Hologram> getAllHolograms() {
        return holograms.values();
    }

    public int getHologramCount() {
        return holograms.size();
    }

    public List<String> getHologramNames() {
        return new ArrayList<>(holograms.keySet());
    }

    public List<Hologram> getHologramsNear(Location location, double radius) {
        List<Hologram> result = new ArrayList<>();
        for (Hologram holo : holograms.values()) {
            if (holo.getLocation() != null && 
                holo.getLocation().getWorld() != null &&
                holo.getLocation().getWorld().equals(location.getWorld()) &&
                holo.getLocation().distanceSquared(location) <= radius * radius) {
                result.add(holo);
            }
        }
        return result;
    }
    
    /**
     * 获取已加载的世界列表
     */
    public Set<String> getLoadedWorlds() {
        return new HashSet<>(loadedWorlds);
    }
    
    /**
     * 检查世界是否已加载全息图
     */
    public boolean isWorldLoaded(String worldName) {
        return loadedWorlds.contains(worldName.toLowerCase());
    }
}
