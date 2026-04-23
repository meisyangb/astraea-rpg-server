package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.GuildService;
import cn.guangdian.rpgcore.service.api.MarriageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一头顶显示管理器 - 全部使用 TextDisplay
 * 
 * 显示层级（从下到上）：
 * - 第1行：血量
 * - 第2行：称号 + 玩家名 + 婚姻
 * - 第3行：工会
 */
public class NameDisplayManager {
    
    private final JavaPlugin plugin;
    private final Map<UUID, TextDisplay> healthDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> titleDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> guildDisplays = new ConcurrentHashMap<>();
    
    private final Map<UUID, String> lastHealthText = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTitleText = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastGuildText = new ConcurrentHashMap<>();
    
    private final Map<UUID, Boolean> showTitle = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showGuild = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showMarriage = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showHealth = new ConcurrentHashMap<>();
    
    private SyncScheduler scheduler;
    private ExternalServiceIntegration externalServices;
    private MiniMessageService miniMessage;
    
    private long updateTaskId = -1;
    
    // 配置
    private boolean enabled = true;
    private boolean healthEnabled = true;
    private boolean titleEnabled = true;
    private boolean guildEnabled = true;
    
    private boolean defaultShowTitle = true;
    private boolean defaultShowGuild = true;
    private boolean defaultShowMarriage = true;
    private boolean defaultShowHealth = true;
    
    private String healthFormat = "<red>❤ %d";
    private String marriageFormat = "<light_purple> ♥%s";
    private String guildFormat = "<dark_aqua>[%s]";
    
    private float healthHeight = 0.3f;
    private float titleHeight = 0.6f;
    private float guildHeight = 0.9f;
    
    private boolean debug = false;
    
