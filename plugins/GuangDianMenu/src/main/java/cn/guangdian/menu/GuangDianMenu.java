package cn.guangdian.menu;

import cn.guangdian.menu.adapter.MenuServiceAdapter;
import cn.guangdian.menu.command.GuangDianMenuCommand;
import cn.guangdian.menu.placeholder.MenuPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.gui.GUIManager;
import cn.guangdian.rpgcore.gui.action.ActionExecutor;
import cn.guangdian.rpgcore.gui.model.MenuData;
import cn.guangdian.rpgcore.gui.model.MenuHolder;
import cn.guangdian.rpgcore.gui.model.MenuItem;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 光点菜单插件 - 使用 RPGCore CommandFramework
 */
public class GuangDianMenu extends AbstractRPGPlugin implements Listener {

    private static GuangDianMenu instance;
    private FileConfiguration config;
    private final Set<String> claimedStarterKit = ConcurrentHashMap.newKeySet();
    private org.bukkit.scoreboard.Objective starterKitObjective;
    private final Map<UUID, String> playerMenus = new ConcurrentHashMap<>();

    private NamespacedKey menuItemKey;
    private MenuServiceAdapter serviceAdapter;
    private GUIManager guiManager;

    private SoundService soundService;
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;
    private ExternalServiceIntegration externalServices;
    private BiFunction<String, Player, String> placeholderProcessor;

    // RPGCore CommandFramework
    private GuangDianMenuCommand adminCommand;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        menuItemKey = new NamespacedKey(this, "menu_item");

        initRPGCoreServices();
        initStarterKitScoreboard();

        if (guiManager != null) {
            guiManager.loadMenusFromFolder(this, "menus");
            getLogger().info("GUIManager 已加载 " + guiManager.getMenuCount() + " 个菜单");
        }

        // 初始化命令系统 (使用 RPGCore CommandFramework)
        initCommandFramework();

        getServer().getPluginManager().registerEvents(this, this);
        serviceAdapter = new MenuServiceAdapter(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MenuPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
    }

    /**
     * 初始化 RPGCore CommandFramework
     */
    private void initCommandFramework() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            CommandFramework framework = CommandFramework.getInstance();

            // 注册 guangdianmenu 管理命令
            adminCommand = new GuangDianMenuCommand(this);
            framework.registerCommand(adminCommand);

