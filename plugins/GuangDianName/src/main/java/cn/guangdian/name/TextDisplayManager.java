package cn.guangdian.name;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.service.api.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TextDisplayManager {
    
    private final JavaPlugin plugin;
    private final Map<UUID, TextDisplay> playerTextDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastGuildName = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> showGuild = new ConcurrentHashMap<>();
    private long updateTaskId = -1;
    private SyncScheduler scheduler;
    
    private boolean enabled = true;
    private String guildFormat = "<dark_aqua>[%s<dark_aqua>]";
    public float displayHeight = 2.8f;
    private boolean debug = false;
    private boolean defaultShowGuild = true;
    
    public TextDisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
        hookScheduler();
        loadSettings();
    }
    
    private void hookScheduler() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            scheduler = rpgCore.getScheduler();
        }
    }
    
    public void loadSettings() {
        enabled = plugin.getConfig().getBoolean("title-display.guild-textdisplay-enabled", true);
        guildFormat = plugin.getConfig().getString("title-display.guild-format", "<dark_aqua>[%s<dark_aqua>]");
        displayHeight = (float) plugin.getConfig().getDouble("title-display.guild-display-height", 2.8);
        defaultShowGuild = plugin.getConfig().getBoolean("title-display.default-show-guild", true);
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public void startUpdateTask() {
        if (!enabled) {
            return;
        }
        
        if (scheduler == null) {
            plugin.getLogger().warning("[TextDisplayManager] Scheduler not available");
            return;
        }
        
        updateTaskId = scheduler.runSyncRepeating(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerTextDisplay(player);
            }
        }, 20L, 20L);
    }
    
    public void stopUpdateTask() {
        if (scheduler != null && updateTaskId >= 0) {
            scheduler.cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }
    
    public void createTextDisplay(Player player) {
        if (!enabled) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        showGuild.putIfAbsent(playerId, defaultShowGuild);
        
        if (!showGuild.getOrDefault(playerId, defaultShowGuild)) {
            return;
        }
        
        if (playerTextDisplays.containsKey(playerId)) {
            return;
        }
        
        String guildName = getGuildName(player);
        if (guildName == null || guildName.isEmpty()) {
            return;
        }
        
        TextDisplay textDisplay = player.getWorld().spawn(player.getLocation(), TextDisplay.class);
        
        textDisplay.text(plugin instanceof GuangDianName gdn ? 
            gdn.getMiniMessageService().colorize(String.format(guildFormat, guildName)) :
            net.kyori.adventure.text.Component.text(String.format(guildFormat, guildName)));
        textDisplay.setVisibleByDefault(true);
        textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        textDisplay.setShadowed(true);
        textDisplay.setSeeThrough(false);
        
        textDisplay.setTransformation(
            new org.bukkit.util.Transformation(
                new Vector3f(0.0f, displayHeight, 0.0f),
                new org.joml.Quaternionf(),
                new Vector3f(1.0f, 1.0f, 1.0f),
                new org.joml.Quaternionf()
            )
        );
        
        textDisplay.setInvulnerable(true);
        textDisplay.setPersistent(false);
        
        try {
            textDisplay.addScoreboardTag("gdn_guild_display");
        } catch (Exception e) {
            // 忽略
        }
        
        player.addPassenger(textDisplay);
        
        playerTextDisplays.put(playerId, textDisplay);
        lastGuildName.put(playerId, guildName);
        
        log("[创建] " + player.getName() + " 工会TextDisplay: " + guildName);
    }
    
    public void updatePlayerTextDisplay(Player player) {
        if (!enabled) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (!showGuild.getOrDefault(playerId, defaultShowGuild)) {
            if (playerTextDisplays.containsKey(playerId)) {
                removeTextDisplay(player);
            }
            return;
        }
        
        TextDisplay textDisplay = playerTextDisplays.get(playerId);
        
        String guildName = getGuildName(player);
        String lastGuild = lastGuildName.get(playerId);
        
        if (guildName == null || guildName.isEmpty()) {
            if (textDisplay != null) {
                removeTextDisplay(player);
            }
            return;
        }
        
        if (textDisplay == null) {
            createTextDisplay(player);
            return;
        }
        
        if (!guildName.equals(lastGuild)) {
            textDisplay.text(plugin instanceof GuangDianName gdn ? 
                gdn.getMiniMessageService().colorize(String.format(guildFormat, guildName)) :
                net.kyori.adventure.text.Component.text(String.format(guildFormat, guildName)));
            lastGuildName.put(playerId, guildName);
            log("[更新] " + player.getName() + " 工会: " + guildName);
        }
        
        if (!textDisplay.isValid() || textDisplay.isDead()) {
            playerTextDisplays.remove(playerId);
            createTextDisplay(player);
            return;
        }
        
        boolean isPassenger = false;
        for (Entity passenger : player.getPassengers()) {
            if (passenger.equals(textDisplay)) {
                isPassenger = true;
                break;
            }
        }
        
        if (!isPassenger) {
            player.addPassenger(textDisplay);
        }
    }
    
    public boolean toggleShowGuild(Player player) {
        UUID playerId = player.getUniqueId();
        boolean current = showGuild.getOrDefault(playerId, defaultShowGuild);
        boolean newState = !current;
        showGuild.put(playerId, newState);
        
        if (newState) {
            createTextDisplay(player);
        } else {
            removeTextDisplay(player);
        }
        
        return newState;
    }
    
    public boolean isShowGuild(Player player) {
        return showGuild.getOrDefault(player.getUniqueId(), defaultShowGuild);
    }
    
    public void removeTextDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        TextDisplay textDisplay = playerTextDisplays.remove(playerId);
        lastGuildName.remove(playerId);
        
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.remove();
            log("[移除] " + player.getName() + " 工会TextDisplay");
        }
    }
    
    private String getGuildName(Player player) {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) {
            return null;
        }
        
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
            if (debug) {
                plugin.getLogger().warning("[TextDisplayManager] 获取工会失败: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    public void clear() {
        stopUpdateTask();
        
        for (TextDisplay textDisplay : playerTextDisplays.values()) {
            if (textDisplay != null && textDisplay.isValid()) {
                textDisplay.remove();
            }
        }
        
        playerTextDisplays.clear();
        lastGuildName.clear();
        showGuild.clear();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    private String translateColors(String text) {
        if (text == null) return "";
        // 返回原始字符串，由调用方使用 MiniMessageService 解析
        return text;
    }
    
    private void log(String message) {
        if (debug) {
            plugin.getLogger().info("[TextDisplayManager] " + message);
        }
    }
}