    public NameDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        hookServices();
        loadSettings();
    }
    
    private void hookServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            scheduler = rpgCore.getScheduler();
            externalServices = rpgCore.getExternalServices();
            miniMessage = rpgCore.getMiniMessageService();
            plugin.getLogger().info("[NameDisplayManager] 已连接到 RPGCore 服务");
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }
    
    public void loadSettings() {
        enabled = plugin.getConfig().getBoolean("display.enabled", true);
        healthEnabled = plugin.getConfig().getBoolean("display.health-enabled", true);
        titleEnabled = plugin.getConfig().getBoolean("display.title-enabled", true);
        guildEnabled = plugin.getConfig().getBoolean("display.guild-enabled", true);
        
        defaultShowTitle = plugin.getConfig().getBoolean("display.default-show-title", true);
        defaultShowGuild = plugin.getConfig().getBoolean("display.default-show-guild", true);
        defaultShowMarriage = plugin.getConfig().getBoolean("display.default-show-marriage", true);
        defaultShowHealth = plugin.getConfig().getBoolean("display.default-show-health", true);
        
        healthFormat = plugin.getConfig().getString("display.health-format", "<red>❤ %d");
        marriageFormat = plugin.getConfig().getString("display.marriage-format", "<light_purple> ♥%s");
        guildFormat = plugin.getConfig().getString("display.guild-format", "<dark_aqua>[%s]");
        
        healthHeight = (float) plugin.getConfig().getDouble("display.health-height", 2.2);
        titleHeight = (float) plugin.getConfig().getDouble("display.title-height", 2.5);
        guildHeight = (float) plugin.getConfig().getDouble("display.guild-height", 2.8);
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void startUpdateTask() {
        if (!enabled) return;
        if (scheduler == null) {
            plugin.getLogger().warning("[NameDisplayManager] Scheduler not available");
            return;
        }
        
        updateTaskId = scheduler.runSyncRepeating(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateDisplays(player);
            }
        }, 20L, 20L);
        
        plugin.getLogger().info("[NameDisplayManager] 更新任务已启动");
    }
    
    public void stopUpdateTask() {
        if (scheduler != null && updateTaskId >= 0) {
            scheduler.cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }
    
    public void initPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        
        showTitle.putIfAbsent(playerId, defaultShowTitle);
        showGuild.putIfAbsent(playerId, defaultShowGuild);
        showMarriage.putIfAbsent(playerId, defaultShowMarriage);
        showHealth.putIfAbsent(playerId, defaultShowHealth);
        
        createDisplays(player);
        log("[初始化] " + player.getName());
    }
    
    private void createDisplays(Player player) {
        if (healthEnabled && showHealth.getOrDefault(player.getUniqueId(), defaultShowHealth)) {
            createHealthDisplay(player);
        }
        if (titleEnabled && showTitle.getOrDefault(player.getUniqueId(), defaultShowTitle)) {
            createTitleDisplay(player);
        }
        if (guildEnabled && showGuild.getOrDefault(player.getUniqueId(), defaultShowGuild)) {
            createGuildDisplay(player);
        }
    }
    
    private void createHealthDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        if (healthDisplays.containsKey(playerId)) return;
        
        int health = getHealth(player);
        String text = String.format(healthFormat, health);
        
        TextDisplay display = createTextDisplay(player, text, healthHeight);
        if (display != null) {
            healthDisplays.put(playerId, display);
            lastHealthText.put(playerId, text);
        }
    }
    
    private void createTitleDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        if (titleDisplays.containsKey(playerId)) return;
        
        String text = buildTitleText(player);
        
        TextDisplay display = createTextDisplay(player, text, titleHeight);
        if (display != null) {
            titleDisplays.put(playerId, display);
            lastTitleText.put(playerId, text);
        }
    }
    
    private void createGuildDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        if (guildDisplays.containsKey(playerId)) return;
        
        String guildName = getGuildName(player);
        if (guildName == null || guildName.isEmpty()) return;
        
        String text = String.format(guildFormat, guildName);
        
        TextDisplay display = createTextDisplay(player, text, guildHeight);
        if (display != null) {
            guildDisplays.put(playerId, display);
            lastGuildText.put(playerId, text);
        }
    }
    
    private TextDisplay createTextDisplay(Player player, String text, float height) {
        try {
            TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class);
            
            display.text(miniMessage.colorize(text));
            display.setVisibleByDefault(true);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setTransformation(
                new org.bukkit.util.Transformation(
                    new Vector3f(0.0f, height, 0.0f),
                    new org.joml.Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.Quaternionf()
                )
            );
            display.setInvulnerable(true);
            display.setPersistent(false);
            
            player.addPassenger(display);
            
            return display;
        } catch (Exception e) {
            if (debug) {
                plugin.getLogger().warning("[NameDisplayManager] 创建TextDisplay失败: " + e.getMessage());
            }
            return null;
        }
    }
    
    public void updateDisplays(Player player) {
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        
        // 更新血量显示
        if (healthEnabled && showHealth.getOrDefault(playerId, defaultShowHealth)) {
            updateHealthDisplay(player);
        } else {
            removeHealthDisplay(player);
        }
        
        // 更新称号显示
        if (titleEnabled && showTitle.getOrDefault(playerId, defaultShowTitle)) {
            updateTitleDisplay(player);
        } else {
            removeTitleDisplay(player);
        }
        
        // 更新工会显示
        if (guildEnabled && showGuild.getOrDefault(playerId, defaultShowGuild)) {
            updateGuildDisplay(player);
        } else {
            removeGuildDisplay(player);
        }
        
        // 确保乘客关系
        ensurePassengerRelationships(player);
    }
    
    private void updateHealthDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        int health = getHealth(player);
        String text = String.format(healthFormat, health);
        
        TextDisplay display = healthDisplays.get(playerId);
        if (display == null || !display.isValid() || display.isDead()) {
            healthDisplays.remove(playerId);
            createHealthDisplay(player);
            return;
        }
        
        String lastText = lastHealthText.get(playerId);
        if (!text.equals(lastText)) {
            display.text(miniMessage.colorize(text));
            lastHealthText.put(playerId, text);
        }
    }
    
    private void updateTitleDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        String text = buildTitleText(player);
        
        TextDisplay display = titleDisplays.get(playerId);
        if (display == null || !display.isValid() || display.isDead()) {
            titleDisplays.remove(playerId);
            createTitleDisplay(player);
            return;
        }
        
        String lastText = lastTitleText.get(playerId);
        if (!text.equals(lastText)) {
            display.text(miniMessage.colorize(text));
            lastTitleText.put(playerId, text);
        }
    }
    
    private void updateGuildDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        String guildName = getGuildName(player);
        
        if (guildName == null || guildName.isEmpty()) {
            removeGuildDisplay(player);
            return;
        }
        
        String text = String.format(guildFormat, guildName);
        
        TextDisplay display = guildDisplays.get(playerId);
        if (display == null || !display.isValid() || display.isDead()) {
            guildDisplays.remove(playerId);
            createGuildDisplay(player);
            return;
        }
        
        String lastText = lastGuildText.get(playerId);
        if (!text.equals(lastText)) {
            display.text(miniMessage.colorize(text));
            lastGuildText.put(playerId, text);
        }
    }
    
    private String buildTitleText(Player player) {
        StringBuilder text = new StringBuilder();
        
        // 称号
        String prefix = getLuckPermsPrefix(player);
        if (prefix != null && !prefix.isEmpty()) {
            text.append(prefix).append(" ");
        }
        
        // 玩家名
        text.append("<white>").append(player.getName());
        
        // 婚姻
        if (showMarriage.getOrDefault(player.getUniqueId(), defaultShowMarriage)) {
            String partnerName = getPartnerName(player);
            if (partnerName != null && !partnerName.isEmpty()) {
                text.append(String.format(marriageFormat, partnerName));
            }
        }
        
        return text.toString();
    }
    
    private void ensurePassengerRelationships(Player player) {
        for (TextDisplay display : new TextDisplay[]{
            healthDisplays.get(player.getUniqueId()),
            titleDisplays.get(player.getUniqueId()),
            guildDisplays.get(player.getUniqueId())
        }) {
            if (display != null && display.isValid()) {
                boolean isPassenger = false;
                for (Entity passenger : player.getPassengers()) {
                    if (passenger.equals(display)) {
                        isPassenger = true;
                        break;
                    }
                }
                if (!isPassenger) {
                    player.addPassenger(display);
                }
            }
        }
    }
    
    private int getHealth(Player player) {
        if (player == null || !player.isOnline()) return 0;
        if (player.isDead()) return 0;
        
        double health = player.getHealth();
        if (health <= 0) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            return (int) Math.ceil(maxHealth);
        }
        return Math.max(1, (int) Math.ceil(health));
    }
    
    private String getLuckPermsPrefix(Player player) {
        if (externalServices == null) return "";
        return externalServices.getPlayerPrefix(player);
    }
    
    private String getPartnerName(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return null;
        
        try {
            MarriageService marriageService = rpgCore.getServiceRegistry().getService(MarriageService.class);
            if (marriageService != null) {
                return marriageService.getPartner(player.getUniqueId());
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("[NameDisplayManager] 获取婚姻失败: " + e.getMessage());
        }
        return null;
    }
    
    private String getGuildName(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return null;
        
        try {
            GuildService guildService = rpgCore.getServiceRegistry().getService(GuildService.class);
            if (guildService != null) {
                Object guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild != null) {
                    try {
                        var nameField = guild.getClass().getField("name");
                        return (String) nameField.get(guild);
                    } catch (Exception e) {
                        return guild.toString();
                    }
                }
            }
        } catch (Exception e) {
            if (debug) plugin.getLogger().warning("[NameDisplayManager] 获取工会失败: " + e.getMessage());
        }
        return null;
    }
    
    // ========== 切换显示 ==========
    
    public boolean toggleTitle(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showTitle.getOrDefault(playerId, defaultShowTitle);
        boolean newValue = !current;
        showTitle.put(playerId, newValue);
        
        if (newValue) {
            createTitleDisplay(player);
        } else {
            removeTitleDisplay(player);
        }
        return newValue;
    }
    
    public boolean toggleGuild(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showGuild.getOrDefault(playerId, defaultShowGuild);
        boolean newValue = !current;
        showGuild.put(playerId, newValue);
        
        if (newValue) {
            createGuildDisplay(player);
        } else {
            removeGuildDisplay(player);
        }
        return newValue;
    }
    
    public boolean toggleMarriage(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showMarriage.getOrDefault(playerId, defaultShowMarriage);
        boolean newValue = !current;
        showMarriage.put(playerId, newValue);
        
        updateTitleDisplay(player);
        return newValue;
    }
    
    public boolean toggleHealth(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showHealth.getOrDefault(playerId, defaultShowHealth);
        boolean newValue = !current;
        showHealth.put(playerId, newValue);
        
        if (newValue) {
            createHealthDisplay(player);
        } else {
            removeHealthDisplay(player);
        }
        return newValue;
    }
    
    // ========== 获取状态 ==========
    
    public String getShowTitleStatus(Player player) {
        boolean status = showTitle.getOrDefault(player.getUniqueId(), defaultShowTitle);
        return status ? "<green>开启" : "<red>关闭";
    }
    
    public String getShowGuildStatus(Player player) {
        boolean status = showGuild.getOrDefault(player.getUniqueId(), defaultShowGuild);
        return status ? "<green>开启" : "<red>关闭";
    }
    
    public String getShowMarriageStatus(Player player) {
        boolean status = showMarriage.getOrDefault(player.getUniqueId(), defaultShowMarriage);
        return status ? "<green>开启" : "<red>关闭";
    }
    
    public String getShowHealthStatus(Player player) {
        boolean status = showHealth.getOrDefault(player.getUniqueId(), defaultShowHealth);
        return status ? "<green>开启" : "<red>关闭";
    }
    
    /**
     * 检查玩家是否启用显示
     */
    public boolean isDisplayEnabled(UUID playerId) {
        return showTitle.getOrDefault(playerId, defaultShowTitle);
    }
    
    /**
     * 设置玩家是否启用显示
     */
    public void setDisplayEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        showTitle.put(playerId, enabled);
        showGuild.put(playerId, enabled);
        showMarriage.put(playerId, enabled);
        showHealth.put(playerId, enabled);
        
        if (enabled) {
            initPlayer(player);
        } else {
            removeAllDisplays(player);
        }
    }
    
    // ========== 移除显示 ==========
    
    private void removeHealthDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        TextDisplay display = healthDisplays.remove(playerId);
        lastHealthText.remove(playerId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
    
    private void removeTitleDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        TextDisplay display = titleDisplays.remove(playerId);
        lastTitleText.remove(playerId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
    
    private void removeGuildDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        TextDisplay display = guildDisplays.remove(playerId);
        lastGuildText.remove(playerId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
    
    public void removeAllDisplays(Player player) {
        removeHealthDisplay(player);
        removeTitleDisplay(player);
        removeGuildDisplay(player);
        
        UUID playerId = player.getUniqueId();
        showTitle.remove(playerId);
        showGuild.remove(playerId);
        showMarriage.remove(playerId);
        showHealth.remove(playerId);
    }
    
    // ========== 清理 ==========
    
    public void clear() {
        stopUpdateTask();
        
        for (TextDisplay display : healthDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        for (TextDisplay display : titleDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        for (TextDisplay display : guildDisplays.values()) {
            if (display != null && display.isValid()) display.remove();
        }
        
        healthDisplays.clear();
        titleDisplays.clear();
        guildDisplays.clear();
        lastHealthText.clear();
        lastTitleText.clear();
        lastGuildText.clear();
        showTitle.clear();
        showGuild.clear();
        showMarriage.clear();
        showHealth.clear();
    }
    
    private void log(String message) {
        if (debug) {
            plugin.getLogger().info("[NameDisplayManager] " + message);
        }
    }
}