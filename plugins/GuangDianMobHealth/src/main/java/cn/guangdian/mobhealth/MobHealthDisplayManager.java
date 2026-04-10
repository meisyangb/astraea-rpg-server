package cn.guangdian.mobhealth;

import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobHealthDisplayManager {

    private final GuangDianMobHealth plugin;
    private final MythicMobsHook mythicMobsHook;
    private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();
    private final Map<UUID, HealthData> healthCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalNames = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> originalNameVisible = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private long timeoutCheckTaskId = -1;

    private boolean enabled = true;
    private float displayHeight = 2.5f;
    private boolean hideVanillaName = true;
    private boolean hideMythicMobsName = true;
    private int displayTimeout = 100;
    private String format = "{name}§r {health_bar}§r §a{health}";
    private int barLength = 10;
    private String barSymbol = "█";
    private String emptyColor = "§7";
    private String bracketLeft = "§8[";
    private String bracketRight = "§8]";
    private List<BarColorRange> barColors = new ArrayList<>();
    private double minHealth = 0;
    private Set<EntityType> excludedTypes = new HashSet<>();
    private boolean mythicMobsOnly = false;
    private boolean showVanilla = true;

    public MobHealthDisplayManager(GuangDianMobHealth plugin, MythicMobsHook mythicMobsHook) {
        this.plugin = plugin;
        this.mythicMobsHook = mythicMobsHook;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("enabled", true);
        displayHeight = (float) plugin.getConfig().getDouble("display.height", 2.5);
        hideVanillaName = plugin.getConfig().getBoolean("display.hide-vanilla-name", true);
        hideMythicMobsName = plugin.getConfig().getBoolean("display.hide-mythicmobs-name", true);
        displayTimeout = plugin.getConfig().getInt("display.timeout", 100);
        format = plugin.getConfig().getString("display.format", "{name}§r {health_bar}§r §a{health}");
        barLength = plugin.getConfig().getInt("display.bar.length", 10);
        barSymbol = plugin.getConfig().getString("display.bar.symbol", "█");
        emptyColor = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("display.bar.empty-color", "&7"));
        bracketLeft = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("display.bar.bracket-left", "§8["));
        bracketRight = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("display.bar.bracket-right", "§8]"));
        
        barColors.clear();
        List<String> colorList = plugin.getConfig().getStringList("display.bar.colors");
        for (String colorEntry : colorList) {
            String[] parts = colorEntry.split(":");
            if (parts.length == 2) {
                String[] range = parts[0].split("-");
                if (range.length == 2) {
                    try {
                        int min = Integer.parseInt(range[0].trim());
                        int max = Integer.parseInt(range[1].trim());
                        String color = ChatColor.translateAlternateColorCodes('&', parts[1]);
                        barColors.add(new BarColorRange(min, max, color));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        minHealth = plugin.getConfig().getDouble("filter.min-health", 0);
        excludedTypes.clear();
        for (String type : plugin.getConfig().getStringList("filter.excluded-types")) {
            try {
                excludedTypes.add(EntityType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        mythicMobsOnly = plugin.getConfig().getBoolean("filter.mythicmobs-only", false);
        showVanilla = plugin.getConfig().getBoolean("filter.show-vanilla", true);
    }

    public void startUpdateTask() {
        if (!enabled) return;
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;
        
        timeoutCheckTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            long now = System.currentTimeMillis();
            List<UUID> toRemove = new ArrayList<>();
            
            for (Map.Entry<UUID, Long> entry : lastAttackTime.entrySet()) {
                UUID entityId = entry.getKey();
                long lastTime = entry.getValue();
                
                if (now - lastTime > displayTimeout * 50L) {
                    toRemove.add(entityId);
                }
            }
            
            for (UUID id : toRemove) {
                removeDisplay(id);
                plugin.debug("超时移除显示: " + id);
            }
        }, 20L, 20L);
    }

    public void stopUpdateTask() {
        if (timeoutCheckTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(timeoutCheckTaskId);
            }
            timeoutCheckTaskId = -1;
        }
    }

    public void updateLastAttackTime(UUID entityId) {
        lastAttackTime.put(entityId, System.currentTimeMillis());
    }

    public void createDisplay(LivingEntity entity) {
        if (!enabled) return;
        if (!shouldDisplay(entity)) return;
        
        UUID entityId = entity.getUniqueId();
        if (displays.containsKey(entityId)) {
            updateDisplay(entity, displays.get(entityId));
            lastAttackTime.put(entityId, System.currentTimeMillis());
            return;
        }
        
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (maxHealth < minHealth) return;
        
        lastAttackTime.put(entityId, System.currentTimeMillis());
        
        boolean isMythicMob = mythicMobsHook.isMythicMob(entity);
        
        if ((hideVanillaName && !isMythicMob) || (hideMythicMobsName && isMythicMob)) {
            String originalName = entity.getCustomName();
            boolean wasVisible = entity.isCustomNameVisible();
            
            originalNames.put(entityId, originalName != null ? originalName : "");
            originalNameVisible.put(entityId, wasVisible);
            
            entity.setCustomNameVisible(false);
            
            plugin.debug("隐藏原版名字: " + entity.getName() + " (MythicMob: " + isMythicMob + ")");
        }
        
        Location loc = entity.getLocation().clone().add(0, displayHeight, 0);
        World world = entity.getWorld();
        
        TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
            td.setVisibleByDefault(true);
            td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setInvulnerable(true);
            td.setPersistent(false);
            td.addScoreboardTag("gdmobhealth_display");
        });
        
        updateDisplay(entity, display);
        
        entity.addPassenger(display);
        displays.put(entityId, display);
        
        plugin.debug("创建显示: " + entity.getName() + " (" + entityId + ")");
    }

    public void updateDisplay(LivingEntity entity, TextDisplay display) {
        if (entity == null || display == null || !display.isValid()) return;
        
        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        
        UUID entityId = entity.getUniqueId();
        HealthData cached = healthCache.get(entityId);
        
        if (cached != null && 
            Math.abs(cached.health - health) < 1 && 
            Math.abs(cached.maxHealth - maxHealth) < 1) {
            return;
        }
        
        String name = getEntityName(entity);
        String healthBar = buildHealthBar(health, maxHealth);
        int percent = (int) ((health / maxHealth) * 100);
        
        String text = format
            .replace("{name}", name)
            .replace("{health_bar}", healthBar)
            .replace("{health}", String.valueOf((int) health))
            .replace("{max_health}", String.valueOf((int) maxHealth))
            .replace("{percent}", String.valueOf(percent));
        
        display.text(Component.text(text));
        healthCache.put(entityId, new HealthData(health, maxHealth));
    }

    public void updateDisplay(LivingEntity entity) {
        if (entity == null) return;
        TextDisplay display = displays.get(entity.getUniqueId());
        if (display != null && display.isValid()) {
            updateDisplay(entity, display);
        }
    }

    public void removeDisplay(UUID entityId) {
        TextDisplay display = displays.remove(entityId);
        healthCache.remove(entityId);
        lastAttackTime.remove(entityId);
        
        String originalName = originalNames.remove(entityId);
        Boolean wasVisible = originalNameVisible.remove(entityId);
        
        if (display != null && display.isValid()) {
            Entity vehicle = display.getVehicle();
            if (vehicle instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) vehicle;
                if (originalName != null && !originalName.isEmpty()) {
                    entity.setCustomName(originalName);
                }
                if (wasVisible != null) {
                    entity.setCustomNameVisible(wasVisible);
                }
            }
            display.remove();
            plugin.debug("移除显示: " + entityId);
        }
    }

    public void removeDisplay(LivingEntity entity) {
        removeDisplay(entity.getUniqueId());
    }

    public void clear() {
        for (Map.Entry<UUID, TextDisplay> entry : displays.entrySet()) {
            TextDisplay display = entry.getValue();
            if (display != null && display.isValid()) {
                Entity vehicle = display.getVehicle();
                if (vehicle instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) vehicle;
                    String originalName = originalNames.get(entry.getKey());
                    Boolean wasVisible = originalNameVisible.get(entry.getKey());
                    if (originalName != null && !originalName.isEmpty()) {
                        entity.setCustomName(originalName);
                    }
                    if (wasVisible != null) {
                        entity.setCustomNameVisible(wasVisible);
                    }
                }
                display.remove();
            }
        }
        displays.clear();
        healthCache.clear();
        originalNames.clear();
        originalNameVisible.clear();
        lastAttackTime.clear();
    }

    private boolean shouldDisplay(LivingEntity entity) {
        if (excludedTypes.contains(entity.getType())) return false;
        
        boolean isMythicMob = mythicMobsHook.isMythicMob(entity);
        
        if (mythicMobsOnly && !isMythicMob) return false;
        if (!showVanilla && !isMythicMob) return false;
        
        return true;
    }

    private String getEntityName(LivingEntity entity) {
        String mythicName = mythicMobsHook.getMythicMobName(entity);
        if (mythicName != null && !mythicName.isEmpty()) {
            return mythicName;
        }
        
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        
        return entity.getName();
    }

    private String buildHealthBar(double health, double maxHealth) {
        if (maxHealth <= 0) return bracketLeft + bracketRight;
        
        int percent = (int) ((health / maxHealth) * 100);
        int filled = (int) ((health / maxHealth) * barLength);
        filled = Math.max(0, Math.min(barLength, filled));
        
        String fillColor = getBarColor(percent);
        
        StringBuilder bar = new StringBuilder();
        bar.append(bracketLeft);
        
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(fillColor).append(barSymbol);
            } else {
                bar.append(emptyColor).append(barSymbol);
            }
        }
        
        bar.append(bracketRight);
        return bar.toString();
    }

    private String getBarColor(int percent) {
        for (BarColorRange range : barColors) {
            if (percent >= range.min && percent <= range.max) {
                return range.color;
            }
        }
        return "§a";
    }

    public boolean hasDisplay(UUID entityId) {
        return displays.containsKey(entityId);
    }

    public int getDisplayCount() {
        return displays.size();
    }
    
    /**
     * 显示实体血量（公开方法，供Adapter调用）
     */
    public void showHealth(LivingEntity entity) {
        createDisplay(entity);
    }
    
    /**
     * 隐藏实体血量（公开方法，供Adapter调用）
     */
    public void hideHealth(LivingEntity entity) {
        if (entity == null) return;
        removeDisplay(entity.getUniqueId());
    }

    private static class HealthData {
        final double health;
        final double maxHealth;
        
        HealthData(double health, double maxHealth) {
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

    private static class BarColorRange {
        final int min;
        final int max;
        final String color;
        
        BarColorRange(int min, int max, String color) {
            this.min = min;
            this.max = max;
            this.color = color;
        }
    }
}
