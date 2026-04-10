package cn.guangdian.menu;

import cn.guangdian.menu.adapter.MenuServiceAdapter;
import cn.guangdian.menu.placeholder.MenuPlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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
import java.util.stream.Collectors;
import java.util.logging.Level;

public class GuangDianMenu extends AbstractRPGPlugin implements Listener, CommandExecutor, TabExecutor {

    private static GuangDianMenu instance;
    private FileConfiguration config;
    final Map<String, MenuData> menus = new HashMap<>();
    private final Set<String> claimedStarterKit = new HashSet<>();
    private org.bukkit.scoreboard.Objective starterKitObjective;
    private final Map<UUID, String> playerMenus = new HashMap<>();

    private ExternalServiceIntegration externalServices;
    private SyncScheduler scheduler;
    private NamespacedKey menuItemKey;
    private MenuServiceAdapter serviceAdapter;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        menuItemKey = new NamespacedKey(this, "menu_item");

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

    @Override
    protected void onPluginDisable() {
        menus.clear();
        playerMenus.clear();
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
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
        String title = section.getString("title", "&8菜单");
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

        return new MenuItem(
                material,
                section.getString("name", "&f物品"),
                section.getStringList("lore"),
                slots,
                section.getString("action", ""),
                section.getStringList("actions"),
                section.getString("skull", "")
        );
    }