            getLogger().info("已注册 CommandFramework 命令");
        } else {
            // 降级处理：使用传统命令注册
            getLogger().warning("RPGCore 未加载，使用传统命令注册方式");
            initLegacyCommands();
        }
    }

    /**
     * 传统命令注册方式 (降级处理)
     */
    private void initLegacyCommands() {
        if (getCommand("menu") != null) {
            getCommand("menu").setExecutor(new LegacyMenuCommand(this));
            getCommand("menu").setTabCompleter(new LegacyMenuCommand(this));
        }
        if (getCommand("guangdianmenu") != null) {
            getCommand("guangdianmenu").setExecutor(new LegacyAdminCommand(this));
            getCommand("guangdianmenu").setTabCompleter(new LegacyAdminCommand(this));
        }
    }

    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            soundService = rpgCore.getSoundService();
            miniMessage = rpgCore.getMiniMessageService();
            externalServices = rpgCore.getExternalServices();
            guiManager = GUIManager.getInstance();

            if (miniMessage != null) {
                miniMessageParser = miniMessage.getMiniMessage();
            }

            placeholderProcessor = (text, player) -> processPlaceholders(player, text);
            guiManager.setDefaultPlaceholderProcessor(placeholderProcessor);

            getLogger().info("已连接到 RPGCore 服务");
        } else {
            soundService = SoundService.getInstance();
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
            placeholderProcessor = (text, player) -> processPlaceholders(player, text);
            getLogger().warning("RPGCore 未加载，使用本地服务");
        }
    }

    @Override
    protected void onPluginDisable() {
        playerMenus.clear();
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }
    }

    @Override
    protected String getPluginName() {
        return "GuangDianMenu";
    }

    private void initStarterKitScoreboard() {
        try {
            org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) return;

            org.bukkit.scoreboard.Scoreboard scoreboard = manager.getMainScoreboard();
            starterKitObjective = scoreboard.getObjective("StarterKit");
            if (starterKitObjective == null) {
                starterKitObjective = scoreboard.registerNewObjective("StarterKit", "dummy", "新手礼包");
            }
            getLogger().info("新手礼包记分板已初始化");
        } catch (Exception e) {
            getLogger().warning("记分板初始化失败: " + e.getMessage());
        }
    }

    private boolean hasClaimedStarterKit(Player player) {
        if (starterKitObjective != null) {
            return starterKitObjective.getScore(player.getName()).getScore() > 0;
        }
        return claimedStarterKit.contains(player.getUniqueId().toString());
    }

    private void markStarterKitClaimed(Player player) {
        if (starterKitObjective != null) {
            org.bukkit.scoreboard.Score score = starterKitObjective.getScore(player.getName());
            score.setScore(1);
        } else {
            claimedStarterKit.add(player.getUniqueId().toString());
        }
    }

    public void openMenu(Player player, String menuName) {
        if (guiManager != null && guiManager.hasMenu(menuName)) {
            guiManager.openMenu(player, menuName);
            playerMenus.put(player.getUniqueId(), menuName.toLowerCase());
            playClickSound(player);
            return;
        }

        player.sendMessage(miniMessage.colorize(config.getString("messages.menu-not-found", "<red>菜单不存在!")));
    }

    public void reloadMenus() {
        if (guiManager != null) {
            guiManager.reloadMenus(this, "menus");
        }
    }

    private ItemStack createItemStack(Player player, MenuItem item) {
        ItemStack itemStack = new ItemStack(item.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String name = item.getName();
        if (placeholderProcessor != null) {
            name = placeholderProcessor.apply(name, player);
        }
        meta.displayName(miniMessage.colorize(name));

        List<String> lore = item.getLore();
        List<Component> loreComponents = lore.stream()
                .map(line -> {
                    String processed = placeholderProcessor != null ? placeholderProcessor.apply(line, player) : line;
                    return miniMessage.colorize(processed);
                })
                .collect(Collectors.toList());
        meta.lore(loreComponents);

        if (item.getMaterial() == Material.PLAYER_HEAD && item.getSkullOwner() != null && !item.getSkullOwner().isEmpty() && meta instanceof SkullMeta skullMeta) {
            String skullOwner = placeholderProcessor != null ? placeholderProcessor.apply(item.getSkullOwner(), player) : item.getSkullOwner();
            skullMeta.setOwner(skullOwner);
        }

        if (item.isGlowing()) {
            itemStack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createMenuItem() {
        ConfigurationSection section = config.getConfigurationSection("menu-item");
        String materialName = section != null ? section.getString("material", "COMPASS") : "COMPASS";
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.COMPASS;
        }

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String menuName = section != null ? section.getString("menu", config.getString("default-menu", "main")) : config.getString("default-menu", "main");
        String name = section != null ? section.getString("name", "<gold><bold>主菜单") : "<gold><bold>主菜单";
        List<String> lore = section != null ? section.getStringList("lore") : List.of();

        meta.displayName(miniMessage.colorize(name));
        meta.lore(lore.stream().map(l -> miniMessage.colorize(l)).collect(Collectors.toList()));
        meta.setUnbreakable(section == null || section.getBoolean("unbreakable", true));
        meta.getPersistentDataContainer().set(menuItemKey, PersistentDataType.STRING, menuName.toLowerCase());
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(meta);

        if (section == null || section.getBoolean("glowing", true)) {
            itemStack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            ItemMeta updatedMeta = itemStack.getItemMeta();
            if (updatedMeta != null) {
                updatedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(updatedMeta);
            }
        }

        return itemStack;
    }

    public void giveMenuItem(Player player, boolean replaceConfiguredSlot) {
        ItemStack menuItem = createMenuItem();
        int preferredSlot = Math.max(0, Math.min(8, config.getInt("menu-item.inventory-slot", 4)));
        if (replaceConfiguredSlot) {
            player.getInventory().setItem(preferredSlot, menuItem);
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(menuItem);
        for (ItemStack value : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), value);
        }
    }

    private boolean hasMenuItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
                continue;
            }
            String menuName = item.getItemMeta().getPersistentDataContainer().get(menuItemKey, PersistentDataType.STRING);
            if (menuName != null && !menuName.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String getMenuNameFromItem(ItemMeta meta) {
        String taggedMenu = meta.getPersistentDataContainer().get(menuItemKey, PersistentDataType.STRING);
        if (taggedMenu != null && !taggedMenu.isEmpty()) {
            return taggedMenu.toLowerCase();
        }

        String displayName = meta.getDisplayName();
        if (displayName == null) {
            return null;
        }

        String loreTrigger = config.getString("right-click.lore-trigger", "<yellow>右键打开菜单");
        if (loreTrigger != null && !loreTrigger.isEmpty()) {
            List<String> lore = meta.getLore();
            boolean hasTrigger = false;
            if (lore != null) {
                for (String loreLine : lore) {
                    String plainLoreLine = stripColor(loreLine);
                    String plainLoreTrigger = stripColor(loreTrigger);
                    if (plainLoreLine.contains(plainLoreTrigger)) {
                        hasTrigger = true;
                        break;
                    }
                }
            }
            if (!hasTrigger) {
                return null;
            }
        }

        String plainName = stripColor(displayName);
        if (plainName.contains("菜单") || plainName.contains("Menu") || plainName.contains("menu")) {
            return config.getString("default-menu", "main");
        }
        if (plainName.contains("主菜单")) return "main";
        if (plainName.contains("世界传送") || plainName.contains("传送")) return "worlds";
        if (plainName.contains("设置")) return "settings";
        return null;
    }

    private String processPlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }

        if (externalServices != null) {
            text = externalServices.parsePlaceholders(player, text);
        }

        text = text.replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%player_displayname%", player.getDisplayName())
                .replace("%player_level%", String.valueOf(player.getLevel()))
                .replace("%player_health%", String.valueOf((int) player.getHealth()))
                .replace("%player_max_health%", String.valueOf((int) player.getMaxHealth()))
                .replace("%player_food%", String.valueOf(player.getFoodLevel()))
                .replace("%player_exp%", String.valueOf((int) (player.getExp() * 100)))
                .replace("%player_ping%", String.valueOf(player.getPing()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()));

        text = processLuckPermsPlaceholders(player, text);
        text = processVaultPlaceholders(player, text);
        return text;
    }

    private String processLuckPermsPlaceholders(Player player, String text) {
        String prefix = "<gray>[玩家]";
        String suffix = "";
        String primaryGroup = "default";

        if (externalServices != null) {
            prefix = externalServices.getPlayerPrefix(player);
            suffix = externalServices.getPlayerSuffix(player);
            primaryGroup = externalServices.getPlayerPrimaryGroup(player);
            if (prefix == null || prefix.isEmpty()) prefix = "<gray>[玩家]";
            if (suffix == null) suffix = "";
            if (primaryGroup == null || primaryGroup.isEmpty()) primaryGroup = "default";
        }

        return text.replace("%luckperms_prefix%", prefix)
                .replace("%luckperms_suffix%", suffix)
                .replace("%luckperms_primary_group_name%", primaryGroup);
    }

    private String processVaultPlaceholders(Player player, String text) {
        if (externalServices == null || !externalServices.isVaultEnabled()) {
            return text.replace("%vault_eco_balance%", "0")
                    .replace("%vault_eco_balance_fixed%", "0.00")
                    .replace("%vault_eco_balance_formatted%", "0<gold>金币");
        }

        double balance = externalServices.getBalance(player);
        return text
                .replace("%vault_eco_balance%", String.valueOf((long) balance))
                .replace("%vault_eco_balance_fixed%", String.format(java.util.Locale.US, "%.2f", balance))
                .replace("%vault_eco_balance_formatted%", String.valueOf((long) balance) + "<gold>金币");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String menuName = holder.getMenuId();
        MenuData menuData = guiManager != null ? guiManager.getMenu(menuName) : null;

        if (menuData == null) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        MenuItem item = menuData.getItemBySlot(slot);
        if (item != null && item.hasPermission(player)) {
            List<String> actions = item.getActions();
            if (actions != null && !actions.isEmpty()) {
                boolean isStarterKit = actions.stream().anyMatch(a -> a != null && a.contains("阿斯特瑞亚"));
                if (isStarterKit && hasClaimedStarterKit(player)) {
                    player.sendMessage(miniMessage.colorize("<red>你已领取过新手礼包！"));
                    return;
                }
                if (isStarterKit) {
                    markStarterKitClaimed(player);
                }
                executeActions(player, actions);
            } else if (item.getAction() != null && !item.getAction().isEmpty()) {
                boolean isStarterKit = item.getAction().contains("阿斯特瑞亚");
                if (isStarterKit && hasClaimedStarterKit(player)) {
                    player.sendMessage(miniMessage.colorize("<red>你已领取过新手礼包！"));
                    return;
                }
                if (isStarterKit) {
                    markStarterKitClaimed(player);
                }
                executeAction(player, item.getAction());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!config.getBoolean("right-click.enabled", true)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String menuName = getMenuNameFromItem(item.getItemMeta());
        if (menuName == null) return;
        if (!player.hasPermission("guangdian.menu.use")) {
            player.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        openMenu(player, menuName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("menu-item.auto-give-on-join", false)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("guangdian.menu.use")) return;
        if (config.getBoolean("menu-item.only-if-missing", true) && hasMenuItem(player)) return;

        giveMenuItem(player, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            playerMenus.remove(event.getPlayer().getUniqueId());
        }
    }

    private void executeAction(Player player, String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        ActionExecutor executor = new ActionExecutor(player, placeholderProcessor);

        if (action.startsWith("menu:")) {
            openMenu(player, action.substring(5));
        } else if (action.startsWith("message:")) {
            String message = action.substring(8);
            if (placeholderProcessor != null) {
                message = placeholderProcessor.apply(message, player);
            }
            player.sendMessage(miniMessage.colorize(message));
        } else if (action.startsWith("close")) {
            player.closeInventory();
        } else {
            executor.execute(action);
        }

        playClickSound(player);
    }

    private void executeActions(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        ActionExecutor executor = new ActionExecutor(player, placeholderProcessor);

        for (String action : actions) {
            if (action != null && !action.isEmpty()) {
                if (action.startsWith("menu:")) {
                    openMenu(player, action.substring(5));
                } else if (action.startsWith("message:")) {
                    String message = action.substring(8);
                    if (placeholderProcessor != null) {
                        message = placeholderProcessor.apply(message, player);
                    }
                    player.sendMessage(miniMessage.colorize(message));
                } else if (action.startsWith("close")) {
                    player.closeInventory();
                } else {
                    executor.execute(action);
                }
            }
        }

        playClickSound(player);
    }

    private void playClickSound(Player player) {
        if (soundService != null) {
            String soundName = config.getString("messages.click-sound", "minecraft:block.note_block.pling");
            float volume = (float) config.getDouble("messages.click-volume", 1.0);
            float pitch = (float) config.getDouble("messages.click-pitch", 1.0);
            soundService.playSound(player, soundName, volume, pitch);
        }
    }

    private String stripColor(String text) {
        if (text == null) return "";
        return cn.guangdian.rpgcore.util.TextStripper.stripAll(text);
    }

    public static GuangDianMenu getInstance() {
        return instance;
    }

    public boolean openMenuAPI(Player player, String menuName) {
        if (player == null || menuName == null) return false;
        openMenu(player, menuName);
        return true;
    }

    public boolean hasMenuAPI(String menuName) {
        return menuName != null && guiManager != null && guiManager.hasMenu(menuName);
    }

    public List<String> getMenuNamesAPI() {
        if (guiManager != null) {
            return new ArrayList<>(guiManager.getMenuNames());
        }
        return new ArrayList<>();
    }

    public void reloadMenusAPI() {
        reloadMenus();
    }

    public int getMenuCountAPI() {
        return guiManager != null ? guiManager.getMenuCount() : 0;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public String getPlayerMenu(UUID uuid) {
        return playerMenus.get(uuid);
    }
}
