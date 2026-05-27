package cn.guangdian.tab;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.tab.adapter.TabServiceAdapter;
import cn.guangdian.tab.placeholder.TabPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class GuangDianTab extends AbstractRPGPlugin implements Listener, TabCompleter {

    private static GuangDianTab instance;
    private TabServiceAdapter tabServiceAdapter;
    
    private long refreshTaskId = -1;
    private long headerFooterTaskId = -1;

    private final List<GroupFormat> groupFormats = new ArrayList<>();
    private final ConcurrentHashMap<UUID, String> lastNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastHeaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastFooters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, PlayerCache> playerCache = new ConcurrentHashMap<>();
    private final AtomicReference<CachedTps> cachedTps = new AtomicReference<>();

    // RPGCore 服务引用
    private MiniMessageService miniMessage;
    private final MiniMessage miniMessageParser = MiniMessage.miniMessage();

    private FileConfiguration config;
    private GroupFormat defaultFormat;
    private long refreshTicks;
    private long headerFooterTicks;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();
        config = getConfig();
        loadFormats();
        registerEvents();
        registerCommands();
        startTasks();

        // 初始化 RPGCore 服务适配器
        tabServiceAdapter = new TabServiceAdapter(this);

        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TabPlaceholder(this).register();
            getLogger().info("已注册 PlaceholderAPI 扩展!");
        }

        refreshAll();
        getLogger().info("GuangDianTab enabled.");
    }

    @Override
    protected void onPluginDisable() {
        // 注销 RPGCore 服务适配器
        if (tabServiceAdapter != null) {
            tabServiceAdapter.unregister();
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
            if (refreshTaskId >= 0) {
                scheduler.cancelTask(refreshTaskId);
            }
            if (headerFooterTaskId >= 0) {
                scheduler.cancelTask(headerFooterTaskId);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayer(player);
        }
        getLogger().info("GuangDianTab disabled.");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianTab";
    }

    private void initRPGCoreServices() {
        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore MiniMessageService");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore MiniMessageService: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    private void registerCommands() {
        if (getCommand("gdtab") != null) {
            getCommand("gdtab").setExecutor(this);
            getCommand("gdtab").setTabCompleter(this);
        }
        if (getCommand("tablist") != null) {
            getCommand("tablist").setExecutor(this);
        }
    }

    private PlayerCache getOrCreateCache(Player player) {
        long expireMs = config.getLong("cache.expire-ms", 30000L);
        UUID uuid = player.getUniqueId();
        PlayerCache cache = playerCache.get(uuid);
        if (cache != null && !cache.isExpired(expireMs)) {
            return cache;
        }
        String primaryGroup = getPrimaryGroupFromApi(player);
        String prefix = getPrefixFromApi(player);
        String suffix = getSuffixFromApi(player);
        cache = new PlayerCache(primaryGroup, prefix, suffix);
        playerCache.put(uuid, cache);
        return cache;
    }

    private String getPrimaryGroupFromApi(Player player) {
        if (externalServices == null) return "default";
        return externalServices.getPlayerPrimaryGroup(player);
    }

    private String getPrefixFromApi(Player player) {
        if (externalServices == null) return "";
        return externalServices.getPlayerPrefix(player);
    }

    private String getSuffixFromApi(Player player) {
        if (externalServices == null) return "";
        return externalServices.getPlayerSuffix(player);
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadFormats() {
        refreshTicks = Math.max(1L, config.getLong("refresh-interval", 1000L) / 50L);
        headerFooterTicks = Math.max(1L, config.getLong("header.refresh-interval", 1000L) / 50L);

        ConfigurationSection defaultSection = config.getConfigurationSection("default-format");
        defaultFormat = new GroupFormat(
            "default",
            defaultSection == null ? "<gray>" : defaultSection.getString("prefix", "<gray>"),
            defaultSection == null ? "%player_name%" : defaultSection.getString("name", "%player_name%"),
            defaultSection == null ? "" : defaultSection.getString("suffix", ""),
            defaultSection == null ? 0 : defaultSection.getInt("weight", 0),
            defaultSection == null ? "" : defaultSection.getString("condition", "")
        );

        groupFormats.clear();
        ConfigurationSection groupSection = config.getConfigurationSection("group-formats");
        if (groupSection != null) {
            for (String key : groupSection.getKeys(false)) {
                ConfigurationSection section = groupSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                groupFormats.add(new GroupFormat(
                    key,
                    section.getString("prefix", ""),
                    section.getString("name", "%player_name%"),
                    section.getString("suffix", ""),
                    section.getInt("weight", 0),
                    section.getString("condition", "")
                ));
            }
        }

        groupFormats.sort(Comparator.comparingInt(GroupFormat::weight).reversed());
    }

    private void startTasks() {
        if (scheduler == null) {
            getLogger().warning("Scheduler not available, tasks not started");
            return;
        }
        
        refreshTaskId = scheduler.runSyncRepeating(() -> {
            cachedTps.set(new CachedTps(Bukkit.getTPS()));
            refreshAll();
        }, 10L, refreshTicks);

        headerFooterTaskId = scheduler.runSyncRepeating(() -> {
            updateAllHeadersAndFooters();
        }, 20L, headerFooterTicks);
    }

    public void refreshAll() {
        if (!config.getBoolean("enabled", true)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                resetPlayer(player);
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTab(player);
        }
    }

    public void updateAllHeadersAndFooters() {
        if (!config.getBoolean("enabled", true)) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateHeaderFooter(player);
        }
    }

    public void updatePlayerTab(Player player) {
        if (!player.hasPermission("guangdian.tab.use")) {
            resetPlayer(player);
            return;
        }

        if (config.getBoolean("advanced.hide-spectators", false) && player.getGameMode() == GameMode.SPECTATOR) {
            resetPlayer(player);
            return;
        }

        List<String> disabledWorlds = config.getStringList("advanced.disabled-worlds");
        if (disabledWorlds.contains(player.getWorld().getName())) {
            resetPlayer(player);
            return;
        }

        GroupFormat format = resolveFormat(player);
        String built = buildTabName(player, format);
        // 将 & 转换为 § (legacy 格式)，因为 Bukkit Tab API 需要 legacy 格式
        String legacyBuilt = translateToLegacy(built);
        legacyBuilt = limit(legacyBuilt, 80);

        String old = lastNames.put(player.getUniqueId(), legacyBuilt);
        if (!Objects.equals(old, legacyBuilt)) {
            player.setPlayerListName(legacyBuilt);
            if (config.getBoolean("nametag.enabled", false)) {
                player.setDisplayName(legacyBuilt);
            }
        }

        if (config.getBoolean("sort-by-weight", true)) {
            player.setPlayerListOrder(buildOrder(player, format.weight()));
        }
    }

    public void updateHeaderFooter(Player player) {
        if (!player.hasPermission("guangdian.tab.use")) {
            return;
        }

        if (config.getBoolean("header.enabled", true)) {
            String header = buildMultiline(player, config.getStringList("header.lines"));
            // 将 & 转换为 § (legacy 格式)
            String legacyHeader = translateToLegacy(header);
            String old = lastHeaders.put(player.getUniqueId(), legacyHeader);
            if (!Objects.equals(old, legacyHeader)) {
                player.setPlayerListHeader(legacyHeader);
            }
        }

        if (config.getBoolean("footer.enabled", true)) {
            String footer = buildMultiline(player, config.getStringList("footer.lines"));
            // 将 & 转换为 § (legacy 格式)
            String legacyFooter = translateToLegacy(footer);
            String old = lastFooters.put(player.getUniqueId(), legacyFooter);
            if (!Objects.equals(old, legacyFooter)) {
                player.setPlayerListFooter(legacyFooter);
            }
        }
    }

    private String buildMultiline(Player player, List<String> lines) {
        List<String> output = new ArrayList<>();
        for (String line : lines) {
            output.add(process(player, line));
        }
        return String.join("\n", output);
    }

    private GroupFormat resolveFormat(Player player) {
        String cachedPrimaryGroup = getOrCreateCache(player).primaryGroup();
        for (GroupFormat format : groupFormats) {
            if (matchesCondition(player, format.condition(), format.key(), cachedPrimaryGroup)) {
                return format;
            }
        }
        return defaultFormat;
    }

    private boolean matchesCondition(Player player, String condition, String fallbackKey, String cachedPrimaryGroup) {
        if (cachedPrimaryGroup == null) {
            cachedPrimaryGroup = "default";
        }
        if ((condition == null || condition.isBlank()) && fallbackKey != null && fallbackKey.equalsIgnoreCase(cachedPrimaryGroup)) {
            return true;
        }

        if (condition == null || condition.isBlank()) {
            return false;
        }

        String[] checks = condition.split("&&");
        for (String rawCheck : checks) {
            String check = rawCheck.trim();
            if (check.startsWith("permission:")) {
                if (!player.hasPermission(check.substring("permission:".length()))) {
                    return false;
                }
            } else if (check.startsWith("group:")) {
                if (!cachedPrimaryGroup.equalsIgnoreCase(check.substring("group:".length()))) {
                    return false;
                }
            } else if (check.startsWith("world:")) {
                if (!player.getWorld().getName().equalsIgnoreCase(check.substring("world:".length()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private String buildTabName(Player player, GroupFormat format) {
        String specialTag = buildSpecialTags(player);
        return specialTag + process(player, format.prefix() + format.name() + format.suffix());
    }

    private String buildSpecialTags(Player player) {
        if (externalServices == null || !externalServices.isPlaceholderAPIEnabled()) {
            return "";
        }

        StringBuilder tags = new StringBuilder();
        String allPlaceholders = "%essentials_afk%|%cmi_user_afk%|%gdcombat_in_combat%|%gdguild_name%|%gdmarriage_partner%";
        String parsed = externalServices.parsePlaceholders(player, allPlaceholders);
        String[] values = parsed.split("\\|");

        String afkValue = values.length > 0 ? values[0] : "";
        String cmiAfkValue = values.length > 1 ? values[1] : "";
        String combatValue = values.length > 2 ? values[2] : "";
        String guildName = values.length > 3 ? values[3] : "";
        String spouse = values.length > 4 ? values[4] : "";

        if (config.getBoolean("advanced.afk.enabled", true) && (isTruthyValue(afkValue) || isTruthyValue(cmiAfkValue))) {
            tags.append(config.getString("advanced.afk.format", "<gray>[AFK] <white>"));
        }

        if (config.getBoolean("special-tags.combat.enabled", true) && isTruthyValue(combatValue)) {
            tags.append(config.getString("special-tags.combat.in-combat", ""));
        }

        if (config.getBoolean("special-tags.guild.enabled", true)) {
            if (guildName != null && !guildName.isBlank() && !guildName.equals("%gdguild_name%")) {
                tags.append(config.getString("special-tags.guild.has-guild", "").replace("%gdguild_name%", guildName));
            } else {
                tags.append(config.getString("special-tags.guild.no-guild", ""));
            }
        }

        if (config.getBoolean("special-tags.marriage.enabled", true)) {
            if (spouse != null && !spouse.isBlank() && !spouse.equals("%gdmarriage_partner%")) {
                tags.append(config.getString("special-tags.marriage.married", "").replace("%gdmarriage_partner%", spouse));
            } else {
                tags.append(config.getString("special-tags.marriage.single", ""));
            }
        }

        return tags.toString();
    }

    private boolean isTruthyValue(String value) {
        if (value == null) {
            return false;
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("1") || value.equals("yes") || value.equals("y");
    }

    private String process(Player player, String input) {
        if (input == null) {
            return "";
        }

        PlayerCache cache = getOrCreateCache(player);

        StringBuilder sb = new StringBuilder(input);
        replaceAll(sb, "%player%", player.getName());
        replaceAll(sb, "%player_name%", player.getName());
        replaceAll(sb, "%player_displayname%", player.getDisplayName());
        replaceAll(sb, "%player_level%", String.valueOf(player.getLevel()));
        replaceAll(sb, "%player_health%", String.valueOf((int) Math.ceil(player.getHealth())));
        replaceAll(sb, "%player_max_health%", String.valueOf((int) Math.ceil(player.getMaxHealth())));
        replaceAll(sb, "%player_food%", String.valueOf(player.getFoodLevel()));
        replaceAll(sb, "%player_ping%", String.valueOf(player.getPing()));
        replaceAll(sb, "%player_world%", player.getWorld().getName());
        replaceAll(sb, "%world%", player.getWorld().getName());
        replaceAll(sb, "%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        replaceAll(sb, "%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        replaceAll(sb, "%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        replaceAll(sb, "%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        replaceAll(sb, "%luckperms_prefix%", cache.prefix());
        replaceAll(sb, "%luckperms_suffix%", cache.suffix());
        replaceAll(sb, "%luckperms_primary_group_name%", cache.primaryGroup());

        String output = applyVault(sb.toString(), player);
        output = applyServerTps(output);
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            output = externalServices.parsePlaceholders(player, output);
        }
        return output;
    }

    private void replaceAll(StringBuilder sb, String target, String replacement) {
        int index;
        while ((index = sb.indexOf(target)) != -1) {
            sb.replace(index, index + target.length(), replacement);
        }
    }

    private String applyServerTps(String output) {
        CachedTps cached = cachedTps.get();
        double[] tps = cached != null ? cached.tps() : Bukkit.getTPS();
        output = output.replace("%server_tps%", formatTps(readTps(tps, 0)));
        output = output.replace("%server_tps_1%", formatTps(readTps(tps, 0)));
        output = output.replace("%server_tps_5%", formatTps(readTps(tps, 1)));
        output = output.replace("%server_tps_15%", formatTps(readTps(tps, 2)));
        return output;
    }

    private double readTps(double[] tps, int index) {
        if (tps == null || index < 0 || index >= tps.length) {
            return 20.0D;
        }
        double value = tps[index];
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 20.0D;
        }
        return Math.min(20.0D, value);
    }

    private String formatTps(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String placeholder(Player player, String placeholder) {
        if (externalServices == null || !externalServices.isPlaceholderAPIEnabled()) {
            return placeholder;
        }
        return externalServices.parsePlaceholders(player, placeholder);
    }

    private String applyVault(String output, Player player) {
        if (externalServices == null || !externalServices.isVaultEnabled()) {
            return output.replace("%vault_eco_balance%", "0")
                        .replace("%vault_eco_balance_fixed%", "0.00")
                        .replace("%vault_eco_balance_formatted%", "0");
        }
        double balance = externalServices.getBalance(player);
        return output
            .replace("%vault_eco_balance%", String.valueOf((long) balance))
            .replace("%vault_eco_balance_fixed%", String.format(Locale.US, "%.2f", balance))
            .replace("%vault_eco_balance_formatted%", String.valueOf((long) balance));
    }

    private int buildOrder(Player player, int weight) {
        int clampedWeight = Math.max(0, Math.min(1000, weight));
        int nameComponent = Math.abs(player.getName().toLowerCase(Locale.ROOT).hashCode()) % 100;
        return (1000 - clampedWeight) * 100 + nameComponent;
    }

    private void resetPlayer(Player player) {
        lastNames.remove(player.getUniqueId());
        lastHeaders.remove(player.getUniqueId());
        lastFooters.remove(player.getUniqueId());
        player.setPlayerListName(player.getName());
        player.setPlayerListHeader(null);
        player.setPlayerListFooter(null);
    }

    /**
     * 将 MiniMessage 格式转换为 legacy § 格式（用于 setPlayerListName 等 Bukkit Tab API）
     * Bukkit Tab API 只支持 legacy 颜色代码，不支持 MiniMessage
     * 使用 RPGCore MiniMessageService.legacyColorize 方法，然后将 & 替换为 §
     */
    private String translateToLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // 如果 miniMessage 服务不可用，使用简单的字符替换
        if (miniMessage == null) {
            getLogger().warning("MiniMessageService 不可用，使用降级处理");
            return simpleMiniMessageToLegacy(input);
        }
        
        try {
            // 使用 RPGCore MiniMessageService 将 MiniMessage 转换为 legacy 格式
            String result = miniMessage.legacyColorize(input);
            // 将 & 替换为 §，因为 Bukkit Tab API 需要 § 格式
            result = result.replace('&', '§');
            if (config.getBoolean("advanced.debug", false)) {
                getLogger().info("[Debug] translateToLegacy: '" + input + "' -> '" + result + "'");
            }
            return result;
        } catch (Exception e) {
            getLogger().warning("MiniMessage 转换失败: " + e.getMessage() + ", 输入: " + input);
            return simpleMiniMessageToLegacy(input);
        }
    }
    
    /**
     * 简单的 MiniMessage 到 legacy 格式转换（降级方案）
     */
    private String simpleMiniMessageToLegacy(String input) {
        return input
            .replace("<black>", "§0").replace("<dark_blue>", "§1")
            .replace("<dark_green>", "§2").replace("<dark_aqua>", "§3")
            .replace("<dark_red>", "§4").replace("<dark_purple>", "§5")
            .replace("<gold>", "§6").replace("<gray>", "§7")
            .replace("<dark_gray>", "§8").replace("<blue>", "§9")
            .replace("<green>", "§a").replace("<aqua>", "§b")
            .replace("<red>", "§c").replace("<light_purple>", "§d")
            .replace("<yellow>", "§e").replace("<white>", "§f")
            .replace("<bold>", "§l").replace("<underlined>", "§n")
            .replace("<strikethrough>", "§m").replace("<italic>", "§o")
            .replace("<reset>", "§r");
    }

    /**
     * 将 & 颜色代码转换为 MiniMessage 格式
     */
    private String translateToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input
            .replace("<black>", "<black>").replace("<dark_blue>", "<dark_blue>")
            .replace("<dark_green>", "<dark_green>").replace("<dark_aqua>", "<dark_aqua>")
            .replace("<dark_red>", "<dark_red>").replace("<dark_purple>", "<dark_purple>")
            .replace("<gold>", "<gold>").replace("<gray>", "<gray>")
            .replace("<dark_gray>", "<dark_gray>").replace("<blue>", "<blue>")
            .replace("<green>", "<green>").replace("<aqua>", "<aqua>")
            .replace("<red>", "<red>").replace("<light_purple>", "<light_purple>")
            .replace("<yellow>", "<yellow>").replace("<white>", "<white>")
            .replace("<obfuscated>", "<obfuscated>").replace("<bold>", "<bold>")
            .replace("<strikethrough>", "<strikethrough>").replace("<underlined>", "<underlined>")
            .replace("<italic>", "<italic>").replace("<reset>", "<reset>");
    }

    /**
     * 使用 MiniMessage 解析颜色代码并返回 Component（用于 sendMessage）
     */
    private Component translate(String input) {
        return miniMessageParser.deserialize(translateToMiniMessage(input));
    }

    private String limit(String input, int maxLength) {
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        scheduler.runSyncLater(() -> {
            updatePlayerTab(event.getPlayer());
            updateHeaderFooter(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastNames.remove(uuid);
        lastHeaders.remove(uuid);
        lastFooters.remove(uuid);
        playerCache.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            Player player = event.getPlayer();
            scheduler.runSyncLater(() -> {
                updatePlayerTab(player);
                updateHeaderFooter(player);
            }, 1L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tablist")) {
            sendInfo(sender);
            return true;
        }

        if (!command.getName().equalsIgnoreCase("gdtab")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("guangdian.tab.reload")) {
                sender.sendMessage(translate(config.getString("messages.no-permission", "<red>No permission.")));
                return true;
            }
            reloadConfig();
            config = getConfig();
            loadFormats();
            if (scheduler != null) {
                if (refreshTaskId >= 0) {
                    scheduler.cancelTask(refreshTaskId);
                }
                if (headerFooterTaskId >= 0) {
                    scheduler.cancelTask(headerFooterTaskId);
                }
            }
            startTasks();
            refreshAll();
            updateAllHeadersAndFooters();
            sender.sendMessage(translate(config.getString("messages.config-reloaded", "<green>GuangDianTab reloaded.")));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("cache")) {
            sender.sendMessage(translate("<yellow>Cached names: <white>" + lastNames.size()));
            sender.sendMessage(translate("<yellow>Cached headers: <white>" + lastHeaders.size()));
            sender.sendMessage(translate("<yellow>Cached footers: <white>" + lastFooters.size()));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translate("<gold>/gdtab reload <gray>Reload config"));
        sender.sendMessage(translate("<gold>/gdtab info <gray>Plugin info"));
        sender.sendMessage(translate("<gold>/gdtab cache <gray>Cache info"));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(translate("<gold>GuangDianTab <gray>v" + getDescription().getVersion()));
        sender.sendMessage(translate("<yellow>Refresh: <white>" + (refreshTicks * 50L) + "ms"));
        sender.sendMessage(translate("<yellow>Header/Footer: <white>" + (headerFooterTicks * 50L) + "ms"));
        if (externalServices != null) {
            sender.sendMessage(translate("<yellow>External Services: <white>" + externalServices.getExternalServiceStatus()));
        } else {
            sender.sendMessage(translate("<yellow>External Services: <red>not connected"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.add("info");
            completions.add("cache");
            completions.add("help");
        }
        return completions;
    }

    public static GuangDianTab getInstance() {
        return instance;
    }

    public String getPrefixForPlayer(Player player) {
        return resolveFormat(player).prefix();
    }

    public String getSuffixForPlayer(Player player) {
        return resolveFormat(player).suffix();
    }

    public String getGroupForPlayer(Player player) {
        return resolveFormat(player).key();
    }

    private record GroupFormat(String key, String prefix, String name, String suffix, int weight, String condition) {
    }

    private static class PlayerCache {
        private final String primaryGroup;
        private final String prefix;
        private final String suffix;
        private final long cacheTime;

        public PlayerCache(String primaryGroup, String prefix, String suffix) {
            this.primaryGroup = primaryGroup;
            this.prefix = prefix;
            this.suffix = suffix;
            this.cacheTime = System.currentTimeMillis();
        }

        public String primaryGroup() {
            return primaryGroup;
        }

        public String prefix() {
            return prefix;
        }

        public String suffix() {
            return suffix;
        }

        public boolean isExpired(long expireMs) {
            return System.currentTimeMillis() - cacheTime > expireMs;
        }
    }

    private static class CachedTps {
        private final double[] tps;
        private final long cacheTime;

        public CachedTps(double[] tps) {
            this.tps = tps;
            this.cacheTime = System.currentTimeMillis();
        }

        public double[] tps() {
            return tps;
        }

        public long cacheTime() {
            return cacheTime;
        }
    }
}