    public void openMenu(Player player, String menuName) {
        MenuData menuData = menus.get(menuName.toLowerCase());
        if (menuData == null) {
            player.sendMessage(color(config.getString("messages.menu-not-found", "&c菜单不存在!")));
            return;
        }

        // Paper 1.21.4: 使用 Component 作为标题
        String titleText = color(processPlaceholders(player, menuData.getTitle()));
        net.kyori.adventure.text.Component title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(titleText);
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

        meta.setDisplayName(color(processPlaceholders(player, item.getName())));
        meta.setLore(item.getLore().stream().map(line -> color(processPlaceholders(player, line))).collect(Collectors.toList()));
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
        String name = section != null ? section.getString("name", "&6&l主菜单") : "&6&l主菜单";
        List<String> lore = section != null ? section.getStringList("lore") : List.of();

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).collect(Collectors.toList()));
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

        String loreTrigger = config.getString("right-click.lore-trigger", "&e右键打开菜单");
        if (loreTrigger != null && !loreTrigger.isEmpty()) {
            List<String> lore = meta.getLore();
            boolean hasTrigger = false;
            if (lore != null) {
                for (String loreLine : lore) {
                    if (ChatColor.stripColor(loreLine).contains(ChatColor.stripColor(loreTrigger))) {
                        hasTrigger = true;
                        break;
                    }
                }
            }
            if (!hasTrigger) {
                return null;
            }
        }

        String plainName = ChatColor.stripColor(displayName);
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
        String prefix = "&7[玩家]";
        String suffix = "";
        String primaryGroup = "default";
        
        if (externalServices != null) {
            prefix = externalServices.getPlayerPrefix(player);
            suffix = externalServices.getPlayerSuffix(player);
            primaryGroup = externalServices.getPlayerPrimaryGroup(player);
            if (prefix == null || prefix.isEmpty()) prefix = "&7[玩家]";
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
                    .replace("%vault_eco_balance_formatted%", "0金币");
        }
        
        double balance = externalServices.getBalance(player);
        return text
                .replace("%vault_eco_balance%", String.valueOf((long) balance))
                .replace("%vault_eco_balance_fixed%", String.format(java.util.Locale.US, "%.2f", balance))
                .replace("%vault_eco_balance_formatted%", String.valueOf((long) balance) + "金币");
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

        MenuData menuData = menus.get(holder.getMenuName());
        if (menuData == null) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        MenuItem item = menuData.getItemBySlot(slot);
        if (item != null) {
            getLogger().info("[DEBUG] 点击了物品: " + item.getName() + ", action: " + item.getAction() + ", actions: " + item.getActions());
             if (item.getActions() != null && !item.getActions().isEmpty()) {
                 getLogger().info("[DEBUG] 执行 actions 列表");
                 // 检查是否是新手礼包命令
                 boolean isStarterKit = false;
                 for (String action : item.getActions()) {
                     if (action != null && action.contains("阿斯特瑞亚")) {
                         isStarterKit = true;
                         break;
                     }
                 }
                 if (isStarterKit) {
                     if (hasClaimedStarterKit(player)) {
                         player.sendMessage("§c你已领取过新手礼包！");
                         return;
                     }
                     markStarterKitClaimed(player);
                 }
                 executeActionFromList(player, item.getActions());
             } else if (item.getAction() != null && !item.getAction().isEmpty()) {
                 getLogger().info("[DEBUG] 执行单个 action");
                 // 检查是否是新手礼包命令
                 boolean isStarterKit = item.getAction().contains("阿斯特瑞亚");
                 if (isStarterKit) {
                     if (hasClaimedStarterKit(player)) {
                         player.sendMessage("§c你已领取过新手礼包！");
                         return;
                     }
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
            player.sendMessage(color(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
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
            player.sendMessage(color(processPlaceholders(player, action.substring(8))));
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

    private void executeMultipleCommands(Player player, String commands, boolean isConsole) {
        getLogger().info("[DEBUG] executeMultipleCommands 收到: " + commands + ", isConsole=" + isConsole);
        String[] cmds = commands.split("&&");
        for (String cmd : cmds) {
            String trimmedCmd = cmd.trim();
            if (!trimmedCmd.isEmpty()) {
                // 如果命令以 mm 开头（MythicMobs），通过控制台执行但不添加前缀
                if (trimmedCmd.toLowerCase().startsWith("mm ")) {
                    getLogger().info("[DEBUG] MythicMobs 命令通过控制台执行: " + trimmedCmd);
                    final String mmCmd = trimmedCmd;
                    scheduler.runSyncLater(() -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), mmCmd);
                    }, 0L);
                } else {
                    // 其他命令正常处理
                    if (isConsole && !trimmedCmd.toLowerCase().startsWith("console:")) {
                        trimmedCmd = "console:" + trimmedCmd;
                    }
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

    private void playClickSound(Player player) {
        try {
            Sound sound = Sound.valueOf(config.getString("messages.click-sound", "BLOCK_NOTE_BLOCK_PLING"));
            player.playSound(player.getLocation(), sound, (float) config.getDouble("messages.click-volume", 1.0), (float) config.getDouble("messages.click-pitch", 1.0));
        } catch (Exception e) {
            // 音效名称无效或播放失败，忽略
            getLogger().fine("播放点击音效失败: " + e.getMessage());
        }
    }

    private String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("该命令只能由玩家执行!");
                return true;
            }
            if (!player.hasPermission("guangdian.menu.use")) {
                player.sendMessage(color(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
                return true;
            }
            String menuName = args.length > 0 ? args[0].toLowerCase() : config.getString("default-menu", "main");
            openMenu(player, menuName);
            return true;
        }

        if (command.getName().equalsIgnoreCase("guangdianmenu")) {
            if (!sender.hasPermission("guangdian.menu.admin")) {
                sender.sendMessage(color(config.getString("messages.no-permission", "&c您没有权限执行此操作!")));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig();
                loadMenus();
                sender.sendMessage(color(config.getString("messages.config-reloaded", "&a菜单配置已重新加载!")));
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(color("&c玩家不在线或不存在!"));
                        return true;
                    }
                    giveMenuItem(target, false);
                    sender.sendMessage(color("&a已发放主菜单物品给玩家: &e" + target.getName()));
                    return true;
                }

                if (sender instanceof Player player) {
                    giveMenuItem(player, false);
                    sender.sendMessage(color("&a已发放主菜单物品!"));
                    return true;
                }

                sender.sendMessage(color("&e用法: /guangdianmenu give <玩家>"));
                return true;
            }

            sender.sendMessage(color("&e用法: /guangdianmenu reload|give [玩家]"));
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

        private MenuItem(Material material, String name, List<String> lore, List<Integer> slots, String action, List<String> actions, String skull) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.slots = slots;
            this.action = action;
            this.actions = actions;
            this.skull = skull;
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
    }

}
