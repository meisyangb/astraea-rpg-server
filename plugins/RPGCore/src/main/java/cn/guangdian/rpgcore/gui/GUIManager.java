package cn.guangdian.rpgcore.gui;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.gui.model.MenuData;
import cn.guangdian.rpgcore.gui.model.MenuHolder;
import cn.guangdian.rpgcore.gui.model.MenuItem;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public final class GUIManager {

    private static GUIManager instance;

    private GUIListener listener;
    private boolean initialized = false;
    private Logger logger;
    private Plugin plugin;
    private MiniMessageService miniMessageService;
    private final Map<String, MenuData> menus;
    private final Map<UUID, String> playerOpenMenus;
    private BiFunction<String, Player, String> defaultPlaceholderProcessor;

    private GUIManager() {
        this.menus = new ConcurrentHashMap<>();
        this.playerOpenMenus = new ConcurrentHashMap<>();
    }

    /**
     * 获取 GUIManager 单例实例
     *
     * <p>使用双重检查锁定（DCL）实现线程安全的延迟初始化。</p>
     *
     * @return GUIManager 实例
     */
    public static GUIManager getInstance() {
        GUIManager result = instance;
        if (result == null) {
            synchronized (GUIManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new GUIManager();
                }
            }
        }
        return result;
    }

    public void initialize(@NotNull RPGCore plugin) {
        if (initialized) {
            plugin.getLogger().warning("[GUIManager] 已经初始化过了!");
            return;
        }

        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.miniMessageService = plugin.getMiniMessageService();

        listener = new GUIListener();
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(listener, plugin);

        defaultPlaceholderProcessor = (text, player) -> processDefaultPlaceholders(text, player);

        initialized = true;
        logger.info("[GUIManager] 已初始化");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        // 关闭所有玩家打开的 GUI
        for (Player player : Bukkit.getOnlinePlayers()) {
            GUI gui = listener.getPlayerGUI(player);
            if (gui != null) {
                player.closeInventory();
            }
        }

        // 清理所有数据
        menus.clear();
        playerOpenMenus.clear();
        listener = null;
        initialized = false;
        
        // 清理单例引用，防止类加载器泄漏
        instance = null;
        
        logger.info("[GUIManager] 已关闭");
    }

    public @NotNull Listener getListener() {
        if (!initialized) {
            throw new IllegalStateException("GUIManager 未初始化!");
        }
        return listener;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void registerMenu(@NotNull MenuData menu) {
        menus.put(menu.getId().toLowerCase(), menu);
        logger.info("[GUIManager] 注册菜单: " + menu.getId());
    }

    public void unregisterMenu(@NotNull String menuId) {
        menus.remove(menuId.toLowerCase());
        logger.info("[GUIManager] 注销菜单: " + menuId);
    }

    @Nullable
    public MenuData getMenu(@NotNull String menuId) {
        return menus.get(menuId.toLowerCase());
    }

    public boolean hasMenu(@NotNull String menuId) {
        return menus.containsKey(menuId.toLowerCase());
    }

    @NotNull
    public Collection<MenuData> getAllMenus() {
        return menus.values();
    }

    @NotNull
    public Set<String> getMenuNames() {
        return menus.keySet();
    }

    public void loadMenusFromFolder(@NotNull Plugin plugin, @NotNull String folderName) {
        File folder = new File(plugin.getDataFolder(), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        loadMenusFromFolder(folder);
        logger.info("[GUIManager] 从 " + folder.getPath() + " 加载了 " + menus.size() + " 个菜单");
    }

    public void loadMenusFromFolder(@NotNull File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                loadMenusFromFolder(file);
            } else if (file.getName().endsWith(".yml")) {
                loadMenuFromFile(file);
            }
        }
    }

    public void loadMenuFromFile(@NotNull File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String fileName = file.getName().replace(".yml", "");

            if (config.contains("title") && config.contains("size")) {
                String menuName = config.getString("menu-name", fileName);
                MenuData menu = loadMenuData(menuName, config);
                if (menu != null) {
                    registerMenu(menu);
                }
            } else if (config.contains("menus")) {
                ConfigurationSection menusSection = config.getConfigurationSection("menus");
                if (menusSection != null) {
                    for (String menuName : menusSection.getKeys(false)) {
                        ConfigurationSection menuSection = menusSection.getConfigurationSection(menuName);
                        if (menuSection != null) {
                            MenuData menu = loadMenuData(menuName, menuSection);
                            if (menu != null) {
                                registerMenu(menu);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("[GUIManager] 加载菜单文件失败: " + file.getPath() + " - " + e.getMessage());
        }
    }

    @Nullable
    public MenuData loadMenuData(@NotNull String menuId, @NotNull ConfigurationSection section) {
        String title = section.getString("title", "<dark_gray>菜单");
        int size = section.getInt("size", 27);

        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 27;
        }

        MenuData menu = MenuData.builder(menuId, title, size)
                .placeholderProcessor(defaultPlaceholderProcessor)
                .build();

        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                if (itemSection != null) {
                    MenuItem item = loadMenuItem(itemSection);
                    if (item != null) {
                        menu.addItem(itemId, item);
                    }
                }
            }
        }

        return menu;
    }

    @Nullable
    public MenuItem loadMenuItem(@NotNull ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        String name = section.getString("name", "<white>物品");
        List<String> lore = section.getStringList("lore");
        String action = section.getString("action", "");
        List<String> actions = section.getStringList("actions");
        String skullOwner = section.getString("skull", "");
        boolean glowing = section.getBoolean("glowing", false);
        String permission = section.getString("permission", "");

        List<Integer> slots = new ArrayList<>();
        Object slotObj = section.get("slot");
        if (slotObj instanceof Number) {
            slots.add(((Number) slotObj).intValue());
        } else if (section.isList("slots")) {
            for (Object value : section.getList("slots")) {
                if (value instanceof Number) {
                    slots.add(((Number) value).intValue());
                }
            }
        } else if (section.isList("slot")) {
            for (Object value : section.getList("slot")) {
                if (value instanceof Number) {
                    slots.add(((Number) value).intValue());
                }
            }
        }

        MenuItem.Builder builder = MenuItem.builder(material)
                .name(name)
                .lore(lore)
                .slots(slots)
                .glowing(glowing)
                .permission(permission.isEmpty() ? null : permission)
                .placeholderProcessor(defaultPlaceholderProcessor);

        if (!skullOwner.isEmpty()) {
            builder.skullOwner(skullOwner);
        }

        if (actions != null && !actions.isEmpty()) {
            builder.actions(actions);
        } else if (!action.isEmpty()) {
            builder.action(action);
        }

        return builder.build();
    }

    public void openMenu(@NotNull Player player, @NotNull String menuId) {
        MenuData menuData = menus.get(menuId.toLowerCase());
        if (menuData == null) {
            player.sendMessage(miniMessageService.red("菜单不存在: " + menuId));
            return;
        }

        String processedTitle = processDefaultPlaceholders(menuData.getTitle(), player);
        Component title = miniMessageService.colorize(processedTitle);

        MenuHolder holder = new MenuHolder(menuData.getId());
        Inventory inventory = Bukkit.createInventory(holder, menuData.getSize(), title);

        for (Map.Entry<Integer, MenuItem> entry : menuData.getItemsBySlot().entrySet()) {
            int slot = entry.getKey();
            MenuItem menuItem = entry.getValue();
            if (slot >= 0 && slot < inventory.getSize()) {
                ItemStack itemStack = menuItem.build(player);
                inventory.setItem(slot, itemStack);
            }
        }

        listener.registerPlayerGUI(player, new SimpleGUI(processedTitle, menuData.getSize(), inventory, menuData, defaultPlaceholderProcessor));
        player.openInventory(inventory);
        playerOpenMenus.put(player.getUniqueId(), menuId.toLowerCase());
    }

    public void closeMenu(@NotNull Player player) {
        playerOpenMenus.remove(player.getUniqueId());
        player.closeInventory();
    }

    @Nullable
    public String getPlayerOpenMenu(@NotNull Player player) {
        return playerOpenMenus.get(player.getUniqueId());
    }

    public void reloadMenus(@NotNull Plugin plugin, @NotNull String folderName) {
        menus.clear();
        loadMenusFromFolder(plugin, folderName);
    }

    private String processDefaultPlaceholders(String text, Player player) {
        if (text == null) return "";
        text = text.replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%player_level%", String.valueOf(player.getLevel()))
                .replace("%player_health%", String.valueOf((int) player.getHealth()))
                .replace("%player_max_health%", String.valueOf((int) player.getMaxHealth()))
                .replace("%player_food%", String.valueOf(player.getFoodLevel()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
            if (externalServices != null) {
                text = externalServices.parsePlaceholders(player, text);
            }
        }

        return text;
    }

    public void setDefaultPlaceholderProcessor(@Nullable BiFunction<String, Player, String> processor) {
        this.defaultPlaceholderProcessor = processor;
    }

    public int getMenuCount() {
        return menus.size();
    }

    private static class SimpleGUI extends GUI {
        private final MenuData menuData;
        private final BiFunction<String, Player, String> placeholderProcessor;
        private final Inventory externalInventory;

        SimpleGUI(String title, int size, Inventory externalInventory, MenuData menuData, BiFunction<String, Player, String> placeholderProcessor) {
            super(title, size, externalInventory);
            this.externalInventory = externalInventory;
            this.menuData = menuData;
            this.placeholderProcessor = placeholderProcessor;
        }

        @Override
        public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= getSize()) {
                return;
            }

            MenuItem menuItem = menuData.getItemBySlot(slot);
            if (menuItem != null && menuItem.hasPermission((Player) event.getWhoClicked())) {
                event.setCancelled(true);

                Player player = (Player) event.getWhoClicked();
                List<String> actions = menuItem.getActions();
                if (actions != null && !actions.isEmpty()) {
                    cn.guangdian.rpgcore.gui.action.ActionExecutor executor =
                        new cn.guangdian.rpgcore.gui.action.ActionExecutor(player, placeholderProcessor);
                    executor.executeAll(actions);
                }
            }
        }
    }
}