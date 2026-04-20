package cn.guangdian.chat;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.chat.adapter.ChatServiceAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GuangDianChat extends AbstractRPGPlugin implements Listener, TabCompleter {

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(https?://|www\\.)\\S+");
    private static final int LUCKPERMS_CACHE_DURATION_SECONDS = 30;

    private static GuangDianChat instance;

    private final Map<String, String> worldAliases = new ConcurrentHashMap<>();
    private final Map<UUID, CachedLuckPermsMeta> luckPermsCache = new ConcurrentHashMap<>();
    private FileConfiguration config;
    private ExternalServiceIntegration externalServices;
    private ChatServiceAdapter chatServiceAdapter;

    // RPGCore 服务引用 - 优先使用 RPGCore，本地实现作为降级
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;

    private static class CachedLuckPermsMeta {
        final String prefix;
        final String primaryGroup;
        final long timestamp;

        CachedLuckPermsMeta(String prefix, String primaryGroup) {
            this.prefix = prefix;
            this.primaryGroup = primaryGroup;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > LUCKPERMS_CACHE_DURATION_SECONDS * 1000L;
        }
    }

    @Override
    protected void onPluginEnable() {
        instance = this;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().severe("PlaceholderAPI is required.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        config = getConfig();

        // 初始化 RPGCore 服务（优先使用 RPGCore，本地实现作为降级）
        initRPGCoreServices();

        loadWorldAliases();
        registerEvents();
        registerCommands();
        startLuckPermsCleanupTask();

        // 注册 RPGCore 服务适配器
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                this.externalServices = rpgCore.getExternalServices();
                chatServiceAdapter = new cn.guangdian.chat.adapter.ChatServiceAdapter(this);
                getLogger().info("已集成 RPGCore 服务系统!");
            }
        }

        getLogger().info("GuangDianChat enabled.");
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    miniMessage = rpgCore.getMiniMessageService();
                    if (miniMessage != null) {
                        miniMessageParser = miniMessage.getMiniMessage();
                        getLogger().info("已连接到 RPGCore MiniMessageService");
                    }
                }
            } catch (Exception e) {
                getLogger().warning("连接 RPGCore 服务失败: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，初始化本地降级服务
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
            getLogger().info("使用本地 MiniMessageService（降级）");
        }
    }

    @Override
    protected void onPluginDisable() {
        if (chatServiceAdapter != null) {
            chatServiceAdapter.unregister();
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        getLogger().info("GuangDianChat disabled.");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianChat";
    }
    
    private void startLuckPermsCleanupTask() {
        if (externalServices == null) return;
        scheduler.runAsyncRepeating(() -> {
            long now = System.currentTimeMillis();
            luckPermsCache.entrySet().removeIf(entry ->
                now - entry.getValue().timestamp > LUCKPERMS_CACHE_DURATION_SECONDS * 1000L * 2
            );
        }, LUCKPERMS_CACHE_DURATION_SECONDS * 2 * 20L, LUCKPERMS_CACHE_DURATION_SECONDS * 2 * 20L);
    }
    
    /**
     * 刷新玩家缓存
     */
    public void refreshPlayerCache(UUID playerId) {
        luckPermsCache.remove(playerId);
    }

    private void loadWorldAliases() {
        worldAliases.clear();
        ConfigurationSection section = config.getConfigurationSection("world-aliases");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            worldAliases.put(key, section.getString(key, key));
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void registerCommands() {
        if (getCommand("gdchat") != null) {
            getCommand("gdchat").setExecutor(this);
            getCommand("gdchat").setTabCompleter(this);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        String rawMessage = event.getMessage();
        boolean global = isGlobalMessage(rawMessage);
        String message = stripGlobalPrefix(rawMessage);

        if (!player.hasPermission(config.getString("settings.url-permission", "guangdian.chat.url"))
            && URL_PATTERN.matcher(message).find()) {
            player.sendMessage(miniMessage.colorize(config.getString("messages.no-url-permission", "<red>You cannot send URLs.")));
            event.setCancelled(true);
            return;
        }

        String decoratedMessage = applyFormattingPermissions(player, message);
        String format = resolveFormat(player, global);

        CachedLuckPermsMeta cachedMeta = getCachedLuckPermsMeta(player);

        StringBuilder sb = new StringBuilder(format);
        sb.replace(0, sb.length(), processWithCachedMeta(sb.toString(), player, cachedMeta));
        sb.replace(0, sb.length(), sb.toString()
            .replace("%channel%", global ? config.getString("settings.global-label", "<gold>[Global]") : config.getString("settings.local-label", "<gray>[Local]"))
            .replace("%message%", decoratedMessage));

        format = sb.toString();

        if (!global) {
            int range = config.getInt("settings.chat-range", 0);
            if (range > 0) {
                double rangeSquared = (double) range * range;
                event.getRecipients().clear();
                List<Player> worldPlayers = player.getWorld().getPlayers();
                for (Player recipient : worldPlayers) {
                    if (recipient.getLocation().distanceSquared(player.getLocation()) <= rangeSquared) {
                        event.getRecipients().add(recipient);
                    }
                }
                if (!event.getRecipients().contains(player)) {
                    event.getRecipients().add(player);
                }
            }
        }

        event.setMessage(decoratedMessage);
        
        // 使用 MiniMessage 发送格式化的消息，而不是使用 setFormat
        event.setCancelled(true);
        Component chatComponent = miniMessage.colorize(format);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(chatComponent);
        }
        // 同时发送给控制台
        Bukkit.getConsoleSender().sendMessage(chatComponent);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        luckPermsCache.remove(event.getPlayer().getUniqueId());
    }

    private String buildMultiline(String basePath) {
        List<String> lines = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            String path = index == 1 ? basePath : basePath + "-" + index;
            if (config.contains(path)) {
                lines.add(config.getString(path, ""));
            }
        }
        return String.join("\n", lines);
    }

    private boolean isGlobalMessage(String rawMessage) {
        String globalPrefix = config.getString("settings.global-prefix", "!");
        return globalPrefix != null && !globalPrefix.isEmpty() && rawMessage.startsWith(globalPrefix);
    }

    private String stripGlobalPrefix(String rawMessage) {
        String globalPrefix = config.getString("settings.global-prefix", "!");
        if (isGlobalMessage(rawMessage)) {
            return rawMessage.substring(globalPrefix.length()).trim();
        }
        return rawMessage;
    }

    private String resolveFormat(Player player, boolean global) {
        String group = getCachedPrimaryGroup(player);
        String formatPath = "group-formats." + group;
        String base = config.getString(formatPath, config.getString("default-format", "&7[%channel%] %player_name%: &f%message%"));

        if (global) {
            return config.getString("settings.global-format", "%channel% " + base);
        }
        return config.getString("settings.local-format", "%channel% " + base);
    }

    /**
     * 应用聊天格式权限 - 使用 MiniMessage 格式
     * 将玩家输入的 & 颜色代码转换为 MiniMessage 格式
     */
    private String applyFormattingPermissions(Player player, String message) {
        boolean allowColors = config.getBoolean("settings.allow-colors", true)
            && player.hasPermission(config.getString("settings.color-permission", "guangdian.chat.color"));
        boolean allowFormatting = config.getBoolean("settings.allow-formatting", true)
            && player.hasPermission(config.getString("settings.format-permission", "guangdian.chat.format"));
        boolean allowMagic = player.hasPermission("guangdian.chat.magic");

        StringBuilder output = new StringBuilder(message.length());
        for (int index = 0; index < message.length(); index++) {
            char current = message.charAt(index);
            if (current == '&' && index + 1 < message.length()) {
                char code = Character.toLowerCase(message.charAt(index + 1));
                String miniMessageTag = getMiniMessageTag(code);
                if (miniMessageTag != null) {
                    if (isColorCode(code) && allowColors) {
                        output.append(miniMessageTag);
                        index++;
                        continue;
                    }
                    if (isFormatCode(code) && allowFormatting && (code != 'k' || allowMagic)) {
                        output.append(miniMessageTag);
                        index++;
                        continue;
                    }
                }
            }
            output.append(current);
        }
        return output.toString();
    }

    /**
     * 将传统颜色代码转换为 MiniMessage 标签
     */
    private String getMiniMessageTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private boolean isColorCode(char code) {
        return "0123456789abcdefr".indexOf(code) >= 0;
    }

    private boolean isFormatCode(char code) {
        return "klmnor".indexOf(code) >= 0;
    }

    private String process(Player player, String input) {
        if (input == null) {
            return "";
        }

        CachedLuckPermsMeta cachedMeta = getCachedLuckPermsMeta(player);
        return processWithCachedMeta(input, player, cachedMeta);
    }

    private String processWithCachedMeta(String input, Player player, CachedLuckPermsMeta cachedMeta) {
        String worldName = worldAliases.getOrDefault(player.getWorld().getName(), player.getWorld().getName());

        StringBuilder sb = new StringBuilder(input);
        replaceAll(sb, "%player%", player.getName());
        replaceAll(sb, "%player_name%", player.getName());
        replaceAll(sb, "%displayname%", player.getDisplayName());
        replaceAll(sb, "%player_displayname%", player.getDisplayName());
        replaceAll(sb, "%player_level%", String.valueOf(player.getLevel()));
        replaceAll(sb, "%player_health%", String.valueOf((int) Math.ceil(player.getHealth())));
        replaceAll(sb, "%player_max_health%", String.valueOf((int) Math.ceil(player.getMaxHealth())));
        replaceAll(sb, "%player_ping%", String.valueOf(player.getPing()));
        replaceAll(sb, "%player_world%", worldName);
        replaceAll(sb, "%world%", worldName);
        replaceAll(sb, "%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        replaceAll(sb, "%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        
        String prefix = cachedMeta != null ? cachedMeta.prefix : "";
        replaceAll(sb, "%luckperms_prefix%", prefix != null ? prefix : "");
        String suffix = cachedMeta != null && cachedMeta.primaryGroup != null ? cachedMeta.primaryGroup : "default";
        replaceAll(sb, "%luckperms_suffix%", "");
        replaceAll(sb, "%luckperms_primary_group_name%", suffix);

        String result = sb.toString();
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            result = externalServices.parsePlaceholders(player, result);
        }
        
        return result;
    }

    private void replaceAll(StringBuilder sb, String target, String replacement) {
        int index;
        while ((index = sb.indexOf(target)) != -1) {
            sb.replace(index, index + target.length(), replacement);
        }
    }

    private CachedLuckPermsMeta getCachedLuckPermsMeta(Player player) {
        CachedLuckPermsMeta cached = luckPermsCache.get(player.getUniqueId());
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        String prefix = "";
        String primaryGroup = "default";

        if (externalServices != null) {
            prefix = externalServices.getPlayerPrefix(player);
            primaryGroup = externalServices.getPlayerPrimaryGroup(player);
        }

        CachedLuckPermsMeta newCache = new CachedLuckPermsMeta(prefix, primaryGroup);
        luckPermsCache.put(player.getUniqueId(), newCache);
        return newCache;
    }

    private String getCachedPrimaryGroup(Player player) {
        CachedLuckPermsMeta cached = getCachedLuckPermsMeta(player);
        return cached != null ? cached.primaryGroup : "default";
    }

    private String getLuckPermsMeta(Player player, boolean prefix) {
        CachedLuckPermsMeta cached = getCachedLuckPermsMeta(player);
        return cached != null ? cached.prefix : "";
    }

    private String getPrimaryGroup(Player player) {
        CachedLuckPermsMeta cached = getCachedLuckPermsMeta(player);
        return cached != null ? cached.primaryGroup : "default";
    }

    /**
     * 使用 MiniMessage 解析颜色代码
     * 将 & 颜色代码转换为 MiniMessage 格式
     */
    private String color(String input) {
        if (input == null) return "";
        // 将传统 & 颜色代码转换为 MiniMessage 格式
        return input
            .replace("&0", "<black>").replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>")
            .replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&k", "<obfuscated>").replace("&l", "<bold>")
            .replace("&m", "<strikethrough>").replace("&n", "<underlined>")
            .replace("&o", "<italic>").replace("&r", "<reset>");
    }

    /**
     * 发送 MiniMessage 格式的消息给 CommandSender
     */
    private void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(miniMessage.colorize(text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("gdchat")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendMessage(sender, "<gold>/gdchat reload <gray>Reload config");
            sendMessage(sender, "<gold>/gdchat info <gray>Plugin info");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("guangdian.chat.admin")) {
                sendMessage(sender, config.getString("messages.no-permission", "<red>No permission."));
                return true;
            }
            reloadConfig();
            config = getConfig();
            loadWorldAliases();
            luckPermsCache.clear();
            sendMessage(sender, config.getString("messages.config-reloaded", "<green>GuangDianChat reloaded."));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sendMessage(sender, "<gold>GuangDianChat <gray>v" + getDescription().getVersion());
            if (externalServices != null) {
                sendMessage(sender, "<yellow>External Services: <white>" + externalServices.getExternalServiceStatus());
            } else {
                sendMessage(sender, "<yellow>External Services: <red>not connected");
            }
            sendMessage(sender, "<yellow>Chat range: <white>" + config.getInt("settings.chat-range", 0));
            sendMessage(sender, "<yellow>Global prefix: <white>" + config.getString("settings.global-prefix", "!"));
            sendMessage(sender, "<yellow>LuckPerms cache: <white>" + luckPermsCache.size() + " entries");
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.add("info");
            completions.add("help");
        }
        return completions;
    }

    public static GuangDianChat getInstance() {
        return instance;
    }

    public String getPrefix(Player player) {
        return getLuckPermsMeta(player, true);
    }

    public String getSuffix(Player player) {
        return getLuckPermsMeta(player, false);
    }

    public String getGroup(Player player) {
        return getPrimaryGroup(player);
    }
}
