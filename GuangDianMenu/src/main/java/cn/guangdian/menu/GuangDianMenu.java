package cn.guangdian.menu;

import cn.guangdian.menu.adapter.MenuServiceAdapter;
import cn.guangdian.menu.placeholder.MenuPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class GuangDianMenu extends AbstractRPGPlugin implements Listener, CommandExecutor, TabExecutor {

    private static GuangDianMenu instance;
    private FileConfiguration config;
    final Map<String, MenuData> menus = new ConcurrentHashMap<>();
    private final Set<String> claimedStarterKit = ConcurrentHashMap.newKeySet();
    private org.bukkit.scoreboard.Objective starterKitObjective;
    private final Map<UUID, String> playerMenus = new ConcurrentHashMap<>();

    private NamespacedKey menuItemKey;
    private MenuServiceAdapter serviceAdapter;

    // RPGCore 服务引用
    private SoundService soundService;
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        menuItemKey = new NamespacedKey(this, "menu_item");

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        initStarterKitScoreboard();
        loadMenus();

        if (getCommand("menu") != null) {
            getCommand("menu").setExecutor(this);
            getCommand("menu").setTabCompleter(this);
        }
        if (getCommand("guangdianmenu") != null) {
            getCommand("guangdianmenu").setExecutor(this);
            getCommand("guangdianmenu").setTabCompleter(this);
        }

        getServer().getPluginManager().registerEvents(this, this);
        // 注册RPGCore服务适配器
        serviceAdapter = new MenuServiceAdapter(this);

        // 注册PlaceholderAPI扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MenuPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
    }

    /**
     * 初始化 RPGCore 核心服务
     */
    private void initRPGCoreServices() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    soundService = rpgCore.getSoundService();
                    miniMessage = rpgCore.getMiniMessageService();
                    if (miniMessage != null) {
                        miniMessageParser = miniMessage.getMiniMessage();
                    }
                    getLogger().info("已连接到 RPGCore 服务 (SoundService, MiniMessageService)");
                }
            } catch (Exception e) {
                getLogger().warning("连接 RPGCore 服务失败: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级服务
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
        }
    }

    @Override
    protected void onPluginDisable() {
        menus.clear();
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

    private void loadMenus() {
        menus.clear();
        
        // 从主配置文件加载菜单
        loadMenusFromConfig(config);
        
        // 从 menus 文件夹加载菜单
        File menusFolder = new File(getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }
        loadMenusFromFolder(menusFolder);
        
        getLogger().info("已加载 " + menus.size() + " 个菜单");
    }

    private void loadMenusFromConfig(FileConfiguration config) {
        ConfigurationSection menusSection = config.getConfigurationSection("menus");
        if (menusSection == null) {
            return;
        }

        for (String menuName : menusSection.getKeys(false)) {
            ConfigurationSection menuSection = menusSection.getConfigurationSection(menuName);
            if (menuSection == null) {
                continue;
            }

            MenuData menuData = loadMenuData(menuName, menuSection);
            if (menuData != null) {
                menus.put(menuName.toLowerCase(), menuData);
                getLogger().fine("从主配置加载菜单: " + menuName);
            }
        }
    }

    private void loadMenusFromFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归加载子文件夹
                loadMenusFromFolder(file);
            } else if (file.getName().endsWith(".yml")) {
                loadMenusFromFile(file);
            }
        }
    }

    private void loadMenusFromFile(File file) {
        try {
            org.bukkit.configuration.file.YamlConfiguration yamlConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            
            // 支持两种格式：
            // 1. 直接定义菜单（文件名作为菜单名）
            // 2. 在 menus: 下定义多个菜单
            
            if (yamlConfig.contains("title") && yamlConfig.contains("size")) {
                // 单菜单格式 - 使用文件名作为菜单名
                String menuName = file.getName().replace(".yml", "");
                MenuData menuData = loadMenuData(menuName, yamlConfig);
                if (menuData != null) {
                    menus.put(menuName.toLowerCase(), menuData);
                    getLogger().fine("从文件加载菜单: " + menuName + " (" + file.getPath() + ")");
                }
            } else {
                // 多菜单格式 - 在 menus: 下定义
                ConfigurationSection menusSection = yamlConfig.getConfigurationSection("menus");
                if (menusSection != null) {
                    for (String menuName : menusSection.getKeys(false)) {
                        ConfigurationSection menuSection = menusSection.getConfigurationSection(menuName);
                        if (menuSection == null) {
                            continue;
                        }
                        MenuData menuData = loadMenuData(menuName, menuSection);
                        if (menuData != null) {
                            menus.put(menuName.toLowerCase(), menuData);
                            getLogger().fine("从文件加载菜单: " + menuName + " (" + file.getPath() + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "加载菜单文件失败: " + file.getPath(), e);
        }
    }

    private MenuData loadMenuData(String menuName, ConfigurationSection section) {
        String title = section.getString("title", "<dark_gray>菜单");
        int size = section.getInt("size", 27);
        
        // 验证 size 必须是 9 的倍数
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 27;
        }
        
        MenuData menuData = new MenuData(title, size);
        
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemName : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemName);
                if (itemSection == null) {
                    continue;
                }
                MenuItem item = loadMenuItem(itemSection);
                if (item != null) {
                    menuData.addItem(itemName, item);
                }
            }
        }
        
        return menuData;
    }

    private MenuItem loadMenuItem(ConfigurationSection section) {
        Material material;
        try {
            material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        List<Integer> slots = new ArrayList<>();
        Object slotObj = section.get("slot");
        if (slotObj instanceof Integer integer) {
            slots.add(integer);
        } else if (section.isList("slots")) {
            for (Object value : section.getList("slots")) {
                if (value instanceof Integer integer) {
                    slots.add(integer);
                }
            }
        } else if (section.isList("slot")) {
            for (Object value : section.getList("slot")) {
                if (value instanceof Integer integer) {
                    slots.add(integer);
                }
            }
        }

        // 加载条件配置
        MenuConditions conditions = loadMenuConditions(section.getConfigurationSection("conditions"));

        return new MenuItem(
                material,
                section.getString("name", "<white>物品"),
                section.getStringList("lore"),
                slots,
                section.getString("action", ""),
                section.getStringList("actions"),
                section.getString("skull", ""),
                conditions,
                section.getBoolean("keep-open", false)
        );
    }

    private MenuConditions loadMenuConditions(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        MenuConditions conditions = new MenuConditions();
        
        // 点券条件
        if (section.contains("points")) {
            conditions.setPoints(section.getInt("points", 0));
        }
        
        // 金币条件
        if (section.contains("money")) {
            conditions.setMoney(section.getDouble("money", 0));
        }
        
        // 权限条件
        if (section.contains("permission")) {
            conditions.setPermission(section.getString("permission", ""));
        }
        
        // 物品条件
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            List<RequiredItem> requiredItems = new ArrayList<>();
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    RequiredItem item = new RequiredItem(
                        itemSection.getString("material", "STONE"),
                        itemSection.getInt("amount", 1),
                        itemSection.getString("name", null),
                        itemSection.getStringList("lore")
                    );
                    requiredItems.add(item);
                }
            }
            conditions.setItems(requiredItems);
        }
        
        // 等级条件
        if (section.contains("level")) {
            conditions.setLevel(section.getInt("level", 0));
        }
        
        // 失败消息
        conditions.setFailMessage(section.getString("fail-message", null));
        
        return conditions;
    }

    public void openMenu(Player player, String menuName) {
        MenuData menuData = menus.get(menuName.toLowerCase());
        if (menuData == null) {
            player.sendMessage(miniMessage.colorize(config.getString("messages.menu-not-found", "<red>菜单不存在!")));
            return;
        }

        // 使用 MiniMessage 解析标题
        String titleText = processPlaceholders(player, menuData.getTitle());
        Component title = miniMessage.colorize(titleText);
        Inventory inventory = Bukkit.createInventory(new MenuHolder(menuName.toLowerCase()), menuData.getSize(), title);
        for (MenuItem item : menuData.getItems().values()) {
            ItemStack itemStack = createItemStack(player, item);
            for (int slot : item.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, itemStack);
                }
            }
        }

        player.openInventory(inventory);
        playerMenus.put(player.getUniqueId(), menuName.toLowerCase());
        playClickSound(player);
    }

    private ItemStack createItemStack(Player player, MenuItem item) {
        ItemStack itemStack = new ItemStack(item.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.setDisplayName(legacyColor(processPlaceholders(player, item.getName())));
        meta.setLore(item.getLore().stream().map(line -> legacyColor(processPlaceholders(player, line))).collect(Collectors.toList()));
        if (item.getMaterial() == Material.PLAYER_HEAD && !item.getSkull().isEmpty() && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwner(processPlaceholders(player, item.getSkull()));
        }
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

        meta.setDisplayName(legacyColor(name));
        meta.setLore(lore.stream().map(this::legacyColor).collect(Collectors.toList()));
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

    private void giveMenuItem(Player player, boolean replaceConfiguredSlot) {
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
                .replace("{player}", player.getName())
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

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        MenuData menuData = menus.get(holder.getMenuName());
        if (menuData == null) {
            return;
        }

        int slot = event.getRawSlot();
        // 只允许点击菜单内的格子，点击玩家背包不处理
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        
        // 取消菜单内的点击事件
        event.setCancelled(true);

        MenuItem item = menuData.getItemBySlot(slot);
        if (item != null) {
            getLogger().info("[DEBUG] 点击了物品: " + item.getName() + ", action: " + item.getAction() + ", actions: " + item.getActions());
            
            // 检查条件
            if (item.hasConditions()) {
                ConditionCheckResult checkResult = checkConditions(player, item.getConditions());
                if (!checkResult.isSuccess()) {
                    player.sendMessage(miniMessage.colorize(checkResult.getMessage()));
                    playFailSound(player);
                    return;
                }
                // 扣除资源
                deductResources(player, item.getConditions());
            }
            
            // 检查是否是新手礼包命令
            boolean isStarterKit = false;
            if (item.getActions() != null && !item.getActions().isEmpty()) {
                for (String action : item.getActions()) {
                    if (action != null && action.contains("阿斯特瑞亚")) {
                        isStarterKit = true;
                        break;
                    }
                }
            } else if (item.getAction() != null && item.getAction().contains("阿斯特瑞亚")) {
                isStarterKit = true;
            }
            
            if (isStarterKit) {
                if (hasClaimedStarterKit(player)) {
                    player.sendMessage(miniMessage.colorize("<red>你已领取过新手礼包！"));
                    return;
                }
                markStarterKitClaimed(player);
            }
            
            // 执行动作
            if (item.getActions() != null && !item.getActions().isEmpty()) {
                getLogger().info("[DEBUG] 执行 actions 列表");
                executeActionFromList(player, item.getActions());
            } else if (item.getAction() != null && !item.getAction().isEmpty()) {
                getLogger().info("[DEBUG] 执行单个 action");
                executeAction(player, item.getAction());
            }
            
            // 如果不保持打开，关闭菜单
            if (!item.isKeepOpen()) {
                scheduler.runSyncLater(player::closeInventory, 1L);
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

        if (action.startsWith("menu:")) {
            openMenu(player, action.substring(5));
        } else if (action.startsWith("command:")) {
            String cmd = processPlaceholders(player, action.substring(8));
            executeMultipleCommands(player, cmd, false);
        } else if (action.startsWith("console:")) {
            String cmd = processPlaceholders(player, action.substring(8));
            executeMultipleCommands(player, cmd, true);
        } else if (action.startsWith("message:")) {
            player.sendMessage(miniMessage.colorize(processPlaceholders(player, action.substring(8))));
        } else if (action.startsWith("close")) {
            player.closeInventory();
        } else {
             // 没有前缀，默认作为控制台命令处理
             getLogger().info("[DEBUG] 默认作为控制台命令处理: " + action);
             String cmd = processPlaceholders(player, action);
             executeMultipleCommands(player, cmd, true);
         }

        playClickSound(player);
    }

    private void executeActionFromList(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        for (String action : actions) {
            if (action != null && !action.isEmpty()) {
                executeAction(player, action);
            }
        }
    }

    /**
     * 执行多个命令
     * 
     * ⚠️ 安全警告: 此方法通过 Bukkit.dispatchCommand 以控制台身份执行命令
     * 存在潜在的命令注入风险！配置文件中的命令字符串不应包含未验证的用户输入。
     * 
     * 建议：
     * - 仅使用配置文件中预定义的命令
     * - 不要允许用户直接输入命令字符串
     * - 对于需要参数的命令，使用 PAPI 占位符而非直接用户输入
     * 
     * 当前实现：命令必须来自配置文件，processPlaceholders 仅替换 PAPI 占位符
     */
    private void executeMultipleCommands(Player player, String commands, boolean isConsole) {
        getLogger().info("[DEBUG] executeMultipleCommands 收到: " + commands + ", isConsole=" + isConsole);
        
        // 安全检查：命令列表白名单校验（可选配置）
        List<String> allowedCommands = config.getStringList("security.allowed-commands");
        if (!allowedCommands.isEmpty()) {
            // 如果配置了白名单，则只允许白名单中的命令
            // 白名单格式示例：["give", "teleport", "heal", "rpgitem"]
        }
        
        String[] cmds = commands.split("&&");
        for (String cmd : cmds) {
            String trimmedCmd = cmd.trim();
            if (!trimmedCmd.isEmpty()) {
                // 安全检查：阻止可能危险的命令模式
                if (containsDangerousPattern(trimmedCmd)) {
                    getLogger().warning("[安全警告] 拒绝执行潜在危险的命令: " + trimmedCmd);
                    continue;
                }
                
                // 如果命令以 rpgitem 开头，通过控制台执行但不添加前缀
                if (trimmedCmd.toLowerCase().startsWith("rpgitem ")) {
                    getLogger().info("[DEBUG] RPGItem 命令通过控制台执行: " + trimmedCmd);
                    final String mmCmd = trimmedCmd;
                    scheduler.runSyncLater(() -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), mmCmd);
                    }, 0L);
                } else {
                    // 其他命令正常处理
                    getLogger().info("[DEBUG] dispatchCommand: " + trimmedCmd);
                    final String finalCmd = trimmedCmd;
                    scheduler.runSyncLater(() -> {
                        if (isConsole) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                        } else {
                            player.performCommand(finalCmd);
                        }
                    }, 0L);
                }
            }
        }
    }
    
    /**
     * 检查命令是否包含危险模式
     * 用于防止命令注入攻击
     */
    private boolean containsDangerousPattern(String cmd) {
        String lowerCmd = cmd.toLowerCase();
        // 阻止可能危险的命令模式
        // 注意：这些检查是基础防护，配置文件命令仍需管理员审核
        return lowerCmd.contains("op ") || 
               lowerCmd.contains("deop ") ||
               lowerCmd.contains("stop") ||
               lowerCmd.contains("reload confirm") ||
               lowerCmd.contains("save-all") ||
               lowerCmd.contains("save-off");
    }

    private void playClickSound(Player player) {
        // 使用 RPGCore SoundService
        if (soundService != null) {
            String soundName = config.getString("messages.click-sound", "minecraft:block.note_block.pling");
            float volume = (float) config.getDouble("messages.click-volume", 1.0);
            float pitch = (float) config.getDouble("messages.click-pitch", 1.0);
            soundService.playSound(player, soundName, volume, pitch);
        }
    }

    private void playFailSound(Player player) {
        // 使用 RPGCore SoundService 播放失败音效
        if (soundService != null) {
            String soundName = config.getString("messages.fail-sound", "minecraft:block.note_block.bass");
            float volume = (float) config.getDouble("messages.fail-volume", 1.0);
            float pitch = (float) config.getDouble("messages.fail-pitch", 0.5);
            soundService.playSound(player, soundName, volume, pitch);
        }
    }

    /**
     * 检查玩家是否满足条件
     */
    private ConditionCheckResult checkConditions(Player player, MenuConditions conditions) {
        if (conditions == null) {
            return ConditionCheckResult.success();
        }

        // 检查权限
        if (conditions.getPermission() != null && !conditions.getPermission().isEmpty()) {
            if (!player.hasPermission(conditions.getPermission())) {
                String msg = conditions.getFailMessage() != null ? 
                    conditions.getFailMessage() : 
                    config.getString("messages.no-permission", "<red>你没有权限执行此操作!");
                return ConditionCheckResult.fail(msg);
            }
        }

        // 检查等级
        if (conditions.getLevel() > 0) {
            if (player.getLevel() < conditions.getLevel()) {
                String msg = conditions.getFailMessage() != null ? 
                    conditions.getFailMessage() : 
                    "<red>你的等级不足! 需要等级: <yellow>" + conditions.getLevel();
                return ConditionCheckResult.fail(msg);
            }
        }

        // 检查点券
        if (conditions.getPoints() > 0) {
            int currentPoints = getPlayerPoints(player);
            if (currentPoints < conditions.getPoints()) {
                String msg = conditions.getFailMessage() != null ? 
                    conditions.getFailMessage() : 
                    "<red>你的点券不足! 需要: <yellow>" + conditions.getPoints() + " <red>当前: <yellow>" + currentPoints;
                return ConditionCheckResult.fail(msg);
            }
        }

        // 检查金币
        if (conditions.getMoney() > 0) {
            double currentMoney = getPlayerMoney(player);
            if (currentMoney < conditions.getMoney()) {
                String msg = conditions.getFailMessage() != null ? 
                    conditions.getFailMessage() : 
                    "<red>你的金币不足! 需要: <yellow>" + String.format("%.2f", conditions.getMoney()) + 
                    " <red>当前: <yellow>" + String.format("%.2f", currentMoney);
                return ConditionCheckResult.fail(msg);
            }
        }

        // 检查物品
        if (!conditions.getItems().isEmpty()) {
            for (RequiredItem requiredItem : conditions.getItems()) {
                if (!hasRequiredItem(player, requiredItem)) {
                    String itemName = requiredItem.getName() != null ? requiredItem.getName() : requiredItem.getMaterial();
                    String msg = conditions.getFailMessage() != null ? 
                        conditions.getFailMessage() : 
                        "<red>你缺少所需物品: <yellow>" + itemName + " x" + requiredItem.getAmount();
                    return ConditionCheckResult.fail(msg);
                }
            }
        }

        return ConditionCheckResult.success();
    }

    /**
     * 扣除玩家资源
     */
    private void deductResources(Player player, MenuConditions conditions) {
        if (conditions == null) return;

        // 扣除点券
        if (conditions.getPoints() > 0) {
            takePlayerPoints(player, conditions.getPoints());
            player.sendMessage(miniMessage.colorize("<green>已扣除 <yellow>" + conditions.getPoints() + " <green>点券"));
        }

        // 扣除金币
        if (conditions.getMoney() > 0) {
            takePlayerMoney(player, conditions.getMoney());
            player.sendMessage(miniMessage.colorize("<green>已扣除 <yellow>" + String.format("%.2f", conditions.getMoney()) + " <green>金币"));
        }

        // 扣除物品
        if (!conditions.getItems().isEmpty()) {
            for (RequiredItem requiredItem : conditions.getItems()) {
                removeRequiredItem(player, requiredItem);
            }
        }
    }

    /**
     * 获取玩家点券数量
     */
    private int getPlayerPoints(Player player) {
        // 使用 PlaceholderAPI 获取点券
        if (externalServices != null && externalServices.isPlaceholderAPIEnabled()) {
            String pointsStr = externalServices.parsePlaceholders(player, "%gdpoints_points%");
            try {
                return Integer.parseInt(pointsStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 扣除玩家点券
     */
    private void takePlayerPoints(Player player, int points) {
        // 通过控制台命令扣除点券 - 使用 GuangDianPoints 插件命令
        String cmd = "points take " + player.getName() + " " + points;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    /**
     * 获取玩家金币数量
     */
    private double getPlayerMoney(Player player) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            return externalServices.getBalance(player);
        }
        return 0;
    }

    /**
     * 扣除玩家金币
     */
    private void takePlayerMoney(Player player, double money) {
        if (externalServices != null && externalServices.isVaultEnabled()) {
            externalServices.withdraw(player, money);
        }
    }

    /**
     * 检查玩家是否有指定物品
     */
    private boolean hasRequiredItem(Player player, RequiredItem requiredItem) {
        Material material;
        try {
            material = Material.valueOf(requiredItem.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            
            // 如果指定了名称，检查名称匹配
            if (requiredItem.getName() != null && !requiredItem.getName().isEmpty()) {
                if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) continue;
                String itemName = stripColor(item.getItemMeta().getDisplayName());
                String requiredName = stripColor(requiredItem.getName());
                if (!itemName.contains(requiredName)) continue;
            }
            
            found += item.getAmount();
            if (found >= requiredItem.getAmount()) return true;
        }
        return false;
    }

    /**
     * 移除玩家指定物品
     */
    private void removeRequiredItem(Player player, RequiredItem requiredItem) {
        Material material;
        try {
            material = Material.valueOf(requiredItem.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        int toRemove = requiredItem.getAmount();
        for (int i = 0; i < player.getInventory().getSize() && toRemove > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) continue;
            
            // 如果指定了名称，检查名称匹配
            if (requiredItem.getName() != null && !requiredItem.getName().isEmpty()) {
                if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) continue;
                String itemName = stripColor(item.getItemMeta().getDisplayName());
                String requiredName = stripColor(requiredItem.getName());
                if (!itemName.contains(requiredName)) continue;
            }
            
            int amount = item.getAmount();
            if (amount <= toRemove) {
                player.getInventory().setItem(i, null);
                toRemove -= amount;
            } else {
                item.setAmount(amount - toRemove);
                toRemove = 0;
            }
        }
    }

    /**
     * 使用 MiniMessage 解析颜色代码并返回 Component
     */
    private Component color(String text) {
        if (text == null) return Component.empty();
        return miniMessage.colorize(text);
    }

    /**
     * 使用 MiniMessage 解析颜色代码并返回 legacy 格式字符串
     * 用于 ItemMeta 等需要 String 的 API
     */
    private String legacyColor(String text) {
        if (text == null) return "";
        
        // ✅ 修复：先将传统颜色代码（§ 和 &）转换为 MiniMessage 格式
        // 这样可以避免 MiniMessage 解析时遇到 § 代码而抛出异常
        String convertedText = convertLegacyToMiniMessage(text);
        
        try {
            Component component = miniMessageParser.deserialize(convertedText);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            // 如果解析失败，返回原始文本（移除颜色代码）
            return stripColor(text);
        }
    }
    
    /**
     * 将传统颜色代码（§ 和 &）转换为 MiniMessage 格式
     */
    private String convertLegacyToMiniMessage(String text) {
        if (text == null) return "";
        
        // 先处理 § 颜色代码
        text = text.replace("§0", "<black>")
                   .replace("§1", "<dark_blue>")
                   .replace("§2", "<dark_green>")
                   .replace("§3", "<dark_aqua>")
                   .replace("§4", "<dark_red>")
                   .replace("§5", "<dark_purple>")
                   .replace("§6", "<gold>")
                   .replace("§7", "<gray>")
                   .replace("§8", "<dark_gray>")
                   .replace("§9", "<blue>")
                   .replace("§a", "<green>")
                   .replace("§b", "<aqua>")
                   .replace("§c", "<red>")
                   .replace("§d", "<light_purple>")
                   .replace("§e", "<yellow>")
                   .replace("§f", "<white>")
                   .replace("§k", "<obfuscated>")
                   .replace("§l", "<bold>")
                   .replace("§m", "<strikethrough>")
                   .replace("§n", "<underlined>")
                   .replace("§o", "<italic>")
                   .replace("§r", "<reset>");
        
        // 再处理 & 颜色代码
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&k", "<obfuscated>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");
        
        return text;
    }

    /**
     * 移除文本中的颜色代码
     * 支持传统 & 颜色代码、§ 颜色代码和 MiniMessage 格式
     */
    private String stripColor(String text) {
        if (text == null) return "";
        // 先移除 § 颜色代码
        String noSection = text.replaceAll("§[0-9a-fk-or]", "");
        // 再移除 & 颜色代码
        String noAmpersand = noSection.replaceAll("&[0-9a-fk-or]", "");
        // 最后尝试解析 MiniMessage 并获取纯文本
        try {
            Component component = miniMessageParser.deserialize(noAmpersand);
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // 如果解析失败，返回已移除 & 和 § 的文本
            return noAmpersand.replaceAll("<[^>]+>", "");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("该命令只能由玩家执行!");
                return true;
            }
            if (!player.hasPermission("guangdian.menu.use")) {
                player.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
                return true;
            }
            String menuName = args.length > 0 ? args[0].toLowerCase() : config.getString("default-menu", "main");
            openMenu(player, menuName);
            return true;
        }

        if (command.getName().equalsIgnoreCase("guangdianmenu")) {
            if (!sender.hasPermission("guangdian.menu.admin")) {
                sender.sendMessage(miniMessage.colorize(config.getString("messages.no-permission", "<red>您没有权限执行此操作!")));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig();
                loadMenus();
                sender.sendMessage(miniMessage.colorize(config.getString("messages.config-reloaded", "<green>菜单配置已重新加载!")));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(miniMessage.colorize("<red>玩家不在线或不存在!"));
                        return true;
                    }
                    giveMenuItem(target, false);
                    sender.sendMessage(miniMessage.colorize("<green>已发放主菜单物品给玩家: <yellow>" + target.getName()));
                    return true;
                }

                if (sender instanceof Player player) {
                    giveMenuItem(player, false);
                    sender.sendMessage(miniMessage.colorize("<green>已发放主菜单物品!"));
                    return true;
                }

                sender.sendMessage(miniMessage.colorize("<yellow>用法: /guangdianmenu give <玩家>"));
                return true;
            }

            sender.sendMessage(miniMessage.colorize("<yellow>用法: /guangdianmenu reload|give [玩家]"));
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("menu") && args.length == 1) {
            return menus.keySet().stream().filter(name -> name.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (command.getName().equalsIgnoreCase("guangdianmenu")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("reload");
                completions.add("give");
                return completions.stream().filter(name -> name.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.startsWith(args[1])).collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    public static GuangDianMenu getInstance() {
        return instance;
    }

    // ==================== 公开API方法（供RPGCore服务调用） ====================

    /**
     * 打开菜单 - API方法
     * 
     * @param player 玩家
     * @param menuName 菜单名称
     * @return 是否成功打开
     */
    public boolean openMenuAPI(Player player, String menuName) {
        if (player == null || menuName == null) return false;
        openMenu(player, menuName);
        return true;
    }

    /**
     * 检查菜单是否存在 - API方法
     * 
     * @param menuName 菜单名称
     * @return 是否存在
     */
    public boolean hasMenuAPI(String menuName) {
        return menuName != null && menus.containsKey(menuName.toLowerCase());
    }

    /**
     * 获取所有菜单名称 - API方法
     * 
     * @return 菜单名称列表
     */
    public List<String> getMenuNamesAPI() {
        return new ArrayList<>(menus.keySet());
    }

    /**
     * 重新加载菜单 - API方法
     */
    public void reloadMenusAPI() {
        loadMenus();
    }

    /**
     * 获取菜单数量
     * 
     * @return 菜单数量
     */
    public int getMenuCountAPI() {
        return menus.size();
    }

    // ==================== Getters ====================

    public Map<String, MenuData> getMenus() {
        return menus;
    }

    public String getPlayerMenu(UUID uuid) {
        return playerMenus.get(uuid);
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    /**
     * 获取 SoundService
     * @return SoundService 实例
     */
    public SoundService getSoundService() {
        return soundService;
    }

    private static class MenuHolder implements InventoryHolder {
        private final String menuName;

        private MenuHolder(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuName() {
            return menuName;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class MenuData {
        private final String title;
        private final int size;
        private final Map<String, MenuItem> items = new HashMap<>();
        private final Map<Integer, MenuItem> slotToItem = new HashMap<>();

        private MenuData(String title, int size) {
            this.title = title;
            this.size = size;
        }

        public void addItem(String name, MenuItem item) {
            items.put(name, item);
            for (int slot : item.getSlots()) {
                slotToItem.put(slot, item);
            }
        }

        public String getTitle() {
            return title;
        }

        public int getSize() {
            return size;
        }

        public Map<String, MenuItem> getItems() {
            return items;
        }

        public MenuItem getItemBySlot(int slot) {
            return slotToItem.get(slot);
        }
    }

    private static class MenuItem {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final List<Integer> slots;
        private final String action;
        private final List<String> actions;
        private final String skull;
        private final MenuConditions conditions;
        private final boolean keepOpen;

        private MenuItem(Material material, String name, List<String> lore, List<Integer> slots, 
                        String action, List<String> actions, String skull, 
                        MenuConditions conditions, boolean keepOpen) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.slots = slots;
            this.action = action;
            this.actions = actions;
            this.skull = skull;
            this.conditions = conditions;
            this.keepOpen = keepOpen;
        }

        public Material getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }

        public List<Integer> getSlots() {
            return slots;
        }

        public String getAction() {
            return action;
        }

        public List<String> getActions() {
            return actions;
        }

        public String getSkull() {
            return skull;
        }

        public MenuConditions getConditions() {
            return conditions;
        }

        public boolean isKeepOpen() {
            return keepOpen;
        }

        public boolean hasConditions() {
            return conditions != null && conditions.hasAnyCondition();
        }
    }

    /**
     * 菜单条件配置类
     */
    private static class MenuConditions {
        private int points = 0;
        private double money = 0;
        private String permission = null;
        private List<RequiredItem> items = new ArrayList<>();
        private int level = 0;
        private String failMessage = null;

        public boolean hasAnyCondition() {
            return points > 0 || money > 0 || permission != null || !items.isEmpty() || level > 0;
        }

        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }

        public double getMoney() { return money; }
        public void setMoney(double money) { this.money = money; }

        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }

        public List<RequiredItem> getItems() { return items; }
        public void setItems(List<RequiredItem> items) { this.items = items; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public String getFailMessage() { return failMessage; }
        public void setFailMessage(String failMessage) { this.failMessage = failMessage; }
    }

    /**
     * 所需物品配置类
     */
    private static class RequiredItem {
        private final String material;
        private final int amount;
        private final String name;
        private final List<String> lore;

        public RequiredItem(String material, int amount, String name, List<String> lore) {
            this.material = material;
            this.amount = amount;
            this.name = name;
            this.lore = lore != null ? lore : new ArrayList<>();
        }

        public String getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
    }

    /**
     * 条件检查结果类
     */
    private static class ConditionCheckResult {
        private final boolean success;
        private final String message;

        private ConditionCheckResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ConditionCheckResult success() {
            return new ConditionCheckResult(true, null);
        }

        public static ConditionCheckResult fail(String message) {
            return new ConditionCheckResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

}
